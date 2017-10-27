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
        "        de ongewisheid vóór de <lb/>liefelijke toestemming!</s>\n" + //
        "</text>");
    VariantWitnessGraph wQ = importer.importXML("Q", "<text>\n" + //
        "    <s>Hoe zoet moet nochtans zijn dit <del>werven om</del><add>trachten naar</add> een <lb/>vrouw !\n" + //
        "        Die dagen van nerveuze verwachting vóór de liefelijke toestemming.</s>\n" + //
        "</text>");

    String expectedDotF = "digraph VariantWitnessGraph{\n" + //
        "graph [rankdir=LR]\n" + //
        "labelloc=b\n" + //
        "begin [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "F_000 [label=<Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/><i>F: /text/s</i>>]\n" + //
        "F_006 [label=<<br/><i>F: /text/s/lb</i>>]\n" + //
        "F_007 [label=<werven&#9251;om<br/><i>F: /text/s/del</i>>]\n" + //
        "F_009 [label=<trachten&#9251;naar<br/><i>F: /text/s/add</i>>]\n" + //
        "F_011 [label=<&#9251;een&#9251;vrouw,&#x21A9;<br/>&#9251;de&#9251;ongewisheid&#9251;vóór&#9251;de&#9251;<br/><i>F: /text/s</i>>]\n" + //
        "F_019 [label=<<br/><i>F: /text/s/lb</i>>]\n" + //
        "F_020 [label=<liefelijke&#9251;toestemming!<br/><i>F: /text/s</i>>]\n" + //
        "end [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "F_000->F_006\n" + //
        "F_006->F_007\n" + //
        "F_006->F_009\n" + //
        "F_007->F_011\n" + //
        "F_009->F_011\n" + //
        "F_011->F_019\n" + //
        "F_019->F_020\n" + //
        "F_020->end\n" + //
        "begin->F_000\n" + //
        "}";
    verifyDotExport(wF, expectedDotF);

    String expectedDotQ = "digraph VariantWitnessGraph{\n" + //
        "graph [rankdir=LR]\n" + //
        "labelloc=b\n" + //
        "begin [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "Q_000 [label=<Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/><i>Q: /text/s</i>>]\n" + //
        "Q_006 [label=<werven&#9251;om<br/><i>Q: /text/s/del</i>>]\n" + //
        "Q_008 [label=<trachten&#9251;naar<br/><i>Q: /text/s/add</i>>]\n" + //
        "Q_010 [label=<&#9251;een&#9251;<br/><i>Q: /text/s</i>>]\n" + //
        "Q_012 [label=<<br/><i>Q: /text/s/lb</i>>]\n" + //
        "Q_013 [label=<vrouw&#9251;!&#x21A9;<br/>&#9251;Die&#9251;dagen&#9251;van&#9251;nerveuze&#9251;verwachting&#9251;vóór&#9251;de&#9251;liefelijke&#9251;toestemming.<br/><i>Q: /text/s</i>>]\n" + //
        "end [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "Q_000->Q_006\n" + //
        "Q_000->Q_008\n" + //
        "Q_006->Q_010\n" + //
        "Q_008->Q_010\n" + //
        "Q_010->Q_012\n" + //
        "Q_012->Q_013\n" + //
        "Q_013->end\n" + //
        "begin->Q_000\n" + //
        "}";
    verifyDotExport(wQ, expectedDotQ);

    CollationGraph collation = HyperCollater.collate(wF, wQ);
    System.out.println(collation);

    String dot = DotFactory.fromCollationGraph(collation);
    System.out.println(dot);
    String expected = "digraph CollationGraph{\n" + //
        "labelloc=b\n" + //
        "}";
    writeGraph(dot);
    assertThat(dot).isEqualTo(expected);

  }

}
