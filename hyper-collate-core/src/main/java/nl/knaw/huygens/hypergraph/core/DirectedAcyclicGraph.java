package nl.knaw.huygens.hypergraph.core;

/*-
 * #%L
 * hypergraph
 * =======
 * Copyright (C) 2017 Huygens ING (KNAW)
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

// nu zou ik wel topological sort willen hebben
// teveel gedoe, kan ook gewoon een root node maken
public class DirectedAcyclicGraph<N> extends Hypergraph<N, TraditionalEdge> {
  private N root;

  public DirectedAcyclicGraph() {
    super(GraphType.ORDERED);
  }

  @Override
  public void addNode(N node, String label) {
    super.addNode(node, label);
  }

  public void setRootNode(N root) {
    this.root = root;
  }

  // Question: do we want labels here?
  public void addDirectedEdge(N source, N target) {
    TraditionalEdge edge = new TraditionalEdge();
    super.addDirectedHyperedge(edge, "", source, target);
  }

  public List<N> traverse() {
    Stack<N> nodesToVisit = new Stack<>();
    nodesToVisit.add(root);
    List<N> result = new ArrayList<>();
    while (!nodesToVisit.isEmpty()) {
      N pop = nodesToVisit.pop();
      result.add(pop);
      Collection<TraditionalEdge> outgoingEdges = this.getOutgoingEdges(pop);
      for (TraditionalEdge e : outgoingEdges) {
        N target = this.getTarget(e);
        nodesToVisit.add(target);
      }
    }
    return result;
  }

  public N getTarget(TraditionalEdge e) {
    Collection<N> nodes = super.getTargets(e);
    if (nodes.size() != 1) {
      throw new RuntimeException("trouble!");
    }
    return nodes.iterator().next();
  }

}
