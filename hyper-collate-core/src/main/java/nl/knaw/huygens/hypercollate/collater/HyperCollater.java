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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import eu.interedition.collatex.VariantGraph.Vertex;
import nl.knaw.huygens.hypercollate.model.CollationGraph;
import nl.knaw.huygens.hypercollate.model.TokenVertex;
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph;

public class HyperCollater {

  public static CollationGraph collate(VariantWitnessGraph... graphs) {
    CollationGraph collationGraph = new CollationGraph();
    Iterator<VariantWitnessGraph> witnessIterable = Arrays.asList(graphs).iterator();
    VariantWitnessGraph firstWitness = witnessIterable.next();

    merge(collationGraph, firstWitness, Collections.emptyMap());

    return collationGraph;
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
