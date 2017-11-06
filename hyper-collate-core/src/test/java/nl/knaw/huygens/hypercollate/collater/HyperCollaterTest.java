package nl.knaw.huygens.hypercollate.collater;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

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
import nl.knaw.huygens.hypercollate.tools.CollationGraphNodeMerger;
import nl.knaw.huygens.hypercollate.tools.DotFactory;

public class HyperCollaterTest extends HyperCollateTest {

  @Test
  public void testHierarchy() {
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

    CollationGraph collation0 = HyperCollater.collate(wF, wQ);
    CollationGraph collation = CollationGraphNodeMerger.merge(collation0);

    String dot = DotFactory.fromCollationGraph(collation);
    // System.out.println(dot);
    String expected = "digraph CollationGraph{\n" + //
        "labelloc=b\n" + //
        "t000 [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t001 [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t002 [label=<F,Q: Hoe&#9251;<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t003 [label=<F,Q: naar<br/>F,Q: <i>/text/s/add</i>>]\n" + //
        "t004 [label=<F,Q: &#9251;<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t005 [label=<F,Q: een&#9251;<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t006 [label=<F: vrouw<br/>Q: vrouw&#9251;<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t007 [label=<F: ,&#x21A9;<br/>&#9251;<br/>F: <i>/text/s</i>>]\n" + //
        "t008 [label=<F: de&#9251;<br/>F: <i>/text/s</i>>]\n" + //
        "t009 [label=<F: ongewisheid&#9251;<br/>F: <i>/text/s</i>>]\n" + //
        "t010 [label=<F,Q: vóór&#9251;<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t011 [label=<F,Q: de&#9251;<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t012 [label=<F: <br/>F: <i>/text/s/lb</i>>]\n" + //
        "t013 [label=<F,Q: zoet&#9251;<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t014 [label=<F,Q: liefelijke&#9251;<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t015 [label=<F,Q: toestemming<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t016 [label=<F: !<br/>F: <i>/text/s</i>>]\n" + //
        "t017 [label=<F,Q: moet&#9251;<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t018 [label=<F,Q: nochtans&#9251;<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t019 [label=<F,Q: zijn&#9251;<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t020 [label=<F,Q: dit&#9251;<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t021 [label=<F: <br/>F: <i>/text/s/lb</i>>]\n" + //
        "t022 [label=<F,Q: werven&#9251;<br/>F,Q: <i>/text/s/del</i>>]\n" + //
        "t023 [label=<F,Q: om<br/>F,Q: <i>/text/s/del</i>>]\n" + //
        "t024 [label=<F,Q: trachten&#9251;<br/>F,Q: <i>/text/s/add</i>>]\n" + //
        "t025 [label=<Q: <br/>Q: <i>/text/s/lb</i>>]\n" + //
        "t026 [label=<Q: !&#x21A9;<br/>&#9251;<br/>Q: <i>/text/s</i>>]\n" + //
        "t027 [label=<Q: Die&#9251;<br/>Q: <i>/text/s</i>>]\n" + //
        "t028 [label=<Q: dagen&#9251;<br/>Q: <i>/text/s</i>>]\n" + //
        "t029 [label=<Q: van&#9251;<br/>Q: <i>/text/s</i>>]\n" + //
        "t030 [label=<Q: nerveuze&#9251;<br/>Q: <i>/text/s</i>>]\n" + //
        "t031 [label=<Q: verwachting&#9251;<br/>Q: <i>/text/s</i>>]\n" + //
        "t032 [label=<Q: .<br/>Q: <i>/text/s</i>>]\n" + //
        "t000->t002[label=\"F,Q\"]\n" + //
        "t002->t013[label=\"F,Q\"]\n" + //
        "t003->t004[label=\"F,Q\"]\n" + //
        "t004->t005[label=\"F,Q\"]\n" + //
        "t005->t006[label=\"F\"]\n" + //
        "t005->t025[label=\"Q\"]\n" + //
        "t006->t007[label=\"F\"]\n" + //
        "t006->t026[label=\"Q\"]\n" + //
        "t007->t008[label=\"F\"]\n" + //
        "t008->t009[label=\"F\"]\n" + //
        "t009->t010[label=\"F\"]\n" + //
        "t010->t011[label=\"F,Q\"]\n" + //
        "t011->t012[label=\"F\"]\n" + //
        "t011->t014[label=\"Q\"]\n" + //
        "t012->t014[label=\"F\"]\n" + //
        "t013->t017[label=\"F,Q\"]\n" + //
        "t014->t015[label=\"F,Q\"]\n" + //
        "t015->t016[label=\"F\"]\n" + //
        "t015->t032[label=\"Q\"]\n" + //
        "t016->t001[label=\"F\"]\n" + //
        "t017->t018[label=\"F,Q\"]\n" + //
        "t018->t019[label=\"F,Q\"]\n" + //
        "t019->t020[label=\"F,Q\"]\n" + //
        "t020->t021[label=\"F\"]\n" + //
        "t020->t022[label=\"Q\"]\n" + //
        "t020->t024[label=\"Q\"]\n" + //
        "t021->t022[label=\"F\"]\n" + //
        "t021->t024[label=\"F\"]\n" + //
        "t022->t023[label=\"F,Q\"]\n" + //
        "t023->t004[label=\"F,Q\"]\n" + //
        "t024->t003[label=\"F,Q\"]\n" + //
        "t025->t006[label=\"Q\"]\n" + //
        "t026->t027[label=\"Q\"]\n" + //
        "t027->t028[label=\"Q\"]\n" + //
        "t028->t029[label=\"Q\"]\n" + //
        "t029->t030[label=\"Q\"]\n" + //
        "t030->t031[label=\"Q\"]\n" + //
        "t031->t010[label=\"Q\"]\n" + //
        "t032->t001[label=\"Q\"]\n" + //
        "}";
    writeGraph(dot);
    assertThat(dot).isEqualTo(expected);
  }

