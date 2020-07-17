package nl.knaw.huygens.hypergraph.core

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

/*
 * Generic Hypergraph definition
 * Directed, labelled, hyperedges (one to many)
 * Are we going to make the child nodes ordered?
 * @author: Ronald Haentjens Dekker
 */

import java.util.*

open class Hypergraph<N, H> protected constructor(private val graphType: GraphType) {
    private var mappingFunction: (N) -> MutableCollection<H>
    private val nodes: MutableSet<N>
    private val incomingEdges: MutableMap<N, MutableCollection<H>>
    private val outgoingEdges: MutableMap<N, MutableCollection<H>>
    private val sourceNode: MutableMap<H, N>
    private val targetNodes: MutableMap<H, MutableCollection<N>>
    private val edgeLabels: MutableMap<H, String>
    private val nodeLabels: MutableMap<N, String>
    protected open fun addNode(node: N, label: String) {
        nodes.add(node)
        nodeLabels[node] = label
    }

    @SafeVarargs
    protected fun addDirectedHyperEdge(edge: H, label: String, source: N, vararg targets: N) {
        // TODO: check whether source node is in nodes
        // NOTE: The way it is done now, is that nodes are not added explicitly to the graph
        // NOTE: but rather indirectly through the edges.
        // set source
        sourceNode[edge] = source
        // set targets
        if (GraphType.ORDERED == graphType) {
            val targetList: MutableList<N> = mutableListOf(*targets)
            targetNodes[edge] = targetList
        } else {
            // convert Array target to set
            val targetSet: MutableSet<N> = mutableSetOf(*targets)
            targetNodes[edge] = targetSet
        }
        // set incoming
        for (target in targets) {
            incomingEdges.computeIfAbsent(target, mappingFunction).add(edge)
        }
        // set outgoing
        outgoingEdges.computeIfAbsent(source, mappingFunction).add(edge)
        // set label
        edgeLabels[edge] = label
    }

    @SafeVarargs
    protected fun addTargetsToHyperEdge(edge: H, vararg targets: N) {
        if (!targetNodes.containsKey(edge)) {
            throw RuntimeException("unknown hyperedge $edge")
        }
        val collection = targetNodes[edge] ?: error("targetNodes[$edge] == null")
        collection.addAll(listOf(*targets))
        for (target in targets) {
            incomingEdges.computeIfAbsent(target, mappingFunction).add(edge)
        }
    }

    fun getTargets(e: H): Collection<N>? {
        return targetNodes[e]
    }

    fun getSource(e: H): N? {
        return sourceNode[e]
    }

    fun getOutgoingEdges(node: N): Collection<H> =
            outgoingEdges.getOrDefault(node, listOf())

    fun getIncomingEdges(node: N): Collection<H> =
            incomingEdges.getOrDefault(node, listOf())

    enum class GraphType {
        ORDERED, UNORDERED
    }

    init {
        nodes = HashSet()
        incomingEdges = HashMap()
        outgoingEdges = HashMap()
        sourceNode = HashMap()
        targetNodes = HashMap()
        edgeLabels = HashMap()
        nodeLabels = HashMap()
        // create switch
        mappingFunction =
                if (GraphType.ORDERED == graphType) {
                    { ArrayList() }
                } else {
                    { HashSet() }
                }
    }
}