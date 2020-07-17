package nl.knaw.huygens.hypercollate.model

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

import com.google.common.base.Preconditions
import eu.interedition.collatex.Token
import nl.knaw.huygens.hypergraph.core.Hypergraph
import org.slf4j.LoggerFactory
import java.util.*
import java.util.stream.Stream

class CollationGraph
//    textStartNode.setSigils(sigils);
//    textEndNode.setSigils(sigils);
@JvmOverloads constructor(val sigils: MutableList<String> = ArrayList()) : Hypergraph<Node, Edge>(GraphType.ORDERED) {
    val textStartNode = TextDelimiterNode()
    val textEndNode = TextDelimiterNode()

    private val markupNodeIndex: MutableMap<Markup, MarkupNode> = HashMap()

    fun addTextNodeWithTokens(vararg tokens: Token): TextNode {
        val newNode = TextNode(*tokens)
        addNode(newNode, TextNode.LABEL)
        return newNode
    }

    fun addMarkupNode(sigil: String?, markup: Markup): MarkupNode {
        val newNode = MarkupNode(sigil!!, markup)
        addNode(newNode, MarkupNode.LABEL)
        markupNodeIndex[markup] = newNode
        return newNode
    }

    fun linkMarkupToText(markupNode: MarkupNode, textNode: TextNode) {
        val markupHyperEdges = getOutgoingEdges(markupNode)
                .filterIsInstance<MarkupHyperEdge>()

        if (markupHyperEdges.isEmpty()) {
            val newEdge = MarkupHyperEdge()
            addDirectedHyperEdge(newEdge, MarkupHyperEdge.LABEL, markupNode, textNode)
        } else {
            if (markupHyperEdges.size != 1) {
                throw RuntimeException(
                        "MarkupNode "
                                + markupNode
                                + " should have exactly 1 MarkupHyperEdge, but has "
                                + markupHyperEdges.size)
            }
            val edge = markupHyperEdges[0]
            addTargetsToHyperEdge(edge, textNode)
        }
    }

    val isEmpty: Boolean
        get() = sigils.isEmpty()

    fun getTarget(edge: TextEdge): TextNode {
        val nodes = getTargets(edge) ?: error("getTargets(edge) == null")
        if (nodes.size != 1) {
            throw RuntimeException("trouble!")
        }
        return nodes.iterator().next() as TextNode
    }

    fun addDirectedEdge(source: Node, target: Node, sigils: Set<String>) {
        val edge = TextEdge(sigils.toMutableSet())
        super.addDirectedHyperEdge(edge, TextEdge.LABEL, source, target)
    }

    fun getOutgoingTextEdgeStream(source: Node): Stream<TextEdge> =
            getOutgoingEdges(source)
                    .filterIsInstance<TextEdge>()
                    .stream()

    fun traverseTextNodes(): List<TextNode> {
        val visitedNodes: MutableSet<Node> = HashSet()
        val nodesToVisit = Stack<Node>()
        nodesToVisit.add(textStartNode)
        val result: MutableList<TextNode> = ArrayList()
        while (!nodesToVisit.isEmpty()) {
            val pop = nodesToVisit.pop()
            if (!visitedNodes.contains(pop)) {
                if (pop is TextNode) {
                    result.add(pop)
                }
                visitedNodes.add(pop)
                getOutgoingTextEdgeStream(pop)
                        .forEach { e: TextEdge ->
                            val target = getTarget(e)
                            nodesToVisit.add(target)
                        }
            } else {
                LOG.debug("revisiting node {}", pop)
            }
        }
        return result
    }

    fun getIncomingTextEdgeStream(node: TextNode): Stream<TextEdge> =
            getIncomingEdges(node).filterIsInstance<TextEdge>().stream()

    val markupStream: Stream<Markup>
        get() = markupNodeIndex.keys.stream()

    val markupNodeStream: Stream<MarkupNode>
        get() = markupNodeIndex.values.stream()

    fun getTextNodeStreamForMarkup(markup: Markup): Stream<TextNode> {
        val originalMarkupNode = getMarkupNode(markup)
        val markupHyperEdges = getOutgoingEdges(originalMarkupNode)
                .filterIsInstance<MarkupHyperEdge>()
        Preconditions.checkArgument(markupHyperEdges.size == 1)
        return getTargets(markupHyperEdges[0])!!
                .filterIsInstance<TextNode>()
                .stream()
    }

    fun getMarkupNode(markup: Markup): MarkupNode {
        return getMarkupNode(markupNodeIndex[markup]!!)
    }

    fun getMarkupNodeStreamForTextNode(textNode: TextNode): Stream<MarkupNode> =
            getIncomingEdges(textNode)
                    .filterIsInstance<MarkupHyperEdge>()
                    .map { getSource(it) }
                    .filterIsInstance<MarkupNode>()
                    .stream()

    private fun getMarkupNode(markupNode: Node): MarkupNode =
            markupNode as MarkupNode

    companion object {
        private val LOG = LoggerFactory.getLogger(CollationGraph::class.java)
    }

}
