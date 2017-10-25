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

import java.util.Map;
import java.util.TreeMap;

import nl.knaw.huygens.hypercollate.model.SimpleTokenVertex;

public class Match {

  Map<String, SimpleTokenVertex> tokenVertexMap = new TreeMap<>();

  public Match(SimpleTokenVertex... matchingTokenVertices) {
    for (SimpleTokenVertex mtv : matchingTokenVertices) {
      tokenVertexMap.put(mtv.getToken().getWitness().getSigil(), mtv);
    }
  }

  public Iterable<SimpleTokenVertex> getTokenVertexList() {
    return tokenVertexMap.values();
  }

  public SimpleTokenVertex getTokenVertexForWitness(String sigil) {
    return tokenVertexMap.get(sigil);
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    tokenVertexMap.forEach((k, v) -> //
    stringBuilder.append(k)//
        .append(":")//
        .append(v.getIndexNumber())//
        .append(":'")//
        .append(v.getContent().replace("\n", "\\n"))//
        .append("' "));
    return stringBuilder.toString();
  }
}
