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
import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;

import org.junit.Test;

import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph;
import nl.knaw.huygens.hypercollate.tools.DotFactory;
import nl.knaw.huygens.hypercollate.tools.TokenMerger;

public class XMLImporterTest {

  @Test
  public void testImportFromString() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wg0 = importer.importXML("A", "<xml>Mondays are <del>well good</del><add>def bad</add>!</xml>");
    VariantWitnessGraph wg = TokenMerger.merge(wg0);

    String dot = DotFactory.fromVariantWitnessGraph(wg);
    System.out.println(dot);
    String expected = "digraph VariantWitnessGraph{\n" + //
        "graph [rankdir=LR]\n" + //
        "labelloc=b\n" + //
        "st [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t0 [label=<Mondays&#9251;are&#9251;<br/><i>A: /xml</i>>]\n" + //
        "t2 [label=<well&#9251;good<br/><i>A: /xml/del</i>>]\n" + //
        "t4 [label=<def&#9251;bad<br/><i>A: /xml/add</i>>]\n" + //
        "t6 [label=<!<br/><i>A: /xml</i>>]\n" + //
        "et [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "st->t0\n" + //
        "t0->t2\n" + //
        "t0->t4\n" + //
        "t2->t6\n" + //
        "t4->t6\n" + //
        "t6->et\n" + //
        "}";
    assertThat(dot).isEqualTo(expected);
  }

  @Test
  public void testImportFromFile() {
    XMLImporter importer = new XMLImporter();
    InputStream resourceAsStream = getClass().getResourceAsStream("/witness.xml");
    VariantWitnessGraph wg0 = importer.importXML("A", resourceAsStream);
    VariantWitnessGraph wg = TokenMerger.merge(wg0);

    String dot = DotFactory.fromVariantWitnessGraph(wg);
    System.out.println(dot);
    String expected = "digraph VariantWitnessGraph{\n" + //
        "graph [rankdir=LR]\n" + //
        "labelloc=b\n" + //
        "st [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t0 [label=<De&#9251;vent&#9251;was&#9251;woedend&#9251;en&#9251;maakte&#9251;<br/><i>A: /text/s</i>>]\n" + //
        "t6 [label=<Shiriar<br/><i>A: /text/s/del</i>>]\n" + //
        "t7 [label=<den&#9251;bedremmelden&#9251;<br/><i>A: /text/s/add</i>>]\n" + //
        "t11 [label=<uit&#9251;voor&#9251;\"lompen&#9251;boer\".<br/><i>A: /text/s</i>>]\n" + //
        "t9 [label=<man<br/><i>A: /text/s/add/del</i>>]\n" + //
        "t10 [label=<Sultan<br/><i>A: /text/s/add/add</i>>]\n" + //
        "et [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "st->t0\n" + //
        "t0->t6\n" + //
        "t0->t7\n" + //
        "t10->t11\n" + //
        "t11->et\n" + //
        "t6->t11\n" + //
        "t7->t10\n" + //
        "t7->t9\n" + //
        "t9->t11\n" + //
        "}";
    assertThat(dot).isEqualTo(expected);
  }

  @Test
  public void testDelWithoutAdd() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wg0 = importer.importXML("A", "<xml>Ja toch! <del>Niet dan?</del> Ik dacht het wel!</xml>");

    VariantWitnessGraph wg = TokenMerger.merge(wg0);

    String dot = DotFactory.fromVariantWitnessGraph(wg);
    System.out.println(dot);
    String expected = "digraph VariantWitnessGraph{\n" + //
        "graph [rankdir=LR]\n" + //
        "labelloc=b\n" + //
        "st [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t0 [label=<Ja&#9251;toch!&#9251;<br/><i>A: /xml</i>>]\n" + //
        "t3 [label=<Niet&#9251;dan?<br/><i>A: /xml/del</i>>]\n" + //
        "t6 [label=<Ik&#9251;dacht&#9251;het&#9251;wel!<br/><i>A: /xml</i>>]\n" + //
        "et [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "st->t0\n" + //
        "t0->t3\n" + //
        "t3->t6\n" + //
        "t6->et\n" + //
        "}";
    assertThat(dot).isEqualTo(expected);
  }

}
