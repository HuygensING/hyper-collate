package nl.knaw.huygens.hypercollate.importer;

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

import java.io.InputStream;

import org.junit.Test;

import nl.knaw.huygens.hypercollate.HyperCollateTest;
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph;

public class XMLImporterTest extends HyperCollateTest {

  @Test
  public void testImportFromString() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wg0 = importer.importXML("A", "<xml>Mondays are <del>well good</del><add>def bad</add>!</xml>");
    String expectedDot = "digraph VariantWitnessGraph{\n" + //
        "graph [rankdir=LR]\n" + //
        "labelloc=b\n" + //
        "begin [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "A_000 [label=<Mondays&#9251;are&#9251;<br/><i>A: /xml</i>>]\n" + //
        "A_002 [label=<well&#9251;good<br/><i>A: /xml/del</i>>]\n" + //
        "A_004 [label=<def&#9251;bad<br/><i>A: /xml/add</i>>]\n" + //
        "A_006 [label=<!<br/><i>A: /xml</i>>]\n" + //
        "end [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "A_000->A_002\n" + //
        "A_000->A_004\n" + //
        "A_002->A_006\n" + //
        "A_004->A_006\n" + //
        "A_006->end\n" + //
        "begin->A_000\n" + //
        "}";
    verifyDotExport(wg0, expectedDot);
  }

