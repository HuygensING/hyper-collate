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

import eu.interedition.collatex.Token;
import static java.util.stream.Collectors.joining;
import nl.knaw.huygens.hypergraph.core.DirectedAcyclicGraph;

import java.util.*;

public class CollationGraph extends DirectedAcyclicGraph<CollationGraph.Node> {

  private final List<String> sigils;
  private Node endNode;

  public CollationGraph(List<String> sigils) {
    this.sigils = sigils;
    setRootNode(new Node());
  }

  public CollationGraph() {
    this.sigils = new ArrayList<>();
    setRootNode(new Node());
  }

  public Node addNodeWithTokens(Token... tokens) {
    Node newNode = new Node(tokens);
    addNode(newNode, "");
    // System.out.println("adding " + newNode);
    return newNode;
  }

  public Node getRootNode() {
    return traverse().iterator().next();
  }

  public void setEndNode(Node endNode) {
    this.endNode = endNode;
  }

  public Node getEndNode() {
    return this.endNode;
  }

  public boolean isEmpty() {
    return sigils.isEmpty();
  }

  public static class Node {
    final Map<String, Token> tokenMap = new HashMap<>();

    Node(Token... tokens) {
      for (Token token : tokens) {
        addToken(token);
      }
    }

    public void addToken(Token token) {
      if (token != null && token.getWitness() != null) {
        tokenMap.put(token.getWitness().getSigil(), token);
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
      String tokensString = getSigils()//
          .stream()//
          .sorted()//
          .map(tokenMap::get)//
          .map(Token::toString)//
          .collect(joining(", "));
      return "(" + tokensString + ")";
    }

  }

  public List<String> getSigils() {
    return sigils;
  }

}
