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

import nl.knaw.huygens.hypercollate.model.CollationGraph
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph
import nl.knaw.huygens.hypercollate.rest.DotEngine
import nl.knaw.huygens.hypercollate.tools.CollationGraphNodeJoiner
import nl.knaw.huygens.hypercollate.tools.CollationGraphVisualizer
import nl.knaw.huygens.hypercollate.tools.DotFactory
import nl.knaw.huygens.hypercollate.tools.TokenMerger
import java.io.ByteArrayOutputStream

fun VariantWitnessGraph.asSVGPair(
        dotEngine: DotEngine,
        colored: Boolean = true,
        join: Boolean = false,
        emphasizeWhitespace: Boolean = false
): Pair<String, String> =
        asRenderPair(dotEngine, colored, join, emphasizeWhitespace, OutputFormat.SVG())

fun VariantWitnessGraph.asPNGPair(
        dotEngine: DotEngine,
        colored: Boolean = true,
        join: Boolean = false,
        emphasizeWhitespace: Boolean = false
): Pair<String, String> =
        asRenderPair(dotEngine, colored, join, emphasizeWhitespace, OutputFormat.PNG())

fun VariantWitnessGraph.asDot(
        join: Boolean = false,
        emphasizeWhitespace: Boolean = false
): String {
    val graph = if (join) {
        TokenMerger.merge(this);
    } else this
    return DotFactory(emphasizeWhitespace).fromVariantWitnessGraphSimple(graph)
}

fun VariantWitnessGraph.asColoredDot(
        join: Boolean = false,
        emphasizeWhitespace: Boolean = false
): String {
    val graph = if (join) {
        TokenMerger.merge(this);
    } else this
    return DotFactory(emphasizeWhitespace).fromVariantWitnessGraphColored(graph)
}

fun CollationGraph.asASCIITable(emphasizeWhitespace: Boolean = false): String =
        CollationGraphVisualizer.toTableASCII(this, emphasizeWhitespace)

fun CollationGraph.asHTMLString(): String =
        CollationGraphVisualizer.toTableHTML(this)

fun CollationGraph.asSVGPair(
        dotEngine: DotEngine,
        join: Boolean = false,
        emphasizeWhitespace: Boolean = false
): Pair<String, String> =
        asRenderPair(dotEngine, join, emphasizeWhitespace, OutputFormat.SVG())

fun CollationGraph.asPNGPair(
        dotEngine: DotEngine,
        join: Boolean = false,
        emphasizeWhitespace: Boolean = false
): Pair<String, String> =
        asRenderPair(dotEngine, join, emphasizeWhitespace, OutputFormat.PNG())

fun CollationGraph.asRenderPair(
        dotEngine: DotEngine,
        join: Boolean,
        emphasizeWhitespace: Boolean,
        format: OutputFormat
): Pair<String, String> {
    val dot: String = asDot(join, emphasizeWhitespace,false)
    return renderDot(dotEngine, dot, format)
}

fun CollationGraph.asDot(
        join: Boolean = false,
        emphasizeWhitespace: Boolean = false,
        hideMarkup: Boolean = false
): String {
    val graph = if (join) {
        CollationGraphNodeJoiner.join(this)
    } else this
    return CollationGraphVisualizer.toDot(graph, emphasizeWhitespace, hideMarkup)
}

fun renderDot(
        dotEngine: DotEngine,
        dot: String,
        format: OutputFormat
): Pair<String, String> {
    val outputStream = ByteArrayOutputStream()
    dotEngine.renderAs(format.extension, dot, outputStream)
    val rendered = outputStream.toString("UTF-8")
    return Pair(format.mimeType, rendered)
}

sealed class OutputFormat(val extension: String, val mimeType: String) {
    class SVG() : OutputFormat("svg", "image/svg+xml")
    class PNG() : OutputFormat("png", "image/png")
}

fun VariantWitnessGraph.asRenderPair(
        dotEngine: DotEngine,
        colored: Boolean,
        join: Boolean,
        emphasizeWhitespace: Boolean,
        format: OutputFormat
): Pair<String, String> {
    val dot: String = if (colored) {
        asDot(join, emphasizeWhitespace)
    } else {
        asColoredDot(join, emphasizeWhitespace)
    }
    return renderDot(dotEngine, dot, format)
}
