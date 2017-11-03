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

import nl.knaw.huygens.hypercollate.HyperCollateTest;
import nl.knaw.huygens.hypercollate.importer.XMLImporter;
import nl.knaw.huygens.hypercollate.model.CollationGraph;
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph;
import nl.knaw.huygens.hypercollate.tools.DotFactory;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
    String expected = "digraph CollationGraph{\n" +
        "labelloc=b\n" +
        "t000 [label=\"\";shape=doublecircle,rank=middle]\n" +
        "t001 [label=<F,Q: Hoe&#9251;<br/>F,Q: <i>/text/s</i>>]\n" +
        "t002 [label=<F,Q: zoet&#9251;<br/>F,Q: <i>/text/s</i>>]\n" +
        "t003 [label=<F,Q: moet&#9251;<br/>F,Q: <i>/text/s</i>>]\n" +
        "t004 [label=<F,Q: nochtans&#9251;<br/>F,Q: <i>/text/s</i>>]\n" +
        "t005 [label=<F,Q: zijn&#9251;<br/>F,Q: <i>/text/s</i>>]\n" +
        "t006 [label=<F,Q: dit&#9251;<br/>F,Q: <i>/text/s</i>>]\n" +
        "t007 [label=<F: <br/>F: <i>/text/s/lb</i>>]\n" +
        "t008 [label=<F,Q: werven&#9251;<br/>F,Q: <i>/text/s/del</i>>]\n" +
        "t009 [label=<F,Q: om<br/>F,Q: <i>/text/s/del</i>>]\n" +
        "t010 [label=<F,Q: &#9251;<br/>F,Q: <i>/text/s</i>>]\n" +
        "t011 [label=<F,Q: een&#9251;<br/>F,Q: <i>/text/s</i>>]\n" +
        "t012 [label=<F: vrouw<br/>Q: vrouw&#9251;<br/>F,Q: <i>/text/s</i>>]\n" +
        "t013 [label=<Q: !&#x21A9;<br/>&#9251;<br/>Q: <i>/text/s</i>>]\n" +
        "t014 [label=<Q: Die&#9251;<br/>Q: <i>/text/s</i>>]\n" +
        "t015 [label=<Q: dagen&#9251;<br/>Q: <i>/text/s</i>>]\n" +
        "t016 [label=<Q: van&#9251;<br/>Q: <i>/text/s</i>>]\n" +
        "t017 [label=<Q: nerveuze&#9251;<br/>Q: <i>/text/s</i>>]\n" +
        "t018 [label=<Q: verwachting&#9251;<br/>Q: <i>/text/s</i>>]\n" +
        "t019 [label=<F,Q: vóór&#9251;<br/>F,Q: <i>/text/s</i>>]\n" +
        "t020 [label=<F,Q: de&#9251;<br/>F,Q: <i>/text/s</i>>]\n" +
        "t021 [label=<F,Q: liefelijke&#9251;<br/>F,Q: <i>/text/s</i>>]\n" +
        "t022 [label=<F,Q: toestemming<br/>F,Q: <i>/text/s</i>>]\n" +
        "t023 [label=<F: !<br/>F: <i>/text/s</i>>]\n" +
        "t024 [label=\"\";shape=doublecircle,rank=middle]\n" +
        "t025 [label=<Q: .<br/>Q: <i>/text/s</i>>]\n" +
        "t026 [label=<F: <br/>F: <i>/text/s/lb</i>>]\n" +
        "t027 [label=<F: ,&#x21A9;<br/>&#9251;<br/>F: <i>/text/s</i>>]\n" +
        "t028 [label=<F: de&#9251;<br/>F: <i>/text/s</i>>]\n" +
        "t029 [label=<F: ongewisheid&#9251;<br/>F: <i>/text/s</i>>]\n" +
        "t030 [label=<Q: <br/>Q: <i>/text/s/lb</i>>]\n" +
        "t031 [label=<F,Q: trachten&#9251;<br/>F,Q: <i>/text/s/add</i>>]\n" +
        "t032 [label=<F,Q: naar<br/>F,Q: <i>/text/s/add</i>>]\n" +
        "t000->t001[label=\"F,Q\"]\n" +
        "t001->t002[label=\"F,Q\"]\n" +
        "t002->t003[label=\"F,Q\"]\n" +
        "t003->t004[label=\"F,Q\"]\n" +
        "t004->t005[label=\"F,Q\"]\n" +
        "t005->t006[label=\"F,Q\"]\n" +
        "t006->t007[label=\"F\"]\n" +
        "t006->t008[label=\"Q\"]\n" +
        "t007->t008[label=\"F\"]\n" +
        "t008->t009[label=\"F,Q\"]\n" +
        "t032->t010[label=\"F,Q\"]\n" +
        "t009->t010[label=\"F,Q\"]\n" +
        "t010->t011[label=\"F,Q\"]\n" +
        "t030->t012[label=\"Q\"]\n" +
        "t011->t012[label=\"F\"]\n" +
        "t012->t013[label=\"Q\"]\n" +
        "t013->t014[label=\"Q\"]\n" +
        "t014->t015[label=\"Q\"]\n" +
        "t015->t016[label=\"Q\"]\n" +
        "t016->t017[label=\"Q\"]\n" +
        "t017->t018[label=\"Q\"]\n" +
        "t018->t019[label=\"Q\"]\n" +
        "t029->t019[label=\"F\"]\n" +
        "t019->t020[label=\"F,Q\"]\n" +
        "t020->t021[label=\"Q\"]\n" +
        "t026->t021[label=\"F\"]\n" +
        "t021->t022[label=\"F,Q\"]\n" +
        "t022->t023[label=\"F\"]\n" +
        "t025->t024[label=\"Q\"]\n" +
        "t023->t024[label=\"F\"]\n" +
        "t022->t025[label=\"Q\"]\n" +
        "t020->t026[label=\"F\"]\n" +
        "t012->t027[label=\"F\"]\n" +
        "t027->t028[label=\"F\"]\n" +
        "t028->t029[label=\"F\"]\n" +
        "t011->t030[label=\"Q\"]\n" +
        "t007->t031[label=\"F\"]\n" +
        "t006->t031[label=\"Q\"]\n" +
        "t031->t032[label=\"F,Q\"]\n" +
        "}";
    writeGraph(dot);
    assertThat(dot).isEqualTo(expected);

  }

}
