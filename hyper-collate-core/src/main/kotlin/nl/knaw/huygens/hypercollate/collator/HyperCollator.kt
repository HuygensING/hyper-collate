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
import java.util.function.BiFunction
import java.util.stream.Collectors.toList

class HyperCollator {

    fun collate(vararg graphs: VariantWitnessGraph): CollationGraph {
        val sigils: MutableList<String> = mutableListOf()
        val witnesses: MutableList<VariantWitnessGraph> = mutableListOf()
        val rankings: MutableList<VariantWitnessGraphRanking> = mutableListOf()
        Arrays.stream(graphs)
                .sorted(Comparator.comparing { obj: VariantWitnessGraph -> obj.sigil })
                .forEach { graph: VariantWitnessGraph ->
                    sigils.add(graph.sigil)
                    witnesses.add(graph)
                    rankings.add(VariantWitnessGraphRanking.of(graph))
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
            visualize(collationGraph)
        }
        return collationGraph
    }

    private fun visualize(collationGraph: CollationGraph) {
        val dot = CollationGraphVisualizer.toDot(collationGraph, true, false)
        LOG.debug("dot={}", dot)
    }

    fun initialize(
            collationGraph: CollationGraph,
            collatedTokenVertexMap: MutableMap<TokenVertex, TextNode>,
            markupNodeIndex: MutableMap<Markup, MarkupNode>,
            witnessGraph: VariantWitnessGraph) {
        val sigil = witnessGraph.sigil
        collationGraph.sigils.add(sigil)
        addMarkupNodes(collationGraph, markupNodeIndex, witnessGraph)
        collatedTokenVertexMap[witnessGraph.startTokenVertex] = collationGraph.textStartNode
        witnessGraph.vertices()
                .filterIsInstance<SimpleTokenVertex>()
                .forEach { tokenVertex: TokenVertex ->
                    addCollationNode(
                            collationGraph,
                            collatedTokenVertexMap,
                            tokenVertex,
                            witnessGraph,
                            markupNodeIndex)
                }
        collatedTokenVertexMap[witnessGraph.endTokenVertex] = collationGraph.textEndNode
        addEdges(collationGraph, collatedTokenVertexMap)
    }

    private fun addMarkupNodes(
            collationGraph: CollationGraph,
            markupNodeIndex: MutableMap<Markup, MarkupNode>,
            witnessGraph: VariantWitnessGraph) {
        witnessGraph
                .markupStream
                .forEach { markup: Markup ->
                    val markupNode = collationGraph.addMarkupNode(witnessGraph.sigil, markup)
                    markupNodeIndex[markup] = markupNode
                }
    }

    private fun getCollatedMatches(
            collatedTokenVertexMap: Map<TokenVertex, TextNode>,
            matches: List<Match>,
            sigil: String
    ): List<CollatedMatch> =
            matches.map { toCollatedMatch(it, sigil, collatedTokenVertexMap) }

    private fun toCollatedMatch(
            match: Match,
            sigil: String,
            collatedTokenVertexMap: Map<TokenVertex, TextNode>
    ): CollatedMatch {
        val vertex = match.tokenVertexList.firstOrNull { tv: TokenVertex -> tv.sigil != sigil } ?: error("No vertex!")
        val tokenVertexForWitness = match.getTokenVertexForWitness(sigil)
                ?: error("no TokenVertex found for sigil $sigil")
        val node = collatedTokenVertexMap[vertex] ?: error("no node found for vertex")
        return CollatedMatch(node, tokenVertexForWitness)
                .setVertexRank(match.getRankForWitness(sigil)!!)
    }

