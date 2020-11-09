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
import java.util.stream.Stream

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
                    .outgoingTokenVertexStream
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
                        .outgoingTokenVertexStream
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
                    val token = getToken() as MarkedUpToken
                    "v${token.witness.sigil}_${format("%03d", token.indexNumber)}"
                }
                is StartTokenVertex -> {
                    "begin"
                }
                is EndTokenVertex -> {
                    "end"
                }
                else -> null
            }

    private val byNode = Comparator.comparing(TextNode::toString)

    fun fromCollationGraph(collation: CollationGraph, hideMarkup: Boolean): String {
        val dotBuilder = StringBuilder("digraph CollationGraph{\nlabelloc=b\n")
        val nodeIdentifiers: MutableMap<TextNode, String> = HashMap()
        val nodes = collation.traverseTextNodes().sortedWith(byNode)
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

    private fun StringBuilder.appendEdgeLines(
            collation: CollationGraph,
            nodeIdentifiers: Map<TextNode, String>,
            nodes: List<TextNode>
    ) {
        val edgeLines: MutableSet<String> = TreeSet()
        for (node in nodes) {
            collation
                    .getIncomingTextEdgeStream(node)
                    .forEach { e: TextEdge ->
                        val source = collation.getSource(e)
                        val target: Node = collation.getTarget(e)
                        val edgeLabel = e.sigils.sorted().joinToString(",") { it.extendedSigil(collation.getMarkupNodeStreamForTextNode(node)) }
                        edgeLines += "${nodeIdentifiers[source]}->${nodeIdentifiers[target]}[label=\"$edgeLabel\"${e.penWidthParameter()}]\n"
                    }
        }
        edgeLines.forEach { str: String? -> append(str) }
    }

    private fun String.extendedSigil(markupNodeStream: Stream<MarkupNode>): String {
        val parentMarkupNode = markupNodeStream
                .filter { it.sigil == this }
                .sorted { n1, n2 -> n2.markup.depth.compareTo(n1.markup.depth) }
                .findFirst()
        if (!parentMarkupNode.isPresent) {
            return this
        }
        val parentMarkup = parentMarkupNode.get().markup
        val extension = when (parentMarkup.tagName) {
            "add" -> "+"
            "del" -> "-"
            else -> null
        }
        return if (extension == null)
            this
        else
//            "${this}<sup>$extension</sup>"
            this

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

    private fun TextNode.generateNodeLabel(hideMarkup: Boolean): String {
        val label = StringBuilder()
        val sortedSigils = sigils.sorted()
        val contentLabel: MutableMap<String, String> = HashMap()
        val markupLabel: MutableMap<String, String> = HashMap()
        prepare(sortedSigils, this, contentLabel, markupLabel)
        val joinedSigils = sortedSigils.joinToString(",")
        label.appendContent(contentLabel, sortedSigils, joinedSigils)
        if (!hideMarkup) {
            if (label.isNotEmpty()) {
                label.append("<br/>")
            }
            label.appendMarkup(markupLabel, sortedSigils, joinedSigils)
        }
        return label.toString()
    }

    private fun prepare(
            sortedSigils: List<String>,
            node: TextNode,
            contentLabel: MutableMap<String, String>,
            markupLabel: MutableMap<String, String>
    ) {
        sortedSigils.forEach { s: String ->
            val token = node.getTokenForWitness(s)
            if (token != null) {
                val mToken = token as MarkedUpToken
                val markup = mToken.parentXPath
                contentLabel[s] = mToken.content.asLabel(whitespaceCharacter)
                markupLabel[s] = markup
            }
        }
    }

    private fun StringBuilder.appendMarkup(
            markupLabel: Map<String, String>,
            sortedSigils: List<String>,
            joinedSigils: String
    ) {
        val markupLabelSet: Set<String> = markupLabel.values.toSet()
        if (markupLabelSet.size == 1) {
            append(joinedSigils)
                    .append(": <i>")
                    .append(markupLabelSet.iterator().next())
                    .append("</i>")
        } else {
            sortedSigils.forEach { s: String -> append(s).append(": <i>").append(markupLabel[s]).append("</i><br/>") }
        }
    }

    private fun StringBuilder.appendContent(
            contentLabel: Map<String, String>,
            sortedSigils: List<String>,
            joinedSigils: String
    ) {
        val contentLabelSet: Set<String> = contentLabel.values.toSet()
        if (contentLabelSet.size == 1) {
            append(joinedSigils).append(": ").append(contentLabelSet.iterator().next())
        } else {
            val witnessLines = sortedSigils.joinToString("<br/>") { "$it: ${contentLabel[it]}" }
            append(witnessLines)
        }
    }

    private fun String.asLabel(whitespaceCharacter: String): String =
            replace("&".toRegex(), "&amp;")
                    .replace("\n".toRegex(), "&#x21A9;<br/>")
                    .replace(" +".toRegex(), whitespaceCharacter)

}
