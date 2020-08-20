package nl.knaw.huygens.hypercollate.jupyter

/*-
 * #%L
 * hyper-collate-jupyter
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

import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class HCTest() {
    @Test
    fun test_init() {
        HC.init()
    }

    @Test
    fun test_initCell() {
        HC.initCell()
    }

    @Test
    fun test_shutdown() {
        HC.shutdown()
    }

    @Test
    fun test_renderCollationGraph() {
        val wF: VariantWitnessGraph = HC.importVariantWitnessGraphFromXML("A",
                "<text>The dog's big eyes.</text>")
        println("\n--[wf.asDot(true)]----------------------------------------------------------")
        println(wF.asDot(true))
        val wQ = HC.importVariantWitnessGraphFromXML("B",
                "<text>The dog's <del>big black ears</del><add>brown eyes</add>.</text>")
        println("\n--[wQ.asColoredDot()]-------------------------------------------------------")
        println(wQ.asColoredDot())
        val collationGraph = HC.collate(wF, wQ)
        println("\n--[collationGraph.asDot()]--------------------------------------------------")
        println(collationGraph.asDot())
        println("\n--[collationGraph.asHTML()]-------------------------------------------------")
        println(collationGraph.asHTML())
        val render = collationGraph.asASCIITable()
        println("\n--[collationGraph.asASCIITable()]-------------------------------------------")
        println(render)
        val expected = """
            ┌───┬────┬──────┬──────────┬──────────┬────────┬─┐
            │[A]│The │dog's │big       │eyes      │        │.│
            ├───┼────┼──────┼──────────┼──────────┼────────┼─┤
            │[B]│    │      │[+]  brown│[+]   eyes│        │ │
            │   │The │dog's │[-] big   │[-] black │[-] ears│.│
            └───┴────┴──────┴──────────┴──────────┴────────┴─┘""".trimIndent()
        assertThat(render).isEqualTo(expected.replace("\n", System.lineSeparator()))
    }

}
