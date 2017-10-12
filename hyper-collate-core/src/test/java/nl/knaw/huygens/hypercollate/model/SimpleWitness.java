package nl.knaw.huygens.hypercollate.model;

public class SimpleWitness implements Witness {

  private String sigil;

  public SimpleWitness(String sigil) {
    this.sigil = sigil;
  }

  @Override
  public String getSigil() {
    return sigil;
  }

}
