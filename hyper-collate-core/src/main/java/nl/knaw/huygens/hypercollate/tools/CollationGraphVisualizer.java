package nl.knaw.huygens.hypercollate.tools;

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

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_LongestLine;
import eu.interedition.collatex.Token;
import nl.knaw.huygens.hypercollate.model.CollationGraph;
import nl.knaw.huygens.hypercollate.model.MarkedUpToken;

public class CollationGraphVisualizer {

  public static class Cell {
    List<String> layerNames = new ArrayList<>();
    Map<String, String> layerContent = new HashMap<>();

    public Cell(String layerName, String content) {
      layerNames.add(layerName);
      layerContent.put(layerName, content);
    }

    public Cell addLayer(String name) {
      layerNames.add(name);
      return this;
    }

    public Map<String, String> getLayerContent() {
      return this.layerContent;
    }
  }

  public static String toTableASCII(CollationGraph graph) {
    List<String> sigils = graph.getSigils();
    Map<String, List<Cell>> rowMap = new HashMap<>();
    sigils.forEach(sigil -> rowMap.put(sigil, new ArrayList<>()));

    graph.traverse().forEach(node -> {
      sigils.forEach(sigil -> {
        Token token = node.getTokenForWitness(sigil);
        if (token == null) {
          rowMap.get(sigil).add(new Cell("", " "));

        } else {
          MarkedUpToken t = (MarkedUpToken) token;
          String content = t.getContent()//
              .replaceAll("\n", "\\\\n")//
              .replaceAll(" +", "_");
          String parentXPath = t.getParentXPath();
          if (content.isEmpty()) {
            content = "<" + parentXPath.replaceAll(".*/", "") + "/>";
          }
          String layerName = determineLayerName(parentXPath);
          rowMap.get(sigil).add(new Cell(layerName, content));
        }
      });
    });

    AsciiTable table = new AsciiTable();
    CWC_LongestLine cwc = new CWC_LongestLine();
    table.getRenderer().setCWC(cwc);
    table.addRule();
    sigils.forEach(sigil -> {
      List<String> row = rowMap.get(sigil)//
          .stream()//
          .map(CollationGraphVisualizer::toASCII)//
          .collect(toList());//
      row.add(0, "[" + sigil + "]");
      table.addRow(row);
      table.addRule();
    });

    return table.render();
  }

  private static String toASCII(Cell cell) {
    return cell.layerNames//
        .stream()//
        .map(lName -> {
          String content = cell.getLayerContent().get(lName);
          if (lName.equals("add")) {
            content = "[+] " + content;
          } else if (lName.equals("del")) {
            content = "[-] " + content;
          }
          return content;
        })//
        .collect(joining("<br>"));
  }

  private static String determineLayerName(String parentXPath) {
    String layerName = "";
    if (parentXPath.endsWith("/add")) {
      layerName = "add";
    }
    if (parentXPath.endsWith("/del")) {
      layerName = "del";
    }
    return layerName;
  }

  public static String toTableHTML(CollationGraph graph) {
    StringBuilder tableBuilder = new StringBuilder();
    return tableBuilder.toString();
  }

  public static String toDot(CollationGraph graph) {
    return DotFactory.fromCollationGraph(graph);
  }

}
