package nl.knaw.huygens.hypercollate.api;

/*-
 * #%L
 * hyper-collate-api
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

public class WitnessInput {

  private String sigil;
  private String xml;

  public void setSigil(String sigil) {
    this.sigil = sigil;
  }

  public String getSigil() {
    return this.sigil;
  }

  public void setXml(String xml) {
    this.xml = xml;
  }

  public String getXml() {
    return this.xml;
  }

}