    private fun collate(
            collationGraph: CollationGraph,
            witnessGraph: VariantWitnessGraph,
            sortedMatchesForWitness: List<Match>,
            markupNodeIndex: MutableMap<Markup, MarkupNode>,
            collatedTokenVertexMap: MutableMap<TokenVertex, TextNode>) {
        val baseRanking = CollationGraphRanking.of(collationGraph)
        val filteredSortedMatchesForWitness = sortedMatchesForWitness
                .filter { m: Match -> collationGraph.sigils.any { m.hasWitness(it) } }
        val witnessSigil = witnessGraph.sigil
        collationGraph.sigils.add(witnessSigil)
        addMarkupNodes(collationGraph, markupNodeIndex, witnessGraph)
        val matchList = getCollatedMatches(collatedTokenVertexMap, filteredSortedMatchesForWitness, witnessSigil)
                .map { m: CollatedMatch -> adjustRankForCollatedNode(m, baseRanking) }
                .distinct()
        LOG.debug("matchList={}, size={}", matchList, matchList.size)
        val optimalMatchList = getOptimalMatchList(matchList)
        LOG.debug("optimalMatchList={}, size={}", optimalMatchList, optimalMatchList.size)
        val witnessIterator: Iterator<TokenVertex> = VariantWitnessGraphTraversal.of(witnessGraph).iterator()
        val first = witnessIterator.next()
        val rootNode: TextNode = collationGraph.textStartNode
        collatedTokenVertexMap[first] = rootNode
        logCollated(collatedTokenVertexMap)
        while (optimalMatchList.isNotEmpty()) {
            val match: CollatedMatch = optimalMatchList.removeAt(0)
            LOG.debug("match={}", match)
            val tokenVertexForWitnessGraph = match.witnessVertex
            advanceWitness(
                    collationGraph,
                    collatedTokenVertexMap,
                    witnessIterator,
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
            addMarkupHyperEdges(
                    collationGraph, witnessGraph, markupNodeIndex, tokenVertexForWitnessGraph, matchingNode)
        }
        collatedTokenVertexMap[witnessGraph.endTokenVertex] = collationGraph.textEndNode
        logCollated(collatedTokenVertexMap)
        addEdges(collationGraph, collatedTokenVertexMap)
        logCollated(collatedTokenVertexMap)
    }

    private fun addMarkupHyperEdges(
            collationGraph: CollationGraph,
            witnessGraph: VariantWitnessGraph,
            markupNodeIndex: Map<Markup, MarkupNode>,
            tokenVertexForWitnessGraph: TokenVertex,
            matchingNode: TextNode) {
        witnessGraph
                .getMarkupListForTokenVertex(tokenVertexForWitnessGraph)
                .forEach { markup: Markup -> collationGraph.linkMarkupToText(markupNodeIndex[markup], matchingNode) }
    }

    private fun getOptimalMatchList(matchList: List<CollatedMatch>): MutableList<CollatedMatch> =
            OptimalCollatedMatchListAlgorithm().getOptimalCollatedMatchList(matchList)

    private fun adjustRankForCollatedNode(
            m: CollatedMatch, baseRanking: CollationGraphRanking): CollatedMatch {
        val node = m.collatedNode
        val rank = baseRanking.apply(node)
        m.nodeRank = rank
        return m
    }

    private fun logCollated(collatedTokenVertexMap: Map<TokenVertex, TextNode>) {
        val lines: MutableList<String> = ArrayList()
        collatedTokenVertexMap.forEach { (k: TokenVertex, v: TextNode) -> lines.add(k.sigil + ":" + k.token + " -> " + v) }
        LOG.debug("collated={}", lines.sorted().joinToString("\n"))
    }

    private fun advanceWitness(
            collationGraph: CollationGraph,
            collatedTokenVertexMap: MutableMap<TokenVertex, TextNode>,
            tokenVertexIterator: Iterator<TokenVertex>,
            tokenVertexForWitness: TokenVertex,
            witnessGraph: VariantWitnessGraph,
            markupNodeIndex: Map<Markup, MarkupNode>) {
        if (tokenVertexIterator.hasNext()) {
            var nextWitnessVertex = tokenVertexIterator.next()
            while (tokenVertexIterator.hasNext() && nextWitnessVertex != tokenVertexForWitness) {
                addCollationNode(
                        collationGraph,
                        collatedTokenVertexMap,
                        nextWitnessVertex,
                        witnessGraph,
                        markupNodeIndex)
                nextWitnessVertex = tokenVertexIterator.next()
            }
        }
    }

    private fun addCollationNode(
            collationGraph: CollationGraph,
            collatedTokenVertexMap: MutableMap<TokenVertex, TextNode>,
            tokenVertex: TokenVertex,
            witnessGraph: VariantWitnessGraph,
            markupNodeIndex: Map<Markup, MarkupNode>) {
        if (!collatedTokenVertexMap.containsKey(tokenVertex)) {
            val collationNode = collationGraph.addTextNodeWithTokens(tokenVertex.token)
            collationNode.addBranchPath(tokenVertex.sigil, tokenVertex.branchPath)
            collatedTokenVertexMap[tokenVertex] = collationNode
            addMarkupHyperEdges(
                    collationGraph, witnessGraph, markupNodeIndex, tokenVertex, collationNode)
        }
    }

    fun sortAndFilterMatchesByWitness(matches: Set<Match>, sigils: List<String>): Map<String, List<Match>> {
        val map: MutableMap<String, List<Match>> = HashMap()
        sigils.forEach { s: String ->
            val sortedMatchesForWitness = filterAndSortMatchesForWitness(matches, s)
            map[s] = sortedMatchesForWitness
        }
        return map
    }

    private fun filterAndSortMatchesForWitness(matches: Set<Match>, sigil: String): List<Match> {
        val comparator = Comparator { match1: Match, match2: Match ->
            var rank1 = match1.getRankForWitness(sigil) ?: error("invalid sigil $sigil")
            var rank2 = match2.getRankForWitness(sigil) ?: error("invalid sigil $sigil")
            if (rank1 == rank2) {
                rank1 = match1.getLowestRankForWitnessesOtherThan(sigil)
                rank2 = match2.getLowestRankForWitnessesOtherThan(sigil)
            }
            rank1.compareTo(rank2)
        }
        return matches.filter { m: Match -> m.hasWitness(sigil) }
                .sortedWith(comparator)
    }

    fun getPotentialMatches(
            witnesses: List<VariantWitnessGraph>, rankings: List<VariantWitnessGraphRanking>): Set<Match> {
        val allPotentialMatches: MutableSet<Match> = HashSet()
        val vertexToMatch: MutableMap<TokenVertex, Match> = HashMap()
        for (t in permute(witnesses.size)) {
            val witness1 = witnesses[t.left!!]
            val ranking1 = rankings[t.left!!]
            val witness2 = witnesses[t.right!!]
            val ranking2 = rankings[t.right!!]
            match(witness1, witness2, ranking1, ranking2, allPotentialMatches, vertexToMatch)
            val endMatch = getEndMatch(witness1, ranking1, witness2, ranking2)
            allPotentialMatches.add(endMatch)
        }
        return allPotentialMatches
    }

    private fun getEndMatch(
            witness1: VariantWitnessGraph,
            ranking1: VariantWitnessGraphRanking,
            witness2: VariantWitnessGraph,
            ranking2: VariantWitnessGraphRanking): Match {
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
                list.add(Tuple(left, right))
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
            vertexToMatch: MutableMap<TokenVertex, Match>) {
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
                                if (matcher.apply(tv1, tv2)) {
                                    val match = Match(tv1, tv2)
                                            .setRank(sigil1, ranking1.apply(tv1))
                                            .setRank(sigil2, ranking2.apply(tv2))
                                    allPotentialMatches.add(match)
                                    vertexToMatch[tv1] = match
                                    vertexToMatch[tv2] = match
                                }
                            }
                }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(HyperCollator::class.java)

