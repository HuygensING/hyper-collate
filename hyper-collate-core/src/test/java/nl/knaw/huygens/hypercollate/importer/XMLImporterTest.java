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

}
