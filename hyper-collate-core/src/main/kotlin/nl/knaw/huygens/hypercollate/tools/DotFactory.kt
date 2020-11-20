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

import nl.knaw.huygens.hypercollate.model.*
import java.lang.String.format
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Comparator

class DotFactory(emphasizeWhitespace: Boolean) {
    private val whitespaceCharacter: String = if (emphasizeWhitespace) "&#9251;" else "&nbsp;"

    /**
     * Generates a .dot format string for visualizing a variant witness graph.
     *
     * @param graph The variant witness graph for which we are generating a dot file.
     * @return A string containing the contents of a .dot representation of the variant witness graph.
     */
    fun fromVariantWitnessGraphColored(graph: VariantWitnessGraph): String {
        val dotBuilder = StringBuilder("digraph VariantWitnessGraph{\n")
                .append("graph [rankdir=LR]\n")
                .append("node [style=\"filled\";fillcolor=\"white\"]\n")
        val edges: MutableList<String> = ArrayList()
        val openMarkup: MutableSet<Markup> = HashSet()
        val clusterCounter = AtomicInteger()
        val colorContext = ColorContext()
        for (tokenVertex in graph.vertices()) {
            val markupListForTokenVertex = graph.markupListForTokenVertex(tokenVertex)
            val opened: MutableSet<Markup> = HashSet()
            opened.addAll(openMarkup)

            val markupToClose: MutableList<Markup> = ArrayList()
            markupToClose.addAll(opened)
            markupToClose.removeAll(markupListForTokenVertex)
            markupToClose.sortWith(Comparator.comparingInt { obj: Markup -> obj.depth })
            repeat(markupToClose.size) { closeMarkup(dotBuilder) }

            val markupToOpen: MutableList<Markup> = ArrayList()
            markupToOpen.addAll(markupListForTokenVertex)
            markupToOpen.removeAll(opened)
            markupToOpen.sortWith(Comparator.comparingInt { obj: Markup -> obj.depth })
            markupToOpen.forEach { m: Markup -> openMarkup(m, dotBuilder, clusterCounter.getAndIncrement(), colorContext) }

            openMarkup.removeAll(markupToClose)
            openMarkup.addAll(markupToOpen)
            val tokenVariable = tokenVertex.vertexVariable()
            if (tokenVertex is SimpleTokenVertex) {
                dotBuilder
                        .append(tokenVariable)
                        .append(" [label=<")
                        .append(tokenVertex.content.asLabel(whitespaceCharacter))
                        .append(">]\n")
            } else {
                dotBuilder.append(tokenVariable).append(" [label=\"\";shape=doublecircle,rank=middle]\n")
            }
            tokenVertex
                    .outgoingTokenVertexList
                    .forEach { ot: TokenVertex ->
                        val vertexVariable = ot.vertexVariable()
                        edges += "$tokenVariable->$vertexVariable"
                    }
        }
        edges.sorted().forEach { dotBuilder.append(it).append("\n") }
        dotBuilder.append("}")
        return dotBuilder.toString()
    }

    private fun openMarkup(
            m: Markup,
            dotBuilder: StringBuilder,
            clusterNum: Int,
            colorContext: ColorContext
    ) {
        val color = colorContext.colorFor(m.tagName)
        dotBuilder
                .append("subgraph cluster_")
                .append(clusterNum)
                .append(" {\n")
                .append("label=<<i><b>")
                .append(m.tagName)
                .append("</b></i>>\n")
                .append("graph[style=\"rounded,filled\";fillcolor=\"")
                .append(color)
                .append("\"]\n")
    }

    private fun closeMarkup(dotBuilder: StringBuilder) {
        dotBuilder.append("}\n")
    }

