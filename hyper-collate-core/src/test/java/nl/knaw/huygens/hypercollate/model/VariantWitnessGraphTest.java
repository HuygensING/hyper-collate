package nl.knaw.huygens.hypercollate.model;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

public class VariantWitnessGraphTest {

  @Test
  public void test() {
    String xml = "<s>Collating is <del>NP hard</del><add>easy</add>.</s>";

    Markup sMarkup = new Markup("s");
    Markup delMarkup = new Markup("del");
    Markup addMarkup = new Markup("add");

    SimpleTokenVertex mtv0 = aTokenVertex("Collating ");
    SimpleTokenVertex mtv1 = aTokenVertex("is ");
    SimpleTokenVertex mtv2 = aTokenVertex("NP ");
    SimpleTokenVertex mtv3 = aTokenVertex("hard");
    SimpleTokenVertex mtv4 = aTokenVertex("easy");
    SimpleTokenVertex mtv5 = aTokenVertex(".");

    VariantWitnessGraph vwg1 = new VariantWitnessGraph("A");
    vwg1.addMarkup(sMarkup, delMarkup, addMarkup);
    TokenVertex startTokenVertex = vwg1.getStartTokenVertex();
    vwg1.addOutgoingTokenVertexToTokenVertex(startTokenVertex, mtv0); // (START)->(collating)

    vwg1.addOutgoingTokenVertexToTokenVertex(mtv0, mtv1); // (collating)->(is)
    vwg1.addMarkupToTokenVertex(mtv0, sMarkup);

    vwg1.addOutgoingTokenVertexToTokenVertex(mtv1, mtv2); // (is)->(np)
    vwg1.addOutgoingTokenVertexToTokenVertex(mtv1, mtv4); // (is)->(easy)
    vwg1.addMarkupToTokenVertex(mtv1, sMarkup);

    vwg1.addOutgoingTokenVertexToTokenVertex(mtv2, mtv3);// (np)->(hard)
    vwg1.addMarkupToTokenVertex(mtv2, sMarkup);
    vwg1.addMarkupToTokenVertex(mtv2, delMarkup);

    vwg1.addOutgoingTokenVertexToTokenVertex(mtv3, mtv5);// (hard)->(.)
    vwg1.addMarkupToTokenVertex(mtv3, sMarkup);
    vwg1.addMarkupToTokenVertex(mtv3, delMarkup);

    vwg1.addOutgoingTokenVertexToTokenVertex(mtv4, mtv5);// (easy)->(.)
    vwg1.addMarkupToTokenVertex(mtv4, sMarkup);
    vwg1.addMarkupToTokenVertex(mtv4, addMarkup);

    TokenVertex endTokenVertex = vwg1.getEndTokenVertex();
    vwg1.addOutgoingTokenVertexToTokenVertex(mtv5, endTokenVertex);// (.)->(END)
    vwg1.addMarkupToTokenVertex(mtv5, sMarkup);

    List<Markup> hardMarkup = vwg1.getMarkupListForTokenVertex(mtv3);
    assertThat(hardMarkup).containsExactly(sMarkup, delMarkup);

    List<TokenVertex> incoming = mtv5.getIncomingTokenVertexStream().collect(toList());
    assertThat(incoming).containsOnly(mtv3, mtv4);

  }

  private SimpleTokenVertex aTokenVertex(String string) {
    MarkedUpToken token = new MarkedUpToken()//
        .setContent(string)//
        .setNormalizedContent(string.toLowerCase());
    return new SimpleTokenVertex(token);
  }

}
