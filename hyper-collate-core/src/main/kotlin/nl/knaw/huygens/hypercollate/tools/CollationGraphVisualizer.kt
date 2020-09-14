package nl.knaw.huygens.hypercollate.tools

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

import de.vandermeer.asciitable.AsciiTable
import de.vandermeer.asciitable.CWC_LongestLine
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment
import nl.knaw.huygens.hypercollate.model.CollationGraph
import nl.knaw.huygens.hypercollate.model.MarkedUpToken
import nl.knaw.huygens.hypercollate.model.TextNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.math.max

object CollationGraphVisualizer {
    private const val NBSP = "\u00A0"
    val LOG: Logger = LoggerFactory.getLogger(this.javaClass)

    @JvmStatic
    fun toTableASCII(graph: CollationGraph, emphasizeWhitespace: Boolean): String {
        val sigils = graph.sigils
        val whitespaceCharacter = if (emphasizeWhitespace) "_" else " "
        val rowMap: MutableMap<String, MutableList<Cell>> = HashMap()
        sigils.forEach { sigil: String -> rowMap[sigil] = ArrayList() }
        val ranking = CollationGraphRanking.of(graph)
        LOG.info("ranking size={}", ranking.size)
        val maxLayers: MutableMap<String, Int> = HashMap()
        sigils.forEach { sigil: String -> maxLayers[sigil] = 1 }
        for (nodeSet in ranking) {
            if (isBorderNode(nodeSet, graph)) {
                // skip start and end nodes
                continue
            }
            val nodeTokensPerWitness: MutableMap<String, MutableList<MarkedUpToken>> = HashMap()
            sigils.forEach { sigil: String ->
                nodeTokensPerWitness[sigil] = ArrayList()
                nodeSet.forEach { node: TextNode ->
                    val token = node.getTokenForWitness(sigil)
                    if (token != null) {
                        val mToken = token as MarkedUpToken
                        nodeTokensPerWitness[sigil]!!.add(mToken)
                    }
                }
            }
            sigils.forEach { sigil: String ->
                val tokens: List<MarkedUpToken> = nodeTokensPerWitness[sigil]!!
                maxLayers[sigil] = max(maxLayers[sigil]!!, tokens.size)
                val cell = newCell(tokens, whitespaceCharacter)
                rowMap[sigil]!!.add(cell)
            }
        }
        return asciiTable(graph.sigils, rowMap, maxLayers).render()
    }

    private fun isBorderNode(nodeSet: Set<TextNode>, graph: CollationGraph): Boolean {
        if (nodeSet.size != 1) {
            return false
        }
        val node = nodeSet.iterator().next()
        val hasNoIncomingEdges = graph.getIncomingEdges(node).isEmpty()
        val hasNoOutgoingEdges = graph.getOutgoingEdges(node).isEmpty()
        return hasNoIncomingEdges || hasNoOutgoingEdges
    }

