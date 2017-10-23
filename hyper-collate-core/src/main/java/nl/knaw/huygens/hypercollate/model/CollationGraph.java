package nl.knaw.huygens.hypercollate.model;

import java.util.ArrayList;
import java.util.List;

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
import eu.interedition.collatex.VariantGraph.Vertex;

public class CollationGraph {

  private static Vertex startVertex;
  private static Vertex endVertex;
  private List<Vertex> vertices = new ArrayList<>();

  public Vertex getStart() {
    return startVertex;
  }

  public Vertex getEnd() {
    return endVertex;
  }

  public boolean isEmpty() {
    return vertices.isEmpty();
  }

}
