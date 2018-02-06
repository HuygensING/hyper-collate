package nl.knaw.huygens.hypercollate.model;

/*-
 * #%L
 * HyperCollate core
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

import nl.knaw.huygens.hypercollate.HyperCollateTest;
import nl.knaw.huygens.hypercollate.tools.DotFactory;
import org.junit.Test;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class VariantWitnessGraphTest extends HyperCollateTest {

  @Test
  public void test() {
//    String xml = "<s>Collating is <del>NP hard</del><add>easy</add>.</s>";

    Markup sMarkup = new Markup("s");
    Markup delMarkup = new Markup("del");
    Markup addMarkup = new Markup("add");

    SimpleWitness witness = new SimpleWitness("A");
    SimpleTokenVertex mtv0 = aTokenVertex("Collating ", 0L, "/s", witness);
    SimpleTokenVertex mtv1 = aTokenVertex("is ", 1L, "/s", witness);
    SimpleTokenVertex mtv2 = aTokenVertex("NP ", 2L, "/s/del", witness);
    SimpleTokenVertex mtv3 = aTokenVertex("hard", 3L, "/s/del", witness);
    SimpleTokenVertex mtv4 = aTokenVertex("easy", 4L, "/s/add", witness);
    SimpleTokenVertex mtv5 = aTokenVertex(".", 5L, "/s", witness);

    VariantWitnessGraph vwg1 = new VariantWitnessGraph(witness.getSigil());
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

    String dot = new DotFactory(false).fromVariantWitnessGraph(vwg1);
    System.out.println(dot);
    String expected = "digraph VariantWitnessGraph{\n" +//
        "graph [rankdir=LR]\n" +//
        "labelloc=b\n" +//
        "begin [label=\"\";shape=doublecircle,rank=middle]\n" +//
        "A_000 [label=<Collating&nbsp;<br/><i>A: /s</i>>]\n" +//
        "A_001 [label=<is&nbsp;<br/><i>A: /s</i>>]\n" +//
        "A_002 [label=<NP&nbsp;<br/><i>A: /s/del</i>>]\n" +//
        "A_004 [label=<easy<br/><i>A: /s/add</i>>]\n" +//
        "A_003 [label=<hard<br/><i>A: /s/del</i>>]\n" +//
        "A_005 [label=<.<br/><i>A: /s</i>>]\n" +//
        "end [label=\"\";shape=doublecircle,rank=middle]\n" +//
        "A_000->A_001\n" +//
        "A_001->A_002\n" +//
        "A_001->A_004\n" +//
        "A_002->A_003\n" +//
        "A_003->A_005\n" +//
        "A_004->A_005\n" +//
        "A_005->end\n" +//
        "begin->A_000\n" +//
        "}";
    assertThat(dot).isEqualTo(expected);

    String expected2 = "digraph VariantWitnessGraph{\n" +//
        "graph [rankdir=LR]\n" +//
        "labelloc=b\n" +//
        "begin [label=\"\";shape=doublecircle,rank=middle]\n" +//
        "A_000 [label=<Collating&#9251;is&#9251;<br/><i>A: /s</i>>]\n" +//
        "A_002 [label=<NP&#9251;hard<br/><i>A: /s/del</i>>]\n" +//
        "A_004 [label=<easy<br/><i>A: /s/add</i>>]\n" +//
        "A_005 [label=<.<br/><i>A: /s</i>>]\n" +//
        "end [label=\"\";shape=doublecircle,rank=middle]\n" +//
        "A_000->A_002\n" +//
        "A_000->A_004\n" +//
        "A_002->A_005\n" +//
        "A_004->A_005\n" +//
        "A_005->end\n" +//
        "begin->A_000\n" +//
        "}";
    verifyDotExport(vwg1, expected2);
  }

  private SimpleTokenVertex aTokenVertex(String string, Long index, String parentXPath, SimpleWitness witness) {
    MarkedUpToken token = new MarkedUpToken()//
        .setContent(string)//
        .setNormalizedContent(string.toLowerCase())//
        .setParentXPath(parentXPath)//
        .setWitness(witness)//
        .setIndexNumber(index);

    return new SimpleTokenVertex(token);
  }

}
