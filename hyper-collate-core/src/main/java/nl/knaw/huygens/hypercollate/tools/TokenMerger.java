package nl.knaw.huygens.hypercollate.tools;

/*-
 * #%L
 * HyperCollate core
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

import nl.knaw.huygens.hypercollate.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class TokenMerger {

  public static VariantWitnessGraph merge(VariantWitnessGraph originalGraph) {
    VariantWitnessGraph mergedGraph = new VariantWitnessGraph(originalGraph.getSigil());
    originalGraph.getMarkupStream().forEach(mergedGraph::addMarkup);

    Map<Long, TokenVertex> originalToMergedMap = new HashMap<>();
    TokenVertex originaltokenVertex = originalGraph.getStartTokenVertex();
    List<TokenVertex> verticesToAdd = originaltokenVertex.getOutgoingTokenVertexStream().collect(Collectors.toList());
    List<Long> handledTokens = new ArrayList<>();
    AtomicBoolean endTokenHandled = new AtomicBoolean(false);
    TokenVertex mergedVertexToLinkTo = mergedGraph.getStartTokenVertex();
    verticesToAdd.forEach(originalVertex -> handle(originalGraph, mergedGraph, originalToMergedMap, handledTokens, //
        endTokenHandled, originalVertex, mergedVertexToLinkTo));
    return mergedGraph;
  }

  // TODO: introduce context to avoid passing so many parameters
  private static void handle(VariantWitnessGraph originalGraph, VariantWitnessGraph mergedGraph, //
      Map<Long, TokenVertex> originalToMergedMap, List<Long> handledTokens, //
      AtomicBoolean endTokenHandled, //
      TokenVertex originalVertex, TokenVertex mergedVertexToLinkTo) {

    if (originalVertex instanceof EndTokenVertex) {
      if (endTokenHandled.get()) {
        return;
      }
      TokenVertex endTokenVertex = mergedGraph.getEndTokenVertex();
      originalVertex.getIncomingTokenVertexStream().forEach(tv -> {
        Long indexNumber = ((MarkedUpToken) tv.getToken()).getIndexNumber();
        TokenVertex mergedTokenVertex = originalToMergedMap.get(indexNumber);
        mergedGraph.addOutgoingTokenVertexToTokenVertex(mergedTokenVertex, endTokenVertex);
      });
      endTokenHandled.set(true);
      return;
    }

    MarkedUpToken originalToken = (MarkedUpToken) originalVertex.getToken();
    Long tokenNumber = originalToken.getIndexNumber();
    if (handledTokens.contains(tokenNumber)) {
      TokenVertex mergedTokenVertex = originalToMergedMap.get(tokenNumber);
      mergedGraph.addOutgoingTokenVertexToTokenVertex(mergedVertexToLinkTo, mergedTokenVertex);
      return;
    }

    MarkedUpToken mergedToken = new MarkedUpToken()//
        .setContent(originalToken.getContent())//
        .setNormalizedContent(originalToken.getNormalizedContent())//
        .setParentXPath(originalToken.getParentXPath())//
        .setWitness((SimpleWitness) originalToken.getWitness())//
        .setIndexNumber(tokenNumber);

    SimpleTokenVertex mergedVertex = new SimpleTokenVertex(mergedToken)//
        .setBranchPath(originalVertex.getBranchPath());
    originalGraph.getMarkupListForTokenVertex(originalVertex)//
        .forEach(markup -> mergedGraph.addMarkupToTokenVertex(mergedVertex, markup));
    originalToMergedMap.put(tokenNumber, mergedVertex);
    mergedGraph.addOutgoingTokenVertexToTokenVertex(mergedVertexToLinkTo, mergedVertex);
    handledTokens.add(tokenNumber);
    List<TokenVertex> originalOutgoingVertices = originalVertex.getOutgoingTokenVertexStream().collect(Collectors.toList());
    while (canMerge(originalGraph, originalVertex, originalOutgoingVertices)) {
      MarkedUpToken nextOriginalToken = (MarkedUpToken) originalOutgoingVertices.get(0).getToken();
      mergedToken//
          .setContent(mergedToken.getContent() + nextOriginalToken.getContent())//
          .setNormalizedContent(mergedToken.getNormalizedContent() + nextOriginalToken.getNormalizedContent());
      originalToMergedMap.put(nextOriginalToken.getIndexNumber(), mergedVertex);
      originalVertex = originalOutgoingVertices.get(0);
      originalOutgoingVertices = originalVertex.getOutgoingTokenVertexStream().collect(Collectors.toList());
    }
    originalOutgoingVertices.forEach(oVertex -> handle(originalGraph, mergedGraph, originalToMergedMap, handledTokens, endTokenHandled, oVertex, mergedVertex));
  }

  private static boolean canMerge(VariantWitnessGraph originalGraph, TokenVertex originalVertex, List<TokenVertex> originalOutgoingVertices) {
    return originalOutgoingVertices.size() == 1//
        && originalGraph.getMarkupListForTokenVertex(originalVertex).equals(originalGraph.getMarkupListForTokenVertex(originalOutgoingVertices.get(0)));
  }

}
