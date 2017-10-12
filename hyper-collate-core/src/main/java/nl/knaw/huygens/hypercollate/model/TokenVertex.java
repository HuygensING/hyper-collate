package nl.knaw.huygens.hypercollate.model;

import java.util.stream.Stream;

public interface TokenVertex {

  Token getToken();

  void addIncomingTokenVertex(TokenVertex incoming);

  Stream<TokenVertex> getIncomingTokenVertexStream();

  void addOutgoingTokenVertex(TokenVertex outgoing);

  Stream<TokenVertex> getOutgoingTokenVertexStream();

  // void addMarkup(Markup markup);
  //
  // Stream<Markup> getMarkupStream();

}
