package nl.knaw.huygens.hypercollate.collator;

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

import nl.knaw.huygens.hypercollate.HyperCollateTest;
import nl.knaw.huygens.hypercollate.importer.XMLImporter;
import nl.knaw.huygens.hypercollate.model.*;
import nl.knaw.huygens.hypercollate.tools.TokenMerger;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class VariantWitnessGraphTraversalTest extends HyperCollateTest {

  @Test
  public void testVariantWitnessGraphTraversal() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wg0 = importer.importXML("A", "<xml>Eeny, meeny, miny, <del>curly</del><add>moe</add>!</xml>");
    VariantWitnessGraph witnessGraph = TokenMerger.merge(wg0);

    VariantWitnessGraphTraversal traversal = VariantWitnessGraphTraversal.of(witnessGraph);
    // for (TokenVertex tv : traversal) {
    // if (tv instanceof SimpleTokenVertex) {
    // MarkedUpToken markedUpToken = (MarkedUpToken) tv.getToken();
    // System.out.println("'" + markedUpToken.getContent() + "'");
    // }
    // }
    Iterator<TokenVertex> iterator = traversal.iterator();
    TokenVertex tokenVertex = iterator.next();
    assertThat(tokenVertex).isInstanceOf(StartTokenVertex.class);

    tokenVertex = iterator.next();
    assertThat(tokenVertex).isInstanceOf(SimpleTokenVertex.class);
    MarkedUpToken markedUpToken = (MarkedUpToken) tokenVertex.getToken();
    assertThat(markedUpToken.getContent()).isEqualTo("Eeny, meeny, miny, ");

    tokenVertex = iterator.next();
    assertThat(tokenVertex).isInstanceOf(SimpleTokenVertex.class);
    markedUpToken = (MarkedUpToken) tokenVertex.getToken();
    assertThat(markedUpToken.getContent()).isEqualTo("curly");
    List<String> markupTagListForTokenVertex = markupTags(witnessGraph, tokenVertex);
    assertThat(markupTagListForTokenVertex).contains("del");

    tokenVertex = iterator.next();
    assertThat(tokenVertex).isInstanceOf(SimpleTokenVertex.class);
    markedUpToken = (MarkedUpToken) tokenVertex.getToken();
    assertThat(markedUpToken.getContent()).isEqualTo("moe");
    markupTagListForTokenVertex = markupTags(witnessGraph, tokenVertex);
    assertThat(markupTagListForTokenVertex).contains("add");

    tokenVertex = iterator.next();
    assertThat(tokenVertex).isInstanceOf(SimpleTokenVertex.class);
    markedUpToken = (MarkedUpToken) tokenVertex.getToken();
    assertThat(markedUpToken.getContent()).isEqualTo("!");

    tokenVertex = iterator.next();
    assertThat(tokenVertex).isInstanceOf(EndTokenVertex.class);

    assertThat(iterator.hasNext()).isFalse();

  }

  private List<String> markupTags(VariantWitnessGraph witnessGraph, TokenVertex tokenVertex) {
    return witnessGraph.getMarkupListForTokenVertex(tokenVertex)//
        .stream()//
        .map(Markup::getTagName)//
        .collect(toList());
  }

}
