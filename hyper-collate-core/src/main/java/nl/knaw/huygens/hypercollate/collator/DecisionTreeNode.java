package nl.knaw.huygens.hypercollate.collator;

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
import java.util.ArrayList;
import java.util.List;

public class DecisionTreeNode {
  private QuantumCollatedMatchList quantumCollatedMatchList;
  private List<DecisionTreeNode> childNodes = new ArrayList<>();

  public DecisionTreeNode(QuantumCollatedMatchList qcml) {
    this.quantumCollatedMatchList = qcml;
  }

  public static DecisionTreeNode of(QuantumCollatedMatchList qcml) {
    return new DecisionTreeNode(qcml);
  }

  public QuantumCollatedMatchList getQuantumCollatedMatchList() {
    return quantumCollatedMatchList;
  }

  public void addChildNode(DecisionTreeNode node) {
    this.childNodes.add(node);
  }

  public List<DecisionTreeNode> getChildNodes() {
    return childNodes;
  }

}
