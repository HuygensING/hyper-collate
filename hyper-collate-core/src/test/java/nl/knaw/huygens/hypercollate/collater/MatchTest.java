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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import nl.knaw.huygens.hypercollate.HyperCollateTest;
import nl.knaw.huygens.hypercollate.model.TokenVertex;

public class MatchTest extends HyperCollateTest {
  @Test
  public void testGetLowestRankForWitnessesOtherThan() {
    TokenVertex v1 = mockVertexWithSigil("A");
    TokenVertex v2 = mockVertexWithSigil("B");
    TokenVertex v3 = mockVertexWithSigil("C");
    TokenVertex v4 = mockVertexWithSigil("D");
    Match match = new Match(v1, v2, v3, v4)//
        .setRank("A", 1)//
        .setRank("B", 2)//
        .setRank("C", 3)//
        .setRank("D", 4);
    Integer lowestRankForWitnessesOtherThan = match.getLowestRankForWitnessesOtherThan("A");
    assertThat(lowestRankForWitnessesOtherThan).isEqualTo(2);
  }

  private TokenVertex mockVertexWithSigil(String sigil) {
    TokenVertex vertex = mock(TokenVertex.class);
    when(vertex.getSigil()).thenReturn(sigil);
    return vertex;
  }

}
