package nl.knaw.huygens.hypercollate.model;

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
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

public class VariantWitnessGraphTest {

  @Test
  public void test() {
    String xml = "<s>Collating is <del>NP hard</del><add>easy</add>.</s>";

    Markup sMarkup = new Markup("s");
    Markup delMarkup = new Markup("del");
    Markup addMarkup = new Markup("add");

    SimpleTokenVertex mtv0 = aTokenVertex("Collating ");
    SimpleTokenVertex mtv1 = aTokenVertex("is ");
    SimpleTokenVertex mtv2 = aTokenVertex("NP ");
    SimpleTokenVertex mtv3 = aTokenVertex("hard");
    SimpleTokenVertex mtv4 = aTokenVertex("easy");
    SimpleTokenVertex mtv5 = aTokenVertex(".");

    VariantWitnessGraph vwg1 = new VariantWitnessGraph("A");
    vwg1.addMarkup(sMarkup, delMarkup, addMarkup);
    TokenVertex startTokenVertex = vwg1.getStartTokenVertex();
    vwg1.addOutgoingTokenVertexToTokenVertex(startTokenVertex, mtv0); // (START)->(collating)

    vwg1.addOutgoingTokenVertexToTokenVertex(mtv0, mtv1); // (collating)->(is)
    vwg1.addMarkupToTokenVertex(mtv0, sMarkup);

    vwg1.addOutgoingTokenVertexToTokenVertex(mtv1, mtv2); // (is)->(np)
    vwg1.addOutgoingTokenVertexToTokenVertex(mtv1, mtv4); // (is)->(easy)
    vwg1.addMarkupToTokenVertex(mtv1, sMarkup);

    vwg1.addOutgoingTokenVertexToTokenVertex(mtv2, mtv3);// (np)->(hard)
    vwg1.addMarkupToTokenVertex(mtv2, sMarkup);
    vwg1.addMarkupToTokenVertex(mtv2, delMarkup);

    vwg1.addOutgoingTokenVertexToTokenVertex(mtv3, mtv5);// (hard)->(.)
    vwg1.addMarkupToTokenVertex(mtv3, sMarkup);
    vwg1.addMarkupToTokenVertex(mtv3, delMarkup);

    vwg1.addOutgoingTokenVertexToTokenVertex(mtv4, mtv5);// (easy)->(.)
    vwg1.addMarkupToTokenVertex(mtv4, sMarkup);
    vwg1.addMarkupToTokenVertex(mtv4, addMarkup);

    TokenVertex endTokenVertex = vwg1.getEndTokenVertex();
    vwg1.addOutgoingTokenVertexToTokenVertex(mtv5, endTokenVertex);// (.)->(END)
    vwg1.addMarkupToTokenVertex(mtv5, sMarkup);

    List<Markup> hardMarkup = vwg1.getMarkupListForTokenVertex(mtv3);
    assertThat(hardMarkup).containsExactly(sMarkup, delMarkup);

    List<TokenVertex> incoming = mtv5.getIncomingTokenVertexStream().collect(toList());
    assertThat(incoming).containsOnly(mtv3, mtv4);

  }

  private SimpleTokenVertex aTokenVertex(String string) {
    MarkedUpToken token = new MarkedUpToken()//
        .setContent(string)//
        .setNormalizedContent(string.toLowerCase());
    return new SimpleTokenVertex(token);
  }

}