    fun fromVariantWitnessGraphSimple(graph: VariantWitnessGraph): String {
        val dotBuilder = StringBuilder("digraph VariantWitnessGraph{\ngraph [rankdir=LR]\nlabelloc=b\n")
        val edges: MutableList<String> = ArrayList()
        val nextTokens: Deque<TokenVertex> = LinkedList()
        nextTokens += graph.startTokenVertex
        val verticesDone: MutableSet<TokenVertex> = HashSet()
        while (!nextTokens.isEmpty()) {
            val tokenVertex = nextTokens.pop()
            if (tokenVertex !in verticesDone) {
                val tokenVariable = tokenVertex.vertexVariable()
                if (tokenVertex is SimpleTokenVertex) {
                    val markup = graph.sigil + ": " + tokenVertex.parentXPath
                    dotBuilder
                            .append(tokenVariable)
                            .append(" [label=<")
                            .append(tokenVertex.content.asLabel(whitespaceCharacter))
                            .append("<br/><i>")
                            .append(markup)
                            .append("</i>")
                            .append(">]\n")
                } else {
                    dotBuilder.append(tokenVariable).append(" [label=\"\";shape=doublecircle,rank=middle]\n")
                }
                tokenVertex
                        .outgoingTokenVertexList
                        .forEach { ot: TokenVertex ->
                            val vertexVariable = ot.vertexVariable()
                            edges += "$tokenVariable->$vertexVariable"
                            nextTokens += ot
                        }
                verticesDone += tokenVertex
            }
        }
        edges.sorted().forEach { dotBuilder.append(it).append("\n") }
        dotBuilder.append("}")
        return dotBuilder.toString()
    }

    private fun TokenVertex.vertexVariable(): String? =
            when (this) {
                is SimpleTokenVertex -> {
                    val token = token
                    "v${token.witness.sigil}_${format("%03d", token.indexNumber)}"
                }
                is StartTokenVertex -> "begin"
                is EndTokenVertex -> "end"
                else -> null
            }

    class TextNodeComparator : Comparator<TextNode> {
        override fun compare(o1: TextNode?, o2: TextNode?): Int =
                if (o1 == null || o2 == null) 0
                else o1.toString().compareTo(o2.toString())
    }

    fun fromCollationGraph(collation: CollationGraph, hideMarkup: Boolean): String {
        val dotBuilder = StringBuilder("digraph CollationGraph{\nlabelloc=b\n")
        val nodeIdentifiers: MutableMap<TextNode, String> = HashMap()
        val nodes = collation.traverseTextNodes().sortedWith(TextNodeComparator())
        for (i in nodes.indices) {
            val node = nodes[i]
            val nodeId = "t" + String.format("%03d", i)
            nodeIdentifiers[node] = nodeId
            dotBuilder.appendNodeLine(node, nodeId, hideMarkup)
        }
        dotBuilder.appendEdgeLines(collation, nodeIdentifiers, nodes)
        dotBuilder.append("}")
        return dotBuilder.toString()
    }

    private fun StringBuilder.appendNodeLine(
            node: TextNode,
            nodeId: String,
            hideMarkup: Boolean
    ) {
        val labelString = node.generateNodeLabel(hideMarkup)
        if (labelString.isEmpty()) {
            append(nodeId).append(" [label=\"\";shape=doublecircle,rank=middle]\n")
        } else {
            append(nodeId)
                    .append(" [label=<").append(labelString).append(">")
                    .append(node.penWidthParameter()).append("]\n")
        }
    }

    private fun TextNode.penWidthParameter(): String =
            sigils.penWidthParameter()

    private fun TextEdge.penWidthParameter(): String =
            sigils.penWidthParameter()

    private fun Set<String>.penWidthParameter() =
            if (size > 1) ";penwidth=2" else ""

    data class NodeLabels(val content: String, val xpath: String, val sigilExtension: String)

    private fun TextNode.generateNodeLabel(hideMarkup: Boolean): String {
        val label = StringBuilder()
        val sortedSigils = sigils.sorted()
        val nodeLabelMap: Map<String, NodeLabels> = sortedSigils.generateNodeLabelsForTextNode(this)
        val joinedSigils = sortedSigils.joinToString(",")
        label.appendContent(sortedSigils, nodeLabelMap)
        if (!hideMarkup) {
            if (label.isNotEmpty()) {
                label.append("<br/>")
            }
            label.appendMarkup(sortedSigils, nodeLabelMap, joinedSigils)
        }
        return label.toString()
    }

