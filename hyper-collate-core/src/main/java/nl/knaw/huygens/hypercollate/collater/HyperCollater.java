package nl.knaw.huygens.hypercollate.collater;

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

import static java.util.stream.Collectors.toList;
import static nl.knaw.huygens.hypercollate.tools.StreamUtil.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;

import nl.knaw.huygens.hypercollate.model.CollationGraph;
import nl.knaw.huygens.hypercollate.model.SimpleTokenVertex;
import nl.knaw.huygens.hypercollate.model.TokenVertex;
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph;

public class HyperCollater {

  private static BiFunction<SimpleTokenVertex, SimpleTokenVertex, Boolean> matcher = //
      (stv1, stv2) -> {
        if (stv1.getNormalizedContent().isEmpty() && stv2.getNormalizedContent().isEmpty()) {
          if (stv1.getContent().isEmpty() && stv2.getContent().isEmpty()) {
            // both are milestones, so compare their tags.
            String parentTag1 = stv1.getParentXPath().replace("/.*/", "");
            String parentTag2 = stv2.getParentXPath().replace("/.*/", "");
            return parentTag1.equals(parentTag2);
          } else {
            return false;
          }
        }
        return stv1.getNormalizedContent().equals(stv2.getNormalizedContent());
      };

  public static CollationGraph collate(VariantWitnessGraph... graphs) {
    CollationGraph collationGraph = new CollationGraph();
    String sigil1 = graphs[0].getSigil();
    String sigil2 = graphs[1].getSigil();

    Iterator<VariantWitnessGraph> witnessIterable = Arrays.asList(graphs).iterator();
    VariantWitnessGraph witness1 = witnessIterable.next();
    VariantWitnessGraph witness2 = witnessIterable.next();

    List<Match> matches = match(witness1, witness2);

    VariantWitnessGraphRanking ranking1 = VariantWitnessGraphRanking.of(witness1);
    List<Match> matchesSortedByWitness1 = sortMatchesByWitness(matches, sigil1, ranking1);

    VariantWitnessGraphRanking ranking2 = VariantWitnessGraphRanking.of(witness2);
    List<Match> matchesSortedByWitness2 = sortMatchesByWitness(matches, sigil2, ranking2);

    int rank1 = 0;
    int rank2 = 0;
    Iterator<TokenVertex> iterator1 = VariantWitnessGraphTraversal.of(witness1).iterator();
    iterator1.next();
    Iterator<TokenVertex> iterator2 = VariantWitnessGraphTraversal.of(witness2).iterator();
    iterator2.next();

    CollationGraph.Node lastCollatedVertex1 = collationGraph.getRootNode();
    CollationGraph.Node lastCollatedVertex2 = collationGraph.getRootNode();
    while (!matchesSortedByWitness1.isEmpty()) {
      Match matchOption1 = matchesSortedByWitness1.get(0);
      SimpleTokenVertex tokenVertex11 = matchOption1.getTokenVertexForWitness(sigil1);
      int rank11 = ranking1.apply(tokenVertex11);
      SimpleTokenVertex tokenVertex12 = matchOption1.getTokenVertexForWitness(sigil2);
      int rank12 = ranking2.apply(tokenVertex12);
      int diff1 = rank11 - rank1 + rank12 - rank2;

      Match matchOption2 = matchesSortedByWitness2.get(0);
      SimpleTokenVertex tokenVertex21 = matchOption2.getTokenVertexForWitness(sigil1);
      int rank21 = ranking1.apply(tokenVertex21);
      SimpleTokenVertex tokenVertex22 = matchOption2.getTokenVertexForWitness(sigil2);
      int rank22 = ranking2.apply(tokenVertex22);
      int diff2 = rank21 - rank1 + rank22 - rank2;

      Match match;
      if (diff1 >= diff2) {
        match = matchOption1;
        rank1 = rank11;
        rank2 = rank12;
      } else {
        match = matchOption2;
        rank1 = rank21;
        rank2 = rank22;
      }
      // System.out.println(match);

      SimpleTokenVertex tokenVertexForWitness1 = match.getTokenVertexForWitness(sigil1);
      SimpleTokenVertex tokenVertexForWitness2 = match.getTokenVertexForWitness(sigil2);
      removeUnusableMatches(matchesSortedByWitness1, sigil1, sigil2, tokenVertexForWitness1, tokenVertexForWitness2);
      removeUnusableMatches(matchesSortedByWitness2, sigil1, sigil2, tokenVertexForWitness1, tokenVertexForWitness2);
      // System.out.println(matchesSortedByWitness1.size());
      // System.out.println(matchesSortedByWitness2.size());

      if (iterator1.hasNext()) {
        TokenVertex nextWitness1Vertex = iterator1.next();
        while (iterator1.hasNext() && !nextWitness1Vertex.equals(tokenVertexForWitness1)) {
          System.out.println("> " + sigil1);
          CollationGraph.Node newCollatedVertex = collationGraph.addNodeWithTokens(nextWitness1Vertex.getToken());
          collationGraph.addDirectedEdge(lastCollatedVertex1, newCollatedVertex);
          lastCollatedVertex1 = newCollatedVertex;
          nextWitness1Vertex = iterator1.next();
          System.out.println();
        }
      }
      if (iterator2.hasNext()) {
        TokenVertex nextWitness2Vertex = iterator2.next();
        while (iterator2.hasNext() && !nextWitness2Vertex.equals(tokenVertexForWitness2)) {
          System.out.println("> " + sigil2);
          CollationGraph.Node newCollatedVertex = collationGraph.addNodeWithTokens(nextWitness2Vertex.getToken());
          collationGraph.addDirectedEdge(lastCollatedVertex2, newCollatedVertex);
          lastCollatedVertex2 = newCollatedVertex;
          nextWitness2Vertex = iterator2.next();
          System.out.println();
        }
      }
      System.out.println("> " + sigil1 + "+" + sigil2);
      CollationGraph.Node newCollatedVertex = collationGraph.addNodeWithTokens(tokenVertexForWitness1.getToken(), tokenVertexForWitness2.getToken());
      collationGraph.addDirectedEdge(lastCollatedVertex1, newCollatedVertex);
      System.out.println();
      lastCollatedVertex1 = newCollatedVertex;
      lastCollatedVertex2 = newCollatedVertex;

    }
    CollationGraph.Node endNode = collationGraph.addNodeWithTokens();
    collationGraph.addDirectedEdge(lastCollatedVertex1, endNode);
    if (!lastCollatedVertex1.equals(lastCollatedVertex2)) {
      collationGraph.addDirectedEdge(lastCollatedVertex2, endNode);
    }

    return collationGraph;
  }

