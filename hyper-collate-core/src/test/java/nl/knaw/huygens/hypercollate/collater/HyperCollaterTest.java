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

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import nl.knaw.huygens.hypercollate.HyperCollateTest;
import nl.knaw.huygens.hypercollate.importer.XMLImporter;
import nl.knaw.huygens.hypercollate.model.CollationGraph;
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph;
import nl.knaw.huygens.hypercollate.tools.CollationGraphNodeMerger;
import nl.knaw.huygens.hypercollate.tools.DotFactory;

public class HyperCollaterTest extends HyperCollateTest {
  private static final Logger LOG = LoggerFactory.getLogger(HyperCollateTest.class);

  HyperCollater hyperCollater1 = new HyperCollater(new OptimalMatchSetAlgorithm1());
  HyperCollater hyperCollater2 = new HyperCollater(new OptimalMatchSetAlgorithm2());
  HyperCollater[] hyperCollaters = new HyperCollater[] { hyperCollater1, hyperCollater2 };

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

    String expected = "digraph CollationGraph{\n" + //
        "labelloc=b\n" + //
        "t000 [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t001 [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t002 [label=<F,Q: Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t003 [label=<F,Q: &#9251;een&#9251;<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t004 [label=<F: vrouw<br/>Q: vrouw&#9251;<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t005 [label=<F: ,&#x21A9;<br/>&#9251;de&#9251;ongewisheid&#9251;<br/>F: <i>/text/s</i>>]\n" + //
        "t006 [label=<F,Q: vóór&#9251;de&#9251;<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t007 [label=<F: <br/>F: <i>/text/s/lb</i>>]\n" + //
        "t008 [label=<F,Q: liefelijke&#9251;toestemming<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t009 [label=<F: !<br/>F: <i>/text/s</i>>]\n" + //
        "t010 [label=<F: <br/>F: <i>/text/s/lb</i>>]\n" + //
        "t011 [label=<F,Q: werven&#9251;om<br/>F,Q: <i>/text/s/del</i>>]\n" + //
        "t012 [label=<F,Q: trachten&#9251;naar<br/>F,Q: <i>/text/s/add</i>>]\n" + //
        "t013 [label=<Q: <br/>Q: <i>/text/s/lb</i>>]\n" + //
        "t014 [label=<Q: !&#x21A9;<br/>&#9251;Die&#9251;dagen&#9251;van&#9251;nerveuze&#9251;verwachting&#9251;<br/>Q: <i>/text/s</i>>]\n" + //
        "t015 [label=<Q: .<br/>Q: <i>/text/s</i>>]\n" + //
        "t000->t002[label=\"F,Q\"]\n" + //
        "t002->t010[label=\"F\"]\n" + //
        "t002->t011[label=\"Q\"]\n" + //
        "t002->t012[label=\"Q\"]\n" + //
        "t003->t004[label=\"F\"]\n" + //
        "t003->t013[label=\"Q\"]\n" + //
        "t004->t005[label=\"F\"]\n" + //
        "t004->t014[label=\"Q\"]\n" + //
        "t005->t006[label=\"F\"]\n" + //
        "t006->t007[label=\"F\"]\n" + //
        "t006->t008[label=\"Q\"]\n" + //
        "t007->t008[label=\"F\"]\n" + //
        "t008->t009[label=\"F\"]\n" + //
        "t008->t015[label=\"Q\"]\n" + //
        "t009->t001[label=\"F\"]\n" + //
        "t010->t011[label=\"F\"]\n" + //
        "t010->t012[label=\"F\"]\n" + //
        "t011->t003[label=\"F,Q\"]\n" + //
        "t012->t003[label=\"F,Q\"]\n" + //
        "t013->t004[label=\"Q\"]\n" + //
        "t014->t006[label=\"Q\"]\n" + //
        "t015->t001[label=\"Q\"]\n" + //
        "}";

    testHyperCollation(wF, wQ, expected);
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

