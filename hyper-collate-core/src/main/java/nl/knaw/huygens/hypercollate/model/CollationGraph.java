package nl.knaw.huygens.hypercollate.model;

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

import static java.util.stream.Collectors.joining;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import eu.interedition.collatex.Token;
import nl.knaw.huygens.hypergraph.core.DirectedAcyclicGraph;

public class CollationGraph extends DirectedAcyclicGraph<CollationGraph.Node> {

  public CollationGraph() {
    setRootNode(new Node());
  }

  public Node addNodeWithTokens(Token... tokens) {
    Node newNode = new Node(tokens);
    addNode(newNode, "");
    System.out.println("adding " + newNode);
    return newNode;
  }

  public Node getRootNode() {
    return traverse().iterator().next();
  }
  // private static Vertex startVertex;
  // private static Vertex endVertex;
  // private List<Vertex> vertices = new ArrayList<>();
  //
  // public Vertex getStart() {
  // return startVertex;
  // }
  //
  // public Vertex getEnd() {
  // return endVertex;
  // }
  //
  // public boolean isEmpty() {
  // return vertices.isEmpty();
  // }
  //
  // public void connect(Vertex vertex1, Vertex vertex2, String... sigils) {
  // System.out.println("connecting " + vertex1 + " -[" + Arrays.stream(sigils).collect(joining(",")) + "]-> " + vertex2);
  //
  // }
  //
  // public Vertex addVertex(Token... tokens) {
  // Vertex newVertex = new Vertex(tokens);
  // vertices.add(newVertex);
  // System.out.println("adding " + newVertex);
  // return newVertex;
  // }
  //
  // public List<Vertex> getVertices() {
  // return this.vertices;
  // }

  public static class Node {
    Map<String, Token> tokenMap = new HashMap<>();

    public Node(Token... tokens) {
      for (Token token : tokens) {
        if (token != null && token.getWitness() != null) {
          tokenMap.put(token.getWitness().getSigil(), token);
        }
      }
    }

    public Token getTokenForWitness(String sigil) {
      return tokenMap.get(sigil);
    }

    public Set<String> getSigils() {
      return tokenMap.keySet();
    }

    @Override
    public String toString() {
      String tokensString = getSigils().stream().sorted().map(tokenMap::get).map(Token::toString).collect(joining(", "));
      return new StringBuilder("(")//
          .append(tokensString)//
          .append(")")//
          .toString();
    }

  }

}
