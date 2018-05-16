package nl.knaw.huygens.hypercollate.collator;

/*-
 * #%L
 * hyper-collate-core
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

import com.google.common.base.Stopwatch;
import eu.interedition.collatex.dekker.Tuple;
import nl.knaw.huygens.hypercollate.HyperCollateTest;
import nl.knaw.huygens.hypercollate.importer.XMLImporter;
import nl.knaw.huygens.hypercollate.model.*;
import nl.knaw.huygens.hypercollate.model.CollationGraphAssert.MarkupNodeSketch;
import nl.knaw.huygens.hypercollate.model.CollationGraphAssert.TextNodeSketch;
import nl.knaw.huygens.hypercollate.tools.CollationGraphNodeJoiner;
import nl.knaw.huygens.hypercollate.tools.CollationGraphVisualizer;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Sets;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.*;
import static nl.knaw.huygens.hypercollate.HyperCollateAssertions.assertThat;
import static nl.knaw.huygens.hypercollate.model.CollationGraphAssert.markupNodeSketch;
import static nl.knaw.huygens.hypercollate.model.CollationGraphAssert.textNodeSketch;

public class HyperCollatorTest extends HyperCollateTest {
  private static final Logger LOG = LoggerFactory.getLogger(HyperCollateTest.class);

  final HyperCollator hyperCollator = new HyperCollator();

  @Test
  public void testHierarchyWith3Witnesses() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wF = importer.importXML("F", "<text>\n" + //
        "    <s>Hoe zoet moet nochtans zijn dit <lb/><del>werven om</del><add>trachten naar</add> een vrouw,\n" + //
        "        de ongewisheid vóór de <lb/>liefelijke toestemming!</s>\n" + //
        "</text>");
    VariantWitnessGraph wQ = importer.importXML("Q", "<text>\n" + //
        "    <s>Hoe zoet moet nochtans zijn dit <del>werven om</del><add>trachten naar</add> een <lb/>vrouw !\n" + //
        "        Die dagen van nerveuze verwachting vóór de liefelijke toestemming.</s>\n" + //
        "</text>");
    VariantWitnessGraph wZ = importer.importXML("Z", "<text>\n" + //
        "    <s>Hoe zoet moet nochtans zijn dit trachten naar een vrouw !\n" + //
        "        Die dagen van ongewisheid vóór de liefelijke toestemming.</s>\n" + //
        "</text>");

    String expected = "digraph CollationGraph{\n"//
        + "labelloc=b\n"//
        + "t000 [label=\"\";shape=doublecircle,rank=middle]\n"//
        + "t001 [label=\"\";shape=doublecircle,rank=middle]\n"//
        + "t002 [label=<F,Q,Z: Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/>F,Q,Z: <i>/text/s</i>>]\n" //
        + "t003 [label=<F,Q: &#9251;<br/>F,Q: <i>/text/s</i>>]\n"//
        + "t004 [label=<F,Q,Z: een&#9251;<br/>F,Q,Z: <i>/text/s</i>>]\n" //
        + "t005 [label=<F: vrouw<br/>Q: vrouw&#9251;<br/>Z: vrouw&#9251;<br/>F,Q,Z: <i>/text/s</i>>]\n"//
        + "t006 [label=<F: ,&#x21A9;<br/>&#9251;de&#9251;<br/>F: <i>/text/s</i>>]\n" //
        + "t007 [label=<F,Z: ongewisheid&#9251;<br/>F,Z: <i>/text/s</i>>]\n"//
        + "t008 [label=<F,Q,Z: vóór&#9251;de&#9251;<br/>F,Q,Z: <i>/text/s</i>>]\n" //
        + "t009 [label=<F: <br/>F: <i>/text/s/lb</i>>]\n"//
        + "t010 [label=<F,Q,Z: liefelijke&#9251;toestemming<br/>F,Q,Z: <i>/text/s</i>>]\n" //
        + "t011 [label=<F: !<br/>F: <i>/text/s</i>>]\n" //
        + "t012 [label=<F: <br/>F: <i>/text/s/lb</i>>]\n"//
        + "t013 [label=<F,Q: werven&#9251;om<br/>F,Q: <i>/text/s/del</i>>]\n"//
        + "t014 [label=<F: trachten&#9251;naar<br/>Q: trachten&#9251;naar<br/>Z: trachten&#9251;naar&#9251;<br/>F: <i>/text/s/add</i><br/>Q: <i>/text/s/add</i><br/>Z: <i>/text/s</i><br/>>]\n"//
        + "t015 [label=<Q: <br/>Q: <i>/text/s/lb</i>>]\n" //
        + "t016 [label=<Q: !&#x21A9;<br/>&#9251;Die&#9251;dagen&#9251;van&#9251;<br/>Z: !Die&#9251;dagen&#9251;van&#9251;<br/>Q,Z: <i>/text/s</i>>]\n"//
        + "t017 [label=<Q: nerveuze&#9251;verwachting&#9251;<br/>Q: <i>/text/s</i>>]\n" //
        + "t018 [label=<Q,Z: .<br/>Q,Z: <i>/text/s</i>>]\n" //
        + "t000->t002[label=\"F,Q,Z\"]\n"//
        + "t002->t012[label=\"F\"]\n" //
        + "t002->t013[label=\"Q\"]\n" //
        + "t002->t014[label=\"Q,Z\"]\n" //
        + "t003->t004[label=\"F,Q\"]\n" //
        + "t004->t005[label=\"F,Z\"]\n" //
        + "t004->t015[label=\"Q\"]\n"
        + "t005->t006[label=\"F\"]\n" //
        + "t005->t016[label=\"Q,Z\"]\n" //
        + "t006->t007[label=\"F\"]\n" //
        + "t007->t008[label=\"F,Z\"]\n" //
        + "t008->t009[label=\"F\"]\n" //
        + "t008->t010[label=\"Q,Z\"]\n"
        + "t009->t010[label=\"F\"]\n" //
        + "t010->t011[label=\"F\"]\n" //
        + "t010->t018[label=\"Q,Z\"]\n" //
        + "t011->t001[label=\"F\"]\n" //
        + "t012->t013[label=\"F\"]\n" //
        + "t012->t014[label=\"F\"]\n"
        + "t013->t003[label=\"F,Q\"]\n" //
        + "t014->t003[label=\"F,Q\"]\n" //
        + "t014->t004[label=\"Z\"]\n" //
        + "t015->t005[label=\"Q\"]\n" //
        + "t016->t007[label=\"Z\"]\n" //
        + "t016->t017[label=\"Q\"]\n"
        + "t017->t008[label=\"Q\"]\n" //
        + "t018->t001[label=\"Q,Z\"]\n" //
        + "}";

    CollationGraph collationGraph = testHyperCollation3(wF, wQ, wZ, expected);

    // test matching tokens
    TextNodeSketch n1 = textNodeSketch()
        .withWitnessSegmentSketch("F", "Hoe zoet moet nochtans zijn dit ")
        .withWitnessSegmentSketch("Q", "Hoe zoet moet nochtans zijn dit ")
        .withWitnessSegmentSketch("Z", "Hoe zoet moet nochtans zijn dit ");
    TextNodeSketch n2 = textNodeSketch()
        .withWitnessSegmentSketch("F", " ")
        .withWitnessSegmentSketch("Q", " ");
    TextNodeSketch n3 = textNodeSketch()
        .withWitnessSegmentSketch("F", "een ")
        .withWitnessSegmentSketch("Q", "een ")
        .withWitnessSegmentSketch("Z", "een ");
    TextNodeSketch n4 = textNodeSketch()
        .withWitnessSegmentSketch("F", "vrouw")
        .withWitnessSegmentSketch("Q", "vrouw ")
        .withWitnessSegmentSketch("Z", "vrouw ");
    TextNodeSketch n5 = textNodeSketch()
        .withWitnessSegmentSketch("F", "ongewisheid ")
        .withWitnessSegmentSketch("Z", "ongewisheid ");
    TextNodeSketch n6 = textNodeSketch()
        .withWitnessSegmentSketch("F", "liefelijke toestemming")
        .withWitnessSegmentSketch("Z", "liefelijke toestemming")
        .withWitnessSegmentSketch("Q", "liefelijke toestemming");
    TextNodeSketch trachten_naar = textNodeSketch()
        .withWitnessSegmentSketch("F", "trachten naar")
        .withWitnessSegmentSketch("Q", "trachten naar")
        .withWitnessSegmentSketch("Z", "trachten naar ");
    TextNodeSketch werven_om = textNodeSketch()
        .withWitnessSegmentSketch("F", "werven om")
        .withWitnessSegmentSketch("Q", "werven om");

    assertThat(collationGraph).containsTextNodesMatching(n1, n2, n3, n4, n5, n6, trachten_naar, werven_om);

    assertThat(collationGraph).containsMarkupNodesMatching(//
        markupNodeSketch("F", "text"),//
        markupNodeSketch("Q", "text"),//
        markupNodeSketch("Z", "text")
    );

    MarkupNodeSketch f_del = markupNodeSketch("F", "del");
    MarkupNodeSketch q_add = markupNodeSketch("Q", "add");
    assertThat(collationGraph).hasTextNodeMatching(werven_om).withMarkupNodesMatching(f_del);
    assertThat(collationGraph).hasMarkupNodeMatching(q_add).withTextNodesMatching(trachten_naar);
  }

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

    CollationGraph collationGraph = testHyperCollation(wF, wQ, expected);

    // test matching tokens

    assertThat(collationGraph).containsTextNodesMatching(
        textNodeSketch()
            .withWitnessSegmentSketch("F", "Hoe zoet moet nochtans zijn dit ")
            .withWitnessSegmentSketch("Q", "Hoe zoet moet nochtans zijn dit "),
        textNodeSketch()
            .withWitnessSegmentSketch("F", "trachten naar")
            .withWitnessSegmentSketch("Q", "trachten naar"),
        textNodeSketch()
            .withWitnessSegmentSketch("F", "werven om")
            .withWitnessSegmentSketch("Q", "werven om"),
        textNodeSketch()
            .withWitnessSegmentSketch("F", " een ")
            .withWitnessSegmentSketch("Q", " een "),
        textNodeSketch()
            .withWitnessSegmentSketch("F", "vrouw")
            .withWitnessSegmentSketch("Q", "vrouw "),
        textNodeSketch()
            .withWitnessSegmentSketch("F", "vóór de ")
            .withWitnessSegmentSketch("Q", "vóór de "),
        textNodeSketch()
            .withWitnessSegmentSketch("F", "liefelijke toestemming")
            .withWitnessSegmentSketch("Q", "liefelijke toestemming")
    );
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
        "F_000->F_007\n" + //
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
        "t002->t006[label=\"F\"]\n" + //
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

    CollationGraph collationGraph = testHyperCollation(wF, wQ, expected);
    assertThat(collationGraph).containsTextNodesMatching(
        textNodeSketch()
            .withWitnessSegmentSketch("F", "De vent was woedend en maakte ")
            .withWitnessSegmentSketch("Q", "De vent was woedend en maakte "),
        textNodeSketch()
            .withWitnessSegmentSketch("F", "Shiriar")
            .withWitnessSegmentSketch("Q", "Shiriar"),
        textNodeSketch()
            .withWitnessSegmentSketch("F", "den bedremmelden\n        ")
            .withWitnessSegmentSketch("Q", "den bedremmelden "),
        textNodeSketch()
            .withWitnessSegmentSketch("F", "Sultan ")
            .withWitnessSegmentSketch("Q", "Sultan"),
        textNodeSketch()
            .withWitnessSegmentSketch("F", "uit voor \"lompen boer\".")
            .withWitnessSegmentSketch("Q", "uit voor \"lompen boer\".")
    );
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

    CollationGraph collationGraph = testHyperCollation(wF, wQ, expected);
    assertThat(collationGraph).containsOnlyTextNodesMatching(
        textNodeSketch()
            .withWitnessSegmentSketch("A", "The dog's ")
            .withWitnessSegmentSketch("B", "The dog's "),
        textNodeSketch()
            .withWitnessSegmentSketch("A", "big ")
            .withWitnessSegmentSketch("B", "big "),
        textNodeSketch()
            .withWitnessSegmentSketch("B", "black ears"),
        textNodeSketch()
            .withWitnessSegmentSketch("B", "brown "),
        textNodeSketch()
            .withWitnessSegmentSketch("A", "eyes")
            .withWitnessSegmentSketch("B", "eyes"),
        textNodeSketch()
            .withWitnessSegmentSketch("A", ".")
            .withWitnessSegmentSketch("B", ".")
    );
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

    CollationGraph collationGraph = testHyperCollation(wF, wQ, expected);
    assertThat(collationGraph).containsTextNodesMatching(
        textNodeSketch()
            .withWitnessSegmentSketch("A", "b b b b b b b ")
            .withWitnessSegmentSketch("B", "b b b b b b b ")
    );
    assertThat(collationGraph).doesNotContainTextNodesMatching(
        textNodeSketch()
            .withWitnessSegmentSketch("A", "T ")
            .withWitnessSegmentSketch("B", "X ")
    );

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

    CollationGraph collationGraph = testHyperCollation(wF, wQ, expected);
    assertThat(collationGraph).containsTextNodesMatching(
        textNodeSketch()
            .withWitnessSegmentSketch("A", "A ")
            .withWitnessSegmentSketch("B", "A "),
        textNodeSketch()
            .withWitnessSegmentSketch("A", "E C ")
            .withWitnessSegmentSketch("B", "E C "),
        textNodeSketch()
            .withWitnessSegmentSketch("A", "G ")
            .withWitnessSegmentSketch("B", "G ")
    );
    assertThat(collationGraph).doesNotContainTextNodesMatching(
        textNodeSketch()
            .withWitnessSegmentSketch("A", "H")
            .withWitnessSegmentSketch("B", "H ")
    );
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
        "<s> granting, as she stood the chair straight by the dressing table, <add>leaning her bony breast on the hard thorn</add>, her forgiveness of it all.</s>\n" + //
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
        "t007 [label=<T: ,&#9251;<br/>T: <i>/text/p/s</i>>]\n" + //
        "t008 [label=<T: of&#9251;it&#9251;all<br/>T: <i>/text/p/s</i>>]\n" + //
        "t009 [label=<T: ...&#9251;<br/>T: <i>/text/p/s</i>>]\n" + //
        "t000->t002[label=\"H\"]\n" + //
        "t000->t006[label=\"T\"]\n" + //
        "t002->t005[label=\"H\"]\n" + //
        "t002->t007[label=\"T\"]\n" + //
        "t003->t004[label=\"H\"]\n" + //
        "t003->t008[label=\"T\"]\n" + //
        "t004->t001[label=\"H\"]\n" + //
        "t004->t009[label=\"T\"]\n" + //
        "t005->t003[label=\"H\"]\n" + //
        "t006->t002[label=\"T\"]\n" + //
        "t006->t007[label=\"T\"]\n" + //
        "t007->t003[label=\"T\"]\n" + //
        "t008->t004[label=\"T\"]\n" + //
        "t009->t001[label=\"T\"]\n" + //
        "}";

    CollationGraph collationGraph = testHyperCollation(wF, wQ, expected);
    assertThat(collationGraph).containsTextNodesMatching(
        textNodeSketch()
            .withWitnessSegmentSketch("H", "Leaning her bony breast on the hard thorn ")
            .withWitnessSegmentSketch("T", "leaning her bony breast on the hard thorn"),
        textNodeSketch()
            .withWitnessSegmentSketch("H", "her forgiveness")
            .withWitnessSegmentSketch("T", "her forgiveness "),
        textNodeSketch()
            .withWitnessSegmentSketch("H", ".Was it then that she had her consolations  ")
            .withWitnessSegmentSketch("T", ".Was it then that she had her consolations ")
    );
    assertThat(collationGraph).doesNotContainTextNodesMatching(
        textNodeSketch()
            .withWitnessSegmentSketch("H", ", ")
            .withWitnessSegmentSketch("T", ", ")
    );
  }

  @Test
  public void testMaryShellyGodwinFrankensteinFragment1() {
    XMLImporter importer = new XMLImporter();
    String xmlN = "<text>\n" + //
        "<s>so destitute of every hope of consolation to live\n" + //
        "<del rend=\"strikethrough\">-</del>\n" + //
        "<add place=\"overwritten\" hand=\"#pbs\">?</add> oh no - ...\n" + //
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
    String expected = "digraph CollationGraph{\n" + //
        "labelloc=b\n" + //
        "t000 [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t001 [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t002 [label=<F,N: so&#9251;<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>]\n" + //
        "t003 [label=<F,N: ?<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s/add</i><br/>>]\n" + //
        "t004 [label=<F,N: &#9251;<br/>F: <i>/text/p</i><br/>N: <i>/text/s</i><br/>>]\n" + //
        "t005 [label=<F: Oh<br/>N: oh&#9251;<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>]\n" + //
        "t006 [label=<F: ,&#9251;<br/>F: <i>/text/p/s</i>>]\n" + //
        "t007 [label=<F: no<br/>N: no&#9251;<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>]\n" + //
        "t008 [label=<F: !&#9251;<br/>F: <i>/text/p/s</i>>]\n" + //
        "t009 [label=<F: ...&#9251;<br/>N: ...&#x21A9;<br/><br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>]\n" + //
        "t010 [label=<F: infinitely&#9251;miserable,&#9251;so&#9251;<br/>F: <i>/text/p/s</i>>]\n" + //
        "t011 [label=<F,N: destitute&#9251;of&#9251;every&#9251;hope&#9251;of&#9251;consolation&#9251;to&#9251;live<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>]\n" + //
        "t012 [label=<N: -&#9251;<br/>N: <i>/text/s</i>>]\n" + //
        "t013 [label=<N: -<br/>N: <i>/text/s/del</i>>]\n" + //
        "t000->t002[label=\"F,N\"]\n" + //
        "t002->t010[label=\"F\"]\n" + //
        "t002->t011[label=\"N\"]\n" + //
        "t003->t004[label=\"F,N\"]\n" + //
        "t004->t005[label=\"F,N\"]\n" + //
        "t005->t006[label=\"F\"]\n" + //
        "t005->t007[label=\"N\"]\n" + //
        "t006->t007[label=\"F\"]\n" + //
        "t007->t008[label=\"F\"]\n" + //
        "t007->t012[label=\"N\"]\n" + //
        "t008->t009[label=\"F\"]\n" + //
        "t009->t001[label=\"F,N\"]\n" + //
        "t010->t011[label=\"F\"]\n" + //
        "t011->t003[label=\"F,N\"]\n" + //
        "t011->t013[label=\"N\"]\n" + //
        "t012->t009[label=\"N\"]\n" + //
        "t013->t004[label=\"N\"]\n" + //
        "}";
    String expected1 = "digraph CollationGraph{\n" + //
        "labelloc=b\n" + //
        "t000 [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t001 [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t002 [label=<F: so&#9251;infinitely&#9251;miserable,&#9251;<br/>F: <i>/text/p/s</i>>]\n" + //
        "t003 [label=<F,N: ?<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s/add</i><br/>>]\n" + //
        "t004 [label=<F,N: &#9251;<br/>F: <i>/text/p</i><br/>N: <i>/text/s</i><br/>>]\n" + //
        "t005 [label=<F: Oh<br/>N: oh&#9251;<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>]\n" + //
        "t006 [label=<F: ,&#9251;<br/>F: <i>/text/p/s</i>>]\n" + //
        "t007 [label=<F: no<br/>N: no&#9251;<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>]\n" + //
        "t008 [label=<F: !&#9251;<br/>F: <i>/text/p/s</i>>]\n" + //
        "t009 [label=<F: ...&#9251;<br/>N: ...&#x21A9;<br/><br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>]\n" + //
        "t010 [label=<F,N: so&#9251;destitute&#9251;of&#9251;every&#9251;hope&#9251;of&#9251;consolation&#9251;to&#9251;live<br/>F: <i>/text/p/s</i><br/>N: <i>/text/s</i><br/>>]\n" + //
        "t011 [label=<N: -&#9251;<br/>N: <i>/text/s</i>>]\n" + //
        "t012 [label=<N: -<br/>N: <i>/text/s/del</i>>]\n" + //
        "t000->t002[label=\"F\"]\n" + //
        "t000->t010[label=\"N\"]\n" + //
        "t002->t010[label=\"F\"]\n" + //
        "t003->t004[label=\"F,N\"]\n" + //
        "t004->t005[label=\"F,N\"]\n" + //
        "t005->t006[label=\"F\"]\n" + //
        "t005->t007[label=\"N\"]\n" + //
        "t006->t007[label=\"F\"]\n" + //
        "t007->t008[label=\"F\"]\n" + //
        "t007->t011[label=\"N\"]\n" + //
        "t008->t009[label=\"F\"]\n" + //
        "t009->t001[label=\"F,N\"]\n" + //
        "t010->t003[label=\"F,N\"]\n" + //
        "t010->t012[label=\"N\"]\n" + //
        "t011->t009[label=\"N\"]\n" + //
        "t012->t004[label=\"N\"]\n" + //
        "}";

    CollationGraph collationGraph = testHyperCollation(wF, wQ, expected);
    assertThat(collationGraph).containsTextNodesMatching(
        textNodeSketch()
            .withWitnessSegmentSketch("F", "so ")
            .withWitnessSegmentSketch("N", "so "),
        textNodeSketch()
            .withWitnessSegmentSketch("F", "destitute of every hope of consolation to live")
            .withWitnessSegmentSketch("N", "destitute of every hope of consolation to live"),
        textNodeSketch()
            .withWitnessSegmentSketch("F", " ")
            .withWitnessSegmentSketch("N", " "),
        textNodeSketch()
            .withWitnessSegmentSketch("F", "?")
            .withWitnessSegmentSketch("N", "?"),
        textNodeSketch()
            .withWitnessSegmentSketch("F", "Oh")
            .withWitnessSegmentSketch("N", "oh "),
        textNodeSketch()
            .withWitnessSegmentSketch("F", "no")
            .withWitnessSegmentSketch("N", "no "),
        textNodeSketch()
            .withWitnessSegmentSketch("F", "... ")
            .withWitnessSegmentSketch("N", "...\n")
    );
  }

  @Test
  public void testMaryShellyGodwinFrankensteinFragment2() {
    XMLImporter importer = new XMLImporter();
    String xmlN = "<text>\n" + //
        "<s>Frankenstein discovered that I detailed or made notes concerning his history he asked to see them &amp; himself corrected\n" + //
        "<add place=\"superlinear\">and augmented</add>\n" + //
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
    String expected = "digraph CollationGraph{\n" + //
        "labelloc=b\n" + //
        "t000 [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t001 [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t002 [label=<F: Frankenstein&#9251;discovered<br/>N: Frankenstein&#9251;discovered&#9251;<br/>F,N: <i>/text/s</i>>]\n" + //
        "t003 [label=<F: ;&#9251;<br/>F: <i>/text/s</i>>]\n" + //
        "t004 [label=<F,N: he&#9251;asked&#9251;to&#9251;see&#9251;them&#9251;<br/>F,N: <i>/text/s</i>>]\n" + //
        "t005 [label=<F: and&#9251;then&#9251;<br/>F: <i>/text/s</i>>]\n" + //
        "t006 [label=<F: himself&#9251;corrected&#9251;<br/>N: himself&#9251;corrected&#x21A9;<br/><br/>F,N: <i>/text/s</i>>]\n" + //
        "t007 [label=<F: and&#9251;augmented&#9251;<br/>N: and&#9251;augmented<br/>F: <i>/text/s</i><br/>N: <i>/text/s/add</i><br/>>]\n" + //
        "t008 [label=<F: them&#9251;in&#9251;many&#9251;places&#x21A9;<br/><br/>N: them&#9251;in&#9251;many&#9251;places<br/>F,N: <i>/text/s</i>>]\n" + //
        "t009 [label=<F: or<br/>N: or&#9251;<br/>F: <i>/text/s/del</i><br/>N: <i>/text/s</i><br/>>]\n" + //
        "t010 [label=<F: that&#9251;I<br/>N: that&#9251;I&#9251;<br/>F: <i>/text/s/add</i><br/>N: <i>/text/s</i><br/>>]\n" + //
        "t011 [label=<F: &#9251;<br/>F: <i>/text/s</i>>]\n" + //
        "t012 [label=<F: made&#9251;notes&#9251;concerning&#9251;his&#9251;history<br/>N: made&#9251;notes&#9251;concerning&#9251;his&#9251;history&#9251;<br/>F,N: <i>/text/s</i>>]\n" + //
        "t013 [label=<N: &amp;&#9251;<br/>N: <i>/text/s</i>>]\n" + //
        "t014 [label=<N: detailed&#9251;<br/>N: <i>/text/s</i>>]\n" + //
        "t000->t002[label=\"F,N\"]\n" + //
        "t002->t009[label=\"F\"]\n" + //
        "t002->t010[label=\"F,N\"]\n" + //
        "t003->t004[label=\"F\"]\n" + //
        "t004->t005[label=\"F\"]\n" + //
        "t004->t013[label=\"N\"]\n" + //
        "t005->t006[label=\"F\"]\n" + //
        "t006->t007[label=\"F,N\"]\n" + //
        "t006->t008[label=\"N\"]\n" + //
        "t007->t008[label=\"F,N\"]\n" + //
        "t008->t001[label=\"F,N\"]\n" + //
        "t009->t011[label=\"F\"]\n" + //
        "t009->t012[label=\"N\"]\n" + //
        "t010->t011[label=\"F\"]\n" + //
        "t010->t014[label=\"N\"]\n" + //
        "t011->t012[label=\"F\"]\n" + //
        "t012->t003[label=\"F\"]\n" + //
        "t012->t004[label=\"N\"]\n" + //
        "t013->t006[label=\"N\"]\n" + //
        "t014->t009[label=\"N\"]\n" + //
        "}";

    CollationGraph collationGraph = testHyperCollation(wF, wQ, expected);
    assertThat(collationGraph).containsTextNodesMatching(
        textNodeSketch()
            .withWitnessSegmentSketch("F", "Frankenstein discovered")
            .withWitnessSegmentSketch("N", "Frankenstein discovered "),
        textNodeSketch()
            .withWitnessSegmentSketch("F", "that I")
            .withWitnessSegmentSketch("N", "that I "),
        textNodeSketch()
            .withWitnessSegmentSketch("F", "or")
            .withWitnessSegmentSketch("N", "or "),
        textNodeSketch()
            .withWitnessSegmentSketch("F", "made notes concerning his history")
            .withWitnessSegmentSketch("N", "made notes concerning his history "),
        textNodeSketch()
            .withWitnessSegmentSketch("F", "he asked to see them ")
            .withWitnessSegmentSketch("N", "he asked to see them "),
        textNodeSketch()
            .withWitnessSegmentSketch("F", "himself corrected ")
            .withWitnessSegmentSketch("N", "himself corrected\n"),
        textNodeSketch()
            .withWitnessSegmentSketch("F", "and augmented ")
            .withWitnessSegmentSketch("N", "and augmented"),
        textNodeSketch()
            .withWitnessSegmentSketch("F", "them in many places\n")
            .withWitnessSegmentSketch("N", "them in many places")
    );
  }

  @Test
  public void testCollationGraphInitialization() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wF = importer.importXML("F", "<text>\n" + //
        "    <s>Hoe zoet moet nochtans zijn dit <lb/><del>werven om</del><add>trachten naar</add> een vrouw,\n" + //
        "        de ongewisheid vóór de <lb/>liefelijke toestemming!</s>\n" + //
        "</text>");
    CollationGraph collationGraph = new CollationGraph();
    Map<TokenVertex, TextNode> map = new HashMap<>();
    List<Match> matches = new ArrayList<>();
    Map<Markup, MarkupNode> markupNodeIndex = new HashMap<>();
    hyperCollator.initialize(collationGraph, map, markupNodeIndex, wF);
    CollationGraph collation = CollationGraphNodeJoiner.join(collationGraph);

    String dot = CollationGraphVisualizer.toDot(collation, true, false);
    String expected = "digraph CollationGraph{\n" +
        "labelloc=b\n" +
        "t000 [label=<<br/>>]\n" +
        "t001 [label=<<br/>>]\n" +
        "t002 [label=<F: Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/>F: <i>/text/s</i>>]\n" +
        "t003 [label=<F: &#9251;een&#9251;vrouw,&#x21A9;<br/>&#9251;de&#9251;ongewisheid&#9251;vóór&#9251;de&#9251;<br/>F: <i>/text/s</i>>]\n" +
        "t004 [label=<F: <br/>F: <i>/text/s/lb</i>>]\n" +
        "t005 [label=<F: liefelijke&#9251;toestemming!<br/>F: <i>/text/s</i>>]\n" +
        "t006 [label=<F: <br/>F: <i>/text/s/lb</i>>]\n" +
        "t007 [label=<F: werven&#9251;om<br/>F: <i>/text/s/del</i>>]\n" +
        "t008 [label=<F: trachten&#9251;naar<br/>F: <i>/text/s/add</i>>]\n" +
        "t000->t002[label=\"F\"]\n" +
        "t002->t006[label=\"F\"]\n" +
        "t003->t004[label=\"F\"]\n" +
        "t004->t005[label=\"F\"]\n" +
        "t005->t001[label=\"F\"]\n" +
        "t006->t007[label=\"F\"]\n" +
        "t006->t008[label=\"F\"]\n" +
        "t007->t003[label=\"F\"]\n" +
        "t008->t003[label=\"F\"]\n" +
        "}";
    assertThat(dot).isEqualTo(expected);

    String dotWithoutMarkupAndWhitespaceEmphasis = CollationGraphVisualizer.toDot(collation, false, true);
    String expected2 = "digraph CollationGraph{\n" +
        "labelloc=b\n" +
        "t000 [label=\"\";shape=doublecircle,rank=middle]\n" +
        "t001 [label=\"\";shape=doublecircle,rank=middle]\n" +
        "t002 [label=<F: Hoe&nbsp;zoet&nbsp;moet&nbsp;nochtans&nbsp;zijn&nbsp;dit&nbsp;>]\n" +
        "t003 [label=<F: &nbsp;een&nbsp;vrouw,&#x21A9;<br/>&nbsp;de&nbsp;ongewisheid&nbsp;vóór&nbsp;de&nbsp;>]\n" +
        "t004 [label=<F: >]\n" +
        "t005 [label=<F: liefelijke&nbsp;toestemming!>]\n" +
        "t006 [label=<F: >]\n" +
        "t007 [label=<F: werven&nbsp;om>]\n" +
        "t008 [label=<F: trachten&nbsp;naar>]\n" +
        "t000->t002[label=\"F\"]\n" +
        "t002->t006[label=\"F\"]\n" +
        "t003->t004[label=\"F\"]\n" +
        "t004->t005[label=\"F\"]\n" +
        "t005->t001[label=\"F\"]\n" +
        "t006->t007[label=\"F\"]\n" +
        "t006->t008[label=\"F\"]\n" +
        "t007->t003[label=\"F\"]\n" +
        "t008->t003[label=\"F\"]\n" +
        "}";
    assertThat(dotWithoutMarkupAndWhitespaceEmphasis).isEqualTo(expected2);

    // System.out.println(dot);
    // writeGraph(dot, "graph");
  }

  @Test
  public void testPermute() {
    List<Tuple<Integer>> permute1 = hyperCollator.permute(3);
    LOG.info("permute={}", visualize(permute1));
    assertThat(Sets.newHashSet(permute1)).hasSameSizeAs(permute1);
    assertThat(permute1).hasSize(3);

    List<Tuple<Integer>> permute2 = hyperCollator.permute(4);
    LOG.info("permute={}", visualize(permute2));
    assertThat(Sets.newHashSet(permute2)).hasSameSizeAs(permute2);
    assertThat(permute2).hasSize(6);

    List<Tuple<Integer>> permute3 = hyperCollator.permute(10);
    LOG.info("permute={}", visualize(permute3));
    assertThat(Sets.newHashSet(permute3)).hasSameSizeAs(permute3);
    assertThat(permute3).hasSize(45);
  }

  @Test
  public void testPotentialMatches() {
    XMLImporter importer = new XMLImporter();
    String sigil1 = "A";
    String sigil2 = "B";
    String sigil3 = "C";
    VariantWitnessGraph w1 = importer.importXML(sigil1, "<x>the black cat</x>");
    VariantWitnessGraph w2 = importer.importXML(sigil2, "<x>the blue dog</x>");
    VariantWitnessGraph w3 = importer.importXML(sigil3, "<x>the black dog</x>");
    List<VariantWitnessGraph> witnesses = asList(w1, w2, w3);
    List<VariantWitnessGraphRanking> rankings = witnesses.stream()//
        .map(VariantWitnessGraphRanking::of)//
        .collect(toList());
    Set<Match> allPotentialMatches = hyperCollator.getPotentialMatches(witnesses, rankings);
    LOG.info("allPotentialMatches={}", allPotentialMatches);
    String match1 = "<A0,B0>";
    String match2 = "<A0,C0>";
    String match3 = "<A1,C1>";
    String match4 = "<B0,C0>";
    String match5 = "<B2,C2>";
    String match6 = "<A:EndTokenVertex,B:EndTokenVertex>";
    String match7 = "<A:EndTokenVertex,C:EndTokenVertex>";
    String match8 = "<B:EndTokenVertex,C:EndTokenVertex>";
    Assertions.assertThat(allPotentialMatches).hasSize(8);
    Set<String> matchStrings = allPotentialMatches.stream().map(Match::toString).collect(toSet());
    assertThat(matchStrings).contains(match1, match2, match3, match4, match5, match6, match7, match8);

    Map<String, List<Match>> sortAndFilterMatchesByWitness = hyperCollator.sortAndFilterMatchesByWitness(allPotentialMatches, asList(sigil1, sigil2, sigil3));
    LOG.info("sortAndFilterMatchesByWitness={}", sortAndFilterMatchesByWitness);
    assertThat(sortAndFilterMatchesByWitness).containsOnlyKeys(sigil1, sigil2, sigil3);

    List<String> listA = stringList(sortAndFilterMatchesByWitness, sigil1);
    assertThat(listA).containsOnly(match1, match2, match3, match6, match7);

    List<String> listB = stringList(sortAndFilterMatchesByWitness, sigil2);
    assertThat(listB).containsOnly(match4, match1, match5, match6, match8);

    List<String> listC = stringList(sortAndFilterMatchesByWitness, sigil3);
    assertThat(listC).containsOnly(match4, match2, match3, match5, match7, match8);
  }

  private List<String> stringList(Map<String, List<Match>> sortAndFilterMatchesByWitness, String key) {
    return sortAndFilterMatchesByWitness.get(key)//
        .stream()//
        .map(Match::toString)//
        .collect(toList());
  }

  private String visualize(List<Tuple<Integer>> list) {
    return list.stream()//
        .map(t -> MessageFormat.format("<{0},{1}>", t.left, t.right))//
        .collect(joining(""));
  }

  private CollationGraph testHyperCollation(VariantWitnessGraph witness1, VariantWitnessGraph witness2, String expected) {
//    Map<String, Long> collationDuration = new HashMap<>();
    Stopwatch stopwatch = Stopwatch.createStarted();
    CollationGraph collation0 = hyperCollator.collate(witness1, witness2);
    stopwatch.stop();
    long duration = stopwatch.elapsed(TimeUnit.MILLISECONDS);
    LOG.info("Collating took {} ms.", duration);

    CollationGraph collation = CollationGraphNodeJoiner.join(collation0);

    String dot = CollationGraphVisualizer.toDot(collation, true, false);
    LOG.debug("dot=\n{}", dot);
    writeGraph(dot, "graph");
    assertThat(dot).isEqualTo(expected);

    String table = CollationGraphVisualizer.toTableASCII(collation, true);
    LOG.debug("table=\n{}", table);
    return collation;
  }

  private CollationGraph testHyperCollation3(VariantWitnessGraph witness1, VariantWitnessGraph witness2, VariantWitnessGraph witness3, String expected) {
//    Map<String, Long> collationDuration = new HashMap<>();
    Stopwatch stopwatch = Stopwatch.createStarted();
    CollationGraph collation = hyperCollator.collate(witness1, witness2, witness3);
    stopwatch.stop();
    long duration = stopwatch.elapsed(TimeUnit.MILLISECONDS);
    LOG.info("Collating took {} ms.", duration);

    Set<Markup> markupBeforeJoin = collation.getMarkupStream().collect(toSet());
//    LOG.info("before join: collation markup = {}", collation.getMarkupStream().map(Markup::toString).sorted().collect(toList()));

    collation = CollationGraphNodeJoiner.join(collation);

    Set<Markup> markupAfterJoin = collation.getMarkupStream().collect(toSet());
//    LOG.info("after join: collation markup = {}", collation.getMarkupStream().map(Markup::toString).sorted().collect(toList()));

    assertThat(markupAfterJoin).containsExactlyElementsOf(markupBeforeJoin);

    String dot = CollationGraphVisualizer.toDot(collation, true, false);
    LOG.info("dot=\n{}", dot);
    writeGraph(dot, "graph");
    assertThat(dot).isEqualTo(expected);

    String table = CollationGraphVisualizer.toTableASCII(collation, true);
    LOG.info("dot=\n{}", table);

    assertThat(collation).isNotNull();
    return collation;
  }

}