  private static List<Match> sortMatchesByWitness(List<Match> matches, String sigil, VariantWitnessGraphRanking ranking) {
    Comparator<Match> matchComparator = matchComparator(ranking, sigil);
    return matches.stream()//
        .sorted(matchComparator)//
        .collect(toList());
  }

  private static Comparator<Match> matchComparator(VariantWitnessGraphRanking ranking, String sigil) {
    return (match1, match2) -> {
      SimpleTokenVertex vertex1 = match1.getTokenVertexForWitness(sigil);
      Integer rank1 = ranking.apply(vertex1);
      SimpleTokenVertex vertex2 = match2.getTokenVertexForWitness(sigil);
      Integer rank2 = ranking.apply(vertex2);
      return rank1.compareTo(rank2);
    };
  }

  private static void removeUnusableMatches(List<Match> matchesSortedByWitness, //
      String sigil1, String sigil2, //
      SimpleTokenVertex tokenVertexForWitness1, SimpleTokenVertex tokenVertexForWitness2) {
    List<Match> matchesToRemove = matchesSortedByWitness.stream()//
        .filter(m -> m.getTokenVertexForWitness(sigil1).equals(tokenVertexForWitness1) //
            || m.getTokenVertexForWitness(sigil2).equals(tokenVertexForWitness2))//
        .collect(toList());
    matchesSortedByWitness.removeAll(matchesToRemove);
  }

  private static List<Match> match(VariantWitnessGraph witness1, VariantWitnessGraph witness2) {
    List<Match> matches = new ArrayList<>();
    VariantWitnessGraphTraversal traversal1 = VariantWitnessGraphTraversal.of(witness1);
    VariantWitnessGraphTraversal traversal2 = VariantWitnessGraphTraversal.of(witness2);
    stream(traversal1)//
        .filter(tv -> tv instanceof SimpleTokenVertex)//
        .map(SimpleTokenVertex.class::cast)//
        .forEach(tv1 -> stream(traversal2)//
            .filter(tv -> tv instanceof SimpleTokenVertex)//
            .map(SimpleTokenVertex.class::cast)//
            .forEach(tv2 -> {
              if (matcher.apply(tv1, tv2)) {
                matches.add(new Match(tv1, tv2));
              }
            }));
    return matches;
  }

  // private static void merge(CollationGraph collationGraph, VariantWitnessGraph witnessGraph, Map<Object, Object> alignmentMap) {
  // Vertex start = collationGraph.getStart();
  // if (collationGraph.isEmpty()) {
  // TokenVertex startTokenVertex = witnessGraph.getStartTokenVertex();
  // startTokenVertex.getOutgoingTokenVertexStream().forEach(tv -> {
  // // start.add
  // });
  // } else {
  //
  // }
  //
  // }
}