    String expected = "digraph CollationGraph{\n" + //
        "labelloc=b\n" + //
        "t000 [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t001 [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t002 [label=<F,Q: De&#9251;vent&#9251;was&#9251;woedend&#9251;en&#9251;maakte&#9251;<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t003 [label=<F: Sultan&#9251;<br/>Q: Sultan<br/>F: <i>/text/s</i><br/>Q: <i>/text/s/add/add</i><br/>>]\n" + //
        "t004 [label=<F,Q: uit&#9251;voor&#9251;\"lompen&#9251;boer\".<br/>F,Q: <i>/text/s</i>>]\n" + //
        "t005 [label=<F,Q: Shiriar<br/>F,Q: <i>/text/s/del</i>>]\n" + //
        "t006 [label=<F: &#9251;<br/>F: <i>/text/s</i>>]\n" + //
        "t007 [label=<F: den&#9251;bedremmelden&#x21A9;<br/>&#9251;<br/>Q: den&#9251;bedremmelden&#9251;<br/>F: <i>/text/s</i><br/>Q: <i>/text/s/add</i><br/>>]\n" + //
        "t008 [label=<Q: &#9251;<br/>Q: <i>/text/s</i>>]\n" + //
        "t009 [label=<Q: man<br/>Q: <i>/text/s/add/del</i>>]\n" + //
        "t000->t002[label=\"F,Q\"]\n" + //
        "t002->t005[label=\"F,Q\"]\n" + //
        "t002->t007[label=\"Q\"]\n" + //
        "t003->t004[label=\"F\"]\n" + //
        "t003->t008[label=\"Q\"]\n" + //
        "t004->t001[label=\"F,Q\"]\n" + //
        "t005->t006[label=\"F\"]\n" + //
        "t005->t008[label=\"Q\"]\n" + //
        "t006->t007[label=\"F\"]\n" + //
        "t007->t003[label=\"F,Q\"]\n" + //
        "t007->t009[label=\"Q\"]\n" + //
        "t008->t004[label=\"Q\"]\n" + //
        "t009->t008[label=\"Q\"]\n" + //
        "}";

