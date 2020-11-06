package nl.knaw.huygens.hypercollate.model

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
import nl.knaw.huygens.hypercollate.collator.VariantWitnessGraphTraversal
import java.util.*
import java.util.stream.Stream

class VariantWitnessGraph(val sigil: String) {
    val startTokenVertex: TokenVertex
    val endTokenVertex: TokenVertex

    val markupPathToRootForBranch: MutableMap<Int, MutableList<Markup>> = mutableMapOf()

    private val markupList: MutableList<Markup> = ArrayList()
    private val markup2TokenVertexList: MutableMap<Markup, MutableList<TokenVertex>> = HashMap()
    private val tokenVertex2MarkupList: MutableMap<TokenVertex, MutableList<Markup>> = HashMap()

    val markupStream: Stream<Markup>
        get() = markupList.stream()

    fun addMarkup(vararg markup: Markup) {
        Collections.addAll(markupList, *markup)
    }

    fun addOutgoingTokenVertexToTokenVertex(token0: TokenVertex?, token1: TokenVertex?) {
        if (token0 == null || token1 == null) {
            return
        }
        Preconditions.checkNotNull(token0)
        Preconditions.checkNotNull(token1)
        token0.addOutgoingTokenVertex(token1) // (token0)->(token1)
        token1.addIncomingTokenVertex(token0) // (token1)<-(token0)
    }

    fun addMarkupToTokenVertex(tokenVertex: SimpleTokenVertex, markup: Markup) {
        markup2TokenVertexList.putIfAbsent(markup, ArrayList())
        markup2TokenVertexList[markup]!!.add(tokenVertex)
        tokenVertex2MarkupList.putIfAbsent(tokenVertex, ArrayList())
        tokenVertex2MarkupList[tokenVertex]!!.add(markup)
    }

    fun markupListForTokenVertex(tokenVertex: TokenVertex): List<Markup> =
            tokenVertex2MarkupList.getOrDefault(tokenVertex, ArrayList())

    fun tokenVertexListForMarkup(markup: Markup): List<TokenVertex> =
            markup2TokenVertexList.getOrDefault(markup, ArrayList())

    fun vertices(): Iterable<TokenVertex> = VariantWitnessGraphTraversal.of(this)

    init {
        startTokenVertex = StartTokenVertex(sigil)
        endTokenVertex = EndTokenVertex(sigil)
    }
}
