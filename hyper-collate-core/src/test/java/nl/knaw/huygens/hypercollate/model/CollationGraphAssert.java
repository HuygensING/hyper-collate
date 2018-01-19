package nl.knaw.huygens.hypercollate.model;

/*-
 * #%L
 * hyper-collate-core
 * =======
 * Copyright (C) 2017 - 2018 Huygens ING (KNAW)
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

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import org.antlr.v4.misc.OrderedHashMap;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.internal.Iterables;

import java.util.*;

public class CollationGraphAssert extends AbstractObjectAssert<CollationGraphAssert, CollationGraph> {

  /**
   * Creates a new <code>{@link CollationGraphAssert}</code> to make assertions on actual CollationGraph.
   *
   * @param actual the CollationGraph we want to make assertions on.
   */
  public CollationGraphAssert(CollationGraph actual) {
    super(actual, CollationGraphAssert.class);
  }

  @org.assertj.core.util.CheckReturnValue
  public CollationGraphAssert containsTextNodesMatching(NodeSketch... nodeSketches) {
    Set<NodeSketch> actualNodeSketches = getActualNodeSketches();
    Set<NodeSketch> expectedNodeSketches = new HashSet<>(Arrays.asList(nodeSketches));
    expectedNodeSketches.removeAll(actualNodeSketches);

    String errorMessage = "\nNo nodes found matching %s;\nNodes found: %s";
    if (!expectedNodeSketches.isEmpty()) {
      failWithMessage(errorMessage, expectedNodeSketches, actualNodeSketches);
    }

    return myself;
  }

  @org.assertj.core.util.CheckReturnValue
  public CollationGraphAssert doesNotContainTextNodesMatching(NodeSketch... nodeSketches) {
    Set<NodeSketch> actualNodeSketches = getActualNodeSketches();
    List<NodeSketch> nodeSketchList = Arrays.asList(nodeSketches);
    Set<NodeSketch> unexpectedNodeSketches = new HashSet<>(nodeSketchList);
    unexpectedNodeSketches.retainAll(actualNodeSketches);

    String errorMessage = "\nExpected %s not to match with any of %s, but found matches with %s";
    if (!unexpectedNodeSketches.isEmpty()) {
      failWithMessage(errorMessage, actualNodeSketches, nodeSketchList, unexpectedNodeSketches);
    }

    return myself;
  }

  @org.assertj.core.util.CheckReturnValue
  public CollationGraphAssert containsOnlyTextNodesMatching(NodeSketch... nodeSketches) {
    Set<NodeSketch> actualNodeSketches = getActualNodeSketches();
    Set<NodeSketch> expectedNodeSketches = new HashSet<>(Arrays.asList(nodeSketches));
    Iterables.instance().assertContainsAll(info, actualNodeSketches, expectedNodeSketches);
    return myself;
  }

  public static class NodeSketch {
    Map<String, String> witnessTokenSegments = new OrderedHashMap<>();

    public NodeSketch withWitnessSegmentSketch(String sigil, String mergedtokens) {
      witnessTokenSegments.put(sigil, mergedtokens);
      return this;
    }

    @Override
    public int hashCode() {
      return witnessTokenSegments.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof NodeSketch
          && ((NodeSketch) obj).witnessTokenSegments.equals(witnessTokenSegments);
    }

    @Override
    public String toString() {
      return "[\n" + witnessTokenSegments.keySet()//
          .stream()//
          .map(s -> String.format("  (%s:%s)", s, witnessTokenSegments.get(s).replace("\n", "\\n")))//
          .collect(joining(",\n"))//
          + "\n]";
    }
  }

  public static NodeSketch nodeSketch() {
    return new NodeSketch();
  }

  public NodeSketch toNodeSketch(TextNode node) {
    NodeSketch nodeSketch = nodeSketch();
    node.getSigils().forEach(s ->
        nodeSketch.withWitnessSegmentSketch(s, ((MarkedUpToken) node.getTokenForWitness(s)).getContent())
    );
    return nodeSketch;
  }

  /**
   * An entry point for CollationGraphAssert to follow AssertJ standard <code>assertThat()</code> statements.<br>
   * With a static import, one can write directly: <code>assertThat(myCollationGraph)</code> and get specific assertion with code completion.
   *
   * @param actual the CollationGraph we want to make assertions on.
   * @return a new <code>{@link CollationGraphAssert}</code>
   */
  @org.assertj.core.util.CheckReturnValue
  public static CollationGraphAssert assertThat(CollationGraph actual) {
    return new CollationGraphAssert(actual);
  }

  private Set<NodeSketch> getActualNodeSketches() {
    return actual.traverseTextNodes()//
        .stream()//
        .filter(n -> !n.getSigils().isEmpty())//
        .map(this::toNodeSketch)//
        .collect(toSet());
  }

}
