package nl.knaw.huygens.hypercollate;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph;
import nl.knaw.huygens.hypercollate.tools.DotFactory;
import nl.knaw.huygens.hypercollate.tools.TokenMerger;
import org.apache.commons.io.FileUtils;
import static org.assertj.core.api.Assertions.assertThat;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

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

public class HyperCollateTest {

  protected void verifyDotExport(VariantWitnessGraph variantWitnessGraph, String expectedDot) {
    verifyDotExport(variantWitnessGraph, expectedDot, "graph");
  }

  protected void verifyDotExport(VariantWitnessGraph variantWitnessGraph, String expectedDot, String name) {
    VariantWitnessGraph wg = TokenMerger.merge(variantWitnessGraph);
    // VariantWitnessGraph wg = variantWitnessGraph;

    String dot = DotFactory.fromVariantWitnessGraph(wg);
    // System.out.println(dot);
    writeGraph(dot, name);
    assertThat(dot).isEqualTo(expectedDot);
    // showGraph(dot);
  }

  protected void writeGraph(String dot, String name) {
    try {
      FileUtils.write(new File("out/" + name + ".dot"), dot, StandardCharsets.UTF_8);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void showGraph(String dot) {
    try {
      MutableGraph g = Parser.read(dot.replaceAll("=<", "=\"").replaceAll(">]", "\"]"));
      BufferedImage bufferedImage = Graphviz.fromGraph(g).width(4000).render(Format.PNG).toImage();
      JFrame frame = new JFrame();
      frame.getContentPane().setLayout(new FlowLayout());
      frame.getContentPane().add(new JLabel(new ImageIcon(bufferedImage)));
      frame.pack();
      frame.setVisible(true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
