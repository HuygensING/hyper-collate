package nl.knaw.huygens.hypercollate.collater;

import eu.interedition.collatex.dekker.astar.Cost;

public class LostPotential extends Cost<LostPotential> {

  private Integer cost;

  public LostPotential(int cost) {
    this.cost = cost;
  }

  @Override
  protected LostPotential plus(LostPotential other) {
    return new LostPotential(cost + other.getCost());
  }

  @Override
  public int compareTo(LostPotential other) {
    return cost.compareTo(other.getCost());
  }

  public Integer getCost() {
    return cost;
  }
}
