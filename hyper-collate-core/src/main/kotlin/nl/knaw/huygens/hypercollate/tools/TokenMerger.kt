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

import nl.knaw.huygens.hypercollate.model.*
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors

object TokenMerger {
    @JvmStatic
    fun merge(originalGraph: VariantWitnessGraph): VariantWitnessGraph {
        val mergedGraph = VariantWitnessGraph(originalGraph.sigil)
        originalGraph.markupStream.forEach { markup: Markup -> mergedGraph.addMarkup(markup) }
        val originalToMergedMap: MutableMap<Long, TokenVertex> = HashMap()
        val originalTokenVertex = originalGraph.startTokenVertex
        val verticesToAdd = originalTokenVertex.outgoingTokenVertexStream.collect(Collectors.toList())
        val handledTokens: MutableList<Long> = ArrayList()
        val endTokenHandled = AtomicBoolean(false)
        val mergedVertexToLinkTo = mergedGraph.startTokenVertex
        verticesToAdd.forEach { originalVertex: TokenVertex ->
            handle(
                    originalGraph,
                    mergedGraph,
                    originalToMergedMap,
                    handledTokens,
                    endTokenHandled,
                    originalVertex,
                    mergedVertexToLinkTo)
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
            mergedVertexToLinkTo: TokenVertex) {
        var originalVertex = originalVertexIn
        if (originalVertex is EndTokenVertex) {
            if (endTokenHandled.get()) {
                return
            }
            val endTokenVertex = mergedGraph.endTokenVertex
            originalVertex
                    .getIncomingTokenVertexStream()
                    .forEach { tv: TokenVertex ->
                        val indexNumber = (tv.token as MarkedUpToken).indexNumber
                        val mergedTokenVertex = originalToMergedMap[indexNumber]
                        mergedGraph.addOutgoingTokenVertexToTokenVertex(mergedTokenVertex, endTokenVertex)
                    }
            endTokenHandled.set(true)
            return
        }
        val originalToken = originalVertex.token as MarkedUpToken
        val tokenNumber = originalToken.indexNumber
        if (handledTokens.contains(tokenNumber)) {
            val mergedTokenVertex = originalToMergedMap[tokenNumber]
            mergedGraph.addOutgoingTokenVertexToTokenVertex(mergedVertexToLinkTo, mergedTokenVertex)
            return
        }
        val mergedToken = MarkedUpToken()
                .setContent(originalToken.content)
                .setNormalizedContent(originalToken.normalizedContent)
                .setParentXPath(originalToken.parentXPath)
                .setWitness(originalToken.witness as SimpleWitness)
                .setRdg(originalToken.rdg)
                .setIndexNumber(tokenNumber)
        val mergedVertex = SimpleTokenVertex(mergedToken).setBranchPath(originalVertex.branchPath)
        originalGraph
                .getMarkupListForTokenVertex(originalVertex)
                .forEach { markup: Markup -> mergedGraph.addMarkupToTokenVertex(mergedVertex, markup) }
        originalToMergedMap[tokenNumber] = mergedVertex
        mergedGraph.addOutgoingTokenVertexToTokenVertex(mergedVertexToLinkTo, mergedVertex)
        handledTokens.add(tokenNumber)
        var originalOutgoingVertices = originalVertex.outgoingTokenVertexStream.collect(Collectors.toList())
        while (canMerge(originalGraph, originalVertex, originalOutgoingVertices)) {
            val nextOriginalToken = originalOutgoingVertices[0].token as MarkedUpToken
            mergedToken
                    .setContent(mergedToken.content + nextOriginalToken.content).normalizedContent = mergedToken.normalizedContent + nextOriginalToken.normalizedContent
            originalToMergedMap[nextOriginalToken.indexNumber] = mergedVertex
            originalVertex = originalOutgoingVertices[0]
            originalOutgoingVertices = originalVertex.outgoingTokenVertexStream.collect(Collectors.toList())
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

    private fun canMerge(
            originalGraph: VariantWitnessGraph,
            originalVertex: TokenVertex,
            originalOutgoingVertices: List<TokenVertex>): Boolean =
            (originalOutgoingVertices.size == 1
                    && (originalGraph.getMarkupListForTokenVertex(originalVertex)
                    == originalGraph.getMarkupListForTokenVertex(originalOutgoingVertices[0])))
}
