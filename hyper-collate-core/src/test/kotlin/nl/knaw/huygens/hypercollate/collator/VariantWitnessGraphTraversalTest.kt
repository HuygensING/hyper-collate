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

import nl.knaw.huygens.hypercollate.HyperCollateTest
import nl.knaw.huygens.hypercollate.importer.XMLImporter
import nl.knaw.huygens.hypercollate.model.*
import nl.knaw.huygens.hypercollate.tools.TokenMerger
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class VariantWitnessGraphTraversalTest : HyperCollateTest() {
    @Test
    fun testVariantWitnessGraphTraversal() {
        val importer = XMLImporter()
        val wg0 = importer.importXML("A", "<xml>Eeny, meeny, miny, <del>curly</del><add>moe</add>!</xml>")
        val witnessGraph = TokenMerger.merge(wg0)
        val traversal = VariantWitnessGraphTraversal.of(witnessGraph)
        // for (TokenVertex tv : traversal) {
        // if (tv instanceof SimpleTokenVertex) {
        // MarkedUpToken markedUpToken = (MarkedUpToken) tv.getToken();
        // System.out.println("'" + markedUpToken.getContent() + "'");
        // }
        // }
        val iterator: Iterator<TokenVertex> = traversal.iterator()
        var tokenVertex = iterator.next()
        assert(tokenVertex is StartTokenVertex)

        tokenVertex = iterator.next()
        assert(tokenVertex is SimpleTokenVertex)

        var markedUpToken = tokenVertex.token as MarkedUpToken
        assertThat(markedUpToken.content).isEqualTo("Eeny, meeny, miny, ")

        tokenVertex = iterator.next()
        assert(tokenVertex is SimpleTokenVertex)

        markedUpToken = tokenVertex.token as MarkedUpToken
        assertThat(markedUpToken.content).isEqualTo("curly")

        var markupTagListForTokenVertex = markupTags(witnessGraph, tokenVertex)
        assertThat(markupTagListForTokenVertex).contains("del")

        tokenVertex = iterator.next()
        assert(tokenVertex is SimpleTokenVertex)

        markedUpToken = tokenVertex.token as MarkedUpToken
        assertThat(markedUpToken.content).isEqualTo("moe")

        markupTagListForTokenVertex = markupTags(witnessGraph, tokenVertex)
        assertThat(markupTagListForTokenVertex).contains("add")

        tokenVertex = iterator.next()
        assert(tokenVertex is SimpleTokenVertex)

        markedUpToken = tokenVertex.token as MarkedUpToken
        assertThat(markedUpToken.content).isEqualTo("!")

        tokenVertex = iterator.next()
        assert(tokenVertex is EndTokenVertex)
        assertThat(iterator.hasNext()).isFalse()
    }

    private fun markupTags(witnessGraph: VariantWitnessGraph, tokenVertex: TokenVertex): List<String> =
            witnessGraph.getMarkupListForTokenVertex(tokenVertex)
                    .map { it.tagName }

}
