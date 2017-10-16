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
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import nl.knaw.huygens.hypercollate.model.EndTokenVertex;
import nl.knaw.huygens.hypercollate.model.MarkedUpToken;
import nl.knaw.huygens.hypercollate.model.SimpleTokenVertex;
import nl.knaw.huygens.hypercollate.model.StartTokenVertex;
import nl.knaw.huygens.hypercollate.model.TokenVertex;
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph;

public class DotFactory {
  /**
   * Generates a .dot format string for visualizing a variant witness graph.
   *
   * @param graph
   *          The variant witness graph for which we are generating a dot file.
   * @return A string containing the contents of a .dot representation of the
   *         variant witness graph.
   */
  public static String fromVariantWitnessGraph(VariantWitnessGraph graph) {
    StringBuilder dotBuilder = new StringBuilder("digraph VariantWitnessGraph{\ngraph [rankdir=LR]\nlabelloc=b\n");

    // Deque<String> openMarkup = new LinkedList();
    List<String> edges = new ArrayList<>();
    Deque<TokenVertex> nextTokens = new LinkedList<>();
    nextTokens.add(graph.getStartTokenVertex());
    Set<TokenVertex> verticesDone = new HashSet<>();
    while (!nextTokens.isEmpty()) {
      TokenVertex tokenVertex = nextTokens.pop();
      if (!verticesDone.contains(tokenVertex)) {
        String tokenVariable = vertexVariable(tokenVertex);
        if (tokenVertex instanceof SimpleTokenVertex) {
          dotBuilder.append(tokenVariable)//
              .append(" [label=\"")//
              .append(((SimpleTokenVertex) tokenVertex).getContent())//
              .append("\"]\n");
        } else {
          dotBuilder.append(tokenVariable)//
              .append(" [label=\"\";shape=doublecircle,rank=middle]\n");
        }
        tokenVertex.getOutgoingTokenVertexStream().forEach(ot -> {
          String vertexVariable = vertexVariable(ot);
          edges.add(tokenVariable + "->" + vertexVariable);
          nextTokens.add(ot);
        });
        verticesDone.add(tokenVertex);
      }
    }
    edges.forEach(e -> dotBuilder.append(e).append("\n"));
    dotBuilder.append("}");
    return dotBuilder.toString();
  }

  private static String vertexVariable(TokenVertex tokenVertex) {
    if (tokenVertex instanceof SimpleTokenVertex) {
      MarkedUpToken token = (MarkedUpToken) tokenVertex.getToken();
      return "t" + token.getIndexNumber();
    }
    if (tokenVertex instanceof StartTokenVertex) {
      return "st";
    }
    if (tokenVertex instanceof EndTokenVertex) {
      return "et";
    }
    return null;
  }

}
