package nl.knaw.huygens.hypercollate.model;

/*-
 * #%L
 * hyper-collate-core
 * =======
 * Copyright (C) 2017 - 2021 Huygens ING (KNAW)
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

import com.google.common.base.Preconditions;
import nl.knaw.huygens.hypercollate.collator.VariantWitnessGraphTraversal;

import java.util.*;
import java.util.stream.Stream;

public class VariantWitnessGraph {

  private final String sigil;
  private final List<Markup> markupList = new ArrayList<>();

  private final TokenVertex startTokenVertex;
  private final TokenVertex endTokenVertex;

  private final Map<Markup, List<TokenVertex>> markup2TokenVertexList = new HashMap<>();
  private final Map<TokenVertex, List<Markup>> tokenVertex2MarkupList = new HashMap<>();

  public VariantWitnessGraph(String sigil) {
    this.sigil = sigil;
    startTokenVertex = new StartTokenVertex(sigil);
    endTokenVertex = new EndTokenVertex(sigil);
  }

  public TokenVertex getStartTokenVertex() {
    return this.startTokenVertex;
  }

  public TokenVertex getEndTokenVertex() {
    return this.endTokenVertex;
  }

  public String getSigil() {
    return this.sigil;
  }

  public Stream<Markup> getMarkupStream() {
    return this.markupList.stream();
  }

  public void addMarkup(Markup... markup) {
    Collections.addAll(this.markupList, markup);
  }

  public void addOutgoingTokenVertexToTokenVertex(TokenVertex token0, TokenVertex token1) {
    if (token0 == null || token1 == null) {
      return;
    }
    Preconditions.checkNotNull(token0);
    Preconditions.checkNotNull(token1);
    token0.addOutgoingTokenVertex(token1); // (token0)->(token1)
    token1.addIncomingTokenVertex(token0); // (token1)<-(token0)
  }

  public void addMarkupToTokenVertex(SimpleTokenVertex tokenVertex, Markup markup) {
    markup2TokenVertexList.putIfAbsent(markup, new ArrayList<>());
    markup2TokenVertexList.get(markup).add(tokenVertex);
    tokenVertex2MarkupList.putIfAbsent(tokenVertex, new ArrayList<>());
    tokenVertex2MarkupList.get(tokenVertex).add(markup);
  }

  public List<Markup> getMarkupListForTokenVertex(TokenVertex tokenVertex) {
    return tokenVertex2MarkupList.getOrDefault(tokenVertex, new ArrayList<>());
  }

  public List<TokenVertex> getTokenVertexListForMarkup(Markup markup) {
    return markup2TokenVertexList.getOrDefault(markup, new ArrayList<>());
  }

  public Iterable<TokenVertex> vertices() {
    return VariantWitnessGraphTraversal.of(this);
  }
}
