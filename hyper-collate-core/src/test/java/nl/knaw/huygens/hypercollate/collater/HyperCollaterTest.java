package nl.knaw.huygens.hypercollate.collater;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.google.common.base.Stopwatch;

import nl.knaw.huygens.hypercollate.HyperCollateTest;
import nl.knaw.huygens.hypercollate.importer.XMLImporter;
import nl.knaw.huygens.hypercollate.model.CollationGraph;
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph;
import nl.knaw.huygens.hypercollate.tools.CollationGraphNodeJoiner;
import nl.knaw.huygens.hypercollate.tools.CollationGraphVisualizer;

public class HyperCollaterTest extends HyperCollateTest {
  private static final Logger LOG = LoggerFactory.getLogger(HyperCollateTest.class);

  final HyperCollater hyperCollater1 = new HyperCollater(new OptimalMatchSetAlgorithm1());
  private final HyperCollater hyperCollater2 = new HyperCollater(new OptimalMatchSetAlgorithm2());
  private final HyperCollater[] hyperCollaters = new HyperCollater[] { /* hyperCollater1, */hyperCollater2 };

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
  public void testTheDog() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wF = importer.importXML("A", "<text>The dog's big eyes.</text>");
    VariantWitnessGraph wQ = importer.importXML("B", "<text>The dog's <del>big black ears</del><add>brown eyes</add>.</text>");
    String expected = "digraph CollationGraph{\n" + //
        "labelloc=b\n" + //
        "t000 [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t001 [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t002 [label=<A,B: The&#9251;dog's&#9251;<br/>A,B: <i>/text</i>>]\n" + //
        "t003 [label=<A,B: big&#9251;<br/>A: <i>/text</i><br/>B: <i>/text/del</i><br/>>]\n" + //
        "t004 [label=<A,B: eyes<br/>A: <i>/text</i><br/>B: <i>/text/add</i><br/>>]\n" + //
        "t005 [label=<A,B: .<br/>A,B: <i>/text</i>>]\n" + //
        "t006 [label=<B: black&#9251;ears<br/>B: <i>/text/del</i>>]\n" + //
        "t007 [label=<B: brown&#9251;<br/>B: <i>/text/add</i>>]\n" + //
        "t000->t002[label=\"A,B\"]\n" + //
        "t002->t003[label=\"A,B\"]\n" + //
        "t002->t007[label=\"B\"]\n" + //
        "t003->t004[label=\"A\"]\n" + //
        "t003->t006[label=\"B\"]\n" + //
        "t004->t005[label=\"A,B\"]\n" + //
        "t005->t001[label=\"A,B\"]\n" + //
        "t006->t005[label=\"B\"]\n" + //
        "t007->t004[label=\"B\"]\n" + //
        "}";

