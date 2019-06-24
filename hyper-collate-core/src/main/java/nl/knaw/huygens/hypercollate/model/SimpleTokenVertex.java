package nl.knaw.huygens.hypercollate.model;

/*-
 * #%L
 * hyper-collate-core
 * =======
 * Copyright (C) 2017 - 2019 Huygens ING (KNAW)
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

import eu.interedition.collatex.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class SimpleTokenVertex implements TokenVertex, Comparable<SimpleTokenVertex> {

  private final MarkedUpToken token;
  private final List<TokenVertex> incomingVertices = new ArrayList<>();
  private final List<TokenVertex> outgoingVertices = new ArrayList<>();
  private List<Integer> branchPath;

  public SimpleTokenVertex(MarkedUpToken token) {
    this.token = token;
  }

  @Override
  public Token getToken() {
    return token;
  }

  @Override
  public void addIncomingTokenVertex(TokenVertex incoming) {
    incomingVertices.add(incoming);
  }

  @Override
  public Stream<TokenVertex> getIncomingTokenVertexStream() {
    return incomingVertices.stream();
  }

  @Override
  public void addOutgoingTokenVertex(TokenVertex outgoing) {
    outgoingVertices.add(outgoing);
  }

  @Override
  public Stream<TokenVertex> getOutgoingTokenVertexStream() {
    return outgoingVertices.stream();
  }

  @Override
  public String getSigil() {
    return token.getWitness().getSigil();
  }

  public SimpleTokenVertex setBranchPath(List<Integer> branchPath) {
    this.branchPath = branchPath;
    return this;
  }

  @Override
  public List<Integer> getBranchPath() {
    return branchPath;
  }

  public String getContent() {
    return token.getContent();
  }

  public String getNormalizedContent() {
    return token.getNormalizedContent();
  }

  public String getParentXPath() {
    return token.getParentXPath();
  }

  @Override
  public int compareTo(SimpleTokenVertex other) {
    return token.getIndexNumber().compareTo(other.token.getIndexNumber());
  }

  public Long getIndexNumber() {
    return token.getIndexNumber();
  }

}
