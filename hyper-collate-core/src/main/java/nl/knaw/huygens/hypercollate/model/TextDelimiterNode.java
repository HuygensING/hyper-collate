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

import eu.interedition.collatex.Token;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TextDelimiterNode extends TextNode {

  TextDelimiterNode() {
    super();
  }

  @Override
  public void addToken(Token token) {
    throw new RuntimeException("TextDelimiterNodes don't have tokens.");
  }

  @Override
  public Token getTokenForWitness(String sigil) {
    throw new RuntimeException("TextDelimiterNodes don't have tokens.");
  }

  @Override
  public Set<String> getSigils() {
    return Collections.emptySet();
  }

  @Override
  public String toString() {
    return "()";
  }

  @Override
  public List<Integer> getBranchPath(String s) {
    throw new RuntimeException("TextDelimiterNodes don't have a branchPath");
  }

  @Override
  public void addBranchPath(String sigil, List<Integer> branchPath) {
    throw new RuntimeException("TextDelimiterNodes don't have a branchPath");
  }
}
