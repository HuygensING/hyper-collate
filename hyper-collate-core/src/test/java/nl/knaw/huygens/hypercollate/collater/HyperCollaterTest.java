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

import org.junit.Test;

import nl.knaw.huygens.hypercollate.HyperCollateTest;
import nl.knaw.huygens.hypercollate.importer.XMLImporter;
import nl.knaw.huygens.hypercollate.model.CollationGraph;
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph;
import nl.knaw.huygens.hypercollate.tools.DotFactory;

public class HyperCollaterTest extends HyperCollateTest {

  @Test
  public void test() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wF = importer.importXML("F", "<text>\n" + //
        "    <s>Hoe zoet moet nochtans zijn dit <lb/><del>werven om</del><add>trachten naar</add> een vrouw,\n" + //
        "        de ongewisheid v??r de <lb/>liefelijke toestemming!</s>\n" + //
        "</text>");
    VariantWitnessGraph wQ = importer.importXML("Q", "<text>\n" + //
        "    <s>Hoe zoet moet nochtans zijn dit <del>werven om</del><add>trachten naar</add> een <lb/>vrouw !\n" + //
        "        Die dagen van nerveuze verwachting v??r de liefelijke toestemming.</s>\n" + //
        "</text>");

    String expectedDotF = "digraph VariantWitnessGraph{\n" + //
        "graph [rankdir=LR]\n" + //
        "labelloc=b\n" + //
        "st [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t0 [label=<Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/><i>F: /text/s</i>>]\n" + //
        "t6 [label=<<br/><i>F: /text/s/lb</i>>]\n" + //
        "t7 [label=<werven&#9251;om<br/><i>F: /text/s/del</i>>]\n" + //
        "t9 [label=<trachten&#9251;naar<br/><i>F: /text/s/add</i>>]\n" + //
        "t11 [label=<&#9251;een&#9251;vrouw,&#x21A9;<br/>&#9251;de&#9251;ongewisheid&#9251;v??r&#9251;de&#9251;<br/><i>F: /text/s</i>>]\n" + //
        "t21 [label=<<br/><i>F: /text/s/lb</i>>]\n" + //
        "t22 [label=<liefelijke&#9251;toestemming!<br/><i>F: /text/s</i>>]\n" + //
        "et [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "st->t0\n" + //
        "t0->t6\n" + //
        "t11->t21\n" + //
        "t21->t22\n" + //
        "t22->et\n" + //
        "t6->t7\n" + //
        "t6->t9\n" + //
        "t7->t11\n" + //
        "t9->t11\n" + //
        "}";
    verifyDotExport(wF, expectedDotF);

    String expectedDotQ = "digraph VariantWitnessGraph{\n" + //
        "graph [rankdir=LR]\n" + //
        "labelloc=b\n" + //
        "st [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t0 [label=<Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/><i>Q: /text/s</i>>]\n" + //
        "t6 [label=<werven&#9251;om<br/><i>Q: /text/s/del</i>>]\n" + //
        "t8 [label=<trachten&#9251;naar<br/><i>Q: /text/s/add</i>>]\n" + //
        "t10 [label=<&#9251;een&#9251;<br/><i>Q: /text/s</i>>]\n" + //
        "t12 [label=<<br/><i>Q: /text/s/lb</i>>]\n" + //
        "t13 [label=<vrouw&#9251;!&#x21A9;<br/>&#9251;Die&#9251;dagen&#9251;van&#9251;nerveuze&#9251;verwachting&#9251;v??r&#9251;de&#9251;liefelijke&#9251;toestemming.<br/><i>Q: /text/s</i>>]\n" + //
        "et [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "st->t0\n" + //
        "t0->t6\n" + //
        "t0->t8\n" + //
        "t10->t12\n" + //
        "t12->t13\n" + //
        "t13->et\n" + //
        "t6->t10\n" + //
        "t8->t10\n" + //
        "}";
    verifyDotExport(wQ, expectedDotQ);

    CollationGraph collation = HyperCollater.collate(wF, wQ);
    System.out.println(collation);

    String dot = DotFactory.fromCollationGraph(collation);
    System.out.println(dot);
    String expected = "something";
    // assertThat(dot).isEqualTo(expected);

  }

}
