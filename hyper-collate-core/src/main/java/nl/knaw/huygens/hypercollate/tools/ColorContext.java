package nl.knaw.huygens.hypercollate.tools;

/*-
 * #%L
 * HyperCollate core
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.collect.Iterables.cycle;

public class ColorContext {
  private Map<String, String> assignedColors = new HashMap<>();
  private String[] colors = {"yellow", "orange", "#9aed7d", "lightblue", "grey", "#4286f4", "#ef10eb",
      "#9091D4", "#B190D4", "#B3D490", "#D49091", "#E62023", "#86E620", "#8020E6", "#20E6E3"};
  private Iterator<String> colorCycle = cycle(colors).iterator();

  public String colorFor(String tagName) {
    if (assignedColors.containsKey(tagName)) {
      return assignedColors.get(tagName);
    }
    String color = colorCycle.next();
    assignedColors.put(tagName, color);
    return color;
  }
}
