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

import org.junit.Test;

import nl.knaw.huygens.hypercollate.importer.XMLImporter;
import nl.knaw.huygens.hypercollate.model.CollationGraph;
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph;
import nl.knaw.huygens.hypercollate.tools.DotFactory;

public class HyperCollaterTest {

  @Test
  public void test() {
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph wA = importer.importXML("A", "<xml>Ja toch! <del>Niet dan?</del> Ik dacht het wel!</xml>");
    VariantWitnessGraph wB = importer.importXML("B", "<xml>Ja toch! <del>Niet dan?</del> Ik dacht het wel!</xml>");
    CollationGraph collation = HyperCollater.collate(wA, wB);
    System.out.println(collation);

    String dot = DotFactory.fromCollationGraph(collation);
    System.out.println(dot);
    String expected = "something";
    // assertThat(dot).isEqualTo(expected);

  }

}
