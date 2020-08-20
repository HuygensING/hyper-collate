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
package nl.knaw.huygens.hypercollate.jupyter

import nl.knaw.huygens.hypercollate.collator.HyperCollator
import nl.knaw.huygens.hypercollate.importer.XMLImporter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class LibraryConfigTest() {
    @Test
    fun test_init() {
        LibraryConfig.init()
    }

    @Test
    fun test_initCell() {
        LibraryConfig.initCell()
    }

    @Test
    fun test_shutdown() {
        LibraryConfig.shutdown()
    }

    @Test
    fun test_renderCollationGraph() {
        val hyperCollator = HyperCollator()
        val importer = XMLImporter()
        val wF = importer.importXML("A", "<text>The dog's big eyes.</text>")
        val wQ = importer.importXML(
                "B", "<text>The dog's <del>big black ears</del><add>brown eyes</add>.</text>")
        val collationGraph = hyperCollator.collate(wF, wQ)
        val render = LibraryConfig.renderCollationGraph(collationGraph)
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