  @Test
  public void testImportFromFile() {
    XMLImporter importer = new XMLImporter();
    InputStream resourceAsStream = getClass().getResourceAsStream("/witness.xml");
    VariantWitnessGraph wg0 = importer.importXML("A", resourceAsStream);
    String expected = "digraph VariantWitnessGraph{\n" + //
        "graph [rankdir=LR]\n" + //
        "labelloc=b\n" + //
        "begin [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "A_000 [label=<De&#9251;vent&#9251;was&#9251;woedend&#9251;en&#9251;maakte&#9251;<br/><i>A: /text/s</i>>]\n" + //
        "A_006 [label=<Shiriar<br/><i>A: /text/s/del</i>>]\n" + //
        "A_007 [label=<den&#9251;bedremmelden&#9251;<br/><i>A: /text/s/add</i>>]\n" + //
        "A_011 [label=<&#9251;uit&#9251;voor&#9251;\"lompen&#9251;boer\".<br/><i>A: /text/s</i>>]\n" + //
        "A_009 [label=<man<br/><i>A: /text/s/add/del</i>>]\n" + //
        "A_010 [label=<Sultan<br/><i>A: /text/s/add/add</i>>]\n" + //
        "end [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "A_000->A_006\n" + //
        "A_000->A_007\n" + //
        "A_006->A_011\n" + //
        "A_007->A_009\n" + //
        "A_007->A_010\n" + //
        "A_009->A_011\n" + //
        "A_010->A_011\n" + //
        "A_011->end\n" + //
        "begin->A_000\n" + //
        "}";

    verifyDotExport(wg0, expected);
  }

  @Test
  public void testDelWithoutAdd() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wg0 = importer.importXML("A", "<xml>Ja toch! <del>Niet dan?</del> Ik dacht het wel!</xml>");
    String expectedDot = "digraph VariantWitnessGraph{\n" + //
        "graph [rankdir=LR]\n" + //
        "labelloc=b\n" + //
        "begin [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "A_000 [label=<Ja&#9251;toch!&#9251;<br/><i>A: /xml</i>>]\n" + //
        "A_003 [label=<Niet&#9251;dan?<br/><i>A: /xml/del</i>>]\n" + //
        "A_006 [label=<&#9251;Ik&#9251;dacht&#9251;het&#9251;wel!<br/><i>A: /xml</i>>]\n" + //
        "end [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "A_000->A_003\n" + //
        "A_000->A_006\n" + //
        "A_003->A_006\n" + //
        "A_006->end\n" + //
        "begin->A_000\n" + //
        "}";

    verifyDotExport(wg0, expectedDot);
  }

  @Test
  public void testDelWithoutAddAtTheEnd() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wg0 = importer.importXML("A", "<xml>And they lived happily ever after. <del>Or not.</del></xml>");
    String expectedDot = "digraph VariantWitnessGraph{\n" + //
        "graph [rankdir=LR]\n" + //
        "labelloc=b\n" + //
        "begin [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "A_000 [label=<And&#9251;they&#9251;lived&#9251;happily&#9251;ever&#9251;after.&#9251;<br/><i>A: /xml</i>>]\n" + //
        "A_007 [label=<Or&#9251;not.<br/><i>A: /xml/del</i>>]\n" + //
        "end [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "A_000->A_007\n" + //
        "A_000->end\n" + //
        "A_007->end\n" + //
        "begin->A_000\n" + //
        "}";

    verifyDotExport(wg0, expectedDot);
  }

  @Test
  public void testAddWithoutDel() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wg0 = importer.importXML("A", "<xml>Eenie meeny <add>miny</add> moe.</xml>");
    String expectedDot = "digraph VariantWitnessGraph{\n" + //
        "graph [rankdir=LR]\n" + //
        "labelloc=b\n" + //
        "begin [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "A_000 [label=<Eenie&#9251;meeny&#9251;<br/><i>A: /xml</i>>]\n" + //
        "A_002 [label=<miny<br/><i>A: /xml/add</i>>]\n" + //
        "A_003 [label=<&#9251;moe.<br/><i>A: /xml</i>>]\n" + //
        "end [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "A_000->A_002\n" + //
        "A_000->A_003\n" + //
        "A_002->A_003\n" + //
        "A_003->end\n" + //
        "begin->A_000\n" + //
        "}";

    verifyDotExport(wg0, expectedDot);
  }

  @Test
  public void testAddWithoutDelAtTheEnd() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wg0 = importer.importXML("A", "<xml>The End. <add>After credits.</add></xml>");
    String expectedDot = "digraph VariantWitnessGraph{\n" + //
        "graph [rankdir=LR]\n" + //
        "labelloc=b\n" + //
        "begin [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "A_000 [label=<The&#9251;End.&#9251;<br/><i>A: /xml</i>>]\n" + //
        "A_003 [label=<After&#9251;credits.<br/><i>A: /xml/add</i>>]\n" + //
        "end [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "A_000->A_003\n" + //
        "A_000->end\n" + //
        "A_003->end\n" + //
        "begin->A_000\n" + //
        "}";

    verifyDotExport(wg0, expectedDot);
  }

  @Test
  public void testAppRdg() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wg0 = importer.importXML("A", "<xml>Sinterklaas en"//
        + "<app>"//
        + "<rdg wit=\"a\">Zwarte Piet</rdg>"//
        + "<rdg wit=\"b\">Roetpiet</rdg>"//
        + "</app> zijn weer aangekomen.</xml>");//
    String expectedDot = "digraph VariantWitnessGraph{\n" + //
        "graph [rankdir=LR]\n" + //
        "labelloc=b\n" + //
        "begin [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "A_000 [label=<Sinterklaas&#9251;en<br/><i>A: /xml</i>>]\n" + //
        "A_002 [label=<Zwarte&#9251;Piet<br/><i>A: /xml/app/rdg</i>>]\n" + //
        "A_004 [label=<Roetpiet<br/><i>A: /xml/app/rdg</i>>]\n" + ///
        "A_005 [label=<&#9251;zijn&#9251;weer&#9251;aangekomen.<br/><i>A: /xml</i>>]\n" + //
        "end [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "A_000->A_002\n" + //
        "A_000->A_004\n" + //
        "A_002->A_005\n" + //
        "A_004->A_005\n" + //
        "A_005->end\n" + //
        "begin->A_000\n" + //
        "}";

    verifyDotExport(wg0, expectedDot);
  }

  @Test
  public void testWitnessFOrderAppRdgBordalejo() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wg0 = importer.importXML("F", "<text>\n" + //
        "    <s>De vent was woedend en maakte \n" + //
        "        <app>\n" + //
        "            <rdg type=\"l1\">Shiriar</rdg>\n" + //
        "            <rdg type=\"lit\"><hi rend=\"strike\">Shiriar</hi></rdg>\n" + //
        "        </app> den bedremmelden Sultan uit\n" + //
        "        voor \"lompen boer\".</s>\n" + //
        "</text>");//
    String expectedDot = "digraph VariantWitnessGraph{\n" + //
        "graph [rankdir=LR]\n" + //
        "labelloc=b\n" + //
        "begin [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "F_000 [label=<De&#9251;vent&#9251;was&#9251;woedend&#9251;en&#9251;maakte&#9251;<br/><i>F: /text/s</i>>]\n" + //
        "F_006 [label=<Shiriar<br/><i>F: /text/s/app/rdg</i>>]\n" + //
        "F_007 [label=<&#9251;den&#9251;bedremmelden&#9251;Sultan&#9251;uit&#x21A9;<br/>&#9251;voor&#9251;\"lompen&#9251;boer\".<br/><i>F: /text/s</i>>]\n" + //
        "end [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "F_000->F_006\n" + //
        "F_006->F_007\n" + //
        "F_007->end\n" + //
        "begin->F_000\n" + //
        "}";

    verifyDotExport(wg0, expectedDot, "witness-f-order-app-rdg-bordalejo");
  }

  @Test
  public void testWitnessFOrderAppRdgVincent() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wg0 = importer.importXML("F", "<text>\n" + //
        "    <s>De vent was woedend en maakte \n" + //
        "        <app>\n" + //
        "            <rdg><del type=\"instantCorrection\">Shiriar</del></rdg>\n" + //
        "            <rdg type=\"lit\"><hi rend=\"strike\">Shiriar</hi></rdg>\n" + //
        "        </app> den bedremmelden Sultan uit\n" + //
        "        voor \"lompen boer\".</s>\n" + //
        "</text>");
    String expectedDot = "digraph VariantWitnessGraph{\n" + //
        "graph [rankdir=LR]\n" + //
        "labelloc=b\n" + //
        "begin [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "F_000 [label=<De&#9251;vent&#9251;was&#9251;woedend&#9251;en&#9251;maakte&#9251;<br/><i>F: /text/s</i>>]\n" + //
        "F_006 [label=<Shiriar<br/><i>F: /text/s/app/rdg/del</i>>]\n" + //
        "F_007 [label=<&#9251;den&#9251;bedremmelden&#9251;Sultan&#9251;uit&#x21A9;<br/>&#9251;voor&#9251;\"lompen&#9251;boer\".<br/><i>F: /text/s</i>>]\n" + //
        "end [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "F_000->F_006\n" + //
        // "F_000->F_007\n" + //
        "F_006->F_007\n" + //
        "F_007->end\n" + //
        "begin->F_000\n" + //
        "}";

    verifyDotExport(wg0, expectedDot, "witness-f-order-app-rdg-vincent");
  }

  @Test
  public void testWitnessQOrderAppRdgBordalejo() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wg0 = importer.importXML("Q", "<text>\n" + //
        "    <s>De vent was woedend en maakte \n" + //
        "        <app>\n" + //
        "            <rdg type=\"l1\">Shiriar</rdg>\n" + //
        "            <rdg type=\"l2\">den bedremmelden man</rdg>\n" + //
        "            <rdg type=\"l3\">den bedremmelden Sultan</rdg>\n" + //
        "            <rdg type=\"lit\">\n" + //
        "                <hi rend=\"strike\">Shiriar</hi>\n" + //
        "                <hi rend=\"margin\">den bedremmelden</hi>\n" + //
        "                <hi rend=\"strike\">man</hi><hi rend=\"supralinear\">Sultan</hi>\n" + //
        "            </rdg>\n" + //
        "        </app>uit voor <q>\"lompen boer\"</q>.</s>\n" + //
        "</text>");//
    String expectedDot = "digraph VariantWitnessGraph{\n" + //
        "graph [rankdir=LR]\n" + //
        "labelloc=b\n" + //
        "begin [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "Q_000 [label=<De&#9251;vent&#9251;was&#9251;woedend&#9251;en&#9251;maakte&#9251;<br/><i>Q: /text/s</i>>]\n" + //
        "Q_006 [label=<Shiriar<br/><i>Q: /text/s/app/rdg</i>>]\n" + //
        "Q_007 [label=<den&#9251;bedremmelden&#9251;man<br/><i>Q: /text/s/app/rdg</i>>]\n" + //
        "Q_010 [label=<den&#9251;bedremmelden&#9251;Sultan<br/><i>Q: /text/s/app/rdg</i>>]\n" + //
        "Q_013 [label=<uit&#9251;voor&#9251;<br/><i>Q: /text/s</i>>]\n" + //
        "Q_015 [label=<\"lompen&#9251;boer\"<br/><i>Q: /text/s/q</i>>]\n" + //
        "Q_017 [label=<.<br/><i>Q: /text/s</i>>]\n" + //
        "end [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "Q_000->Q_006\n" + //
        "Q_000->Q_007\n" + //
        "Q_000->Q_010\n" + //
        "Q_006->Q_013\n" + //
        "Q_007->Q_013\n" + //
        "Q_010->Q_013\n" + //
        "Q_013->Q_015\n" + //
        "Q_015->Q_017\n" + //
        "Q_017->end\n" + //
        "begin->Q_000\n" + //
        "}";

    verifyDotExport(wg0, expectedDot, "witness-q-order-app-rdg-bordalejo");
  }

  @Test
  public void testWitnessQOrderAppRdgVincent() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wg0 = importer.importXML("Q", "<text>    \n" + //
        "    <s>De vent was woedend en maakte <app>\n" + //
        "        <rdg><del>Shiriar</del></rdg>\n" + //
        "        <rdg>\n" + //
        "            <add>den bedremmelden \n" + //
        "                <app><rdg><del>man</del></rdg>\n" + //
        "                <rdg><add>Sultan</add></rdg></app>\n" + //
        "            </add>\n" + //
        "        </rdg>\n" + //
        "        <rdg type=\"lit\"><hi rend=\"strike\">Shiriar</hi>\n" + //
        "        <hi rend=\"margin\">den bedremmelden</hi>\n" + //
        "        <hi rend=\"strike\">man</hi><hi rend=\"supralinear\">Sultan</hi>\n" + //
        "        </rdg>\n" + //
        "    </app> \n" + //
        "        uit voor <q>\"lompen boer\"</q>.</s>\n" + //
        "</text>");
    String expectedDot = "digraph VariantWitnessGraph{\n" + //
        "graph [rankdir=LR]\n" + //
        "labelloc=b\n" + //
        "begin [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "Q_000 [label=<&#9251;<br/><i>Q: /text</i>>]\n" + //
        "Q_001 [label=<De&#9251;vent&#9251;was&#9251;woedend&#9251;en&#9251;maakte&#9251;<br/><i>Q: /text/s</i>>]\n" + //
        "Q_007 [label=<Shiriar<br/><i>Q: /text/s/app/rdg/del</i>>]\n" + //
        "Q_008 [label=<den&#9251;bedremmelden&#9251;&#x21A9;<br/>&#9251;<br/><i>Q: /text/s/app/rdg/add</i>>]\n" + //
        "Q_012 [label=<&#9251;uit&#9251;voor&#9251;<br/><i>Q: /text/s</i>>]\n" + //
        "Q_010 [label=<man<br/><i>Q: /text/s/app/rdg/add/app/rdg/del</i>>]\n" + //
        "Q_011 [label=<Sultan<br/><i>Q: /text/s/app/rdg/add/app/rdg/add</i>>]\n" + //
        "Q_015 [label=<\"lompen&#9251;boer\"<br/><i>Q: /text/s/q</i>>]\n" + //
        "Q_017 [label=<.<br/><i>Q: /text/s</i>>]\n" + //
        "end [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "Q_000->Q_001\n" + //
        "Q_001->Q_007\n" + //
        "Q_001->Q_008\n" + //
        "Q_007->Q_012\n" + //
        "Q_008->Q_010\n" + //
        "Q_008->Q_011\n" + //
        "Q_010->Q_012\n" + //
        "Q_011->Q_012\n" + //
        "Q_012->Q_015\n" + //
        "Q_015->Q_017\n" + //
        "Q_017->end\n" + //
        "begin->Q_000\n" + //
        "}";

    verifyDotExport(wg0, expectedDot, "witness-q-order-app-rdg-vincent");
  }

  @Test
  public void testWitnessFHierarchyAppRdgBordalejo() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wg0 = importer.importXML("F", "<text>\n" + //
        "    <s>Hoe zoet moet nochtans zijn dit <lb/>\n" + //
        "        <app>\n" + //
        "            <rdg type=\"l1\">werven om</rdg>\n" + //
        "            <rdg type=\"l2\">trachten naar</rdg>\n" + //
        "            <rdg type=\"lit\"><hi rend=\"strike\">werven om</hi> <hi rend=\"supralinear\">trachten naar</hi></rdg>\n" + //
        "        </app> \n" + //
        "        een vrouw, de ongewisheid vóór de <lb/>liefelijke toestemming!</s>\n" + //
        "</text>");
    String expectedDot = "digraph VariantWitnessGraph{\n" + //
        "graph [rankdir=LR]\n" + //
        "labelloc=b\n" + //
        "begin [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "F_000 [label=<Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/><i>F: /text/s</i>>]\n" + //
        "F_006 [label=<<br/><i>F: /text/s/lb</i>>]\n" + //
        "F_007 [label=<werven&#9251;om<br/><i>F: /text/s/app/rdg</i>>]\n" + //
        "F_009 [label=<trachten&#9251;naar<br/><i>F: /text/s/app/rdg</i>>]\n" + //
        "F_011 [label=<&#9251;een&#9251;vrouw,&#9251;de&#9251;ongewisheid&#9251;vóór&#9251;de&#9251;<br/><i>F: /text/s</i>>]\n" + //
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

    verifyDotExport(wg0, expectedDot, "witness-f-hierarchy-app-rdg-bordalejo");
  }

