package nl.knaw.huygens.hypercollate.tools

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

import de.vandermeer.asciitable.AsciiTable
import de.vandermeer.asciitable.CWC_LongestLine
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment
import nl.knaw.huygens.hypercollate.importer.INSTANT_DEL_XPATH_NODE
import nl.knaw.huygens.hypercollate.importer.SEQ0_DEL_XPATH_NODE
import nl.knaw.huygens.hypercollate.importer.TYPE_IMMEDIATE_DEL_XPATH_NODE
import nl.knaw.huygens.hypercollate.model.CollationGraph
import nl.knaw.huygens.hypercollate.model.MarkedUpToken
import nl.knaw.huygens.hypercollate.model.TextNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.math.max

object CollationGraphVisualizer {
    private const val NBSP = "\u00A0"
    val log: Logger = LoggerFactory.getLogger(this.javaClass)

    @JvmStatic
    fun toTableASCII(graph: CollationGraph, emphasizeWhitespace: Boolean): String {
        val sigla = graph.sigla
        val whitespaceCharacter = if (emphasizeWhitespace) "_" else " "
        val rowMap: MutableMap<String, MutableList<Cell>> = HashMap()
        sigla.forEach { siglum: String -> rowMap[siglum] = ArrayList() }
        val ranking = CollationGraphRanking.of(graph)
        log.info("ranking size={}", ranking.size)
        val maxLayers: MutableMap<String, Int> = HashMap()
        sigla.forEach { siglum: String -> maxLayers[siglum] = 1 }
        for (nodeSet in ranking) {
            if (nodeSet.isBorderNode(graph)) {
                // skip start and end nodes
                continue
            }
            val nodeTokensPerWitness: MutableMap<String, MutableList<MarkedUpToken>> = HashMap()
            sigla.forEach { siglum: String ->
                nodeTokensPerWitness[siglum] = ArrayList()
                nodeSet.forEach { node: TextNode ->
                    val token = node.tokenForSiglum(siglum)
                    if (token != null) {
                        val mToken = token as MarkedUpToken
                        nodeTokensPerWitness[siglum]!!.add(mToken)
                    }
                }
            }
            sigla.forEach { siglum: String ->
                val tokens: List<MarkedUpToken> = nodeTokensPerWitness[siglum]!!
                maxLayers[siglum] = max(maxLayers[siglum]!!, tokens.filter { it.content.isNotBlank() }.size)
                val cell = tokens.toCell(whitespaceCharacter)
                rowMap[siglum]!!.add(cell)
            }
        }
        return asciiTable(graph.sigla, rowMap, maxLayers).render()
    }

    private fun Set<TextNode>.isBorderNode(graph: CollationGraph): Boolean {
        if (size != 1) {
            return false
        }
        val node = iterator().next()
        val hasNoIncomingEdges = graph.getIncomingEdges(node).isEmpty()
        val hasNoOutgoingEdges = graph.getOutgoingEdges(node).isEmpty()
        return hasNoIncomingEdges || hasNoOutgoingEdges
    }

    private fun List<MarkedUpToken>.toCell(whitespaceCharacter: String): Cell {
        val cell = Cell()
        if (isEmpty()) {
            cell.addLayer(" ")
        } else {
            sortedBy { it.indexNumber }
                .reversed()
                .filter { it.content.isEmpty() || it.content.isNotBlank() }
                .forEach { token: MarkedUpToken ->
                    var content = token.content.replace("\n".toRegex(), " ")
                        .replace(" +".toRegex(), whitespaceCharacter)
                    val parentXPath = token.parentXPath
                    when {
                        parentXPath.endsWith("/del/add") -> content = "[-+] $content"
                        parentXPath.endsWith("/add") -> content = "[+] $content"
                        parentXPath.endsWith("/del") -> content = "[-] $content"
                        parentXPath.endsWith("/$INSTANT_DEL_XPATH_NODE") -> content = "[-] $content"
                        parentXPath.endsWith("/$SEQ0_DEL_XPATH_NODE") -> content = "[-] $content"
                        parentXPath.endsWith("/$TYPE_IMMEDIATE_DEL_XPATH_NODE") -> content = "[-] $content"
                        //        String layerName = determineLayerName(parentXPath);
                    }
                    if (parentXPath.contains("/rdg")) {
                        val rdg = token.rdg
                        content = "<$rdg> $content".replace(">\\s+\\[".toRegex(), ">[")
                    }
                    if (content.isEmpty()) {
                        content = "<" + parentXPath.replace(".*/".toRegex(), "") + "/>"
                    }
                    //        String layerName = determineLayerName(parentXPath);
                    cell.addLayer(content)
                }
        }
        return cell
    }

    private fun determineLayerName(parentXPath: String): String {
        var layerName = ""
        if (parentXPath.endsWith("/add")) {
            layerName = "add"
        }
        if (parentXPath.endsWith("/del")) {
            layerName = "del"
        }
        return layerName
    }

    private fun Cell.addLayer(content: String) {
        layerContent += content
    }

    private fun asciiTable(
        sigla: List<String>,
        rowMap: Map<String, MutableList<Cell>>,
        cellHeights: Map<String, Int>
    ): AsciiTable {
        val table = AsciiTable()
        table.renderer.cwc = CWC_LongestLine()
        table.addRule()
        sigla.forEach { siglum: String ->
            val row = (rowMap[siglum] ?: error("rowMap[$siglum] == null"))
                .map { cell: Cell -> cell.toASCII(cellHeights[siglum] ?: error("cellHeights[$siglum] == null")) }
                .toMutableList()

            row.add(0, "[$siglum]")
            table.addRow(row)
            table.addRule()
        }
        table.setTextAlignment(TextAlignment.LEFT)
        return table
    }

