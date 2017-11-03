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

import nl.knaw.huygens.hypercollate.model.SimpleTokenVertex;
import nl.knaw.huygens.hypercollate.model.TokenVertex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Match {

  final Map<String, TokenVertex> tokenVertexMap = new TreeMap<>();
  final Map<String, Integer> rankingMap = new TreeMap<>();

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

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    tokenVertexMap.forEach((sigil, vertex) -> {//
      if (vertex instanceof SimpleTokenVertex) {
        SimpleTokenVertex sv = (SimpleTokenVertex) vertex;
        stringBuilder.append(sigil)//
            .append("[")//
            .append(sv.getIndexNumber())//
            .append(",r")//
            .append(rankingMap.get(sigil))//
            .append("] '")//
            .append(sv.getContent().replace("\n", "\\n"))//
            .append("' ");
      } else {
        stringBuilder.append(":").append(vertex.getClass().getSimpleName());
      }
    });
    return stringBuilder.toString();
  }

  public List<String> witnessSigils() {
    return new ArrayList<>(tokenVertexMap.keySet());
  }
}
