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

typealias BranchPath = List<Int>?

class HyperCollator {

    fun collate(vararg graphs: VariantWitnessGraph): CollationGraph {
        val witnesses: MutableList<VariantWitnessGraph> = graphs.sortedBy { it.sigil }.toMutableList()
        val sigils: List<String> = witnesses.map { it.sigil }
        val rankings: List<VariantWitnessGraphRanking> = witnesses.map { VariantWitnessGraphRanking.of(it) }
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
            collationGraph.collate(
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
        log.debug("dot={}", dot)
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
                    this[markup] = collationGraph.addMarkupNode(witnessGraph.sigil, markup)
                }
    }

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

    private fun CollationGraph.collate(
            witnessGraph: VariantWitnessGraph,
            sortedMatchesForWitness: List<Match>,
            markupNodeIndex: MutableMap<Markup, MarkupNode>,
            collatedTokenVertexMap: MutableMap<TokenVertex, TextNode>
    ) {
        val baseRanking = CollationGraphRanking.of(this)
        val filteredSortedMatchesForWitness = sortedMatchesForWitness
                .filter { m: Match -> sigils.any { m.hasWitness(it) } }
        val witnessSigil = witnessGraph.sigil
        sigils += witnessSigil
        markupNodeIndex.addMarkupNodes(this, witnessGraph)
        val matchList = filteredSortedMatchesForWitness
                .map { it.toCollatedMatch(witnessSigil, collatedTokenVertexMap) }
                .map { m: CollatedMatch -> m.adjustRankForCollatedNode(baseRanking) }
                .distinct()
        log.debug("matchList={}, size={}", matchList, matchList.size)
        val optimalMatchList = matchList.optimized(sigils).toMutableList()
        log.debug("optimalMatchList={}, size={}", optimalMatchList, optimalMatchList.size)
        val witnessIterator: Iterator<TokenVertex> = VariantWitnessGraphTraversal.of(witnessGraph).iterator()
        val first = witnessIterator.next()
        val rootNode: TextNode = textStartNode
        collatedTokenVertexMap[first] = rootNode
        while (optimalMatchList.isNotEmpty()) {
            val match: CollatedMatch = optimalMatchList.removeAt(0)
            log.debug("match={}", match)
            val tokenVertexForWitnessGraph = match.witnessVertex
            witnessIterator.advanceWitness(
                    this,
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
            addMarkupHyperEdges(
                    witnessGraph, markupNodeIndex, tokenVertexForWitnessGraph, matchingNode)
        }
        collatedTokenVertexMap[witnessGraph.endTokenVertex] = textEndNode
        addEdges(collatedTokenVertexMap)
    }

    private fun CollationGraph.addMarkupHyperEdges(
            witnessGraph: VariantWitnessGraph,
            markupNodeIndex: Map<Markup, MarkupNode>,
            tokenVertexForWitnessGraph: TokenVertex,
            matchingNode: TextNode
    ) {
        witnessGraph
                .markupListForTokenVertex(tokenVertexForWitnessGraph)
                .forEach { markup: Markup -> linkMarkupToText(markupNodeIndex[markup] ?: error(""), matchingNode) }
    }

    private fun List<CollatedMatch>.optimized(sigils: List<String>): List<CollatedMatch> =
            OptimalCollatedMatchListAlgorithm().getOptimalCollatedMatchList(this, sigils)

    private fun CollatedMatch.adjustRankForCollatedNode(
            baseRanking: CollationGraphRanking
    ): CollatedMatch {
        nodeRank = baseRanking.apply(collatedNode)
        return this
    }

    private fun Map<TokenVertex, TextNode>.logCollated() {
        val lines: List<String> = map { (k: TokenVertex, v: TextNode) -> "${k.sigil}:${k.token} -> $v" }
        log.debug("collated={}", lines.sorted().joinToString("\n"))
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
            val collationNode = addTextNodeWithTokens(tokenVertex.token!!)
            collationNode.addBranchPath(tokenVertex.sigil, tokenVertex.branchPath)
            collatedTokenVertexMap[tokenVertex] = collationNode
            addMarkupHyperEdges(witnessGraph, markupNodeIndex, tokenVertex, collationNode)
        }
    }

    fun sortAndFilterMatchesByWitness(matches: Set<Match>, sigils: List<String>): Map<String, List<Match>> =
            sigils.map { it to matches.sortedAndFilteredForWitness(it) }.toMap()

    private fun Set<Match>.sortedAndFilteredForWitness(sigil: String): List<Match> =
            filter { m: Match -> m.hasWitness(sigil) }
                    .sortedWith(sigil.matchComparator())

    private fun String.matchComparator(): Comparator<Match> =
            Comparator { match1: Match, match2: Match ->
                var rank1 = match1.getRankForWitness(this) ?: error("invalid sigil $this")
                var rank2 = match2.getRankForWitness(this) ?: error("invalid sigil $this")
                if (rank1 == rank2) {
                    rank1 = match1.getLowestRankForWitnessesOtherThan(this)
                    rank2 = match2.getLowestRankForWitnessesOtherThan(this)
                }
                rank1.compareTo(rank2)
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
            allPotentialMatches.addMatchesFor(witness1, witness2, ranking1, ranking2, vertexToMatch)

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

    private fun MutableSet<Match>.addMatchesFor(
            witness1: VariantWitnessGraph,
            witness2: VariantWitnessGraph,
            ranking1: VariantWitnessGraphRanking,
            ranking2: VariantWitnessGraphRanking,
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
                                    this += match
                                    vertexToMatch[tv1] = match
                                    vertexToMatch[tv2] = match
                                }
                            }
                }
    }

    companion object {
        private val log = LoggerFactory.getLogger(HyperCollator::class.java)

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

        private fun CollationGraph.addEdges(collatedTokenVertexMap: Map<TokenVertex, TextNode>) {
            collatedTokenVertexMap
                    .keys
                    .forEach { tv: TokenVertex ->
                        tv.incomingTokenVertexList
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

        fun permute(max: Int): List<Tuple<Int>> {
            val list: MutableList<Tuple<Int>> = ArrayList()
            for (left in 0 until max) {
                for (right in left + 1 until max) {
                    list += Tuple(left, right)
                }
            }
            return list
        }
    }
}
