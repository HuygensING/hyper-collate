package nl.knaw.huygens.hypercollate.model;

public class MarkedUpToken implements Token {
  private String content;
  private String normalizedContent;
  private SimpleWitness witness;

  public MarkedUpToken setContent(String content) {
    this.content = content;
    return this;
  }

  public String getContent() {
    return this.content;
  }

  public MarkedUpToken setNormalizedContent(String normalizedContent) {
    this.normalizedContent = normalizedContent;
    return this;
  }

  public String getNormalizedContent() {
    return this.normalizedContent;
  }

  @Override
  public Witness getWitness() {
    return witness;
  }

}