    testHyperCollation(wF, wQ, expected);
  }

  @Test
  public void testTranspositionAndDuplication() {
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
  public void testDoubleTransposition() {
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

  @Test
  public void testVirginiaWoolfTimePassesFragment() {
    XMLImporter importer = new XMLImporter();
    String xml1 = "<text>\n" + //
        "<div n=\"2\">\n" + //
        "<s>Leaning her bony breast on the hard thorn she crooned out her forgiveness.</s>\n" + //
        "</div>\n" + //
        "<div n=\"3\">\n" + //
        "<s>Was it then that she had her consolations  </s>\n" + //
        "</div>\n" + //
        "</text>";
    LOG.info("H: {}", xml1);
    VariantWitnessGraph wF = importer.importXML("H", xml1);
    String xml2 = "<text>\n" + //
        "<p>\n" + //
        "<s> granting, as she stood the chair straight by the dressing table, <del/><add>leaning her bony breast on the hard thorn</add>, her forgiveness of it all.</s>\n" + //
        "</p>\n" + //
        "<p>\n" + //
        "<s>Was it then that she had her consolations ... </s>\n" + //
        "</p>\n" + //
        "</text>";
    LOG.info("T: {}", xml2);
    VariantWitnessGraph wQ = importer.importXML("T", xml2);
    String expected = "digraph CollationGraph{\n" + //
        "labelloc=b\n" + //
        "t000 [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t001 [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t002 [label=<H: Leaning&#9251;her&#9251;bony&#9251;breast&#9251;on&#9251;the&#9251;hard&#9251;thorn&#9251;<br/>T: leaning&#9251;her&#9251;bony&#9251;breast&#9251;on&#9251;the&#9251;hard&#9251;thorn<br/>H: <i>/text/div/s</i><br/>T: <i>/text/p/s/add</i><br/>>]\n"
        + //
        "t003 [label=<H: her&#9251;forgiveness<br/>T: her&#9251;forgiveness&#9251;<br/>H: <i>/text/div/s</i><br/>T: <i>/text/p/s</i><br/>>]\n" + //
        "t004 [label=<H,T: .Was&#9251;it&#9251;then&#9251;that&#9251;she&#9251;had&#9251;her&#9251;consolations&#9251;<br/>H: <i>/text/div/s</i><br/>T: <i>/text/p/s</i><br/>>]\n" + //
        "t005 [label=<H: she&#9251;crooned&#9251;out&#9251;<br/>H: <i>/text/div/s</i>>]\n" + //
        "t006 [label=<T: &#9251;granting,&#9251;as&#9251;she&#9251;stood&#9251;the&#9251;chair&#9251;straight&#9251;by&#9251;the&#9251;dressing&#9251;table,&#9251;<br/>T: <i>/text/p/s</i>>]\n" + //
        "t007 [label=<T: <br/>T: <i>/text/p/s/del</i>>]\n" + //
        "t008 [label=<T: ,&#9251;<br/>T: <i>/text/p/s</i>>]\n" + //
        "t009 [label=<T: of&#9251;it&#9251;all<br/>T: <i>/text/p/s</i>>]\n" + //
        "t010 [label=<T: ...&#9251;<br/>T: <i>/text/p/s</i>>]\n" + //
        "t000->t002[label=\"H\"]\n" + //
        "t000->t006[label=\"T\"]\n" + //
        "t002->t005[label=\"H\"]\n" + //
        "t002->t008[label=\"T\"]\n" + //
        "t003->t004[label=\"H\"]\n" + //
        "t003->t009[label=\"T\"]\n" + //
        "t004->t001[label=\"H\"]\n" + //
        "t004->t010[label=\"T\"]\n" + //
        "t005->t003[label=\"H\"]\n" + //
        "t006->t002[label=\"T\"]\n" + //
        "t006->t007[label=\"T\"]\n" + //
        "t007->t008[label=\"T\"]\n" + //
        "t008->t003[label=\"T\"]\n" + //
        "t009->t004[label=\"T\"]\n" + //
        "t010->t001[label=\"T\"]\n" + //
        "}";

    testHyperCollation(wF, wQ, expected);
  }

  // @Test
  public void testMaryShellyGodwinFrankensteinFragment() {
    XMLImporter importer = new XMLImporter();
    String xmlN = "<text>\n" + //
        "<s>so destitute of every hope of consolation to live\n" + //
        "<del rend=\"strikethrough\">-</del>\n" + //
        "<add place=\"overwritten\" hand=\"#pbs\">?</add> oh no – ...\n" + //
        "</s>\n" + //
        "</text>";
    VariantWitnessGraph wF = importer.importXML("N", xmlN);
    LOG.info("N: {}", xmlN);
    String xmlF = "<text>\n" + //
        "<p>\n" + //
        "<s>so infinitely miserable, so destitute of every hope of consolation to live?</s> <s>Oh, no! ... </s>\n" + //
        "</p></text>";
    LOG.info("F: {}", xmlF);
    VariantWitnessGraph wQ = importer.importXML("F", xmlF);//
    String expected = "digraph CollationGraph{\n" + "labelloc=b\n" + "t000 [label=\"\";shape=doublecircle,rank=middle]\n" + "t001 [label=\"\";shape=doublecircle,rank=middle]\n"
        + "t002 [label=<F: so&#9251;infinitely&#9251;miserable,&#9251;<br/>F: <i>/text/p/s</i>>]\n" + "t003 [label=<F,N: ?<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s/add</i><br/>>]\n"
        + "t004 [label=<F,N: &#9251;<br/>F: <i>/text/p</i><br/>N: <i>/text/s</i><br/>>]\n" + "t005 [label=<F: Oh<br/>N: oh&#9251;<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>]\n"
        + "t006 [label=<F: ,&#9251;<br/>F: <i>/text/p/s</i>>]\n" + "t007 [label=<F: no<br/>N: no&#9251;<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>]\n"
        + "t008 [label=<F: !&#9251;<br/>F: <i>/text/p/s</i>>]\n" + "t009 [label=<F: ...&#9251;<br/>N: ...&#x21A9;<br/><br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>]\n"
        + "t010 [label=<F,N: so&#9251;destitute&#9251;of&#9251;every&#9251;hope&#9251;of&#9251;consolation&#9251;to&#9251;live<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>]\n"
        + "t011 [label=<N: –&#9251;<br/>N: <i>/text/s</i>>]\n" + "t012 [label=<N: -<br/>N: <i>/text/s/del</i>>]\n" + "t000->t002[label=\"F\"]\n" + "t000->t010[label=\"N\"]\n"
        + "t002->t010[label=\"F\"]\n" + "t003->t004[label=\"F,N\"]\n" + "t004->t005[label=\"F,N\"]\n" + "t005->t006[label=\"F\"]\n" + "t005->t007[label=\"N\"]\n" + "t006->t007[label=\"F\"]\n"
        + "t007->t008[label=\"F\"]\n" + "t007->t011[label=\"N\"]\n" + "t008->t009[label=\"F\"]\n" + "t009->t001[label=\"F,N\"]\n" + "t010->t003[label=\"F,N\"]\n" + "t010->t012[label=\"N\"]\n"
        + "t011->t009[label=\"N\"]\n" + "t012->t004[label=\"N\"]\n" + "}";

    testHyperCollation(wF, wQ, expected);
  }

  // @Test
  public void testMaryShellyGodwinFrankensteinFragment2() {
    XMLImporter importer = new XMLImporter();
    String xmlN = "<text>\n" + //
        "<s>Frankenstein discovered that I detailed or made notes concerning his history he asked to see them &amp; himself corrected\n" + //
        "<del/><add place=\"superlinear\">and augmented</add>\n" + //
        "them in many places</s>\n" + //
        "</text>";
    VariantWitnessGraph wF = importer.importXML("N", xmlN);
    LOG.info("N: {}", xmlN);
    String xmlF = "<text>\n" + //
        "<s>Frankenstein discovered\n" + //
        "<del rend=\"strikethrough\">or</del>\n" + //
        "<add place=\"superlinear\">that I</add> made notes concerning his history; he asked to see them and then himself corrected and augmented them in many places\n" + //
        "</s>\n" + //
        "</text>";
    LOG.info("F: {}", xmlF);
    VariantWitnessGraph wQ = importer.importXML("F", xmlF);//
    String expected = "digraph CollationGraph{\n" + "labelloc=b\n" + "t000 [label=\"\";shape=doublecircle,rank=middle]\n" + "t001 [label=\"\";shape=doublecircle,rank=middle]\n"
        + "t002 [label=<F: Frankenstein&#9251;discovered<br/>N: Frankenstein&#9251;discovered&#9251;<br/>F,N: <i>/text/s</i>>]\n" + "t003 [label=<F: ;&#9251;<br/>F: <i>/text/s</i>>]\n"
        + "t004 [label=<F,N: he&#9251;asked&#9251;to&#9251;see&#9251;them&#9251;<br/>F,N: <i>/text/s</i>>]\n" + "t005 [label=<F: and&#9251;then&#9251;<br/>F: <i>/text/s</i>>]\n"
        + "t006 [label=<F: himself&#9251;corrected&#9251;<br/>N: himself&#9251;corrected&#x21A9;<br/><br/>F,N: <i>/text/s</i>>]\n"
        + "t007 [label=<F: and&#9251;augmented&#9251;<br/>N: and&#9251;augmented<br/>F: <i>/text/s</i><br/>N: <i>/text/s/add</i><br/>>]\n"
        + "t008 [label=<F: them&#9251;in&#9251;many&#9251;places&#x21A9;<br/><br/>N: them&#9251;in&#9251;many&#9251;places<br/>F,N: <i>/text/s</i>>]\n"
        + "t009 [label=<F: or<br/>F: <i>/text/s/del</i>>]\n" + "t010 [label=<F: that&#9251;I<br/>N: that&#9251;I&#9251;<br/>F: <i>/text/s/add</i><br/>N: <i>/text/s</i><br/>>]\n"
        + "t011 [label=<F: &#9251;<br/>F: <i>/text/s</i>>]\n"
        + "t012 [label=<F: made&#9251;notes&#9251;concerning&#9251;his&#9251;history<br/>N: made&#9251;notes&#9251;concerning&#9251;his&#9251;history&#9251;<br/>F,N: <i>/text/s</i>>]\n"
        + "t013 [label=<N: &&#9251;<br/>N: <i>/text/s</i>>]\n" + "t014 [label=<N: <br/>N: <i>/text/s/del</i>>]\n" + "t015 [label=<N: detailed&#9251;or&#9251;<br/>N: <i>/text/s</i>>]\n"
        + "t000->t002[label=\"F,N\"]\n" + "t002->t009[label=\"F\"]\n" + "t002->t010[label=\"F,N\"]\n" + "t003->t004[label=\"F\"]\n" + "t004->t005[label=\"F\"]\n" + "t004->t013[label=\"N\"]\n"
        + "t005->t006[label=\"F\"]\n" + "t006->t007[label=\"F,N\"]\n" + "t006->t014[label=\"N\"]\n" + "t007->t008[label=\"F,N\"]\n" + "t008->t001[label=\"F,N\"]\n" + "t009->t011[label=\"F\"]\n"
        + "t010->t011[label=\"F\"]\n" + "t010->t015[label=\"N\"]\n" + "t011->t012[label=\"F\"]\n" + "t012->t003[label=\"F\"]\n" + "t012->t004[label=\"N\"]\n" + "t013->t006[label=\"N\"]\n"
        + "t014->t008[label=\"N\"]\n" + "t015->t012[label=\"N\"]\n" + "}";

    testHyperCollation(wF, wQ, expected);
  }

  private void testHyperCollation(VariantWitnessGraph witness1, VariantWitnessGraph witness2, String expected) {
    Map<String, Long> collationDuration = new HashMap<>();
    for (HyperCollater hypercollater : hyperCollaters) {
      String name = hypercollater.getOptimalMatchSetFinderName();
      LOG.info("Collating with {}", name);
      Stopwatch stopwatch = Stopwatch.createStarted();
      CollationGraph collation0 = hypercollater.collate(witness1, witness2);
      stopwatch.stop();
      long duration = stopwatch.elapsed(TimeUnit.MILLISECONDS);
      LOG.info("Collating with {} took {} ms.", name, duration);
      collationDuration.put(name, duration);

      CollationGraph collation = CollationGraphNodeJoiner.join(collation0);

      String dot = CollationGraphVisualizer.toDot(collation);
      // System.out.println(dot);
      writeGraph(dot);
      assertThat(dot).isEqualTo(expected);

      String table = CollationGraphVisualizer.toTableASCII(collation);
      System.out.println(table);
    }
    collationDuration.forEach((name, duration) -> LOG.info("Collating with {} took {} ms.", name, duration));
  }

}
