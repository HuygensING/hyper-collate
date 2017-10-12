package nl.knaw.huygens.hypercollate.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class SimpleTokenVertex implements TokenVertex {

  private Token token;
  private List<TokenVertex> incomingVertices = new ArrayList<>();
  private List<TokenVertex> outgoingVertices = new ArrayList<>();

  public SimpleTokenVertex(MarkedUpToken token) {
    this.token = token;
  }

  @Override
  public Token getToken() {
    return token;
  }

  @Override
  public void addIncomingTokenVertex(TokenVertex incoming) {
    incomingVertices.add(incoming);
  }

  @Override
  public Stream<TokenVertex> getIncomingTokenVertexStream() {
    return incomingVertices.stream();
  }

  @Override
  public void addOutgoingTokenVertex(TokenVertex outgoing) {
    outgoingVertices.add(outgoing);
  }

  @Override
  public Stream<TokenVertex> getOutgoingTokenVertexStream() {
    return outgoingVertices.stream();
  }

}
