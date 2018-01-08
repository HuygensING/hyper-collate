package nl.knaw.huygens.hypercollate.dropwizard.db;

/*-
 * #%L
 * hyper-collate-server
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

import com.fasterxml.jackson.annotation.JsonProperty;
import nl.knaw.huygens.hypercollate.api.CollationInput;
import nl.knaw.huygens.hypercollate.api.ResourcePaths;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public class CollationInfo {
  private Instant created;
  private Instant modified;
  private final String uriBase;
  private final CollationInput input;
  private long collationDuration;

  public CollationInfo(UUID documentId, String baseURL, CollationInput input) {
    this.input = input;
    this.uriBase = baseURL + "/" + ResourcePaths.COLLATIONS + "/" + documentId + "/";
  }

  public CollationInfo setCreated(Instant created) {
    this.created = created;
    return this;
  }

  public String getCreated() {
    return created.toString();
  }

  public CollationInfo setModified(Instant modified) {
    this.modified = modified;
    return this;
  }

  public String getModified() {
    return modified.toString();
  }

  public CollationInput getInput() {
    return this.input;
  }

  @JsonProperty("^dot")
  public URI getDotURI() {
    return URI.create(uriBase + ResourcePaths.COLLATIONS_DOT);
  }

  @JsonProperty("^ascii_table")
  public URI getAsciiTableURI() {
    return URI.create(uriBase + ResourcePaths.COLLATIONS_ASCII_TABLE);
  }

  public void setCollationDurationInMilliseconds(long collationDuration) {
    this.collationDuration = collationDuration;
  }

  public long getCollationDurationInMilliseconds() {
    return this.collationDuration;
  }

}
