package nl.knaw.huygens.hypercollate.collator;

/*-
 * #%L
 * hyper-collate-core
 * =======
 * Copyright (C) 2017 - 2020 Huygens ING (KNAW)
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

import nl.knaw.huygens.hypercollate.HyperCollateTest;
import nl.knaw.huygens.hypercollate.importer.XMLImporter;
import nl.knaw.huygens.hypercollate.model.*;
import nl.knaw.huygens.hypercollate.tools.TokenMerger;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class VariantWitnessGraphRankingTest extends HyperCollateTest {

  @Test
  public void test() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wg0 =
        importer.importXML(
            "A", "<xml>Een ongeluk komt <del>nooit</del><add>zelden</add> alleen.</xml>");
    VariantWitnessGraph witnessGraph = TokenMerger.merge(wg0);

    VariantWitnessGraphRanking ranking = VariantWitnessGraphRanking.of(witnessGraph);
    Map<Integer, Set<TokenVertex>> byRank = ranking.getByRank();
    byRank.forEach((key, value) -> System.out.println(key + ":" + value));

    assertThat(byRank.get(0)).hasSize(1);
    assertThat(byRank.get(0).iterator().next()).isInstanceOf(StartTokenVertex.class);

    assertThat(byRank.get(1)).hasSize(1);
    assertThat(byRank.get(1).iterator().next()).isInstanceOf(SimpleTokenVertex.class);
    TokenVertex tokenVertex = byRank.get(1).iterator().next();
    assertThat(tokenVertex).isInstanceOf(SimpleTokenVertex.class);
    String content = ((MarkedUpToken) tokenVertex.getToken()).getContent();
    assertThat(content).isEqualTo("Een ongeluk komt ");

    assertThat(byRank.get(2)).hasSize(2);
    List<TokenVertex> tokenVertices = byRank.get(2).stream().sorted().collect(toList());

    TokenVertex tokenVertex1 = tokenVertices.get(0);
    assertThat(tokenVertex1).isInstanceOf(SimpleTokenVertex.class);
    String content1 = ((MarkedUpToken) tokenVertex1.getToken()).getContent();
    assertThat(content1).isEqualTo("nooit");

    TokenVertex tokenVertex2 = tokenVertices.get(1);
    assertThat(tokenVertex2).isInstanceOf(SimpleTokenVertex.class);
    String content2 = ((MarkedUpToken) tokenVertex2.getToken()).getContent();
    assertThat(content2).isEqualTo("zelden");

    // Map<TokenVertex, Integer> byVertex = ranking.getByVertex();

  }
}