    private fun StringBuilder.appendEdgeLines(
            collation: CollationGraph,
            nodeIdentifiers: Map<TextNode, String>,
            nodes: List<TextNode>
    ) {
        val edgeLines: MutableSet<String> = TreeSet()
        for (node in nodes) {
            collation
                    .getIncomingTextEdgeList(node)
                    .forEach { e: TextEdge ->
                        val source: TextNode = collation.getSource(e) as TextNode
                        val sourceSigilExtensionMap = e.sigils
                                .map { it to (source.tokenForSigil(it) as MarkedUpToken?)?.parentXPath?.branchId() }
                                .toMap()
                        val target: TextNode = collation.getTarget(e)
                        val targetSigilExtensionMap = e.sigils
                                .map { it to (target.tokenForSigil(it) as MarkedUpToken?)?.parentXPath?.branchId() }
                                .toMap()
                        val edgeLabel = e.sigils.sorted().joinToString(",") { sigil: String ->
                            val longestBranchId = listOfNotNull(sourceSigilExtensionMap[sigil], targetSigilExtensionMap[sigil])
                                    .maxByOrNull { s -> s.length }
                            if (longestBranchId.isNullOrEmpty()) {
                                sigil
                            } else {
                                "$sigil<sup>$longestBranchId</sup>"
                            }
                        }
                        edgeLines += "${nodeIdentifiers[source]}->${nodeIdentifiers[target]}[label=<$edgeLabel>${e.penWidthParameter()}]\n"
                    }
        }
        edgeLines.forEach { str: String? -> append(str) }
    }

    private fun List<String>.generateNodeLabelsForTextNode(
            node: TextNode
    ): Map<String, NodeLabels> {
        val map: MutableMap<String, NodeLabels> = mutableMapOf()
        forEach { s: String ->
            val token = node.tokenForSigil(s)
            if (token != null) {
                val mToken = token as MarkedUpToken
                val xpath = mToken.parentXPath
                map[s] = NodeLabels(mToken.content.asLabel(whitespaceCharacter), xpath, xpath.branchId())
            }
        }
        return map
    }

    private fun StringBuilder.appendContent(
            sortedSigils: List<String>,
            nodeLabelMap: Map<String, NodeLabels>
    ) {
        val contentLabelSet: Set<String> = nodeLabelMap.values.map { it.content }.toSet()
        if (contentLabelSet.size == 1) {
            val sigils = sortedSigils.joinToString(",") { extendedSigil(it, nodeLabelMap) }
            append(sigils)
            append(": ").append(contentLabelSet.iterator().next())
        } else {
            val witnessLines = sortedSigils.joinToString("<br/>") { sigil ->
                val eSigil = extendedSigil(sigil, nodeLabelMap)
                "$eSigil: ${nodeLabelMap[sigil]?.content}"
            }
            append(witnessLines)
        }
    }

    private fun extendedSigil(sigil: String, nodeLabelMap: Map<String, NodeLabels>): String {
        val sigilExtension = nodeLabelMap[sigil]?.sigilExtension
        val sup = if (sigilExtension.isNullOrEmpty()) "" else "<sup>${sigilExtension}</sup>"
        return "$sigil$sup"
    }

    private fun StringBuilder.appendMarkup(
            sortedSigils: List<String>,
            nodeLabelMap: Map<String, NodeLabels>,
            joinedSigils: String
    ) {
        val xpathSet: Set<String> = nodeLabelMap.values.map { it.xpath }.toSet()
        if (xpathSet.size == 1) {
            append(joinedSigils)
                    .append(": <i>")
                    .append(xpathSet.iterator().next())
                    .append("</i>")
        } else {
            sortedSigils.forEach { s: String -> append(s).append(": <i>").append(nodeLabelMap[s]?.xpath).append("</i><br/>") }
        }
    }

    private fun String.asLabel(whitespaceCharacter: String): String =
            replace("&".toRegex(), "&amp;")
                    .replace("\n".toRegex(), "&#x21A9;<br/>")
                    .replace(" +".toRegex(), whitespaceCharacter)

    companion object {
        fun String.branchId(): String =
                this.split("/")
                        .filter { it.startsWith("rdg[") || it == "del" || it == "add" }
                        .joinToString("") { tag ->
                            when {
                                tag.startsWith("rdg[") -> tag.replace("^.*?'".toRegex(), "").replace("'.*$".toRegex(), "")
                                tag == "add" -> "+"
                                tag == "del" -> "-"
                                else -> ""
                            }
                        }
    }

}
