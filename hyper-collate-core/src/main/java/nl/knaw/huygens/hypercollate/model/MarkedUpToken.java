package nl.knaw.huygens.hypercollate.model;

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
public class MarkedUpToken implements Token {
  private String content;
  private String normalizedContent;
  private SimpleWitness witness;
  private Long index = 0L;
  private String parentXPath = "";

  public MarkedUpToken setContent(String content) {
    this.content = content;
    return this;
  }

  public String getContent() {
    return this.content;
  }

  public MarkedUpToken setNormalizedContent(String normalizedContent) {
    this.normalizedContent = normalizedContent;
    return this;
  }

  public String getNormalizedContent() {
    return this.normalizedContent;
  }

  @Override
  public Witness getWitness() {
    return witness;
  }

  public MarkedUpToken setIndexNumber(Long index) {
    this.index = index;
    return this;
  }

  public Long getIndexNumber() {
    return this.index;
  }

  public void setParentXPath(String parentXPath) {
    this.parentXPath = parentXPath;
  }

  public String getParentXPath() {
    return this.parentXPath;
  }

}
