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
import java.util.Map;
import java.util.function.BiFunction;

import eu.interedition.collatex.VariantGraph.Vertex;
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
    Iterator<VariantWitnessGraph> witnessIterable = Arrays.asList(graphs).iterator();
    VariantWitnessGraph firstWitness = witnessIterable.next();
    VariantWitnessGraph secondWitness = witnessIterable.next();

    VariantWitnessGraphRanking ranking1 = VariantWitnessGraphRanking.of(firstWitness);
    VariantWitnessGraphRanking ranking2 = VariantWitnessGraphRanking.of(secondWitness);

    List<Match> matches = match(firstWitness, secondWitness);
    Comparator<Match> byFirstWitness = (m1, m2) -> {
      String sigil = graphs[0].getSigil();
      SimpleTokenVertex stv1 = m1.getTokenVertexForWitness(sigil);
      Integer rank1 = ranking1.apply(stv1);
      SimpleTokenVertex stv2 = m2.getTokenVertexForWitness(sigil);
      Integer rank2 = ranking1.apply(stv2);
      return rank1.compareTo(rank2);
    };
    Comparator<Match> bySecondWitness = (m1, m2) -> {
      String sigil = graphs[1].getSigil();
      SimpleTokenVertex stv1 = m1.getTokenVertexForWitness(sigil);
      Integer rank1 = ranking2.apply(stv1);
      SimpleTokenVertex stv2 = m2.getTokenVertexForWitness(sigil);
      Integer rank2 = ranking2.apply(stv2);
      return rank1.compareTo(rank2);
    };
    List<Match> matchesSortedByFirstWitness = matches.stream().sorted(byFirstWitness).collect(toList());
    List<Match> matchesSortedBySecondWitness = matches.stream().sorted(bySecondWitness).collect(toList());

    int rank1 = 0;
    int rank2 = 0;
    String sigil1 = graphs[0].getSigil();
    String sigil2 = graphs[1].getSigil();

    while (!matchesSortedByFirstWitness.isEmpty()) {
      Match matchOption1 = matchesSortedByFirstWitness.get(0);
      Match matchOption2 = matchesSortedBySecondWitness.get(0);

      SimpleTokenVertex tokenVertex11 = matchOption1.getTokenVertexForWitness(sigil1);
      int rank11 = ranking1.apply(tokenVertex11);
      SimpleTokenVertex tokenVertex12 = matchOption1.getTokenVertexForWitness(sigil2);
      int rank12 = ranking2.apply(tokenVertex12);
      int diff1 = rank11 - rank1 + rank12 - rank2;

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
      System.out.println(match);

      matchesSortedByFirstWitness.remove(match);
      matchesSortedBySecondWitness.remove(match);

      // TODO something

    }
    // merge(collationGraph, firstWitness, Collections.emptyMap());

    return collationGraph;
  }

  private static List<Match> match(VariantWitnessGraph firstWitness, VariantWitnessGraph secondWitness) {
    List<Match> matches = new ArrayList<>();
    VariantWitnessGraphTraversal traversal1 = VariantWitnessGraphTraversal.of(firstWitness);
    VariantWitnessGraphTraversal traversal2 = VariantWitnessGraphTraversal.of(secondWitness);
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

  private static void merge(CollationGraph collationGraph, VariantWitnessGraph witnessGraph, Map<Object, Object> alignmentMap) {
    Vertex start = collationGraph.getStart();
    if (collationGraph.isEmpty()) {
      TokenVertex startTokenVertex = witnessGraph.getStartTokenVertex();
      startTokenVertex.getOutgoingTokenVertexStream().forEach(tv -> {
        // start.add
      });
    } else {

    }

  }
}
