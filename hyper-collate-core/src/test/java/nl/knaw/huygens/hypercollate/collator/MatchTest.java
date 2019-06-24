package nl.knaw.huygens.hypercollate.collator;

import eu.interedition.collatex.Token;
import nl.knaw.huygens.hypercollate.HyperCollateTest;
import nl.knaw.huygens.hypercollate.model.TokenVertex;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/*-
 * #%L
 * hyper-collate-core
 * =======
 * Copyright (C) 2017 - 2019 Huygens ING (KNAW)
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
    return new TokenVertex() {
      @Override
      public Token getToken() {
        return null;
      }

      @Override
      public void addIncomingTokenVertex(TokenVertex incoming) {
      }

      @Override
      public Stream<TokenVertex> getIncomingTokenVertexStream() {
        return null;
      }

      @Override
      public void addOutgoingTokenVertex(TokenVertex outgoing) {
      }

      @Override
      public Stream<TokenVertex> getOutgoingTokenVertexStream() {
        return null;
      }

      @Override
      public String getSigil() {
        return sigil;
      }

      @Override
      public List<Integer> getBranchPath() {
        return new ArrayList<>();
      }
    };
  }

}
