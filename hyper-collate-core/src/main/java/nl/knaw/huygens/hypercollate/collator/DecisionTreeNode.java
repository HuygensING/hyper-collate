package nl.knaw.huygens.hypercollate.collator;

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