    private fun Cell.toASCII(cellHeight: Int): String {
        val contentBuilder = StringBuilder()
        // ASCIITable has no TextAlignment.BOTTOM option, so add empty lines manually
        val emptyLinesToAdd = cellHeight - layerContent.size
        for (i in 0 until emptyLinesToAdd) {
            contentBuilder.append("$NBSP<br>") // regular space or just <br> leads to ASCIITable error when rendering
        }
        val joiner = StringJoiner("<br>")
        for (s in layerContent) {
            joiner.add(s)
        }
        val content = joiner.toString()
        return contentBuilder.append(content).toString()
    }

    @JvmStatic
    fun toTableHTML(graph: CollationGraph, emphasizeWhitespace: Boolean): String {
        val sigla = graph.sigla
        val whitespaceCharacter = if (emphasizeWhitespace) "_" else "&nbsp;"
        val ranking = CollationGraphRanking.of(graph)
        val cells: Map<String, MutableList<String>> = sigla.map { it to mutableListOf<String>() }.toMap()
        for (nodeSet in ranking) {
            val colorDispenser = ColorDispenser()
            val tokenBackground: MutableMap<MarkedUpToken, String> = mutableMapOf()
            if (nodeSet.isBorderNode(graph)) {
                // skip start and end nodes
                continue
            }
            val nodeTokensPerWitness: MutableMap<String, MutableList<MarkedUpToken>> = HashMap()
            sigla.forEach { siglum: String ->
                nodeTokensPerWitness[siglum] = ArrayList()
                nodeSet.sortedBy { it.hashCode() }.forEach { node: TextNode ->
                    val isMatch = node.sigla.size > 1
                    val inTextVariation =
                        node.sigla
                            .map { node.tokenForSiglum(it) as MarkedUpToken }
                            .map { it.parentXPath }
                            .any { it.isTextVariationXPath() }
                    val token = node.tokenForSiglum(siglum)
                    if (token != null) {
                        val mToken = token as MarkedUpToken
                        nodeTokensPerWitness[siglum]!!.add(mToken)
                        if (isMatch && inTextVariation) {
                            tokenBackground[mToken] = colorDispenser.dispenseFor(mToken)
                        }

                    }
                }
            }
            for (siglum: String in sigla) {
                val groupedByParentXPath = nodeTokensPerWitness[siglum]!!
                    .sortedBy { it.indexNumber }
                    .groupBy {
                        it.parentXPath.replace("/$INSTANT_DEL_XPATH_NODE", "")
                            .replace("/$TYPE_IMMEDIATE_DEL_XPATH_NODE", "")
                            .replace("/$SEQ0_DEL_XPATH_NODE", "")
                    } // since instant deletions should be on the same line as the text after
                cells[siglum]!! += when {
                    groupedByParentXPath.size == 1 ->
                        groupedByParentXPath.values
                            .flatten()
                            .tokenListToHtml(whitespaceCharacter, tokenBackground)

                    groupedByParentXPath.size > 1 -> { // we have textual variation
                        val keys = groupedByParentXPath.keys.toList()
                        val firstKey = keys[0]

                        when {
                            "/rdg/" in firstKey -> // for readings, first reading goes first
                                keys.map { groupedByParentXPath[it]!! }
                                    .toHtml(whitespaceCharacter, tokenBackground)
                            else ->
                                keys.reversed() // for subst, the dels should go last
                                    .map { groupedByParentXPath[it]!! }
                                    .toHtml(whitespaceCharacter, tokenBackground)
                        }
                    }
                    else -> ""
                }
            }
        }

        val rows: String = graph.sigla.joinToString("\n") { witnessRow(it, cells[it] ?: error("")) }
        return """<table border="1">
            $rows
            </table>""".trimIndent()
    }

    private fun List<List<MarkedUpToken>>.toHtml(
        whitespaceCharacter: String,
        tokenBackground: Map<MarkedUpToken, String>
    ): String =
        joinToString("<br/>") { it.tokenListToHtml(whitespaceCharacter, tokenBackground) }

    private fun List<MarkedUpToken>.tokenListToHtml(
        whitespaceCharacter: String,
        tokenBackground: Map<MarkedUpToken, String>
    ): String =
        this.filter { it.content.isEmpty() || it.content.isNotBlank() }
            .joinToString("&nbsp;") { it.toHtml(whitespaceCharacter, tokenBackground[it]) }

    private fun MarkedUpToken.toHtml(whitespaceCharacter: String, bgColor: String?): String {
        var asHtml = content.replace(" ", whitespaceCharacter)
        if (parentXPath.contains("app/rdg")) {
            asHtml += "<br/>"
        }
        if (bgColor != null) {
            asHtml = """<span style="background-color:$bgColor">$asHtml</span>"""
        }
        return when {
            parentXPath.endsWith("/add") -> asHtml
            parentXPath.contains("/del") -> "<del>$asHtml</del>"
            else -> asHtml
        }
    }

    private fun witnessRow(siglum: String, cells: List<String>): String =
        "<tr><th style=\"background:lightgreen\">$siglum</th>${cells.joinToString("") { "<td>$it</td>" }}</tr>"

    @JvmStatic
    fun toDot(
        graph: CollationGraph,
        emphasizeWhitespace: Boolean,
        hideMarkup: Boolean,
        horizontal: Boolean = false
    ): String =
        DotFactory(emphasizeWhitespace).fromCollationGraph(graph, hideMarkup, horizontal)

    private fun String.isTextVariationXPath(): Boolean =
        this.contains("/subst/")
                || this.endsWith("/add")
                || this.endsWith("/del")
                || this.contains("/app/")

    class Cell {
        val layerContent: MutableList<String> = ArrayList()

        constructor(content: String) {
            layerContent += content
        }

        internal constructor()

    }
}

