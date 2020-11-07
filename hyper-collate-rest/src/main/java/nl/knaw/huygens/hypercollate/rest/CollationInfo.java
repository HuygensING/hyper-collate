package nl.knaw.huygens.hypercollate.rest;

/*-
 * #%L
 * hyper-collate-rest
 * =======
 * Copyright (C) 2017 - 2020 Huygens ING (KNAW)
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import nl.knaw.huygens.hypercollate.api.ResourcePaths;
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@JsonInclude(Include.NON_NULL)

// to make these fields read-only
@JsonIgnoreProperties(
    value = {"^dot", "^png", "^svg", "^ascii_table", "^html_table"},
    allowGetters = true)
public class CollationInfo {

  public enum State {
    needs_witness,
    ready_to_collate,
    is_collated
  }

  private String id;
  private Instant created;
  private Instant modified;
  private String uriBase;
  private Long collationDuration;
  private final Map<String, String> witnesses = new HashMap<>();
  private final Map<String, VariantWitnessGraph> witnessGraphs = new HashMap<>();
  State collationState = State.needs_witness;
  private boolean join = true;

  CollationInfo() {}

  public CollationInfo(String collationId, String baseURL) {
    this.id = collationId;
    setUriBase(baseURL);
  }

  void setUriBase(String baseURL) {
    this.uriBase = baseURL + "/" + ResourcePaths.COLLATIONS + "/" + this.id;
  }

  public String getId() {
    return id;
  }

  public CollationInfo addWitness(String sigil, String xml) {
    witnesses.put(sigil, xml);
    collationState = State.ready_to_collate;
    collationDuration = null;
    setModified(Instant.now());
    return this;
  }

  public Map<String, String> getWitnesses() {
    return witnesses;
  }

  public void addWitnessGraph(String sigil, VariantWitnessGraph variantWitnessGraph) {
    witnessGraphs.put(sigil, variantWitnessGraph);
  }

  @JsonIgnore
  public Map<String, VariantWitnessGraph> getWitnessGraphMap() {
    return witnessGraphs;
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

  public State getCollationState() {
    return collationState;
  }

  @JsonProperty(value = "^png" /* , access = JsonProperty.Access.READ_ONLY */)
  public URI getPNGURI() {
    return URI.create(uriBase + "." + ResourcePaths.COLLATIONS_PNG);
  }

  @JsonProperty(value = "^svg" /* , access = JsonProperty.Access.READ_ONLY */)
  public URI getSVGURI() {
    return URI.create(uriBase + "." + ResourcePaths.COLLATIONS_SVG);
  }

  @JsonProperty(value = "^dot" /* , access = JsonProperty.Access.READ_ONLY */)
  public URI getDotURI() {
    return URI.create(uriBase + "." + ResourcePaths.COLLATIONS_DOT);
  }

  @JsonProperty(value = "^ascii_table" /* , access = JsonProperty.Access.READ_ONLY */)
  public URI getAsciiTableURI() {
    return URI.create(uriBase + "/" + ResourcePaths.COLLATIONS_ASCII_TABLE);
  }

  @JsonProperty(value = "^html_table" /* , access = JsonProperty.Access.READ_ONLY */)
  public URI getHtmlTableURI() {
    return URI.create(uriBase + "/" + ResourcePaths.COLLATIONS_HTML_TABLE);
  }

  @JsonProperty(value = "witness_visualisations")
  public Map<String, Map<String, URI>> getWitnessVisualisationURIs() {
    Map<String, Map<String, URI>> wvu = new HashMap<>();
    witnesses
        .keySet()
        .forEach(
            sigil -> {
              String witnessBaseURI =
                  String.format("%s/%s/%s.", uriBase, ResourcePaths.WITNESSES, sigil);
              Map<String, URI> uriMap = new HashMap<>();
              uriMap.put("^png", URI.create(witnessBaseURI + ResourcePaths.COLLATIONS_PNG));
              uriMap.put("^svg", URI.create(witnessBaseURI + ResourcePaths.COLLATIONS_SVG));
              uriMap.put("^dot", URI.create(witnessBaseURI + ResourcePaths.COLLATIONS_DOT));
              uriMap.put("^xml", URI.create(witnessBaseURI + "xml"));
              wvu.put(sigil, uriMap);
            });
    return wvu;
  }

  public void setCollationDurationInMilliseconds(long collationDuration) {
    this.collationDuration = collationDuration;
    this.collationState = State.is_collated;
  }

  public Long getCollationDurationInMilliseconds() {
    return this.collationDuration;
  }

  public Optional<String> getWitness(String sigil) {
    return Optional.ofNullable(witnesses.get(sigil));
  }

  public boolean getJoin() {
    return join;
  }

  public boolean isJoin() {
    return join;
  }

  public void setJoin(boolean join) {
    this.join = join;
  }
}
