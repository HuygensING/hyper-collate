package nl.knaw.huygens.hypercollate;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph;
import nl.knaw.huygens.hypercollate.tools.DotFactory;
import nl.knaw.huygens.hypercollate.tools.TokenMerger;

public class HyperCollateTest {

  public void verifyDotExport(VariantWitnessGraph variantWitnessGraph, String expectedDot) {
    VariantWitnessGraph wg = TokenMerger.merge(variantWitnessGraph);
    // VariantWitnessGraph wg = variantWitnessGraph;

    String dot = DotFactory.fromVariantWitnessGraph(wg);
    System.out.println(dot);
    writeGraph(dot);
    assertThat(dot).isEqualTo(expectedDot);
    // showGraph(dot);
  }

  public void writeGraph(String dot) {
    try {
      FileUtils.write(new File("out/graph.dot"), dot, Charsets.UTF_8);
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
