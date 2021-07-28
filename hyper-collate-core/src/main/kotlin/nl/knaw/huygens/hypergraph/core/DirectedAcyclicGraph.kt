package nl.knaw.huygens.hypergraph.core

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

import org.slf4j.LoggerFactory
import java.util.*

// nu zou ik wel topological sort willen hebben
// teveel gedoe, kan ook gewoon een root node maken
open class DirectedAcyclicGraph<N> protected constructor() : Hypergraph<N, TraditionalEdge>(GraphType.ORDERED) {
    private var root: N? = null

    public override fun addNode(node: N, label: String) {
        super.addNode(node, label)
    }

    protected fun setRootNode(root: N) {
        this.root = root
    }

    // Question: do we want labels here?
    fun addDirectedEdge(source: N, target: N, sigils: Set<String>) {
        val edge = TraditionalEdge(sigils)
        super.addDirectedHyperEdge(edge, "", source, target)
    }

    fun traverse(): List<N?> {
        val visitedNodes: MutableSet<N?> = HashSet()
        val nodesToVisit = Stack<N?>()
        nodesToVisit += root
        val result: MutableList<N?> = ArrayList()
        while (!nodesToVisit.isEmpty()) {
            val pop = nodesToVisit.pop()!!
            if (pop !in visitedNodes) {
                result += pop
                val outgoingEdges = getOutgoingEdges(pop)
                visitedNodes += pop
                for (e in outgoingEdges!!) {
                    val target = getTarget(e) ?: throw RuntimeException("edge target is null for edge $pop->")
                    nodesToVisit += target
                }
            } else {
                LOG.debug("revisiting node {}", pop)
            }
        }
        return result
    }

    private fun getTarget(e: TraditionalEdge): N {
        val nodes = super.getTargets(e)
        if (nodes!!.size != 1) {
            throw RuntimeException("trouble!")
        }
        return nodes.iterator().next()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DirectedAcyclicGraph::class.java)
    }
}