    testHyperCollation(wF, wQ, expected);
  }

  @Test
  public void testje() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wF = importer.importXML("A", "<text>\n" + //
        "    <s>Lunch,\n" + //
        "        DE soep VOOR DE <lb/>taart!</s>\n" + //
        "</text>");
    VariantWitnessGraph wQ = importer.importXML("B", "<text>\n" + //
        "    <s>Lunch !\n" + //
        "        veel brood VOOR DE taart.</s>\n" + //
        "</text>");
    String expected = "digraph CollationGraph{\n" + //
        "labelloc=b\n" + //
        "t000 [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t001 [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t002 [label=<A: Lunch<br/>B: Lunch&#9251;<br/>A,B: <i>/text/s</i>>]\n" + //
        "t003 [label=<A: ,&#x21A9;<br/>&#9251;DE&#9251;soep&#9251;<br/>A: <i>/text/s</i>>]\n" + //
        "t004 [label=<A,B: VOOR&#9251;DE&#9251;<br/>A,B: <i>/text/s</i>>]\n" + //
        "t005 [label=<A: <br/>A: <i>/text/s/lb</i>>]\n" + //
        "t006 [label=<A,B: taart<br/>A,B: <i>/text/s</i>>]\n" + //
        "t007 [label=<A: !<br/>A: <i>/text/s</i>>]\n" + //
        "t008 [label=<B: !&#x21A9;<br/>&#9251;veel&#9251;brood&#9251;<br/>B: <i>/text/s</i>>]\n" + //
        "t009 [label=<B: .<br/>B: <i>/text/s</i>>]\n" + //
        "t000->t002[label=\"A,B\"]\n" + //
        "t002->t003[label=\"A\"]\n" + //
        "t002->t008[label=\"B\"]\n" + //
        "t003->t004[label=\"A\"]\n" + //
        "t004->t005[label=\"A\"]\n" + //
        "t004->t006[label=\"B\"]\n" + //
        "t005->t006[label=\"A\"]\n" + //
        "t006->t007[label=\"A\"]\n" + //
        "t006->t009[label=\"B\"]\n" + //
        "t007->t001[label=\"A\"]\n" + //
        "t008->t004[label=\"B\"]\n" + //
        "t009->t001[label=\"B\"]\n" + //
        "}";

    testHyperCollation(wF, wQ, expected);
  }

  @Test
  public void testje2() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wF = importer.importXML("A", "<text>T b b b b b b b Y</text>");
    VariantWitnessGraph wQ = importer.importXML("B", "<text>X b b b b b b b T</text>");
    String expected = "digraph CollationGraph{\n" + //
        "labelloc=b\n" + //
        "t000 [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t001 [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t002 [label=<A: T&#9251;<br/>A: <i>/text</i>>]\n" + //
        "t003 [label=<A,B: b&#9251;b&#9251;b&#9251;b&#9251;b&#9251;b&#9251;b&#9251;<br/>A,B: <i>/text</i>>]\n" + //
        "t004 [label=<A: Y<br/>A: <i>/text</i>>]\n" + //
        "t005 [label=<B: X&#9251;<br/>B: <i>/text</i>>]\n" + //
        "t006 [label=<B: T<br/>B: <i>/text</i>>]\n" + //
        "t000->t002[label=\"A\"]\n" + //
        "t000->t005[label=\"B\"]\n" + //
        "t002->t003[label=\"A\"]\n" + //
        "t003->t004[label=\"A\"]\n" + //
        "t003->t006[label=\"B\"]\n" + //
        "t004->t001[label=\"A\"]\n" + //
        "t005->t003[label=\"B\"]\n" + //
        "t006->t001[label=\"B\"]\n" + //
        "}";

    testHyperCollation(wF, wQ, expected);
  }

  @Test
  public void testje3() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wF = importer.importXML("A", "<text>A b C d E C f G H</text>");
    VariantWitnessGraph wQ = importer.importXML("B", "<text>A H i j E C G k</text>");
    String expected = "digraph CollationGraph{\n" + //
        "labelloc=b\n" + //
        "t000 [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t001 [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t002 [label=<A,B: A&#9251;<br/>A,B: <i>/text</i>>]\n" + //
        "t003 [label=<A: b&#9251;C&#9251;d&#9251;<br/>A: <i>/text</i>>]\n" + //
        "t004 [label=<A,B: E&#9251;C&#9251;<br/>A,B: <i>/text</i>>]\n" + //
        "t005 [label=<A: f&#9251;<br/>A: <i>/text</i>>]\n" + //
        "t006 [label=<A,B: G&#9251;<br/>A,B: <i>/text</i>>]\n" + //
        "t007 [label=<A: H<br/>A: <i>/text</i>>]\n" + //
        "t008 [label=<B: H&#9251;i&#9251;j&#9251;<br/>B: <i>/text</i>>]\n" + //
        "t009 [label=<B: k<br/>B: <i>/text</i>>]\n" + //
        "t000->t002[label=\"A,B\"]\n" + //
        "t002->t003[label=\"A\"]\n" + //
        "t002->t008[label=\"B\"]\n" + //
        "t003->t004[label=\"A\"]\n" + //
        "t004->t005[label=\"A\"]\n" + //
        "t004->t006[label=\"B\"]\n" + //
        "t005->t006[label=\"A\"]\n" + //
        "t006->t007[label=\"A\"]\n" + //
        "t006->t009[label=\"B\"]\n" + //
        "t007->t001[label=\"A\"]\n" + //
        "t008->t004[label=\"B\"]\n" + //
        "t009->t001[label=\"B\"]\n" + //
        "}";

    testHyperCollation(wF, wQ, expected);
  }

  private void testHyperCollation(VariantWitnessGraph witness1, VariantWitnessGraph witness2, String expected) {
    for (HyperCollater hypercollater : hyperCollaters) {
      LOG.info("Collating with {}", hypercollater.getOptimalMatchSetFinderName());
      Stopwatch stopwatch = Stopwatch.createStarted();
      CollationGraph collation0 = hypercollater.collate(witness1, witness2);
      stopwatch.stop();
      LOG.info("Collating with {} took {} ms.", hypercollater.getOptimalMatchSetFinderName(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
      CollationGraph collation = CollationGraphNodeMerger.merge(collation0);

      String dot = DotFactory.fromCollationGraph(collation);
      // System.out.println(dot);
      writeGraph(dot);
      assertThat(dot).isEqualTo(expected);
    }
  }

}
