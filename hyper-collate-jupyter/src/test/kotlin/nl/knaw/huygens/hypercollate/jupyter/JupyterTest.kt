/*-
 * #%L
 * hyper-collate-jupyter
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
package nl.knaw.huygens.hypercollate.jupyter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

class JupyterTest {

    @Test
    fun init() {
        HC.init()
    }

    @Test
    fun initCell() {
        HC.initCell()
    }

    @Test
    fun shutdown() {
        HC.shutdown()
    }

    @Test
    fun collate() {
        val a = HC.importXMLWitness("A", "<xml>The white dog's brown ears.</xml>")
        val p = a.asSVGPair()
        assertThat(p.first).isEqualTo("image/svg+xml")

        val b = HC.importXMLWitness("C", File("../notebooks/c.xml"))
        val p2 = b.asPNGPair()
        assertThat(p2.first).isEqualTo("image/png")

        val cg = HC.collate(a, b)
        assertThat(cg).isNotNull

        println(cg.asASCIITable())
        val t = cg.asTable(TableFormat.HTML)
        assertThat(t).isNotEmpty

        val p3 = cg.asSVGPair()
        assertThat(p3.first).isEqualTo("image/svg+xml")

        val p4 = cg.asPNGPair()
        assertThat(p4.first).isEqualTo("image/png")
    }
}
