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

import java.io.File;

import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph;
import nl.knaw.huygens.hypercollate.tools.DotFactory;

public class XMLImporterTest {

  // @Test
  public void testImportFromString() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wg = importer.importXML("A", "<xml>Mondays are <del>well good</del><add>def bad</add>!</xml>");

    String dot = DotFactory.fromVariantWitnessGraph(wg);
    System.out.println(dot);
    String expected = "digraph VariantWitnessGraph{\n"//
        + "graph [rankdir=LR]\n" + //
        "labelloc=b\n" + //
        "st [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "t0 [label=<Mondays&#9251;<br/>{<i>xml</i>}>]\n" + //
        "t1 [label=<are&#9251;<br/>{<i>xml</i>}>]\n" + //
        "t2 [label=<well&#9251;<br/>{<i>xml, del</i>}>]\n" + //
        "t3 [label=<good<br/>{<i>xml, del</i>}>]\n" + //
        "t4 [label=<def&#9251;<br/>{<i>xml, add</i>}>]\n" + //
        "t5 [label=<bad<br/>{<i>xml, add</i>}>]\n" + //
        "t6 [label=<!<br/>{<i>xml</i>}>]\n" + //
        "et [label=\"\";shape=doublecircle,rank=middle]\n" + //
        "st->t0\n" + //
        "t0->t1\n" + //
        "t1->t2\n" + //
        "t2->t3\n" + //
        "t3->t6\n" + //
        "t1->t4\n" + //
        "t4->t5\n" + //
        "t5->t6\n" + //
        "t6->et\n" + //
        "}";
    assertThat(dot).isEqualTo(expected);
  }

  // @Test
  public void testImportFromFile() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wg = importer.importXML("A", new File("test/resources/witness.xml"));

    String dot = DotFactory.fromVariantWitnessGraph(wg);
    System.out.println(dot);
    String expected = "";
    assertThat(dot).isEqualTo(expected);
  }

}
