package nl.knaw.huygens.hypercollate.tools

/*-
 * #%L
 * hyper-collate-core
 * =======
 * Copyright (C) 2017 - 2021 Huygens ING (KNAW)
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

import nl.knaw.huygens.hypercollate.model.*
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

object TokenMerger {
    @JvmStatic
    fun VariantWitnessGraph.joined(): VariantWitnessGraph {
        val mergedGraph = VariantWitnessGraph(siglum)
        markupList.forEach { markup: Markup -> mergedGraph.addMarkup(markup) }
        val originalToMergedMap: MutableMap<Long, TokenVertex> = HashMap()
        val originalTokenVertex = startTokenVertex
        val verticesToAdd = originalTokenVertex.outgoingTokenVertexList
        val handledTokens: MutableList<Long> = ArrayList()
        val endTokenHandled = AtomicBoolean(false)
        val mergedVertexToLinkTo = mergedGraph.startTokenVertex
        verticesToAdd.forEach { originalVertex: TokenVertex ->
            handle(
                    this,
                    mergedGraph,
                    originalToMergedMap,
                    handledTokens,
                    endTokenHandled,
                    originalVertex,
                    mergedVertexToLinkTo
            )
        }
        return mergedGraph
    }

    // TODO: introduce context to avoid passing so many parameters
    private fun handle(
            originalGraph: VariantWitnessGraph,
            mergedGraph: VariantWitnessGraph,
            originalToMergedMap: MutableMap<Long, TokenVertex>,
            handledTokens: MutableList<Long>,
            endTokenHandled: AtomicBoolean,
            originalVertexIn: TokenVertex,
            mergedVertexToLinkTo: TokenVertex
    ) {
        when (originalVertexIn) {
            is EndTokenVertex -> {
                val allIncomingVerticesAreMerged = originalVertexIn.incomingTokenVertexList
                        .map { it.token as MarkedUpToken }
                        .all { originalToMergedMap.containsKey(it.indexNumber) }
                if (endTokenHandled.get() || !allIncomingVerticesAreMerged) {
                    return
                }
                val endTokenVertex = mergedGraph.endTokenVertex
                originalVertexIn
                        .incomingTokenVertexList
                        .forEach { tv: TokenVertex ->
                            val indexNumber = (tv.token as MarkedUpToken).indexNumber
                            val mergedTokenVertex = originalToMergedMap[indexNumber]
                                    ?: error("originalToMergedMap[$indexNumber] is null")
                            mergedGraph.addOutgoingTokenVertexToTokenVertex(mergedTokenVertex, endTokenVertex)
                        }
                endTokenHandled.set(true)
                return
            }
            else -> {
                var originalVertex = originalVertexIn
                val originalToken = originalVertex.token as MarkedUpToken
                val tokenNumber = originalToken.indexNumber
                if (tokenNumber in handledTokens) {
                    val mergedTokenVertex = originalToMergedMap[tokenNumber]
                    mergedGraph.addOutgoingTokenVertexToTokenVertex(mergedVertexToLinkTo, mergedTokenVertex)
                    return
                }
                val mergedToken = MarkedUpToken()
                        .withContent(originalToken.content)
                        .withNormalizedContent(originalToken.normalizedContent)
                        .withParentXPath(originalToken.parentXPath)
                        .withWitness(originalToken.witness as SimpleWitness)
                        .withRdg(originalToken.rdg)
                        .withIndexNumber(tokenNumber)
                val mergedVertex = SimpleTokenVertex(mergedToken).withBranchPath(originalVertex.branchPath)
                originalGraph
                        .markupListForTokenVertex(originalVertex)
                        .forEach { markup: Markup -> mergedGraph.addMarkupToTokenVertex(mergedVertex, markup) }
                originalToMergedMap[tokenNumber] = mergedVertex
                mergedGraph.addOutgoingTokenVertexToTokenVertex(mergedVertexToLinkTo, mergedVertex)
                handledTokens += tokenNumber
                var originalOutgoingVertices = originalVertex.outgoingTokenVertexList
                while (canMerge(originalGraph, originalVertex, originalOutgoingVertices)) {
                    val nextOriginalToken = originalOutgoingVertices[0].token as MarkedUpToken
                    mergedToken.content += nextOriginalToken.content
                    mergedToken.normalizedContent += nextOriginalToken.normalizedContent
                    originalToMergedMap[nextOriginalToken.indexNumber] = mergedVertex
                    originalVertex = originalOutgoingVertices[0]
                    originalOutgoingVertices = originalVertex.outgoingTokenVertexList
                }
                originalOutgoingVertices.forEach { oVertex: TokenVertex ->
                    handle(
                            originalGraph,
                            mergedGraph,
                            originalToMergedMap,
                            handledTokens,
                            endTokenHandled,
                            oVertex,
                            mergedVertex)
                }
            }
        }
    }

    private fun canMerge(
            originalGraph: VariantWitnessGraph,
            originalVertex: TokenVertex,
            originalOutgoingVertices: List<TokenVertex>
    ): Boolean =
            (originalOutgoingVertices.size == 1
                    && (originalGraph.markupListForTokenVertex(originalVertex)
                    == originalGraph.markupListForTokenVertex(originalOutgoingVertices[0])))
}
