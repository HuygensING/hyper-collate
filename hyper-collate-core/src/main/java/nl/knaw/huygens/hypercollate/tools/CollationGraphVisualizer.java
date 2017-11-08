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

  public static String toTableASCII(CollationGraph graph) {
    List<String> sigils = graph.getSigils();
    Map<String, List<String>> rowMap = new HashMap<>();
    sigils.forEach(sigil -> rowMap.put(sigil, new ArrayList<>()));

    graph.traverse().forEach(node -> {
      sigils.forEach(sigil -> {
        Token token = node.getTokenForWitness(sigil);
        if (token == null) {
          rowMap.get(sigil).add(" ");

        } else {
          MarkedUpToken t = (MarkedUpToken) token;
          String content = t.getContent().replaceAll("\n", "\\\\n")//
              .replaceAll(" +", "_");
          if (content.isEmpty()) {
            content = "<" + t.getParentXPath().replaceAll(".*/", "") + "/>";
          }
          rowMap.get(sigil).add(content);
        }
      });
    });

    AsciiTable table = new AsciiTable();
    CWC_LongestLine cwc = new CWC_LongestLine();
    table.getRenderer().setCWC(cwc);
    table.addRule();
    sigils.forEach(sigil -> {
      List<String> row = rowMap.get(sigil);
      row.add(0, "[" + sigil + "]");
      table.addRow(row);
      table.addRule();
    });

    return table.render();
  }

  public static String toTableHTML(CollationGraph graph) {
    StringBuilder tableBuilder = new StringBuilder();
    return tableBuilder.toString();
  }

  public static String toDot(CollationGraph graph) {
    return DotFactory.fromCollationGraph(graph);
  }

}
