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
import nl.knaw.huygens.hypercollate.model.MarkedUpToken
import nl.knaw.huygens.hypercollate.model.SimpleTokenVertex
import nl.knaw.huygens.hypercollate.model.StartTokenVertex
import nl.knaw.huygens.hypercollate.model.TokenVertex
import nl.knaw.huygens.hypercollate.tools.TokenMerger
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.stream.Collectors

class VariantWitnessGraphRankingTest : HyperCollateTest() {
    @Test
    fun test() {
        val importer = XMLImporter()
        val wg0 = importer.importXML(
                "A", "<xml>Een ongeluk komt <del>nooit</del><add>zelden</add> alleen.</xml>")
        val witnessGraph = TokenMerger.merge(wg0)
        val ranking = VariantWitnessGraphRanking.of(witnessGraph)
        val byRank = ranking.byRank
        byRank.forEach { (key: Int, value: Set<TokenVertex?>) -> println("$key:$value") }
        assertThat(byRank[0]).hasSize(1)
        assert(byRank[0]!!.iterator().next() is StartTokenVertex)
        assertThat(byRank[1]).hasSize(1)
        assert(byRank[1]!!.iterator().next() is SimpleTokenVertex)

        val tokenVertex = byRank[1]!!.iterator().next()
        assert(tokenVertex is SimpleTokenVertex)

        val content = (tokenVertex.token as MarkedUpToken).content
        assertThat(content).isEqualTo("Een ongeluk komt ")
        assertThat(byRank[2]).hasSize(2)

        val tokenVertices = byRank[2]!!.stream().sorted().collect(Collectors.toList())
        val tokenVertex1 = tokenVertices[0]
        assert(tokenVertex1 is SimpleTokenVertex)

        val content1 = (tokenVertex1.token as MarkedUpToken).content
        assertThat(content1).isEqualTo("nooit")

        val tokenVertex2 = tokenVertices[1]
        assert(tokenVertex2 is SimpleTokenVertex)

        val content2 = (tokenVertex2.token as MarkedUpToken).content
        assertThat(content2).isEqualTo("zelden")

        // Map<TokenVertex, Integer> byVertex = ranking.getByVertex();
    }
}
