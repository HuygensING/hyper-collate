package nl.knaw.huygens.hypercollate.collator;

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

import eu.interedition.collatex.Token;
import eu.interedition.collatex.dekker.Tuple;
import nl.knaw.huygens.hypercollate.model.*;
import nl.knaw.huygens.hypercollate.tools.CollationGraphRanking;
import nl.knaw.huygens.hypercollate.tools.CollationGraphVisualizer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static nl.knaw.huygens.hypercollate.tools.StreamUtil.stream;

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

  public CollationGraph collate(VariantWitnessGraph... graphs) {
    List<String> sigils = new ArrayList<>();
    List<VariantWitnessGraph> witnesses = new ArrayList<>();
    List<VariantWitnessGraphRanking> rankings = new ArrayList<>();
    Arrays.stream(graphs)//
        .sorted(comparing(VariantWitnessGraph::getSigil))//
        .forEach(graph -> {
          sigils.add(graph.getSigil());
          witnesses.add(graph);
          rankings.add(VariantWitnessGraphRanking.of(graph));
        });

    Set<Match> matches = getPotentialMatches(witnesses, rankings);
    CollationGraph collationGraph = new CollationGraph();
    Map<TokenVertex, TextNode> collatedTokenVertexMap = new HashMap<>();
    VariantWitnessGraph first = witnesses.remove(0);

    Map<String, List<Match>> matchesSortedByRankPerWitness = sortAndFilterMatchesByWitness(matches, sigils);
    Map<Markup, MarkupNode> markupNodeIndex = new HashMap<>();

    initialize(collationGraph, collatedTokenVertexMap, markupNodeIndex, first);
    visualize(collationGraph);

    for (VariantWitnessGraph witnessGraph : witnesses) {
      List<Match> sortedMatchesForWitness = matchesSortedByRankPerWitness.get(witnessGraph.getSigil());
      collate(collationGraph, //
          witnessGraph, //
          sortedMatchesForWitness, //
          markupNodeIndex, //
          collatedTokenVertexMap);
      visualize(collationGraph);
    }
    return collationGraph;
  }

  private void visualize(CollationGraph collationGraph) {
    String dot = CollationGraphVisualizer.toDot(collationGraph, true);
    System.out.println(dot);
  }

  void initialize(CollationGraph collationGraph, //
                  Map<TokenVertex, TextNode> collatedTokenVertexMap, //
                  Map<Markup, MarkupNode> markupNodeIndex,//
                  VariantWitnessGraph witnessGraph) {
    String sigil = witnessGraph.getSigil();
    collationGraph.getSigils().add(sigil);
    addMarkupNodes(collationGraph, markupNodeIndex, witnessGraph);

    collatedTokenVertexMap.put(witnessGraph.getStartTokenVertex(), collationGraph.getTextStartNode());
    stream(witnessGraph.vertices())//
        .filter(SimpleTokenVertex.class::isInstance)//
        .forEach(tokenVertex ->
            addCollationNode(collationGraph, collatedTokenVertexMap, tokenVertex, witnessGraph, markupNodeIndex)
        );
    collatedTokenVertexMap.put(witnessGraph.getEndTokenVertex(), collationGraph.getTextEndNode());
    addEdges(collationGraph, collatedTokenVertexMap);
  }

  private void addMarkupNodes(CollationGraph collationGraph, Map<Markup, MarkupNode> markupNodeIndex, VariantWitnessGraph witnessGraph) {
    witnessGraph.getMarkupStream().forEach(markup -> {
      MarkupNode markupNode = collationGraph.addMarkupNode(witnessGraph.getSigil(), markup);
      markupNodeIndex.put(markup, markupNode);
    });
  }

  private List<CollatedMatch> getCollatedMatches(Map<TokenVertex, TextNode> collatedTokenVertexMap,//
                                                 List<Match> matches, String sigil) {
    return matches.stream()//
//        .peek(System.out::println)//
        .map(match -> toCollatedMatch(match, sigil, collatedTokenVertexMap))//
        .collect(toList());
  }

  private CollatedMatch toCollatedMatch(Match match, String sigil, Map<TokenVertex, TextNode> collatedTokenVertexMap) {
    TokenVertex vertex = stream(match.getTokenVertexList())//
        .filter(tv -> !tv.getSigil().equals(sigil))//
        .findFirst()//
        .orElseThrow(() -> new RuntimeException("No vertex!"));
    TokenVertex tokenVertexForWitness = match.getTokenVertexForWitness(sigil);
    TextNode node = collatedTokenVertexMap.get(vertex);
    return new CollatedMatch(node, tokenVertexForWitness).setVertexRank(match.getRankForWitness(sigil));
  }

  private void collate(CollationGraph collationGraph, //
                       VariantWitnessGraph witnessGraph, //
                       List<Match> sortedMatchesForWitness, //
                       Map<Markup, MarkupNode> markupNodeIndex,//
                       Map<TokenVertex, TextNode> collatedTokenVertexMap) {
    Predicate<? super Match> matchesWithCollationGraph = m -> collationGraph.getSigils()//
        .stream()//
        .anyMatch(m::hasWitness);
    CollationGraphRanking baseRanking = CollationGraphRanking.of(collationGraph);

    List<Match> filteredSortedMatchesForWitness = sortedMatchesForWitness.stream()//
        .filter(matchesWithCollationGraph)//
        .collect(toList());

    String witnessSigil = witnessGraph.getSigil();
    collationGraph.getSigils().add(witnessSigil);
    addMarkupNodes(collationGraph, markupNodeIndex, witnessGraph);

    List<CollatedMatch> matchList = getCollatedMatches(collatedTokenVertexMap, filteredSortedMatchesForWitness, witnessSigil)//
        .stream()//
//        .peek(System.out::println)//
        .map(m -> adjustRankForCollatedNode(m, baseRanking))//
        .distinct()//
        .collect(toList());
    LOG.info("matchList={}", matchList);
    OptimalCollatedMatchListAlgorithm optimalCollatedMatchListAlgorithm = new OptimalCollatedMatchListAlgorithm();
    List<CollatedMatch> optimalMatchList = optimalCollatedMatchListAlgorithm.getOptimalCollatedMatchList(matchList);
//    DecisionTreeNode decisionTreeRootNode = optimalCollatedMatchListAlgorithm.getDecisionTreeRootNode();
//    visualize(decisionTreeRootNode);

    LOG.info("optimalMatchList={}", optimalMatchList);

    Iterator<TokenVertex> witnessIterator = VariantWitnessGraphTraversal.of(witnessGraph).iterator();
    TokenVertex first = witnessIterator.next();
    TextNode rootNode = collationGraph.getTextStartNode();
    collatedTokenVertexMap.put(first, rootNode);

    logCollated(collatedTokenVertexMap);
    while (!optimalMatchList.isEmpty()) {
      CollatedMatch match = optimalMatchList.remove(0);
      System.out.println(match);
      TokenVertex tokenVertexForWitnessGraph = match.getWitnessVertex();
      advanceWitness(collationGraph, collatedTokenVertexMap, witnessIterator, tokenVertexForWitnessGraph, witnessGraph, markupNodeIndex);

      TextNode matchingNode = match.getCollatedNode();
      Token token = tokenVertexForWitnessGraph.getToken();
      if (token != null) {
        matchingNode.addToken(token);
        matchingNode.addBranchPath(tokenVertexForWitnessGraph.getSigil(), tokenVertexForWitnessGraph.getBranchPath());
      }
      collatedTokenVertexMap.put(tokenVertexForWitnessGraph, matchingNode);
      addMarkupHyperEdges(collationGraph, witnessGraph, markupNodeIndex, tokenVertexForWitnessGraph, matchingNode);
    }
    collatedTokenVertexMap.put(witnessGraph.getEndTokenVertex(), collationGraph.getTextEndNode());

    logCollated(collatedTokenVertexMap);
    addEdges(collationGraph, collatedTokenVertexMap);
    logCollated(collatedTokenVertexMap);
  }

  private void addMarkupHyperEdges(CollationGraph collationGraph, VariantWitnessGraph witnessGraph, Map<Markup, MarkupNode> markupNodeIndex, TokenVertex tokenVertexForWitnessGraph, TextNode matchingNode) {
    witnessGraph.getMarkupListForTokenVertex(tokenVertexForWitnessGraph)//
        .forEach(markup -> collationGraph.linkMarkupToText(markupNodeIndex.get(markup), matchingNode));
  }

  private List<CollatedMatch> getOptimalMatchList(List<CollatedMatch> matchList) {
    return new OptimalCollatedMatchListAlgorithm().getOptimalCollatedMatchList(matchList);
  }

  private CollatedMatch adjustRankForCollatedNode(CollatedMatch m, CollationGraphRanking baseRanking) {
    TextNode node = m.getCollatedNode();
    Integer rank = baseRanking.apply(node);
    m.setNodeRank(rank);
    return m;
  }

  private void logCollated(Map<TokenVertex, TextNode> collatedTokenVertexMap) {
    List<String> lines = new ArrayList<>();
    collatedTokenVertexMap.forEach((k, v) -> lines.add(k.getSigil() + ":" + k.getToken() + " -> " + v));
    LOG.info("collated={}", lines.stream().sorted().collect(joining("\n")));
  }

  private void advanceWitness(CollationGraph collationGraph, //
                              Map<TokenVertex, TextNode> collatedTokenVertexMap, //
                              Iterator<TokenVertex> tokenVertexIterator, //
                              TokenVertex tokenVertexForWitness,//
                              VariantWitnessGraph witnessGraph,//
                              Map<Markup, MarkupNode> markupNodeIndex) {
    if (tokenVertexIterator.hasNext()) {
      TokenVertex nextWitnessVertex = tokenVertexIterator.next();
      while (tokenVertexIterator.hasNext() && !nextWitnessVertex.equals(tokenVertexForWitness)) {
        addCollationNode(collationGraph, collatedTokenVertexMap, nextWitnessVertex, witnessGraph, markupNodeIndex);
        nextWitnessVertex = tokenVertexIterator.next();
      }
    }
  }

  private static void addEdges(CollationGraph collationGraph, Map<TokenVertex, TextNode> collatedTokenVertexMap) {
    collatedTokenVertexMap.keySet()
        .forEach(tv -> tv.getIncomingTokenVertexStream()
            .forEach(itv -> {
              TextNode source = collatedTokenVertexMap.get(itv);
              TextNode target = collatedTokenVertexMap.get(tv);
              if (source == null) {
                throw new RuntimeException("source=null, target=" + target);
              }
              if (target == null) {
                throw new RuntimeException("target=null, source=" + source);
              }
              List<TextNode> existingTargetNodes = collationGraph.getOutgoingTextEdgeStream(source)
                  .map(collationGraph::getTarget)//
                  .map(TextNode.class::cast)//
                  .collect(toList());
              String sigil = tv.getSigil();
              if (!existingTargetNodes.contains(target)) {
                Set<String> sigils = new HashSet<>();
                sigils.add(sigil);
                collationGraph.addDirectedEdge(source, target, sigils);
                // System.out.println("> " + source + " -> " + target);
              } else {
                TextEdge edge = collationGraph.getOutgoingTextEdgeStream(source)
                    .filter(e -> target.equals(collationGraph.getTarget(e)))//
                    .findFirst()//
                    .orElseThrow(() -> new RuntimeException("There should be an edge!"));
                edge.addSigil(sigil);
                // System.err.println("duplicate edge: " + source + " -> " + target);
              }
            }));
  }

  private void addCollationNode(CollationGraph collationGraph, //
                                Map<TokenVertex, TextNode> collatedTokenVertexMap, //
                                TokenVertex tokenVertex,//
                                VariantWitnessGraph witnessGraph,//
                                Map<Markup, MarkupNode> markupNodeIndex) {
    if (!collatedTokenVertexMap.containsKey(tokenVertex)) {
      TextNode collationNode = collationGraph.addTextNodeWithTokens(tokenVertex.getToken());
      collationNode.addBranchPath(tokenVertex.getSigil(), tokenVertex.getBranchPath());
      collatedTokenVertexMap.put(tokenVertex, collationNode);
      addMarkupHyperEdges(collationGraph, witnessGraph, markupNodeIndex, tokenVertex, collationNode);
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
      VariantWitnessGraphRanking ranking1 = rankings.get(t.left);
      VariantWitnessGraph witness2 = witnesses.get(t.right);
      VariantWitnessGraphRanking ranking2 = rankings.get(t.right);
      match(witness1, witness2, ranking1, ranking2, allPotentialMatches, vertexToMatch);

      Match endMatch = getEndMatch(witness1, ranking1, witness2, ranking2);
      allPotentialMatches.add(endMatch);
    }

    return allPotentialMatches;
  }

  private Match getEndMatch(VariantWitnessGraph witness1, VariantWitnessGraphRanking ranking1, VariantWitnessGraph witness2, VariantWitnessGraphRanking ranking2) {
    TokenVertex endTokenVertex1 = witness1.getEndTokenVertex();
    TokenVertex endTokenVertex2 = witness2.getEndTokenVertex();
    Match endMatch = new Match(endTokenVertex1, endTokenVertex2);
    String sigil1 = witness1.getSigil();
    Integer rank1 = ranking1.apply(endTokenVertex1);
    endMatch.setRank(sigil1, rank1);
    String sigil2 = witness2.getSigil();
    Integer rank2 = ranking2.apply(endTokenVertex2);
    endMatch.setRank(sigil2, rank2);
    return endMatch;
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
        .filter(SimpleTokenVertex.class::isInstance)//
        .map(SimpleTokenVertex.class::cast)//
        .forEach(tv1 -> stream(traversal2)//
            .filter(SimpleTokenVertex.class::isInstance)//
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

  private void visualize(DecisionTreeNode decisionTreeRootNode) {
    int indent = 0;
    visualize(decisionTreeRootNode, indent);
  }

  private void visualize(DecisionTreeNode decisionTreeNode, int indent) {
    String tab = indent == 0 ? "" : StringUtils.repeat("| ", indent - 1) + "|-";
    System.out.println(tab + decisionTreeNode.getQuantumCollatedMatchList().toString());
    for (DecisionTreeNode treeNode : decisionTreeNode.getChildNodes()) {
      visualize(treeNode, indent + 1);
    }
  }


}
