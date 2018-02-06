package nl.knaw.huygens.hypercollate.tools;

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

import com.google.common.base.Preconditions;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_LongestLine;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;
import eu.interedition.collatex.Token;
import nl.knaw.huygens.hypercollate.model.CollationGraph;
import nl.knaw.huygens.hypercollate.model.MarkedUpToken;
import nl.knaw.huygens.hypercollate.model.TextNode;

import java.util.*;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class CollationGraphVisualizer {

  private static final String NBSP = "\u00A0";

  public static class Cell {
    final List<String> layerNames = new ArrayList<>();
    final Map<String, String> layerContent = new HashMap<>();

    public Cell(String layerName, String content) {
      layerNames.add(layerName);
      layerContent.put(layerName, content);
    }

    Cell() {
    }

    Cell addLayer(String name) {
      layerNames.add(name);
      return this;
    }

    Map<String, String> getLayerContent() {
      return this.layerContent;
    }
  }

  public static String toTableASCII(CollationGraph graph, boolean emphasizeWhitespace) {
    List<String> sigils = graph.getSigils();
    String whitespaceCharacter = emphasizeWhitespace ? "_" : " ";
    Map<String, List<Cell>> rowMap = new HashMap<>();
    sigils.forEach(sigil -> rowMap.put(sigil, new ArrayList<>()));

    CollationGraphRanking ranking = CollationGraphRanking.of(graph);

    Map<String, Integer> maxLayers = new HashMap<>();
    sigils.forEach(sigil -> maxLayers.put(sigil, 1));
    for (Set<TextNode> nodeSet : ranking) {
      if (isBorderNode(nodeSet, graph)) {
        // skip start and end nodes
        continue;
      }
      Map<String, List<MarkedUpToken>> nodeTokensPerWitness = new HashMap<>();
      sigils.forEach(sigil -> {
        nodeTokensPerWitness.put(sigil, new ArrayList<>());
        nodeSet.stream()//
            .filter(TextNode.class::isInstance)//
            .map(TextNode.class::cast)//
            .forEach(node -> {
              Token token = node.getTokenForWitness(sigil);
              if (token != null) {
                MarkedUpToken mToken = (MarkedUpToken) token;
                nodeTokensPerWitness.get(sigil).add(mToken);
              }
            });
      });
      sigils.forEach(sigil -> {
        List<MarkedUpToken> tokens = nodeTokensPerWitness.get(sigil);
        maxLayers.put(sigil, Math.max(maxLayers.get(sigil), tokens.size()));
        Cell cell = newCell(tokens, whitespaceCharacter);
        rowMap.get(sigil).add(cell);
      });
    }

    return asciiTable(graph.getSigils(), rowMap, maxLayers).render();
  }

  private static boolean isBorderNode(Set<TextNode> nodeSet, CollationGraph graph) {
    if (nodeSet.size() != 1) {
      return false;
    }
    TextNode node = nodeSet.iterator().next();
    Boolean hasNoIncomingEdges = graph.getIncomingEdges(node).isEmpty();
    Boolean hasNoOutgoingEdges = graph.getOutgoingEdges(node).isEmpty();
    return hasNoIncomingEdges || hasNoOutgoingEdges;
  }

  private static Cell newCell(List<MarkedUpToken> tokens, String whitespaceCharacter) {
    Cell cell = new Cell();
    if (tokens.isEmpty()) {
      setCellLayer(cell, "", " ");
    } else {
      tokens.forEach(token -> {
        String content = token.getContent()//
            .replaceAll("\n", " ")//
            .replaceAll(" +", whitespaceCharacter);
        String parentXPath = token.getParentXPath();
        if (content.isEmpty()) {
          content = "<" + parentXPath.replaceAll(".*/", "") + "/>";
        }
        String layerName = determineLayerName(parentXPath);
        setCellLayer(cell, layerName, content);
      });
    }
    return cell;
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

  private static void setCellLayer(Cell cell, String layerName, String content) {
    cell.addLayer(layerName);
    String previousContent = cell.getLayerContent().put(layerName, content);
    Preconditions.checkState(previousContent == null);
  }

  private static AsciiTable asciiTable(List<String> sigils, Map<String, List<Cell>> rowMap, Map<String, Integer> cellHeights) {
    AsciiTable table = new AsciiTable()//
        .setTextAlignment(TextAlignment.LEFT);
    CWC_LongestLine cwc = new CWC_LongestLine();
    table.getRenderer().setCWC(cwc);
    table.addRule();
    sigils.forEach(sigil -> {
      List<String> row = rowMap.get(sigil)//
          .stream()//
          .map(cell -> toASCII(cell, cellHeights.get(sigil)))//
          .collect(toList());//
      row.add(0, "[" + sigil + "]");
      table.addRow(row);
      table.addRule();
    });
    return table;
  }

  private static String toASCII(Cell cell, int cellHeight) {
    StringBuilder contentBuilder = new StringBuilder();
    // ASCIITable has no TextAlignment.BOTTOM option, so add empty lines manually
    int emptyLinesToAdd = cellHeight - cell.layerNames.size();
    for (int i = 0; i < emptyLinesToAdd; i++) {
      contentBuilder.append(NBSP + "<br>"); // regular space or just <br> leads to ASCIITable error when rendering
    }
    String content = cell.layerNames//
        .stream()//
        .sorted()//
        .map(lName -> cellLine(cell, lName))//
        .collect(joining("<br>"));
    return contentBuilder.append(content).toString();
  }

  private static String cellLine(Cell cell, String lName) {
    String content = cell.getLayerContent().get(lName);
    if (lName.equals("add")) {
      content = "[+] " + content;
    } else if (lName.equals("del")) {
      content = "[-] " + content;
    }
    return content;
  }

  public static String toTableHTML(CollationGraph graph) {
    return "";
  }

  public static String toDot(CollationGraph graph, boolean emphasizeWhitespace) {
    return new DotFactory(emphasizeWhitespace).fromCollationGraph(graph);
  }

}
