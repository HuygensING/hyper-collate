package nl.knaw.huygens.hypercollate.jupyter

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

import nl.knaw.huygens.graphviz.DotEngine
import nl.knaw.huygens.hypercollate.model.CollationGraph
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph
import nl.knaw.huygens.hypercollate.tools.CollationGraphNodeJoiner
import nl.knaw.huygens.hypercollate.tools.CollationGraphVisualizer
import nl.knaw.huygens.hypercollate.tools.DotFactory
import nl.knaw.huygens.hypercollate.tools.TokenMerger.joined

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
): String =
    DotFactory(emphasizeWhitespace).fromVariantWitnessGraphSimple(this.optionallyJoined(join))

private fun VariantWitnessGraph.optionallyJoined(join: Boolean): VariantWitnessGraph =
    if (join)
        this.joined()
    else
        this

fun VariantWitnessGraph.asColoredDot(
    join: Boolean = false,
    emphasizeWhitespace: Boolean = false
): String =
    DotFactory(emphasizeWhitespace).fromVariantWitnessGraphColored(this.optionallyJoined(join))

fun CollationGraph.asTable(
    format: TableFormat = TableFormat.ASCII,
    join: Boolean = false,
    emphasizeWhitespace: Boolean = false
): String =
    when (format) {
        is TableFormat.ASCII -> this.asASCIITable(join, emphasizeWhitespace)
        is TableFormat.HTML -> this.asHTMLString(join, emphasizeWhitespace)
    }

fun CollationGraph.asASCIITable(join: Boolean = true, emphasizeWhitespace: Boolean = false): String =
    CollationGraphVisualizer.toTableASCII(this.optionallyJoined(join), emphasizeWhitespace)

fun CollationGraph.asHTMLString(join: Boolean = true, emphasizeWhitespace: Boolean = false): String =
    CollationGraphVisualizer.toTableHTML(this.optionallyJoined(join), emphasizeWhitespace)

fun CollationGraph.asSVGPair(
    join: Boolean = false,
    emphasizeWhitespace: Boolean = false,
    hideMarkup: Boolean = false,
    horizontal: Boolean = false
): Pair<String, String> =
    asRenderPair(join, emphasizeWhitespace, hideMarkup, horizontal, OutputFormat.SVG())

fun CollationGraph.asPNGPair(
    join: Boolean = false,
    emphasizeWhitespace: Boolean = false,
    hideMarkup: Boolean = false,
    horizontal: Boolean = false
): Pair<String, String> =
    asRenderPair(join, emphasizeWhitespace, hideMarkup, horizontal, OutputFormat.PNG())

fun CollationGraph.asRenderPair(
    join: Boolean,
    emphasizeWhitespace: Boolean,
    hideMarkup: Boolean,
    horizontal: Boolean,
    format: OutputFormat
): Pair<String, String> {
    val dot: String = asDot(join, emphasizeWhitespace, hideMarkup, horizontal)
    return renderDot(dot, format)
}

fun CollationGraph.asDot(
    join: Boolean = false,
    emphasizeWhitespace: Boolean = false,
    hideMarkup: Boolean = false,
    horizontal: Boolean = false
): String =
    CollationGraphVisualizer.toDot(this.optionallyJoined(join), emphasizeWhitespace, hideMarkup, horizontal)

private fun CollationGraph.optionallyJoined(join: Boolean): CollationGraph =
    if (join) {
        CollationGraphNodeJoiner.join(this)
    } else this

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