  @Test
  public void testWitnessFHierarchyAppRdgVincent() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wg0 = importer.importXML("F", "<text>\n" + //
        "    <s>Hoe zoet moet nochtans zijn dit <lb/>\n" + //
        "        <app>\n" + //
        "            <rdg><del>werven om</del></rdg>\n" + //
        "            <rdg><add>trachten naar</add></rdg>\n" + //
        "            <rdg type=\"lit\"><hi rend=\"strike\">werven om</hi> <hi rend=\"supralinear\">trachten naar</hi></rdg>\n" + //
        "        </app> \n" + //
        "        een vrouw, de ongewisheid vóór de <lb/>liefelijke toestemming!</s>\n" + //
        "</text>");
    String expectedDot = "digraph VariantWitnessGraph{\n" + //
        "graph [rankdir=LR]\n" + //
        "labelloc=b\n" + //
        "begin [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "F_000 [label=<Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/><i>F: /text/s</i>>]\n" + //
        "F_006 [label=<<br/><i>F: /text/s/lb</i>>]\n" + //
        "F_007 [label=<werven&#9251;om<br/><i>F: /text/s/app/rdg/del</i>>]\n" + //
        "F_009 [label=<trachten&#9251;naar<br/><i>F: /text/s/app/rdg/add</i>>]\n" + //
        "F_011 [label=<&#9251;een&#9251;vrouw,&#9251;de&#9251;ongewisheid&#9251;vóór&#9251;de&#9251;<br/><i>F: /text/s</i>>]\n" + //
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

