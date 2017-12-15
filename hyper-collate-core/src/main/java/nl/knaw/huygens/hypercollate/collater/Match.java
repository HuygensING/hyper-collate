package nl.knaw.huygens.hypercollate.collater;

import com.google.common.base.Joiner;
import nl.knaw.huygens.hypercollate.model.SimpleTokenVertex;
import nl.knaw.huygens.hypercollate.model.TokenVertex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/*-
 * #%L
 * hyper-collate-core
 * =======
 * Copyright (C) 2017 Huygens ING (KNAW)
 * =======
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * #L%
 */

public class Match {

  private final Map<String, TokenVertex> tokenVertexMap = new TreeMap<>();
  private final Map<String, Integer> rankingMap = new TreeMap<>();

  public Match(TokenVertex... matchingTokenVertices) {
    for (TokenVertex mtv : matchingTokenVertices) {
      String sigil = mtv.getSigil();
      tokenVertexMap.put(sigil, mtv);
    }
  }

  public Iterable<TokenVertex> getTokenVertexList() {
    return tokenVertexMap.values();
  }

  public TokenVertex getTokenVertexForWitness(String sigil) {
    return tokenVertexMap.get(sigil);
  }

  public Match setRank(String sigil, Integer rank) {
    rankingMap.put(sigil, rank);
    return this;
  }

  public Integer getRankForWitness(String sigil) {
    return rankingMap.get(sigil);
  }

  public Integer getLowestRankForWitnessesOtherThan(String s) {
    return rankingMap.entrySet()//
        .stream()//
        .filter(e -> !e.getKey().equals(s))//
        .mapToInt(Map.Entry::getValue)//
        .min()//
        .getAsInt();
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder("<");
    List<String> vertexStrings = new ArrayList<>();
    tokenVertexMap.forEach((sigil, vertex) -> {//
      StringBuilder vString = new StringBuilder();
      if (vertex instanceof SimpleTokenVertex) {
        SimpleTokenVertex sv = (SimpleTokenVertex) vertex;
        vString.append(sigil)//
            .append(sv.getIndexNumber());
        // vString.append(sigil)//
        // .append("[")//
        // .append(sv.getIndexNumber())//
        // .append(",r")//
        // .append(rankingMap.get(sigil))//
        // .append("]:'")//
        // .append(sv.getContent().replace("\n", "\\n"))//
        // .append("'");
      } else {
        vString.append(sigil).append(":").append(vertex.getClass().getSimpleName());
      }
      vertexStrings.add(vString.toString());
    });
    return stringBuilder.append(Joiner.on(",").join(vertexStrings)).append(">").toString();
  }

  public List<String> witnessSigils() {
    return new ArrayList<>(tokenVertexMap.keySet());
  }

  public Match addTokenVertex(SimpleTokenVertex tokenVertex) {
    tokenVertexMap.put(tokenVertex.getSigil(), tokenVertex);
    return this;
  }

  public boolean hasWitness(String sigil) {
    return tokenVertexMap.containsKey(sigil);
  }

}
