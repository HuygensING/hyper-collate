package nl.knaw.huygens.hypercollate.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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

import eu.interedition.collatex.Token;

public class StartTokenVertex implements TokenVertex {
  private final List<TokenVertex> outgoingTokenVertices = new ArrayList<>();
  private final String sigil;

  StartTokenVertex(String sigil) {
    this.sigil = sigil;
  }

  @Override
  public Token getToken() {
    return null;
  }

  @Override
  public Stream<TokenVertex> getIncomingTokenVertexStream() {
    return Stream.empty();
  }

  @Override
  public Stream<TokenVertex> getOutgoingTokenVertexStream() {
    return outgoingTokenVertices.stream();
  }

  @Override
  public String getSigil() {
    return sigil;
  }

  @Override
  public String getSubSigil() {
    return sigil;
  }

  @Override
  public void addIncomingTokenVertex(TokenVertex incoming) {
    throw new RuntimeException(this.getClass().getName() + " has no incoming TokenVertex");
  }

  @Override
  public void addOutgoingTokenVertex(TokenVertex outgoing) {
    this.outgoingTokenVertices.add(outgoing);
  }

}
