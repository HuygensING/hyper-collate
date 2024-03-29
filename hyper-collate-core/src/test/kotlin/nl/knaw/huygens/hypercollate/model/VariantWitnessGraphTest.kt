package nl.knaw.huygens.hypercollate.model

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

import nl.knaw.huygens.hypercollate.HyperCollateTest
import nl.knaw.huygens.hypercollate.tools.DotFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.stream.Collectors

class VariantWitnessGraphTest : HyperCollateTest() {
    @Test
    fun test() {
        //    String xml = "<s>Collating is <del>NP hard</del><add>easy</add>.</s>";
        val sMarkup = Markup("s")
        val delMarkup = Markup("del")
        val addMarkup = Markup("add")
        val witness = SimpleWitness("A")
        val mtv0 = aTokenVertex("Collating ", 0L, "/s", witness)
        val mtv1 = aTokenVertex("is ", 1L, "/s", witness)
        val mtv2 = aTokenVertex("NP ", 2L, "/s/del", witness)
        val mtv3 = aTokenVertex("hard", 3L, "/s/del", witness)
        val mtv4 = aTokenVertex("easy", 4L, "/s/add", witness)
        val mtv5 = aTokenVertex(".", 5L, "/s", witness)
        val vwg1 = VariantWitnessGraph(witness.sigil)
        vwg1.addMarkup(sMarkup, delMarkup, addMarkup)
        val startTokenVertex = vwg1.startTokenVertex
        vwg1.addOutgoingTokenVertexToTokenVertex(startTokenVertex, mtv0) // (START)->(collating)
        vwg1.addOutgoingTokenVertexToTokenVertex(mtv0, mtv1) // (collating)->(is)
        vwg1.addMarkupToTokenVertex(mtv0, sMarkup)
        vwg1.addOutgoingTokenVertexToTokenVertex(mtv1, mtv2) // (is)->(np)
        vwg1.addOutgoingTokenVertexToTokenVertex(mtv1, mtv4) // (is)->(easy)
        vwg1.addMarkupToTokenVertex(mtv1, sMarkup)
        vwg1.addOutgoingTokenVertexToTokenVertex(mtv2, mtv3) // (np)->(hard)
        vwg1.addMarkupToTokenVertex(mtv2, sMarkup)
        vwg1.addMarkupToTokenVertex(mtv2, delMarkup)
        vwg1.addOutgoingTokenVertexToTokenVertex(mtv3, mtv5) // (hard)->(.)
        vwg1.addMarkupToTokenVertex(mtv3, sMarkup)
        vwg1.addMarkupToTokenVertex(mtv3, delMarkup)
        vwg1.addOutgoingTokenVertexToTokenVertex(mtv4, mtv5) // (easy)->(.)
        vwg1.addMarkupToTokenVertex(mtv4, sMarkup)
        vwg1.addMarkupToTokenVertex(mtv4, addMarkup)
        val endTokenVertex = vwg1.endTokenVertex
        vwg1.addOutgoingTokenVertexToTokenVertex(mtv5, endTokenVertex) // (.)->(END)
        vwg1.addMarkupToTokenVertex(mtv5, sMarkup)
        val hardMarkup = vwg1.getMarkupListForTokenVertex(mtv3)
        assertThat(hardMarkup).containsExactly(sMarkup, delMarkup)

        val incoming = mtv5.incomingTokenVertexStream.collect(Collectors.toList())
        assertThat(incoming).containsOnly(mtv3, mtv4)

        val dot = DotFactory(false).fromVariantWitnessGraphSimple(vwg1)
        LOG.info("dot=\n{}", dot)
        val expected = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            vA_000 [label=<Collating&nbsp;<br/><i>A: /s</i>>]
            vA_001 [label=<is&nbsp;<br/><i>A: /s</i>>]
            vA_002 [label=<NP&nbsp;<br/><i>A: /s/del</i>>]
            vA_004 [label=<easy<br/><i>A: /s/add</i>>]
            vA_003 [label=<hard<br/><i>A: /s/del</i>>]
            vA_005 [label=<.<br/><i>A: /s</i>>]
            end [label="";shape=doublecircle,rank=middle]
            begin->vA_000
            vA_000->vA_001
            vA_001->vA_002
            vA_001->vA_004
            vA_002->vA_003
            vA_003->vA_005
            vA_004->vA_005
            vA_005->end
            }
            """.trimIndent()
        assertThat(dot).isEqualTo(expected)

        val expected2 = """
            digraph VariantWitnessGraph{
            graph [rankdir=LR]
            labelloc=b
            begin [label="";shape=doublecircle,rank=middle]
            vA_000 [label=<Collating&#9251;is&#9251;<br/><i>A: /s</i>>]
            vA_002 [label=<NP&#9251;hard<br/><i>A: /s/del</i>>]
            vA_004 [label=<easy<br/><i>A: /s/add</i>>]
            vA_005 [label=<.<br/><i>A: /s</i>>]
            end [label="";shape=doublecircle,rank=middle]
            begin->vA_000
            vA_000->vA_002
            vA_000->vA_004
            vA_002->vA_005
            vA_004->vA_005
            vA_005->end
            }
            """.trimIndent()
        verifyDotExport(vwg1, expected2)
    }

    private fun aTokenVertex(
            string: String,
            index: Long,
            parentXPath: String,
            witness: SimpleWitness
    ): SimpleTokenVertex {
        val token = MarkedUpToken()
                .setContent(string)
                .setNormalizedContent(string.toLowerCase())
                .setParentXPath(parentXPath)
                .setWitness(witness)
                .setIndexNumber(index)
        return SimpleTokenVertex(token)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(VariantWitnessGraphTest::class.java)
    }
}
