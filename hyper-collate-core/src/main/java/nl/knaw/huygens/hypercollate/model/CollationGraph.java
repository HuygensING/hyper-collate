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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.interedition.collatex.Token;

public class CollationGraph {

  private static Vertex startVertex;
  private static Vertex endVertex;
  private List<Vertex> vertices = new ArrayList<>();

  public Vertex getStart() {
    return startVertex;
  }

  public Vertex getEnd() {
    return endVertex;
  }

  public boolean isEmpty() {
    return vertices.isEmpty();
  }

  public void connect(Vertex vertex1, Vertex vertex2, String... sigils) {
    System.out.println("connecting " + vertex1 + " -[" + Arrays.stream(sigils).collect(joining(",")) + "]-> " + vertex2);

  }

  public Vertex addVertex(Token... tokens) {
    Vertex newVertex = new Vertex(tokens);
    vertices.add(newVertex);
    System.out.println("adding " + newVertex);
    return newVertex;
  }

  public static class Vertex {
    Map<String, Token> tokenMap = new HashMap<>();

    public Vertex(Token... tokens) {
      for (Token token : tokens) {
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
      String tokensString = getSigils().stream().sorted().map(tokenMap::get).map(Token::toString).collect(joining(", "));
      return new StringBuilder("(")//
          .append(tokensString)//
          .append(")")//
          .toString();
    }

  }

}