    verifyDotExport(wg0, expectedDot, "witness-f-hierarchy-app-rdg-vincent");
  }

  @Test
  public void testWitnessQHierarchyAppRdgBordalejo() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wg0 = importer.importXML("F", "<text>\n" + //
        "    <s>Hoe zoet moet nochtans zijn dit \n" + //
        "        <app>\n" + //
        "            <rdg type=\"l1\">werven om</rdg>\n" + //
        "            <rdg type=\"l2\">trachten naar</rdg>\n" + //
        "            <rdg type=\"lit\"><hi rend=\"strike\">werven om</hi> <hi rend=\"supralinear\">trachten naar</hi></rdg>\n" + //
        "        </app> \n" + //
        "        een <lb/>vrouw !</s>\n" + //
        "    <s>Die dagen van nerveuze verwachting vóór de liefelijke toestemming.</s>\n" + //
        "</text>");
    String expectedDot = "digraph VariantWitnessGraph{\n" + //
        "graph [rankdir=LR]\n" + //
        "labelloc=b\n" + //
        "begin [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "F_000 [label=<Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/><i>F: /text/s</i>>]\n" + //
        "F_006 [label=<werven&#9251;om<br/><i>F: /text/s/app/rdg</i>>]\n" + //
        "F_008 [label=<trachten&#9251;naar<br/><i>F: /text/s/app/rdg</i>>]\n" + //
        "F_010 [label=<&#9251;een&#9251;<br/><i>F: /text/s</i>>]\n" + //
        "F_012 [label=<<br/><i>F: /text/s/lb</i>>]\n" + //
        "F_013 [label=<vrouw&#9251;!<br/><i>F: /text/s</i>>]\n" + //
        "F_015 [label=<Die&#9251;dagen&#9251;van&#9251;nerveuze&#9251;verwachting&#9251;vóór&#9251;de&#9251;liefelijke&#9251;toestemming.<br/><i>F: /text/s</i>>]\n" + //
        "end [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "F_000->F_006\n" + //
        "F_000->F_008\n" + //
        "F_006->F_010\n" + //
        "F_008->F_010\n" + //
        "F_010->F_012\n" + //
        "F_012->F_013\n" + //
        "F_013->F_015\n" + //
        "F_015->end\n" + //
        "begin->F_000\n" + //
        "}";

    verifyDotExport(wg0, expectedDot, "witness-q-hierarchy-app-rdg-bordalejo");
  }

  @Test
  public void testWitnessQHierarchyAppRdgVincent() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wg0 = importer.importXML("F", "<text>\n" + //
        "    <s>Hoe zoet moet nochtans zijn dit \n" + //
        "        <app>\n" + //
        "            <rdg><del>werven om</del></rdg>\n" + //
        "            <rdg><add>trachten naar</add></rdg>\n" + //
        "            <rdg type=\"lit\"><hi rend=\"strike\">werven om</hi> <hi rend=\"supralinear\">trachten naar</hi></rdg>\n" + //
        "        </app> \n" + //
        "        een <lb/>vrouw !</s>\n" + //
        "        <s>Die dagen van nerveuze verwachting vóór de liefelijke toestemming.</s>\n" + //
        "</text>");
    String expectedDot = "digraph VariantWitnessGraph{\n" + //
        "graph [rankdir=LR]\n" + //
        "labelloc=b\n" + //
        "begin [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "F_000 [label=<Hoe&#9251;zoet&#9251;moet&#9251;nochtans&#9251;zijn&#9251;dit&#9251;<br/><i>F: /text/s</i>>]\n" + //
        "F_006 [label=<werven&#9251;om<br/><i>F: /text/s/app/rdg/del</i>>]\n" + //
        "F_008 [label=<trachten&#9251;naar<br/><i>F: /text/s/app/rdg/add</i>>]\n" + //
        "F_010 [label=<&#9251;een&#9251;<br/><i>F: /text/s</i>>]\n" + //
        "F_012 [label=<<br/><i>F: /text/s/lb</i>>]\n" + //
        "F_013 [label=<vrouw&#9251;!<br/><i>F: /text/s</i>>]\n" + //
        "F_015 [label=<Die&#9251;dagen&#9251;van&#9251;nerveuze&#9251;verwachting&#9251;vóór&#9251;de&#9251;liefelijke&#9251;toestemming.<br/><i>F: /text/s</i>>]\n" + //
        "end [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "F_000->F_006\n" + //
        "F_000->F_008\n" + //
        "F_006->F_010\n" + //
        "F_008->F_010\n" + //
        "F_010->F_012\n" + //
        "F_012->F_013\n" + //
        "F_013->F_015\n" + //
        "F_015->end\n" + //
        "begin->F_000\n" + //
        "}";

    verifyDotExport(wg0, expectedDot, "witness-q-hierarchy-app-rdg-vincent");
  }

}
