package nl.knaw.huygens.hypercollate.collator.astar;

public abstract class Cost<T extends Cost<T>> implements Comparable<T> {
  protected abstract T plus(T other);
}
