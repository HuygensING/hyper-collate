package nl.knaw.huygens.hypercollate.model;

import java.util.Comparator;

public interface Witness {

  String getSigil();

  Comparator<Witness> SIGIL_COMPARATOR = Comparator.comparing(Witness::getSigil);
}