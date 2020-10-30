package nl.knaw.huygens.hypercollate.collator

/*-
 * #%L
 * hyper-collate-core
 * =======
 * Copyright (C) 2017 - 2020 Huygens ING (KNAW)
 * =======
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import eu.interedition.collatex.dekker.Tuple
import nl.knaw.huygens.hypercollate.model.*
import nl.knaw.huygens.hypercollate.tools.CollationGraphRanking
import nl.knaw.huygens.hypercollate.tools.CollationGraphVisualizer
import org.slf4j.LoggerFactory
import java.util.*
import java.util.stream.Collectors.toSet

class HyperCollator {

    fun collate(vararg graphs: VariantWitnessGraph): CollationGraph {
        val sigils: MutableList<String> = mutableListOf()
        val witnesses: MutableList<VariantWitnessGraph> = mutableListOf()
        val rankings: MutableList<VariantWitnessGraphRanking> = mutableListOf()
        graphs
                .sortedBy { it.sigil }
                .forEach { graph: VariantWitnessGraph ->
                    sigils += graph.sigil
                    witnesses += graph
                    rankings += VariantWitnessGraphRanking.of(graph)
                }
        val matches = getPotentialMatches(witnesses, rankings)
        val collationGraph = CollationGraph()
        val collatedTokenVertexMap: MutableMap<TokenVertex, TextNode> = HashMap()
        val first = witnesses.removeAt(0)
        val matchesSortedByRankPerWitness = sortAndFilterMatchesByWitness(matches, sigils)
        val markupNodeIndex: MutableMap<Markup, MarkupNode> = HashMap()
        initialize(collationGraph, collatedTokenVertexMap, markupNodeIndex, first)
//        visualize(collationGraph)
        for (witnessGraph in witnesses) {
            val sortedMatchesForWitness = matchesSortedByRankPerWitness[witnessGraph.sigil] ?: error("sigil not found")
            collate(
                    collationGraph,
                    witnessGraph,
                    sortedMatchesForWitness,
                    markupNodeIndex,
                    collatedTokenVertexMap)
            collationGraph.visualize()
        }
        return collationGraph
    }

    private fun CollationGraph.visualize() {
        val dot = CollationGraphVisualizer.toDot(this, emphasizeWhitespace = true, hideMarkup = false)
        LOG.debug("dot={}", dot)
    }

    fun initialize(
            collationGraph: CollationGraph,
            collatedTokenVertexMap: MutableMap<TokenVertex, TextNode>,
            markupNodeIndex: MutableMap<Markup, MarkupNode>,
            witnessGraph: VariantWitnessGraph
    ) {
        val sigil = witnessGraph.sigil
        collationGraph.sigils += sigil
        markupNodeIndex.addMarkupNodes(collationGraph, witnessGraph)
        collatedTokenVertexMap[witnessGraph.startTokenVertex] = collationGraph.textStartNode
        witnessGraph.vertices()
                .asSequence()
                .filterIsInstance<SimpleTokenVertex>()
                .forEach { tokenVertex: TokenVertex ->
                    collationGraph.addCollationNode(
                            collatedTokenVertexMap,
                            tokenVertex,
                            witnessGraph,
                            markupNodeIndex)
                }
        collatedTokenVertexMap[witnessGraph.endTokenVertex] = collationGraph.textEndNode
        collationGraph.addEdges(collatedTokenVertexMap)
    }

    private fun MutableMap<Markup, MarkupNode>.addMarkupNodes(
            collationGraph: CollationGraph,
            witnessGraph: VariantWitnessGraph
    ) {
        witnessGraph
                .markupStream
                .forEach { markup: Markup ->
                    val markupNode = collationGraph.addMarkupNode(witnessGraph.sigil, markup)
                    this[markup] = markupNode
                }
    }

    private fun getCollatedMatches(
            collatedTokenVertexMap: Map<TokenVertex, TextNode>,
            matches: List<Match>,
            sigil: String
    ): List<CollatedMatch> =
            matches.map { it.toCollatedMatch(sigil, collatedTokenVertexMap) }

    private fun Match.toCollatedMatch(
            sigil: String,
            collatedTokenVertexMap: Map<TokenVertex, TextNode>
    ): CollatedMatch {
        val vertex = tokenVertexList.firstOrNull { tv: TokenVertex -> tv.sigil != sigil } ?: error("No vertex!")
        val tokenVertexForWitness = getTokenVertexForWitness(sigil)
                ?: error("no TokenVertex found for sigil $sigil")
        val node = collatedTokenVertexMap[vertex] ?: error("no node found for vertex")
        return CollatedMatch(node, tokenVertexForWitness)
                .withVertexRank(getRankForWitness(sigil)!!)
    }

    private fun collate(
            collationGraph: CollationGraph,
            witnessGraph: VariantWitnessGraph,
            sortedMatchesForWitness: List<Match>,
            markupNodeIndex: MutableMap<Markup, MarkupNode>,
            collatedTokenVertexMap: MutableMap<TokenVertex, TextNode>
    ) {
        val baseRanking = CollationGraphRanking.of(collationGraph)
        val filteredSortedMatchesForWitness = sortedMatchesForWitness
                .filter { m: Match -> collationGraph.sigils.any { m.hasWitness(it) } }
        val witnessSigil = witnessGraph.sigil
        collationGraph.sigils += witnessSigil
        markupNodeIndex.addMarkupNodes(collationGraph, witnessGraph)
        val matchList = getCollatedMatches(collatedTokenVertexMap, filteredSortedMatchesForWitness, witnessSigil)
                .map { m: CollatedMatch -> m.adjustRankForCollatedNode(baseRanking) }
                .distinct()
        LOG.debug("matchList={}, size={}", matchList, matchList.size)
        val optimalMatchList = matchList.getOptimalMatchList()
        LOG.debug("optimalMatchList={}, size={}", optimalMatchList, optimalMatchList.size)
        val witnessIterator: Iterator<TokenVertex> = VariantWitnessGraphTraversal.of(witnessGraph).iterator()
        val first = witnessIterator.next()
        val rootNode: TextNode = collationGraph.textStartNode
        collatedTokenVertexMap[first] = rootNode
//        logCollated(collatedTokenVertexMap)
        while (optimalMatchList.isNotEmpty()) {
            val match: CollatedMatch = optimalMatchList.removeAt(0)
            LOG.debug("match={}", match)
            val tokenVertexForWitnessGraph = match.witnessVertex
            witnessIterator.advanceWitness(
                    collationGraph,
                    collatedTokenVertexMap,
                    tokenVertexForWitnessGraph,
                    witnessGraph,
                    markupNodeIndex)
            val matchingNode = match.collatedNode
            val token = tokenVertexForWitnessGraph.token
            if (token != null) {
                matchingNode.addToken(token)
                matchingNode.addBranchPath(
                        tokenVertexForWitnessGraph.sigil, tokenVertexForWitnessGraph.branchPath)
            }
            collatedTokenVertexMap[tokenVertexForWitnessGraph] = matchingNode
            collationGraph.addMarkupHyperEdges(
                    witnessGraph, markupNodeIndex, tokenVertexForWitnessGraph, matchingNode)
        }
        collatedTokenVertexMap[witnessGraph.endTokenVertex] = collationGraph.textEndNode
//        logCollated(collatedTokenVertexMap)
        collationGraph.addEdges(collatedTokenVertexMap)
//        logCollated(collatedTokenVertexMap)
    }

    private fun CollationGraph.addMarkupHyperEdges(
            witnessGraph: VariantWitnessGraph,
            markupNodeIndex: Map<Markup, MarkupNode>,
            tokenVertexForWitnessGraph: TokenVertex,
            matchingNode: TextNode
    ) {
        witnessGraph
                .getMarkupListForTokenVertex(tokenVertexForWitnessGraph)
                .forEach { markup: Markup -> linkMarkupToText(markupNodeIndex[markup], matchingNode) }
    }

    private fun List<CollatedMatch>.getOptimalMatchList(): MutableList<CollatedMatch> =
            OptimalCollatedMatchListAlgorithm().getOptimalCollatedMatchList(this)

    private fun CollatedMatch.adjustRankForCollatedNode(
            baseRanking: CollationGraphRanking
    ): CollatedMatch {
        val node = collatedNode
        val rank = baseRanking.apply(node)
        nodeRank = rank
        return this
    }

    private fun logCollated(collatedTokenVertexMap: Map<TokenVertex, TextNode>) {
        val lines: MutableList<String> = ArrayList()
        collatedTokenVertexMap.forEach { (k: TokenVertex, v: TextNode) -> lines += "${k.sigil}:${k.token} -> $v" }
        LOG.debug("collated={}", lines.sorted().joinToString("\n"))
    }

    private fun Iterator<TokenVertex>.advanceWitness(
            collationGraph: CollationGraph,
            collatedTokenVertexMap: MutableMap<TokenVertex, TextNode>,
            tokenVertexForWitness: TokenVertex,
            witnessGraph: VariantWitnessGraph,
            markupNodeIndex: Map<Markup, MarkupNode>
    ) {
        if (hasNext()) {
            var nextWitnessVertex = next()
            while (hasNext() && nextWitnessVertex != tokenVertexForWitness) {
                collationGraph.addCollationNode(
                        collatedTokenVertexMap,
                        nextWitnessVertex,
                        witnessGraph,
                        markupNodeIndex)
                nextWitnessVertex = next()
            }
        }
    }

    private fun CollationGraph.addCollationNode(
            collatedTokenVertexMap: MutableMap<TokenVertex, TextNode>,
            tokenVertex: TokenVertex,
            witnessGraph: VariantWitnessGraph,
            markupNodeIndex: Map<Markup, MarkupNode>
    ) {
        if (!collatedTokenVertexMap.containsKey(tokenVertex)) {
            val collationNode = addTextNodeWithTokens(tokenVertex.token)
            collationNode.addBranchPath(tokenVertex.sigil, tokenVertex.branchPath)
            collatedTokenVertexMap[tokenVertex] = collationNode
            addMarkupHyperEdges(
                    witnessGraph, markupNodeIndex, tokenVertex, collationNode)
        }
    }

    fun sortAndFilterMatchesByWitness(matches: Set<Match>, sigils: List<String>): Map<String, List<Match>> {
        val map: MutableMap<String, List<Match>> = HashMap()
        sigils.forEach { s: String ->
            map[s] = matches.filterAndSortMatchesForWitness(s)
        }
        return map
    }

    private fun Set<Match>.filterAndSortMatchesForWitness(sigil: String): List<Match> {
        val comparator = Comparator { match1: Match, match2: Match ->
            var rank1 = match1.getRankForWitness(sigil) ?: error("invalid sigil $sigil")
            var rank2 = match2.getRankForWitness(sigil) ?: error("invalid sigil $sigil")
            if (rank1 == rank2) {
                rank1 = match1.getLowestRankForWitnessesOtherThan(sigil)
                rank2 = match2.getLowestRankForWitnessesOtherThan(sigil)
            }
            rank1.compareTo(rank2)
        }
        return filter { m: Match -> m.hasWitness(sigil) }
                .sortedWith(comparator)
    }

    fun getPotentialMatches(
            witnesses: List<VariantWitnessGraph>,
            rankings: List<VariantWitnessGraphRanking>
    ): Set<Match> {
        val allPotentialMatches: MutableSet<Match> = HashSet()
        val vertexToMatch: MutableMap<TokenVertex, Match> = HashMap()
        for (t in permute(witnesses.size)) {
            val witness1 = witnesses[t.left!!]
            val ranking1 = rankings[t.left!!]
            val witness2 = witnesses[t.right!!]
            val ranking2 = rankings[t.right!!]
            match(witness1, witness2, ranking1, ranking2, allPotentialMatches, vertexToMatch)
            val endMatch = getEndMatch(witness1, ranking1, witness2, ranking2)
            allPotentialMatches += endMatch
        }
        return allPotentialMatches
    }

    private fun getEndMatch(
            witness1: VariantWitnessGraph,
            ranking1: VariantWitnessGraphRanking,
            witness2: VariantWitnessGraph,
            ranking2: VariantWitnessGraphRanking
    ): Match {
        val endTokenVertex1 = witness1.endTokenVertex
        val endTokenVertex2 = witness2.endTokenVertex
        val endMatch = Match(endTokenVertex1, endTokenVertex2)
        val sigil1 = witness1.sigil
        val rank1 = ranking1.apply(endTokenVertex1)
        endMatch.setRank(sigil1, rank1)
        val sigil2 = witness2.sigil
        val rank2 = ranking2.apply(endTokenVertex2)
        endMatch.setRank(sigil2, rank2)
        return endMatch
    }

    fun permute(max: Int): List<Tuple<Int>> {
        val list: MutableList<Tuple<Int>> = ArrayList()
        for (left in 0 until max) {
            for (right in left + 1 until max) {
                list += Tuple(left, right)
            }
        }
        return list
    }

    private fun match(
            witness1: VariantWitnessGraph,
            witness2: VariantWitnessGraph,
            ranking1: VariantWitnessGraphRanking,
            ranking2: VariantWitnessGraphRanking,
            allPotentialMatches: MutableSet<Match>,
            vertexToMatch: MutableMap<TokenVertex, Match>
    ) {
        val traversal1 = VariantWitnessGraphTraversal.of(witness1)
        val traversal2 = VariantWitnessGraphTraversal.of(witness2)
        val sigil1 = witness1.sigil
        val sigil2 = witness2.sigil
        traversal1
                .filterIsInstance<SimpleTokenVertex>()
                .forEach { tv1: SimpleTokenVertex ->
                    traversal2
                            .filterIsInstance<SimpleTokenVertex>()
                            .forEach { tv2: SimpleTokenVertex ->
                                if (tv1.matches(tv2)) {
                                    val match = Match(tv1, tv2)
                                            .setRank(sigil1, ranking1.apply(tv1))
                                            .setRank(sigil2, ranking2.apply(tv2))
                                    allPotentialMatches += match
                                    vertexToMatch[tv1] = match
                                    vertexToMatch[tv2] = match
                                }
                            }
                }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(HyperCollator::class.java)

        private fun SimpleTokenVertex.matches(other: SimpleTokenVertex): Boolean {
            if (normalizedContent.isEmpty() && other.normalizedContent.isEmpty()) {
                return if (content.isEmpty() && other.content.isEmpty()) {
                    // both are milestones, so compare their tags.
                    val parentTag1 = parentXPath.replace("/.*/", "")
                    val parentTag2 = other.parentXPath.replace("/.*/", "")
                    parentTag1 == parentTag2
                } else {
                    content == other.content
                }
            }
            return normalizedContent == other.normalizedContent
        }

        private fun CollationGraph.addEdges(
                collatedTokenVertexMap: Map<TokenVertex, TextNode>
        ) {
            collatedTokenVertexMap
                    .keys
                    .forEach { tv: TokenVertex ->
                        tv.incomingTokenVertexStream
                                .forEach { itv: TokenVertex ->
                                    val source = collatedTokenVertexMap[itv] ?: error("source is null!")
                                    val target = collatedTokenVertexMap[tv] ?: error("target is null!")
                                    val existingTargetNodes = getOutgoingTextEdgeStream(source)
                                            .map { edge: TextEdge -> getTarget(edge) }
                                            .map { it as TextNode }
                                            .collect(toSet())
                                    val sigil = tv.sigil
                                    if (target !in existingTargetNodes) {
                                        val sigils: MutableSet<String> = mutableSetOf(sigil)
                                        addDirectedEdge(source, target, sigils)
                                        // System.out.println("> " + source + " -> " + target);
                                    } else {
                                        val edge = getOutgoingTextEdgeStream(source)
                                                .filter { e: TextEdge -> target == getTarget(e) }
                                                .findFirst()
                                                .orElseThrow { RuntimeException("There should be an edge!") }
                                        edge.addSigil(sigil)
                                    }
                                }
                    }
        }
    }
}
