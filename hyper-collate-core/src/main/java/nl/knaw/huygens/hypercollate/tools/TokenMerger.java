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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import nl.knaw.huygens.hypercollate.model.EndTokenVertex;
import nl.knaw.huygens.hypercollate.model.MarkedUpToken;
import nl.knaw.huygens.hypercollate.model.SimpleTokenVertex;
import nl.knaw.huygens.hypercollate.model.TokenVertex;
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph;

public class TokenMerger {

  public static VariantWitnessGraph merge(VariantWitnessGraph originalGraph) {
    VariantWitnessGraph mergedGraph = new VariantWitnessGraph(originalGraph.getSigil());
    originalGraph.getMarkupStream().forEach(mergedGraph::addMarkup);

    Map<Long, TokenVertex> originalToMergedMap = new HashMap<>();
    TokenVertex originaltokenVertex = originalGraph.getStartTokenVertex();
    List<TokenVertex> verticesToAdd = originaltokenVertex.getOutgoingTokenVertexStream().collect(Collectors.toList());
    List<Long> handledTokens = new ArrayList<>();
    TokenVertex mergedVertexToLinkTo = mergedGraph.getStartTokenVertex();
    verticesToAdd.forEach(originalVertex -> {
      handle(originalGraph, mergedGraph, originalToMergedMap, handledTokens, originalVertex, mergedVertexToLinkTo);
    });
    return mergedGraph;
  }

  private static void handle(VariantWitnessGraph originalGraph, VariantWitnessGraph mergedGraph, Map<Long, TokenVertex> originalToMergedMap, List<Long> handledTokens, TokenVertex originalVertex,
      TokenVertex mergedVertexToLinkTo) {
    if (originalVertex instanceof EndTokenVertex) {
      TokenVertex endTokenVertex = mergedGraph.getEndTokenVertex();
      originalVertex.getIncomingTokenVertexStream().forEach(tv -> {
        Long indexNumber = ((MarkedUpToken) tv.getToken()).getIndexNumber();
        TokenVertex mergedTokenVertex = originalToMergedMap.get(indexNumber);
        mergedGraph.addOutgoingTokenVertexToTokenVertex(mergedTokenVertex, endTokenVertex);
      });
      return;
    }

    MarkedUpToken token = (MarkedUpToken) originalVertex.getToken();
    if (handledTokens.contains(token.getIndexNumber())) {
      TokenVertex mergedTokenVertex = originalToMergedMap.get(token.getIndexNumber());
      mergedGraph.addOutgoingTokenVertexToTokenVertex(mergedVertexToLinkTo, mergedTokenVertex);
      return;
    }

    MarkedUpToken mergedToken = new MarkedUpToken()//
        .setContent(token.getContent())//
        .setIndexNumber(token.getIndexNumber());

    SimpleTokenVertex mergedVertex = new SimpleTokenVertex(mergedToken);
    originalGraph.getMarkupListForTokenVertex(originalVertex)//
        .forEach(markup -> mergedGraph.addMarkupToTokenVertex(mergedVertex, markup));
    originalToMergedMap.put(token.getIndexNumber(), mergedVertex);
    mergedGraph.addOutgoingTokenVertexToTokenVertex(mergedVertexToLinkTo, mergedVertex);
    handledTokens.add(token.getIndexNumber());
    List<TokenVertex> originalOutgoingVertices = originalVertex.getOutgoingTokenVertexStream().collect(Collectors.toList());
    while (canMerge(originalGraph, originalVertex, originalOutgoingVertices)) {
      MarkedUpToken nextOriginalToken = (MarkedUpToken) originalOutgoingVertices.get(0).getToken();
      mergedToken.setContent(mergedToken.getContent() + nextOriginalToken.getContent());
      originalToMergedMap.put(nextOriginalToken.getIndexNumber(), mergedVertex);
      originalVertex = originalOutgoingVertices.get(0);
      originalOutgoingVertices = originalVertex.getOutgoingTokenVertexStream().collect(Collectors.toList());
    }
    originalOutgoingVertices.forEach(oVertex -> handle(originalGraph, mergedGraph, originalToMergedMap, handledTokens, oVertex, mergedVertex));
  }

  private static boolean canMerge(VariantWitnessGraph originalGraph, TokenVertex originalVertex, List<TokenVertex> originalOutgoingVertices) {
    return originalOutgoingVertices.size() == 1//
        && originalGraph.getMarkupListForTokenVertex(originalVertex).equals(originalGraph.getMarkupListForTokenVertex(originalOutgoingVertices.get(0)));
  }

}