        private val matcher = BiFunction { stv1: SimpleTokenVertex, stv2: SimpleTokenVertex ->
            if (stv1.normalizedContent.isEmpty() && stv2.normalizedContent.isEmpty()) {
                if (stv1.content.isEmpty() && stv2.content.isEmpty()) {
                    // both are milestones, so compare their tags.
                    val parentTag1 = stv1.parentXPath.replace("/.*/", "")
                    val parentTag2 = stv2.parentXPath.replace("/.*/", "")
                    parentTag1 == parentTag2
                } else {
                    stv1.content == stv2.content
                }
            }
            stv1.normalizedContent == stv2.normalizedContent
        }

        private fun addEdges(
                collationGraph: CollationGraph,
                collatedTokenVertexMap: Map<TokenVertex, TextNode>
        ) {
            collatedTokenVertexMap
                    .keys
                    .forEach { tv: TokenVertex ->
                        tv.incomingTokenVertexStream
                                .forEach { itv: TokenVertex ->
                                    val source = collatedTokenVertexMap[itv] ?: error("source is null!")
                                    val target = collatedTokenVertexMap[tv] ?: error("target is null!")
                                    val existingTargetNodes = collationGraph
                                            .getOutgoingTextEdgeStream(source)
                                            .map { edge: TextEdge? -> collationGraph.getTarget(edge) }
                                            .map { it as TextNode }
                                            .collect(toList())
                                    val sigil = tv.sigil
                                    if (!existingTargetNodes.contains(target)) {
                                        val sigils: MutableSet<String> = mutableSetOf(sigil)
                                        collationGraph.addDirectedEdge(source, target, sigils)
                                        // System.out.println("> " + source + " -> " + target);
                                    } else {
                                        val edge = collationGraph
                                                .getOutgoingTextEdgeStream(source)
                                                .filter { e: TextEdge? -> target == collationGraph.getTarget(e) }
                                                .findFirst()
                                                .orElseThrow { RuntimeException("There should be an edge!") }
                                        edge.addSigil(sigil)
                                        // System.err.println("duplicate edge: " + source + " -> " + target);
                                    }
                                }
                    }
        }
    }
}
