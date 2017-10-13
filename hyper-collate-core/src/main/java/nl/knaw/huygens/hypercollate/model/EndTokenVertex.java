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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class EndTokenVertex implements TokenVertex {

  private final List<TokenVertex> incomingTokenVertices = new ArrayList<>();

  @Override
  public Token getToken() {
    return null;
  }

  @Override
  public Stream<TokenVertex> getIncomingTokenVertexStream() {
    return incomingTokenVertices.stream();
  }

  @Override
  public Stream<TokenVertex> getOutgoingTokenVertexStream() {
    return Stream.empty();
  }

  @Override
  public void addIncomingTokenVertex(TokenVertex incoming) {
    this.incomingTokenVertices.add(incoming);
  }

  @Override
  public void addOutgoingTokenVertex(TokenVertex outgoing) {
    throw new RuntimeException(this.getClass().getName() + " has no outgoing TokenVertex");
  }

}
