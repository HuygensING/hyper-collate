package nl.knaw.huygens.hypercollate.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class StartTokenVertex implements TokenVertex {
  private List<TokenVertex> outgoingTokenVertices = new ArrayList<>();

  @Override
  public Token getToken() {
    return null;
  }

  @Override
  public Stream<TokenVertex> getIncomingTokenVertexStream() {
    return Stream.empty();
  }

  @Override
  public Stream<TokenVertex> getOutgoingTokenVertexStream() {
    return outgoingTokenVertices.stream();
  }

  @Override
  public void addIncomingTokenVertex(TokenVertex incoming) {
    throw new RuntimeException(this.getClass().getName() + " has no incoming TokenVertex");
  }

  @Override
  public void addOutgoingTokenVertex(TokenVertex outgoing) {
    this.outgoingTokenVertices.add(outgoing);
  }

}
