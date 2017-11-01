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

import nl.knaw.huygens.hypercollate.model.CollationGraph;
import nl.knaw.huygens.hypercollate.model.CollationGraph.Node;
import nl.knaw.huygens.hypercollate.model.SimpleTokenVertex;
import nl.knaw.huygens.hypercollate.model.TokenVertex;
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph;
import nl.knaw.huygens.hypergraph.core.TraditionalEdge;

import java.util.*;
import java.util.function.BiFunction;

import static java.util.stream.Collectors.toList;
import static nl.knaw.huygens.hypercollate.tools.StreamUtil.stream;

public class HyperCollater {

  private static final BiFunction<SimpleTokenVertex, SimpleTokenVertex, Boolean> matcher = //
      (stv1, stv2) -> {
        if (stv1.getNormalizedContent().isEmpty() && stv2.getNormalizedContent().isEmpty()) {
          if (stv1.getContent().isEmpty() && stv2.getContent().isEmpty()) {
            // both are milestones, so compare their tags.
            String parentTag1 = stv1.getParentXPath().replace("/.*/", "");
            String parentTag2 = stv2.getParentXPath().replace("/.*/", "");
            return parentTag1.equals(parentTag2);
          } else {
            return stv1.getContent().equals(stv2.getContent());
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
    VariantWitnessGraphRanking ranking2 = VariantWitnessGraphRanking.of(witness2);
    List<Match> matchesSortedByWitness1 = sortMatchesByWitness(matches, sigil1, ranking1, sigil2, ranking2);
    List<Match> matchesSortedByWitness2 = sortMatchesByWitness(matches, sigil2, ranking2, sigil2, ranking2);

    Iterator<TokenVertex> iterator1 = VariantWitnessGraphTraversal.of(witness1).iterator();
    Iterator<TokenVertex> iterator2 = VariantWitnessGraphTraversal.of(witness2).iterator();

    Map<TokenVertex, CollationGraph.Node> collatedTokenVertexMap = new HashMap<>();

    CollationGraph.Node lastCollatedVertex1 = collationGraph.getRootNode();
    collatedTokenVertexMap.put(iterator1.next(), lastCollatedVertex1);
    CollationGraph.Node lastCollatedVertex2 = collationGraph.getRootNode();
    collatedTokenVertexMap.put(iterator2.next(), lastCollatedVertex2);
    while (!matchesSortedByWitness1.isEmpty()) {
      Match match = nextMatch(sigil1, sigil2, ranking1, ranking2, matchesSortedByWitness1, matchesSortedByWitness2);
      System.out.println(match);

      TokenVertex tokenVertexForWitness1 = match.getTokenVertexForWitness(sigil1);
      TokenVertex tokenVertexForWitness2 = match.getTokenVertexForWitness(sigil2);
      List<Match> matchesToRemove = unusableMatches(matchesSortedByWitness1,//
          sigil1, sigil2,//
          tokenVertexForWitness1, tokenVertexForWitness2,//
          ranking1, ranking2);
      matchesSortedByWitness1.removeAll(matchesToRemove);
      matchesSortedByWitness2.removeAll(matchesToRemove);

      advanceWitness(collationGraph, collatedTokenVertexMap, sigil1, iterator1, tokenVertexForWitness1);
      advanceWitness(collationGraph, collatedTokenVertexMap, sigil2, iterator2, tokenVertexForWitness2);
      handleMatch(collationGraph, sigil1, sigil2, collatedTokenVertexMap, tokenVertexForWitness1, tokenVertexForWitness2);
      System.out.println();
    }

    addEndNode(collationGraph, witness1, witness2, collatedTokenVertexMap);
    addEdges(collationGraph, collatedTokenVertexMap);

    return collationGraph;
  }

  private static Match nextMatch(String sigil1, String sigil2, VariantWitnessGraphRanking ranking1, VariantWitnessGraphRanking ranking2, List<Match> matchesSortedByWitness1, List<Match> matchesSortedByWitness2) {
    Match matchOption1 = matchesSortedByWitness1.get(0);
    int diff1 = rankingDifference(sigil1, sigil2, ranking1, ranking2, matchOption1);

    Match matchOption2 = matchesSortedByWitness2.get(0);
    int diff2 = rankingDifference(sigil1, sigil2, ranking1, ranking2, matchOption2);

    return diff1 <= diff2 ? matchOption1 : matchOption2;
  }

  private static int rankingDifference(String sigil1, String sigil2, VariantWitnessGraphRanking ranking1, VariantWitnessGraphRanking ranking2, Match matchOption1) {
    TokenVertex tokenVertex11 = matchOption1.getTokenVertexForWitness(sigil1);
    int rank11 = ranking1.apply(tokenVertex11);
    TokenVertex tokenVertex12 = matchOption1.getTokenVertexForWitness(sigil2);
    int rank12 = ranking2.apply(tokenVertex12);
    return Math.abs(rank11 - rank12);
  }

  private static void addEndNode(CollationGraph collationGraph, VariantWitnessGraph witness1, VariantWitnessGraph witness2, Map<TokenVertex, Node> collatedTokenVertexMap) {
    Node endNode = collationGraph.addNodeWithTokens();
    collatedTokenVertexMap.put(witness1.getEndTokenVertex(), endNode);
    collatedTokenVertexMap.put(witness2.getEndTokenVertex(), endNode);
  }

  private static void handleMatch(CollationGraph collationGraph, String sigil1, String sigil2, Map<TokenVertex, Node> collatedTokenVertexMap, TokenVertex tokenVertexForWitness1, TokenVertex tokenVertexForWitness2) {
    System.out.println("> " + sigil1 + "+" + sigil2);
    Node newCollationNode = collationGraph.addNodeWithTokens(tokenVertexForWitness1.getToken(), tokenVertexForWitness2.getToken());
    collatedTokenVertexMap.put(tokenVertexForWitness1, newCollationNode);
    collatedTokenVertexMap.put(tokenVertexForWitness2, newCollationNode);
  }

  private static void advanceWitness(CollationGraph collationGraph,//
                                     Map<TokenVertex, Node> collatedTokenVertexMap,//
                                     String sigil, Iterator<TokenVertex> tokenVertexIterator,//
                                     TokenVertex tokenVertexForWitness) {
    if (tokenVertexIterator.hasNext()) {
      TokenVertex nextWitnessVertex = tokenVertexIterator.next();
      while (tokenVertexIterator.hasNext() && !nextWitnessVertex.equals(tokenVertexForWitness)) {
        System.out.println("> " + sigil);
        addCollationNode(collationGraph, collatedTokenVertexMap, nextWitnessVertex);
        nextWitnessVertex = tokenVertexIterator.next();
        System.out.println();
      }
    }
  }

  private static void addEdges(CollationGraph collationGraph, Map<TokenVertex, Node> collatedTokenVertexMap) {
    collatedTokenVertexMap.keySet().forEach(tv -> tv.getIncomingTokenVertexStream().forEach(itv -> {
      Node source = collatedTokenVertexMap.get(itv);
      Node target = collatedTokenVertexMap.get(tv);
      if (source == null || target == null) {
        System.out.println();
      }
      List<Node> existingTargetNodes = collationGraph.getOutgoingEdges(source)//
          .stream()//
          .map(collationGraph::getTarget)//
          .collect(toList());
      String sigil = tv.getSigil();
      if (!existingTargetNodes.contains(target)) {
        Set<String> sigils = new HashSet<>();
        sigils.add(sigil);
        collationGraph.addDirectedEdge(source, target, sigils);
        System.out.println("> " + source + " -> " + target);
      } else {
        TraditionalEdge edge = collationGraph.getOutgoingEdges(source)//
            .stream()//
            .filter(e -> collationGraph.getTarget(e).equals(target))//
            .findFirst()//
            .orElseThrow(() -> new RuntimeException("There should be an edge!"));
        edge.getSigils().add(sigil);
        System.err.println("duplicate edge: " + source + " -> " + target);
      }
    }));
  }

  private static void addCollationNode(CollationGraph collationGraph,//
                                       Map<TokenVertex, CollationGraph.Node> collatedTokenMap,//
                                       TokenVertex tokenVertex) {
    if (!collatedTokenMap.containsKey(tokenVertex)) {
      Node collationNode = collationGraph.addNodeWithTokens(tokenVertex.getToken());
      collatedTokenMap.put(tokenVertex, collationNode);
    }
  }

  private static List<Match> sortMatchesByWitness(List<Match> matches,//
                                                  String sigil1, VariantWitnessGraphRanking ranking1,//
                                                  String sigil2, VariantWitnessGraphRanking ranking2) {
    Comparator<Match> matchComparator = matchComparator(ranking1, sigil1, ranking2, sigil2);
    return matches.stream()//
        .sorted(matchComparator)//
        .collect(toList());
  }

  private static Comparator<Match> matchComparator(VariantWitnessGraphRanking ranking1, String sigil1,//
                                                   VariantWitnessGraphRanking ranking2, String sigil2) {
    return (match1, match2) -> {
      TokenVertex vertex1 = match1.getTokenVertexForWitness(sigil1);
      Integer rank1 = ranking1.apply(vertex1);
      TokenVertex vertex2 = match2.getTokenVertexForWitness(sigil1);
      Integer rank2 = ranking1.apply(vertex2);
      if (rank1.equals(rank2)) {
        TokenVertex vertex12 = match1.getTokenVertexForWitness(sigil2);
        rank1 = ranking2.apply(vertex12);
        TokenVertex vertex22 = match2.getTokenVertexForWitness(sigil2);
        rank2 = ranking2.apply(vertex22);
      }
      return rank1.compareTo(rank2);
    };
  }

  private static List<Match> unusableMatches(List<Match> matchesSortedByWitness,//
                                             String sigil1, String sigil2,//
                                             TokenVertex tokenVertexForWitness1, TokenVertex tokenVertexForWitness2,//
                                             VariantWitnessGraphRanking ranking1, VariantWitnessGraphRanking ranking2) {
    int minRank1 = ranking1.apply(tokenVertexForWitness1);
    int minRank2 = ranking2.apply(tokenVertexForWitness2);

    return matchesSortedByWitness.stream()//
        .filter(m -> m.getTokenVertexForWitness(sigil1).equals(tokenVertexForWitness1)//
            || m.getTokenVertexForWitness(sigil2).equals(tokenVertexForWitness2) //
            || ranking1.apply(m.getTokenVertexForWitness(sigil1)) < minRank1 //
            || ranking2.apply(m.getTokenVertexForWitness(sigil2)) < minRank2)//
        .collect(toList());
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
    Match endMatch = new Match(witness1.getEndTokenVertex(), witness2.getEndTokenVertex());
    matches.add(endMatch);
    return matches;
  }

}
