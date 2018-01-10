package nl.knaw.huygens.hypercollate.collator;

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
import eu.interedition.collatex.dekker.Tuple;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import nl.knaw.huygens.hypercollate.model.CollationGraph;
import nl.knaw.huygens.hypercollate.model.CollationGraph.Node;
import nl.knaw.huygens.hypercollate.model.SimpleTokenVertex;
import nl.knaw.huygens.hypercollate.model.TokenVertex;
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph;
import nl.knaw.huygens.hypercollate.tools.CollationGraphRanking;
import nl.knaw.huygens.hypercollate.tools.CollationGraphVisualizer;
import nl.knaw.huygens.hypercollate.tools.StreamUtil;
import static nl.knaw.huygens.hypercollate.tools.StreamUtil.stream;
import nl.knaw.huygens.hypergraph.core.TraditionalEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class HyperCollator {
  private static final Logger LOG = LoggerFactory.getLogger(HyperCollator.class);

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
//  private final OptimalMatchSetFinder optimalMatchSetFinder;
//
//  public HyperCollater(OptimalMatchSetFinder optimalMatchSetFinder) {
//    Preconditions.checkNotNull(optimalMatchSetFinder);
//    this.optimalMatchSetFinder = optimalMatchSetFinder;
//  }

  public CollationGraph collate(VariantWitnessGraph... graphs) {
    List<String> sigils = new ArrayList<>();
    List<VariantWitnessGraph> witnesses = new ArrayList<>();
    List<VariantWitnessGraphRanking> rankings = new ArrayList<>();
    for (VariantWitnessGraph graph : graphs) {
      sigils.add(graph.getSigil());
      witnesses.add(graph);
      rankings.add(VariantWitnessGraphRanking.of(graph));
    }

    Set<Match> matches = getPotentialMatches(witnesses, rankings);
    CollationGraph collationGraph = new CollationGraph();
    Map<TokenVertex, CollationGraph.Node> collatedTokenVertexMap = new HashMap<>();
    VariantWitnessGraph first = witnesses.remove(0);

    Map<String, List<Match>> matchesSortedByRankPerWitness = sortAndFilterMatchesByWitness(matches, sigils);
    initialize(collationGraph, collatedTokenVertexMap, first);

    for (VariantWitnessGraph witnessGraph : witnesses) {
      visualize(collationGraph);
      collate(collationGraph, //
          witnessGraph, //
          matchesSortedByRankPerWitness.get(witnessGraph.getSigil()), //
          collatedTokenVertexMap);
    }
    return collationGraph;
  }

  private void visualize(CollationGraph collationGraph) {
    String dot = CollationGraphVisualizer.toDot(collationGraph);
    System.out.println(dot);
  }

  void initialize(CollationGraph collationGraph, //
                  Map<TokenVertex, Node> collatedTokenVertexMap, //
                  VariantWitnessGraph witnessGraph) {
    String sigil = witnessGraph.getSigil();
    collationGraph.getSigils().add(sigil);
    collatedTokenVertexMap.put(witnessGraph.getStartTokenVertex(), collationGraph.getRootNode());
    StreamUtil.stream(witnessGraph.vertices())//
        .filter(SimpleTokenVertex.class::isInstance)//
        .forEach(tokenVertex -> {
          Node node = collationGraph.addNodeWithTokens(tokenVertex.getToken());
          node.addBranchPath(tokenVertex.getSigil(), tokenVertex.getBranchPath());
          collatedTokenVertexMap.put(tokenVertex, node);
        });
    Node endNode = collationGraph.addNodeWithTokens();
    collationGraph.setEndNode(endNode);
    collatedTokenVertexMap.put(witnessGraph.getEndTokenVertex(), endNode);
    addEdges(collationGraph, collatedTokenVertexMap);
  }

  private List<CollatedMatch> getCollatedMatches(Map<TokenVertex, Node> collatedTokenVertexMap,//
                                                 List<Match> matches, String sigil) {
    return matches.stream()//
        .peek(System.out::println)//
        .map(match -> toCollatedMatch(match, sigil, collatedTokenVertexMap))//
        .collect(toList());
  }

  private CollatedMatch toCollatedMatch(Match match, String sigil, Map<TokenVertex, Node> collatedTokenVertexMap) {
    TokenVertex vertex = StreamUtil.stream(match.getTokenVertexList())//
        .filter(tv -> !tv.getSigil().equals(sigil))//
        .findFirst()//
        .orElseThrow(() -> new RuntimeException("No vertex!"));
    TokenVertex tokenVertexForWitness = match.getTokenVertexForWitness(sigil);
    Node node = collatedTokenVertexMap.get(vertex);
    return new CollatedMatch(node, tokenVertexForWitness).setVertexRank(match.getRankForWitness(sigil));
  }

  private void collate(CollationGraph collationGraph, //
                       VariantWitnessGraph witnessGraph, //
                       List<Match> sortedMatchesForWitness, //
                       Map<TokenVertex, CollationGraph.Node> collatedTokenVertexMap) {
    Predicate<? super Match> matchesWithCollationGraph = m -> collationGraph.getSigils()//
        .stream()//
        .anyMatch(m::hasWitness);
    CollationGraphRanking baseRanking = CollationGraphRanking.of(collationGraph);
    String witnessSigil = witnessGraph.getSigil();

    List<Match> filteredSortedMatchesForWitness = sortedMatchesForWitness.stream()//
        .filter(matchesWithCollationGraph)//
        .collect(toList());

    List<CollatedMatch> matchList = getCollatedMatches(collatedTokenVertexMap, filteredSortedMatchesForWitness, witnessSigil)//
        .stream()//
        .peek(System.out::println)//
        .map(m -> adjustRankForCollatedNode(m, baseRanking))//
        .distinct()//
        .collect(toList());

    List<CollatedMatch> optimalMatchList = getOptimalMatchList(matchList);

    Iterator<TokenVertex> witnessIterator = VariantWitnessGraphTraversal.of(witnessGraph).iterator();
    TokenVertex first = witnessIterator.next();
    Node rootNode = collationGraph.getRootNode();
    collatedTokenVertexMap.put(first, rootNode);

    logCollated(collatedTokenVertexMap);
    while (!optimalMatchList.isEmpty()) {
      CollatedMatch match = optimalMatchList.remove(0);
      System.out.println(match);
      TokenVertex tokenVertexForWitnessGraph = match.getWitnessVertex();
      advanceWitness(collationGraph, collatedTokenVertexMap, witnessIterator, tokenVertexForWitnessGraph);

      Node matchingNode = match.getCollatedNode();
      Token token = tokenVertexForWitnessGraph.getToken();
      if (token != null) {
        matchingNode.addToken(token);
        matchingNode.addBranchPath(tokenVertexForWitnessGraph.getSigil(), tokenVertexForWitnessGraph.getBranchPath());
      }
      collatedTokenVertexMap.put(tokenVertexForWitnessGraph, matchingNode);
    }
    collationGraph.getSigils().add(witnessSigil);
    collatedTokenVertexMap.put(witnessGraph.getEndTokenVertex(), collationGraph.getEndNode());

    logCollated(collatedTokenVertexMap);
    addEdges(collationGraph, collatedTokenVertexMap);
    logCollated(collatedTokenVertexMap);
  }

  private List<CollatedMatch> getOptimalMatchList(List<CollatedMatch> matchList) {
    return new OptimalCollatedMatchListAlgorithm().getOptimalCollatedMatchList(matchList);
  }

  private CollatedMatch adjustRankForCollatedNode(CollatedMatch m, CollationGraphRanking baseRanking) {
    Node node = m.getCollatedNode();
    Integer rank = baseRanking.apply(node);
    m.setNodeRank(rank);
    return m;
  }

  private void logCollated(Map<TokenVertex, CollationGraph.Node> collatedTokenVertexMap) {
    List<String> lines = new ArrayList<>();
    collatedTokenVertexMap.forEach((k, v) -> lines.add(k.getSigil() + ":" + k.getToken() + " -> " + v));
    LOG.info("collated={}", lines.stream().sorted().collect(joining("\n")));
  }

  private static void advanceWitness(CollationGraph collationGraph, //
                                     Map<TokenVertex, Node> collatedTokenVertexMap, //
                                     Iterator<TokenVertex> tokenVertexIterator, //
                                     TokenVertex tokenVertexForWitness) {
    if (tokenVertexIterator.hasNext()) {
      TokenVertex nextWitnessVertex = tokenVertexIterator.next();
      while (tokenVertexIterator.hasNext() && !nextWitnessVertex.equals(tokenVertexForWitness)) {
        addCollationNode(collationGraph, collatedTokenVertexMap, nextWitnessVertex);
        nextWitnessVertex = tokenVertexIterator.next();
      }
    }
  }

  private static void addEdges(CollationGraph collationGraph, Map<TokenVertex, Node> collatedTokenVertexMap) {
    collatedTokenVertexMap.keySet().forEach(tv -> tv.getIncomingTokenVertexStream().forEach(itv -> {
      Node source = collatedTokenVertexMap.get(itv);
      Node target = collatedTokenVertexMap.get(tv);
      if (source == null || target == null) {
        throw new RuntimeException("source or target is null!");
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
        // System.out.println("> " + source + " -> " + target);
      } else {
        TraditionalEdge edge = collationGraph.getOutgoingEdges(source)//
            .stream()//
            .filter(e -> target.equals(collationGraph.getTarget(e)))//
            .findFirst()//
            .orElseThrow(() -> new RuntimeException("There should be an edge!"));
        edge.getSigils().add(sigil);
        // System.err.println("duplicate edge: " + source + " -> " + target);
      }
    }));
  }

  private static void addCollationNode(CollationGraph collationGraph, //
                                       Map<TokenVertex, CollationGraph.Node> collatedTokenMap, //
                                       TokenVertex tokenVertex) {
    if (!collatedTokenMap.containsKey(tokenVertex)) {
      Node collationNode = collationGraph.addNodeWithTokens(tokenVertex.getToken());
      collationNode.addBranchPath(tokenVertex.getSigil(), tokenVertex.getBranchPath());
      collatedTokenMap.put(tokenVertex, collationNode);
    }
  }

  Map<String, List<Match>> sortAndFilterMatchesByWitness(Set<Match> matches, List<String> sigils) {
    Map<String, List<Match>> map = new HashMap<>();
    sigils.forEach(s -> {
      List<Match> sortedMatchesForWitness = filterAndSortMatchesForWitness(matches, s);
      map.put(s, sortedMatchesForWitness);
    });
    return map;
  }

  private List<Match> filterAndSortMatchesForWitness(Set<Match> matches, String sigil) {
    Comparator<Match> comparator = (match1, match2) -> {
      Integer rank1 = match1.getRankForWitness(sigil);
      Integer rank2 = match2.getRankForWitness(sigil);
      if (rank1.equals(rank2)) {
        rank1 = match1.getLowestRankForWitnessesOtherThan(sigil);
        rank2 = match2.getLowestRankForWitnessesOtherThan(sigil);
      }
      return rank1.compareTo(rank2);
    };
    return matches.stream()//
        .filter(m -> m.hasWitness(sigil))//
        .sorted(comparator)//
        .collect(toList());
  }

  Set<Match> getPotentialMatches(List<VariantWitnessGraph> witnesses, List<VariantWitnessGraphRanking> rankings) {
    Set<Match> allPotentialMatches = new HashSet<>();
    Map<TokenVertex, Match> vertexToMatch = new HashMap<>();
    for (Tuple<Integer> t : permute(witnesses.size())) {
      VariantWitnessGraph witness1 = witnesses.get(t.left);
      VariantWitnessGraph witness2 = witnesses.get(t.right);
      VariantWitnessGraphRanking ranking1 = rankings.get(t.left);
      VariantWitnessGraphRanking ranking2 = rankings.get(t.right);
      match(witness1, witness2, ranking1, ranking2, allPotentialMatches, vertexToMatch);
      TokenVertex endTokenVertex1 = witness1.getEndTokenVertex();
      TokenVertex endTokenVertex2 = witness2.getEndTokenVertex();
      Match endMatch = new Match(endTokenVertex1, endTokenVertex2);
      String sigil1 = witness1.getSigil();
      Integer rank1 = ranking1.apply(endTokenVertex1);
      endMatch.setRank(sigil1, rank1);
      String sigil2 = witness2.getSigil();
      Integer rank2 = ranking2.apply(endTokenVertex2);
      endMatch.setRank(sigil2, rank2);
      allPotentialMatches.add(endMatch);
    }

    return allPotentialMatches;
  }

  List<Tuple<Integer>> permute(int max) {
    List<Tuple<Integer>> list = new ArrayList<>();
    for (int left = 0; left < max; left++) {
      for (int right = left + 1; right < max; right++) {
        list.add(new Tuple<>(left, right));
      }
    }
    return list;
  }

  private void match(VariantWitnessGraph witness1, VariantWitnessGraph witness2,//
                     VariantWitnessGraphRanking ranking1, VariantWitnessGraphRanking ranking2,//
                     Set<Match> allPotentialMatches,//
                     Map<TokenVertex, Match> vertexToMatch) {
    VariantWitnessGraphTraversal traversal1 = VariantWitnessGraphTraversal.of(witness1);
    VariantWitnessGraphTraversal traversal2 = VariantWitnessGraphTraversal.of(witness2);
    String sigil1 = witness1.getSigil();
    String sigil2 = witness2.getSigil();
    stream(traversal1)//
        .filter(tv -> tv instanceof SimpleTokenVertex)//
        .map(SimpleTokenVertex.class::cast)//
        .forEach(tv1 -> stream(traversal2)//
            .filter(tv -> tv instanceof SimpleTokenVertex)//
            .map(SimpleTokenVertex.class::cast)//
            .forEach(tv2 -> {
              if (matcher.apply(tv1, tv2)) {
                Match match = new Match(tv1, tv2)//
                    .setRank(sigil1, ranking1.apply(tv1))//
                    .setRank(sigil2, ranking2.apply(tv2));
                allPotentialMatches.add(match);
                vertexToMatch.put(tv1, match);
                vertexToMatch.put(tv2, match);
              }
            }));
  }

//  public String getOptimalMatchSetFinderName() {
//    return optimalMatchSetFinder.getName();
//  }

}
