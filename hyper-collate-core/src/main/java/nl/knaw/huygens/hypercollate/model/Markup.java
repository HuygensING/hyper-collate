package nl.knaw.huygens.hypercollate.model;

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

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class Markup {
  private final String tagName;
  private final Map<String, String> attributeMap = new TreeMap<>();
  private int depth;

  public Markup(String tagName) {
    this.tagName = tagName;
  }

  public Markup addAttribute(String key, String value) {
    attributeMap.put(key, value);
    return this;
  }

  public Optional<String> getAttributeValue(String key) {
    return Optional.ofNullable(attributeMap.get(key));
  }

  public String getTagName() {
    return this.tagName;
  }

  @Override
  public String toString() {
    return String.format("<%s %s>", tagName, attributeMap);
  }

  public Map<String, String> getAttributeMap() {
    return attributeMap;
  }

  public Markup setDepth(int depth) {
    this.depth = depth;
    return this;
  }

  public int getDepth() {
    return this. depth;
  }
}
