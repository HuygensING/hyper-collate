package nl.knaw.huygens.hypercollate.tools

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

import com.google.common.base.Preconditions
import eu.interedition.collatex.Token
import nl.knaw.huygens.hypercollate.model.*
import java.util.*

object CollationGraphNodeJoiner {
    @JvmStatic
    fun join(originalGraph: CollationGraph): CollationGraph {
        val mergedGraph = CollationGraph(originalGraph.sigla)
        originalGraph
                .markupNodeList
                .forEach { markupNode: MarkupNode -> mergedGraph.addMarkupNode(markupNode.siglum, markupNode.markup) }
        val originalToMerged = mergeNodes(originalGraph, mergedGraph)
        copyIncomingEdges(originalGraph, originalToMerged, mergedGraph)
        copyMarkupHyperEdges(originalGraph, originalToMerged, mergedGraph)
        return mergedGraph
    }

    private fun mergeNodes(
            originalGraph: CollationGraph, mergedGraph: CollationGraph): Map<TextNode, TextNode> {
        val originalToMerged: MutableMap<TextNode, TextNode> = HashMap()
        var mergedNode: TextNode = mergedGraph.textStartNode
        var isRootNode = true
        for (originalNode in originalGraph.traverseTextNodes()) {
            if (isRootNode) {
                isRootNode = false
                originalToMerged[originalNode] = mergedNode
                continue
            }
            if (canMergeNodes(mergedNode, originalNode, originalGraph)) {
                mergeNodeTokens(mergedNode, originalNode)
            } else {
                mergedNode = copyNode(originalNode, mergedGraph)
            }
            originalToMerged[originalNode] = mergedNode
        }
        return originalToMerged
    }

    private fun canMergeNodes(
            mergedNode: TextNode, originalNode: TextNode, originalGraph: CollationGraph): Boolean {
        val incomingEdges: Collection<TextEdge> = originalGraph.getIncomingTextEdgeList(originalNode)
        if (incomingEdges.size != 1) {
            return false
        }
        if (mergedNode.sigla != originalNode.sigla) {
            return false
        }
        val incomingEdge = incomingEdges.iterator().next()
        val prevNode = originalGraph.getSource(incomingEdge) as TextNode
        val siglaMatch = prevNode.sigla == mergedNode.sigla
        if (siglaMatch) {
            var parentXPathsMatch = true
            for (s in mergedNode.sigla) {
                val mWitnessToken = mergedNode.tokenForSiglum(s)
                val nWitnessToken = originalNode.tokenForSiglum(s)
                if (nWitnessToken == null) {
                    // it's an endtoken, so not mergeable
                    parentXPathsMatch = false
                }
                val mParentXPath = (mWitnessToken as MarkedUpToken).parentXPath
                val nParentXPath = (nWitnessToken as MarkedUpToken).parentXPath
                parentXPathsMatch = parentXPathsMatch && mParentXPath == nParentXPath
            }
            return parentXPathsMatch
        }
        return false
    }

    private fun mergeNodeTokens(lastNode: TextNode, originalNode: TextNode) {
        originalNode.sigla.forEach { s: String -> lastNode.addBranchPath(s, originalNode.branchPathForSiglum(s)) }
        for (s in lastNode.sigla) {
            val tokenForWitness = lastNode.tokenForSiglum(s) as MarkedUpToken
            val tokenToMerge = originalNode.tokenForSiglum(s) as MarkedUpToken
            tokenForWitness
                    .withContent(tokenForWitness.content + tokenToMerge.content)
                    .normalizedContent = tokenForWitness.normalizedContent + tokenToMerge.normalizedContent
        }
    }

    private fun copyNode(originalNode: TextNode, mergedGraph: CollationGraph): TextNode {
        val tokens: Array<Token> = originalNode.sigla
                .mapNotNull { originalNode.tokenForSiglum(it) }
                .map { cloneToken(it) }
                .toTypedArray()
        val newNode = mergedGraph.addTextNodeWithTokens(*tokens)
        originalNode.sigla.forEach { s: String -> newNode.addBranchPath(s, originalNode.branchPathForSiglum(s)) }
        return newNode
    }

    private fun cloneToken(original: Token): Token {
        if (original is MarkedUpToken) {
            return original.clone()
        }
        throw RuntimeException("Can't clone token of type " + original.javaClass)
    }

    private fun copyIncomingEdges(
            originalGraph: CollationGraph,
            originalToMerged: Map<TextNode, TextNode>,
            mergedGraph: CollationGraph) {
        val linkedNodes: MutableSet<Node?> = HashSet()
        originalGraph
                .traverseTextNodes()
                .forEach { node: TextNode ->
                    val mergedNode: Node? = originalToMerged[node]
                    if (mergedNode !in linkedNodes) {
                        originalGraph
                                .getIncomingTextEdgeList(node)
                                .forEach { e: TextEdge ->
                                    val oSource = originalGraph.getSource(e)
                                    val mSource: Node = originalToMerged[oSource]!!
                                    Preconditions.checkNotNull(mSource)
                                    val oTarget: Node = originalGraph.getTarget(e)
                                    val mTarget: Node = originalToMerged[oTarget]!!
                                    Preconditions.checkNotNull(mTarget)
                                    mergedGraph.addDirectedEdge(mSource, mTarget, e.sigla)
                                }
                        linkedNodes += mergedNode
                    }
                }
    }

    private fun copyMarkupHyperEdges(
            originalGraph: CollationGraph,
            originalToMerged: Map<TextNode, TextNode>,
            mergedGraph: CollationGraph) =
            originalGraph
                    .markupList
                    .forEach { m: Markup ->
                        val mergedMarkupNode = mergedGraph.getMarkupNode(m)
                        originalGraph
                                .getTextNodeListForMarkup(m)
                                .map { key: TextNode -> originalToMerged[key] }
                                .distinct()
                                .forEach { mergedTextNode -> mergedGraph.linkMarkupToText(mergedMarkupNode, mergedTextNode) }
                    }
}
