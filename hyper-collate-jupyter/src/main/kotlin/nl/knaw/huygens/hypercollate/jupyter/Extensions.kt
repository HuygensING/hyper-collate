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

import nl.knaw.huygens.graphviz.DotEngine
import nl.knaw.huygens.hypercollate.model.CollationGraph
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph
import nl.knaw.huygens.hypercollate.tools.CollationGraphNodeJoiner
import nl.knaw.huygens.hypercollate.tools.CollationGraphVisualizer
import nl.knaw.huygens.hypercollate.tools.DotFactory
import nl.knaw.huygens.hypercollate.tools.TokenMerger

private val dotEngine = DotEngine()

fun VariantWitnessGraph.asSVGPair(
        colored: Boolean = true,
        join: Boolean = false,
        emphasizeWhitespace: Boolean = false
): Pair<String, String> =
        asRenderPair(colored, join, emphasizeWhitespace, OutputFormat.SVG())

fun VariantWitnessGraph.asPNGPair(
        colored: Boolean = true,
        join: Boolean = false,
        emphasizeWhitespace: Boolean = false
): Pair<String, String> =
        asRenderPair(colored, join, emphasizeWhitespace, OutputFormat.PNG())

fun VariantWitnessGraph.asDot(
        join: Boolean = false,
        emphasizeWhitespace: Boolean = false
): String {
    val graph = if (join) {
        TokenMerger.merge(this)
    } else this
    return DotFactory(emphasizeWhitespace).fromVariantWitnessGraphSimple(graph)
}

fun VariantWitnessGraph.asColoredDot(
        join: Boolean = false,
        emphasizeWhitespace: Boolean = false
): String {
    val graph = if (join) {
        TokenMerger.merge(this)
    } else this
    return DotFactory(emphasizeWhitespace).fromVariantWitnessGraphColored(graph)
}

fun CollationGraph.asTable(format: TableFormat = TableFormat.ASCII, emphasizeWhitespace: Boolean = false): String =
        when (format) {
            is TableFormat.ASCII -> this.asASCIITable(emphasizeWhitespace)
            is TableFormat.HTML -> this.asHTMLString(emphasizeWhitespace)
        }

fun CollationGraph.asASCIITable(emphasizeWhitespace: Boolean = false): String =
        CollationGraphVisualizer.toTableASCII(this, emphasizeWhitespace)

fun CollationGraph.asHTMLString(emphasizeWhitespace: Boolean = false): String =
        CollationGraphVisualizer.toTableHTML(this, emphasizeWhitespace)

fun CollationGraph.asSVGPair(
        join: Boolean = false,
        emphasizeWhitespace: Boolean = false
): Pair<String, String> =
        asRenderPair(join, emphasizeWhitespace, OutputFormat.SVG())

fun CollationGraph.asPNGPair(
        join: Boolean = false,
        emphasizeWhitespace: Boolean = false
): Pair<String, String> =
        asRenderPair(join, emphasizeWhitespace, OutputFormat.PNG())

fun CollationGraph.asRenderPair(
        join: Boolean,
        emphasizeWhitespace: Boolean,
        format: OutputFormat
): Pair<String, String> {
    val dot: String = asDot(join, emphasizeWhitespace, false)
    return renderDot(dot, format)
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
        dot: String,
        format: OutputFormat
): Pair<String, String> {
    val rendered = dotEngine.renderAs(format.extension, dot)
    return Pair(format.mimeType, rendered)
}

sealed class OutputFormat(val extension: String, val mimeType: String) {
    class SVG : OutputFormat("svg", "image/svg+xml")
    class PNG : OutputFormat("png", "image/png")
}

sealed class TableFormat {
    object ASCII : TableFormat()
    object HTML : TableFormat()
}

fun VariantWitnessGraph.asRenderPair(
        colored: Boolean,
        join: Boolean,
        emphasizeWhitespace: Boolean,
        format: OutputFormat
): Pair<String, String> {
    val dot: String = if (colored) {
        asColoredDot(join, emphasizeWhitespace)
    } else {
        asDot(join, emphasizeWhitespace)
    }
    return renderDot(dot, format)
}

//fun VariantWitnessGraph.show(colored: Boolean = true, join: Boolean = true, emphasizeWhitespace: Boolean = false) = MIME(this.asSVGPair(colored, join, emphasizeWhitespace))
//fun CollationGraph.asHtml() = HTML(this.asHTMLString())
//fun CollationGraph.show(join: Boolean = true, emphasizeWhitespace: Boolean = false) = MIME(this.asSVGPair(join, emphasizeWhitespace))
