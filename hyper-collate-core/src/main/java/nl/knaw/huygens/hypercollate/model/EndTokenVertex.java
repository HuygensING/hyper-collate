package nl.knaw.huygens.hypercollate.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class EndTokenVertex implements TokenVertex {

  private List<TokenVertex> incomingTokenVertices = new ArrayList<>();

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
