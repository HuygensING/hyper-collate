package nl.knaw.huygens.hypercollate.tools;

/*-
 * #%L
 * hyper-collate-core
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import nl.knaw.huygens.hypercollate.model.CollationGraph;
import nl.knaw.huygens.hypercollate.model.CollationGraph.Node;

public class CollationGraphRanking implements Iterable<Set<Node>>, Function<Node, Integer> {
  private final Map<Node, Integer> byNode = new HashMap<>();
  private final SortedMap<Integer, Set<Node>> byRank = new TreeMap<>();

  public static CollationGraphRanking of(CollationGraph graph) {
    final CollationGraphRanking ranking = new CollationGraphRanking();
    List<Node> nodesToRank = new ArrayList<>();
    nodesToRank.add(graph.getRootNode());
    while (!nodesToRank.isEmpty()) {
      Node node = nodesToRank.remove(0);
      AtomicBoolean canRank = new AtomicBoolean(true);
      AtomicInteger rank = new AtomicInteger(-1);
      graph.getIncomingEdges(node)//
          .stream()//
          .map(graph::getSource)//
          .forEach(incoming -> {
            int currentRank = rank.get();
            Integer incomingRank = ranking.byNode.get(incoming);
            if (incomingRank == null) {
              // node has an incoming node that hasn't been ranked yet, so node can't be ranked yet either.
              canRank.set(false);
            } else {
              int max = Math.max(currentRank, incomingRank);
              rank.set(max);
            }
          });
      graph.getOutgoingEdges(node)//
          .stream()//
          .map(graph::getTarget)//
          .forEach(nodesToRank::add);
      if (canRank.get()) {
        rank.getAndIncrement();
        ranking.byNode.put(node, rank.get());
        ranking.byRank.computeIfAbsent(rank.get(), r -> new HashSet<>()).add(node);
      } else {
        nodesToRank.add(node);
      }
    }
    return ranking;
  }

  public Map<Node, Integer> getByNode() {
    return Collections.unmodifiableMap(this.byNode);
  }

  public Map<Integer, Set<Node>> getByRank() {
    return Collections.unmodifiableMap(this.byRank);
  }

  public int size() {
    return byRank.keySet().size();
  }

  @Override
  public Iterator<Set<Node>> iterator() {
    return byRank.values().iterator();
  }

  @Override
  public Integer apply(Node node) {
    return byNode.get(node);
  }

  public Comparator<Node> comparator() {
    return Comparator.comparingInt(byNode::get);
  }

}
