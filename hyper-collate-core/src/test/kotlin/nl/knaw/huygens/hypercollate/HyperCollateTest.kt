package nl.knaw.huygens.hypercollate

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

import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.parse.Parser
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph
import nl.knaw.huygens.hypercollate.tools.DotFactory
import nl.knaw.huygens.hypercollate.tools.TokenMerger.joined
import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions.assertThat
import java.awt.FlowLayout
import java.io.File
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import javax.xml.transform.OutputKeys
import javax.xml.transform.Source
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

open class HyperCollateTest {

    protected fun verifyDotExport(
            variantWitnessGraph: VariantWitnessGraph,
            expectedDot: String,
            name: String = "graph"
    ) {
        val wg = variantWitnessGraph.joined()
        val dot = DotFactory(true).fromVariantWitnessGraphSimple(wg)
        writeGraph(dot, name)
        assertThat(dot).isEqualTo(expectedDot)
    }

    protected fun writeGraph(dot: String?, name: String) {
        try {
            FileUtils.write(File("out/$name.dot"), dot, StandardCharsets.UTF_8)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun showGraph(dot: String) {
        try {
            val g = Parser.read(dot.replace("=<".toRegex(), "=\"").replace(">]".toRegex(), "\"]"))
            val bufferedImage = Graphviz.fromGraph(g).width(4000).render(Format.PNG).toImage()
            val frame = JFrame().apply {
                contentPane.layout = FlowLayout()
                contentPane.add(JLabel(ImageIcon(bufferedImage)))
                pack()
                isVisible = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun String.formatXML(): String =
            try {
                val xmlInput: Source = StreamSource(StringReader(this))
                val stringWriter = StringWriter()
                TransformerFactory.newInstance().newTransformer().apply {
                    setOutputProperty(OutputKeys.INDENT, "yes")
                    setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "no")
                    setOutputProperty("{https://xml.apache.org/xslt}indent-amount", "  ")
                    transform(xmlInput, StreamResult(stringWriter))
                }
                stringWriter.toString().trim { it <= ' ' }
            } catch (e: Exception) {
                println(this)
                throw RuntimeException(e)
            }

    fun String.asXHTML(): String =
            """
            <html>
            ${this.replace("&nbsp;","!#nbsp#!")}
            </html>
            """.formatXML().replace("!#nbsp#!","&nbsp;")
}