  @Test
  public void testOrder() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wF = importer.importXML("F", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
        "<text>\n" + //
        "    <s>De vent was woedend en maakte <del type=\"instantCorrection\">Shiriar</del> den bedremmelden\n" + //
        "        Sultan uit voor \"lompen boer\".</s>\n" + //
        "</text>");
    VariantWitnessGraph wQ = importer.importXML("Q", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
        "<text>\n" + //
        "    <s>De vent was woedend en maakte <del>Shiriar</del>\n" + //
        "        <add>den bedremmelden <del>man</del>\n" + //
        "            <add>Sultan</add></add> uit voor \"lompen boer\".</s>\n" + //
        "</text>");

    String expectedDotF = "digraph VariantWitnessGraph{\n" + //
        "graph [rankdir=LR]\n" + //
        "labelloc=b\n" + //
        "begin [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "F_000 [label=<De&#9251;vent&#9251;was&#9251;woedend&#9251;en&#9251;maakte&#9251;<br/><i>F: /text/s</i>>]\n" + //
        "F_006 [label=<Shiriar<br/><i>F: /text/s/del</i>>]\n" + //
        "F_007 [label=<&#9251;den&#9251;bedremmelden&#x21A9;<br/>&#9251;Sultan&#9251;uit&#9251;voor&#9251;\"lompen&#9251;boer\".<br/><i>F: /text/s</i>>]\n" + //
        "end [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "F_000->F_006\n" + //
        "F_006->F_007\n" + //
        "F_007->end\n" + //
        "begin->F_000\n" + //
        "}";
    verifyDotExport(wF, expectedDotF);

    String expectedDotQ = "digraph VariantWitnessGraph{\n" + //
        "graph [rankdir=LR]\n" + //
        "labelloc=b\n" + //
        "begin [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "Q_000 [label=<De&#9251;vent&#9251;was&#9251;woedend&#9251;en&#9251;maakte&#9251;<br/><i>Q: /text/s</i>>]\n" + //
        "Q_006 [label=<Shiriar<br/><i>Q: /text/s/del</i>>]\n" + //
        "Q_007 [label=<den&#9251;bedremmelden&#9251;<br/><i>Q: /text/s/add</i>>]\n" + //
        "Q_011 [label=<&#9251;uit&#9251;voor&#9251;\"lompen&#9251;boer\".<br/><i>Q: /text/s</i>>]\n" + //
        "Q_009 [label=<man<br/><i>Q: /text/s/add/del</i>>]\n" + //
        "Q_010 [label=<Sultan<br/><i>Q: /text/s/add/add</i>>]\n" + //
        "end [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "Q_000->Q_006\n" + //
        "Q_000->Q_007\n" + //
        "Q_006->Q_011\n" + //
        "Q_007->Q_009\n" + //
        "Q_007->Q_010\n" + //
        "Q_009->Q_011\n" + //
        "Q_010->Q_011\n" + //
        "Q_011->end\n" + //
        "begin->Q_000\n" + //
        "}";
    verifyDotExport(wQ, expectedDotQ);

    CollationGraph collation = HyperCollater.collate(wF, wQ);
    System.out.println(collation);

    String dot = DotFactory.fromCollationGraph(collation);
    System.out.println(dot);
    String expected = "digraph CollationGraph{\n" + //
        "labelloc=b\n" + //
        "t000 [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t001 [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t002 [label=<F,Q: De&#9251;<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t003 [label=<F: Sultan&#9251;<br/>Q: Sultan<br/>F: <i>/text/s</i><br/>Q: <i>/text/s/add/add</i><br/>>]\n" + //
        "t004 [label=<F,Q: uit&#9251;<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t005 [label=<F,Q: voor&#9251;<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t006 [label=<F,Q: \"lompen&#9251;<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t007 [label=<F,Q: boer\"<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t008 [label=<F,Q: .<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t009 [label=<F,Q: vent&#9251;<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t010 [label=<F,Q: was&#9251;<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t011 [label=<F,Q: woedend&#9251;<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t012 [label=<F,Q: en&#9251;<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t013 [label=<F,Q: maakte&#9251;<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t014 [label=<F,Q: Shiriar<br/>F,Q: <i>/text/s/del</i>>]\n" + //
        "t015 [label=<F: &#9251;<br/>F: <i>/text/s</i>>]\n" + //
        "t016 [label=<F,Q: den&#9251;<br/>F: <i>/text/s</i><br/>Q: <i>/text/s/add</i><br/>>]\n" + //
        "t017 [label=<F: bedremmelden&#x21A9;<br/>&#9251;<br/>Q: bedremmelden&#9251;<br/>F: <i>/text/s</i><br/>Q: <i>/text/s/add</i><br/>>]\n" + //
        "t018 [label=<Q: &#9251;<br/>Q: <i>/text/s</i>>]\n" + //
        "t019 [label=<Q: man<br/>Q: <i>/text/s/add/del</i>>]\n" + //
        "t000->t002[label=\"F,Q\"]\n" + //
        "t002->t009[label=\"F,Q\"]\n" + //
        "t003->t004[label=\"F\"]\n" + //
        "t003->t018[label=\"Q\"]\n" + //
        "t004->t005[label=\"F,Q\"]\n" + //
        "t005->t006[label=\"F,Q\"]\n" + //
        "t006->t007[label=\"F,Q\"]\n" + //
        "t007->t008[label=\"F,Q\"]\n" + //
        "t008->t001[label=\"F,Q\"]\n" + //
        "t009->t010[label=\"F,Q\"]\n" + //
        "t010->t011[label=\"F,Q\"]\n" + //
        "t011->t012[label=\"F,Q\"]\n" + //
        "t012->t013[label=\"F,Q\"]\n" + //
        "t013->t014[label=\"F,Q\"]\n" + //
        "t013->t016[label=\"Q\"]\n" + //
        "t014->t015[label=\"F\"]\n" + //
        "t014->t018[label=\"Q\"]\n" + //
        "t015->t016[label=\"F\"]\n" + //
        "t016->t017[label=\"F,Q\"]\n" + //
        "t017->t003[label=\"F,Q\"]\n" + //
        "t017->t019[label=\"Q\"]\n" + //
        "t018->t004[label=\"Q\"]\n" + //
        "t019->t018[label=\"Q\"]\n" + //
        "}";
    writeGraph(dot);
    assertThat(dot).isEqualTo(expected);
  }

}
