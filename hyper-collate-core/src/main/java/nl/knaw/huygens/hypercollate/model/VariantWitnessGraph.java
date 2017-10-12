package nl.knaw.huygens.hypercollate.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class VariantWitnessGraph {

  String sigil = "";
  TokenVertex startTokenVertex = new StartTokenVertex();
  TokenVertex endTokenVertex = new EndTokenVertex();
  List<Markup> markupList = new ArrayList<>();
  Map<Markup, List<TokenVertex>> markup2TokenVertexList = new HashMap<>();
  Map<TokenVertex, List<Markup>> tokenVertex2MarkupList = new HashMap<>();

  public VariantWitnessGraph(String sigil) {
    this.sigil = sigil;
  }

  public TokenVertex getStartTokenVertex() {
    return this.startTokenVertex;
  }

  public TokenVertex getEndTokenVertex() {
    return this.endTokenVertex;
  }

  public String getSigil() {
    return this.sigil;
  }

  public Stream<Markup> getMarkupStream() {
    return this.markupList.stream();
  }

  public void addMarkup(Markup... markup) {
    for (Markup m : markup) {
      this.markupList.add(m);
    }
  }

  public void addOutgoingTokenVertexToTokenVertex(TokenVertex token0, TokenVertex token1) {
    token0.addOutgoingTokenVertex(token1); // (token0)->(token1)
    token1.addIncomingTokenVertex(token0); // (token1)<-(token0)
  }

  public void addMarkupToTokenVertex(SimpleTokenVertex tokenVertex, Markup markup) {
    markup2TokenVertexList.putIfAbsent(markup, new ArrayList<>());
    markup2TokenVertexList.get(markup).add(tokenVertex);
    tokenVertex2MarkupList.putIfAbsent(tokenVertex, new ArrayList<>());
    tokenVertex2MarkupList.get(tokenVertex).add(markup);
  }

  public List<Markup> getMarkupListForTokenVertex(TokenVertex tokenVertex) {
    return tokenVertex2MarkupList.get(tokenVertex);
  }

}
