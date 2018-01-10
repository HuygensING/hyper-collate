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

import nl.knaw.huygens.hypercollate.model.TokenVertex;
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph;

import java.util.*;

public class VariantWitnessGraphTraversal implements Iterable<TokenVertex> {
  private final VariantWitnessGraph graph;

  private VariantWitnessGraphTraversal(VariantWitnessGraph graph) {
    this.graph = graph;
  }

  public static VariantWitnessGraphTraversal of(VariantWitnessGraph graph) {
    return new VariantWitnessGraphTraversal(graph);
  }

  @Override
  public Iterator<TokenVertex> iterator() {
    return new Iterator<TokenVertex>() {

      private final Map<TokenVertex, Long> encountered = new HashMap<>();
      private final Queue<TokenVertex> queue = new ArrayDeque<>();
      private Optional<TokenVertex> next = Optional.of(graph.getStartTokenVertex());

      @Override
      public boolean hasNext() {
        return next.isPresent();
      }

      @Override
      public TokenVertex next() {
        final TokenVertex next = this.next.get();
        next.getOutgoingTokenVertexStream().forEach(outgoing -> {

          final long endEncountered = Optional.ofNullable(encountered.get(outgoing)).orElse(0L);
          final long endIncoming = outgoing.getIncomingTokenVertexStream()//
              .count();

          if (endIncoming == endEncountered) {
            throw new IllegalStateException(String.format("Encountered cycle traversing %s to %s", next, outgoing));
          } else if ((endIncoming - endEncountered) == 1) {
            queue.add(outgoing);
          }

          encountered.put(outgoing, endEncountered + 1);

        });
        this.next = Optional.ofNullable(queue.poll());
        return next;
      }
    };
  }
}
