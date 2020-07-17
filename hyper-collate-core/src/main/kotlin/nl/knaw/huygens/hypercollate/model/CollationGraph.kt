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
import nl.knaw.huygens.hypercollate.model.MarkupHyperEdge
import nl.knaw.huygens.hypercollate.model.MarkupNode
import nl.knaw.huygens.hypercollate.model.TextEdge
import nl.knaw.huygens.hypergraph.core.Hypergraph
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.Function
import java.util.function.Predicate
import java.util.stream.Collectors
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

    fun linkMarkupToText(markupNode: MarkupNode, textNode: TextNode?) {
        val markupHyperEdges = getOutgoingEdges(markupNode).stream()
                .filter(Predicate<Edge> { obj: Edge? -> MarkupHyperEdge::class.java.isInstance(obj) })
                .map(Function<Edge, MarkupHyperEdge> { obj: Edge? -> MarkupHyperEdge::class.java.cast(obj) })
                .collect(Collectors.toList())
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
        val nodes = getTargets(edge)
        if (nodes.size != 1) {
            throw RuntimeException("trouble!")
        }
        return nodes.iterator().next() as TextNode
    }

    fun addDirectedEdge(source: Node, target: Node, sigils: Set<String>) {
        val edge = TextEdge(sigils.toMutableSet())
        super.addDirectedHyperEdge(edge, TextEdge.LABEL, source, target)
    }

    fun getOutgoingTextEdgeStream(source: Node?): Stream<TextEdge> {
        return getOutgoingEdges(source).stream()
                .filter(Predicate<Edge> { obj: Edge? -> TextEdge::class.java.isInstance(obj) })
                .map(Function<Edge, TextEdge> { obj: Edge? -> TextEdge::class.java.cast(obj) })
    }

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
                            val target = getTarget(e) ?: throw RuntimeException("edge target is null for edge $pop->")
                            nodesToVisit.add(target)
                        }
            } else {
                LOG.debug("revisiting node {}", pop)
            }
        }
        return result
    }

    fun getIncomingTextEdgeStream(node: TextNode?): Stream<TextEdge> {
        return getIncomingEdges(node).stream()
                .filter(Predicate<Edge> { obj: Edge? -> TextEdge::class.java.isInstance(obj) })
                .map(Function<Edge, TextEdge> { obj: Edge? -> TextEdge::class.java.cast(obj) })
    }

    val markupStream: Stream<Markup>
        get() = markupNodeIndex.keys.stream()

    val markupNodeStream: Stream<MarkupNode>
        get() = markupNodeIndex.values.stream()

    fun getTextNodeStreamForMarkup(markup: Markup?): Stream<TextNode> {
        val originalMarkupNode = getMarkupNode(markup)
        val markupHyperEdges = getOutgoingEdges(originalMarkupNode).stream()
                .filter(Predicate<Edge> { obj: Edge? -> MarkupHyperEdge::class.java.isInstance(obj) })
                .map(Function<Edge, MarkupHyperEdge> { obj: Edge? -> MarkupHyperEdge::class.java.cast(obj) })
                .collect(Collectors.toList())
        Preconditions.checkArgument(markupHyperEdges.size == 1)
        return getTargets(markupHyperEdges[0]).stream()
                .filter(Predicate<Node> { obj: Node? -> TextNode::class.java.isInstance(obj) })
                .map(Function<Node, TextNode> { obj: Node? -> TextNode::class.java.cast(obj) })
    }

    fun getMarkupNode(markup: Markup?): MarkupNode {
        return getMarkupNode(markupNodeIndex[markup])!!
    }

    fun getMarkupNodeStreamForTextNode(textNode: TextNode?): Stream<MarkupNode> {
        return getIncomingEdges(textNode).stream()
                .filter(Predicate<Edge> { obj: Edge? -> MarkupHyperEdge::class.java.isInstance(obj) })
                .map(Function<Edge, MarkupHyperEdge> { obj: Edge? -> MarkupHyperEdge::class.java.cast(obj) })
                .map { e: MarkupHyperEdge? -> getSource(e) }
                .map(Function<Node, MarkupNode> { obj: Node? -> MarkupNode::class.java.cast(obj) })
    }

    private fun getMarkupNode(markupNode: Node?): MarkupNode? {
        return markupNode as MarkupNode?
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(CollationGraph::class.java)
    }

}