    private fun newCell(tokens: List<MarkedUpToken>, whitespaceCharacter: String): Cell {
        val cell = Cell()
        if (tokens.isEmpty()) {
            setCellLayer(cell, " ")
        } else {
            tokens.forEach { token: MarkedUpToken ->
                var content = token.content.replace("\n".toRegex(), " ").replace(" +".toRegex(), whitespaceCharacter)
                val parentXPath = token.parentXPath
                when {
                    parentXPath.endsWith("/del/add") -> content = "[za] $content"
                    parentXPath.endsWith("/add") -> content = "[z] $content"
                    parentXPath.endsWith("/del") -> content = "[a] $content"
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
                setCellLayer(cell, content)
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

    private fun setCellLayer(cell: Cell, content: String) {
        cell.layerContent += content
        //    String previousContent = cell.getLayerContent().put(layerName, content);
        //    Preconditions.checkState(previousContent == null, "layerName " + layerName + " used
        // twice!");
    }

    private fun asciiTable(
            sigils: List<String>,
            rowMap: Map<String, MutableList<Cell>>,
            cellHeights: Map<String, Int>
    ): AsciiTable {
        val table = AsciiTable().setTextAlignment(TextAlignment.LEFT)
        val cwc = CWC_LongestLine()
        table.renderer.cwc = cwc
        table.addRule()
        sigils.forEach { sigil: String ->
            val row = (rowMap[sigil] ?: error("rowMap[$sigil] == null"))
                    .map { cell: Cell -> toASCII(cell, cellHeights[sigil] ?: error("cellHeights[$sigil] == null")) }
                    .toMutableList()

            row.add(0, "[$sigil]")
            table.addRow(row)
            table.addRule()
        }
        return table
    }

    private fun toASCII(cell: Cell, cellHeight: Int): String {
        val contentBuilder = StringBuilder()
        // ASCIITable has no TextAlignment.BOTTOM option, so add empty lines manually
        val emptyLinesToAdd = cellHeight - cell.layerContent.size
        for (i in 0 until emptyLinesToAdd) {
            contentBuilder.append(
                    "$NBSP<br>") // regular space or just <br> leads to ASCIITable error when rendering
        }
        val layerContent: List<String> = cell.layerContent.sorted().reversed()
        val joiner = StringJoiner("<br>")
        for (s in layerContent) {
            joiner.add(
                    s.replace("\\[z]".toRegex(), "[+]")
                            .replace("\\[a]".toRegex(), "[-]")
                            .replace("\\[za]".toRegex(), "[+-]"))
        }
        val content = joiner.toString()
        return contentBuilder.append(content).toString()
    }

    fun toTableHTML(graph: CollationGraph): String {
        val sigils = graph.sigils
        val ranking = CollationGraphRanking.of(graph)
        val cells: Map<String, MutableList<String>> = sigils.map { it to mutableListOf<String>() }.toMap()
        for (nodeSet in ranking) {
            val matchingTokens = mutableListOf<MarkedUpToken>()
            if (isBorderNode(nodeSet, graph)) {
                // skip start and end nodes
                continue
            }
            val nodeTokensPerWitness: MutableMap<String, MutableList<MarkedUpToken>> = HashMap()
            sigils.forEach { sigil: String ->
                nodeTokensPerWitness[sigil] = ArrayList()
                nodeSet.forEach { node: TextNode ->
                    val isMatch = node.sigils.size > 1
                    val token = node.getTokenForWitness(sigil)
                    if (token != null) {
                        val mToken = token as MarkedUpToken
                        nodeTokensPerWitness[sigil]!!.add(mToken)
                        if (isMatch) {
                            matchingTokens += mToken
                        }
                    }
                }
            }
            for (sigil: String in sigils) {
                cells[sigil]!! += nodeTokensPerWitness[sigil]!!
                        .sortedBy { it.indexNumber }
                        .joinToString("&nbsp;") {
                            var asHtml = it.content.replace(" ", "&nbsp;")
                            if (it.parentXPath.contains("app/rdg")) {
                                asHtml += "<br/>"
                            }
                            if (it in matchingTokens) {
                                asHtml = "<span style=\"background:orange\">$asHtml</span>"
                            }
                            when {
                                it.parentXPath.endsWith("/del/add") -> "<sup><sup>$asHtml</sup></sup>"
                                it.parentXPath.endsWith("/add/del") -> "<sup><del>$asHtml</del></sup>"
                                it.parentXPath.endsWith("/add") -> "<sup>$asHtml</sup>"
                                it.parentXPath.endsWith("/del") -> "<del>$asHtml</del>"
                                else -> asHtml
                            }
                        }
            }
        }

        val rows: String = graph.sigils.joinToString("\n") { witnessRow(it, cells[it]!!) }
        return """<table border="1">
            $rows
            </table>""".trimIndent()
    }

    private fun witnessRow(sigil: String, cells: List<String>): String =
            "<tr><th style=\"background:lightgreen\">$sigil</th>${cells.joinToString("") { "<td>$it</td>" }}</tr>"

    @JvmStatic
    fun toDot(
            graph: CollationGraph,
            emphasizeWhitespace: Boolean,
            hideMarkup: Boolean
    ): String =
            DotFactory(emphasizeWhitespace).fromCollationGraph(graph, hideMarkup)

    class Cell {
        val layerContent: MutableList<String> = ArrayList()

        constructor(content: String) {
            layerContent += content
        }

        internal constructor()

    }
}
