package nl.knaw.huygens.hypercollate.importer;

/*-
 * #%L
 * hyper-collate-core
 * =======
 * Copyright (C) 2017 Huygens ING (KNAW)
 * =======
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import eu.interedition.collatex.simple.SimplePatternTokenizer;
import nl.knaw.huygens.hypercollate.model.*;
import org.apache.commons.io.FileUtils;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.LinkedList;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static nl.knaw.huygens.hypercollate.tools.StreamUtil.stream;

public class XMLImporter {

  private final Function<String, Stream<String>> tokenizer;
  private final Function<String, String> normalizer;

  public XMLImporter(Function<String, Stream<String>> tokenizer, Function<String, String> normalizer) {
    this.tokenizer = tokenizer;
    this.normalizer = normalizer;
  }

  public XMLImporter() {
    this.tokenizer = SimplePatternTokenizer.BY_WS_OR_PUNCT;
    this.normalizer = (String raw) -> raw.trim().toLowerCase();
  }

  public VariantWitnessGraph importXML(String sigil, String xmlString) {
    InputStream inputStream;
    try {
      inputStream = new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8.name()));
      return importXML(sigil, inputStream);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public VariantWitnessGraph importXML(String sigil, File xmlFile) {
    try {
      InputStream input = FileUtils.openInputStream(xmlFile);
      return importXML(sigil, input);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public VariantWitnessGraph importXML(String sigil, InputStream input) {
    VariantWitnessGraph graph = new VariantWitnessGraph(sigil);
    SimpleWitness witness = new SimpleWitness(sigil);
    XMLInputFactory factory = XMLInputFactory.newInstance();
    try {
      XMLEventReader reader = factory.createXMLEventReader(input);
      Context context = new Context(graph, normalizer, witness);
      while (reader.hasNext()) {
        XMLEvent event = reader.nextEvent();
        switch (event.getEventType()) {
        case XMLStreamConstants.START_DOCUMENT:
          handleStartDocument(event, context);
          break;
        case XMLStreamConstants.START_ELEMENT:
          handleStartElement(event.asStartElement(), context);
          break;
        case XMLStreamConstants.CHARACTERS:
          handleCharacters(event.asCharacters(), context);
          break;
        case XMLStreamConstants.END_ELEMENT:
          handleEndElement(event.asEndElement(), context);
          break;
        case XMLStreamConstants.END_DOCUMENT:
          handleEndDocument(event, context);
          break;
        case XMLStreamConstants.PROCESSING_INSTRUCTION:
          handleProcessingInstruction(event, context);
          break;
        case XMLStreamConstants.COMMENT:
          handleComment(event, context);
          break;
        case XMLStreamConstants.SPACE:
          handleSpace(event, context);
          break;
        case XMLStreamConstants.ENTITY_REFERENCE:
          handleEntityReference(event, context);
          break;
        case XMLStreamConstants.ATTRIBUTE:
          handleAttribute(event, context);
          break;
        case XMLStreamConstants.DTD:
          handleDTD(event, context);
          break;
        case XMLStreamConstants.CDATA:
          handleCData(event, context);
          break;
        case XMLStreamConstants.NAMESPACE:
          handleNameSpace(event, context);
          break;
        case XMLStreamConstants.NOTATION_DECLARATION:
          handleNotationDeclaration(event, context);
          break;
        case XMLStreamConstants.ENTITY_DECLARATION:
          handleEntityDeclaration(event, context);
          break;

        default:
          break;
        }

      }

      return graph;
    } catch (XMLStreamException e) {
      throw new RuntimeException(e);
    }
  }

  private void handleStartDocument(XMLEvent event, Context context) {
  }

  private void handleEndDocument(XMLEvent event, Context context) {
    context.closeDocument();
  }

  private void handleStartElement(StartElement startElement, Context context) {
    String tagName = startElement.getName().toString();
    Markup markup = new Markup(tagName);
    startElement.getAttributes().forEachRemaining((Object object) -> {
      Attribute attribute = (Attribute) object;
      String attributeName = attribute.getName().toString();
      String attributeValue = ((Attribute) object).getValue();
      markup.addAttribute(attributeName, attributeValue);
    });
    context.openMarkup(markup);
  }

  private void handleEndElement(EndElement endElement, Context context) {
    String tagName = endElement.getName().toString();
    Markup markup = new Markup(tagName);
    context.closeMarkup(markup);
  }

  private void handleCharacters(Characters characters, Context context) {
    String data = characters.getData();
    if (data.startsWith(" ")) {// because the tokenizer will lose theses leading whitespaces;
      context.addNewToken(" ");
    }
    tokenizer.apply(data).forEach(context::addNewToken);
  }

  private void handleNotationDeclaration(XMLEvent event, Context context) {
    throw new RuntimeException("unexpected event: NotationDeclaration");
  }

  private void handleEntityDeclaration(XMLEvent event, Context context) {
    throw new RuntimeException("unexpected event: EntityDeclaration");
  }

  private void handleNameSpace(XMLEvent event, Context context) {
    throw new RuntimeException("unexpected event: NameSpace");
  }

  private void handleCData(XMLEvent event, Context context) {
    throw new RuntimeException("unexpected event: CData");
  }

  private void handleDTD(XMLEvent event, Context context) {
    throw new RuntimeException("unexpected event: DTD");
  }

  private void handleAttribute(XMLEvent event, Context context) {
    throw new RuntimeException("unexpected event: Attribute");
  }

  private void handleEntityReference(XMLEvent event, Context context) {
    throw new RuntimeException("unexpected event: EntityReference");
  }

  private void handleSpace(XMLEvent event, Context context) {
    throw new RuntimeException("unexpected event: Space");
  }

  private void handleComment(XMLEvent event, Context context) {
  }

  private void handleProcessingInstruction(XMLEvent event, Context context) {
    throw new RuntimeException("unexpected event: ProcessingInstruction");
  }

  private static class Context {

    private final VariantWitnessGraph graph;
    private final Deque<Markup> openMarkup = new LinkedList<>();
    private TokenVertex lastTokenVertex;
    private long tokenCounter = 0L;
    private final Deque<TokenVertex> variationStartVertices = new LinkedList<>(); // the tokenvertices whose outgoing vertices are the variant vertices (add/del)
    private final Deque<TokenVertex> variationEndVertices = new LinkedList<>(); // the tokenvertices that are the last in a <del>
    private final Deque<TokenVertex> unconnectedVertices = new LinkedList<>(); // the last tokenvertex in an <add> which hasn't been linked to the tokenvertex after the </del> yet
    private final Function<String, String> normalizer;
    private final SimpleWitness witness;
    private String parentXPath;

    public Context(VariantWitnessGraph graph, Function<String, String> normalizer, SimpleWitness witness) {
      this.graph = graph;
      this.normalizer = normalizer;
      this.lastTokenVertex = graph.getStartTokenVertex();
      this.witness = witness;
    }

    public void openMarkup(Markup markup) {
      graph.addMarkup(markup);
      openMarkup.push(markup);
      parentXPath = buildParentXPath();
      if (isVariationStartingMarkup(markup)) {
        variationStartVertices.push(lastTokenVertex);
      } else if (isVariationEndingMarkup(markup)) {
        lastTokenVertex = variationStartVertices.pop();
      }
    }

    private boolean isVariationStartingMarkup(Markup markup) {
      return "del".equals(markup.getTagname());
    }

    private boolean isVariationEndingMarkup(Markup markup) {
      return "add".equals(markup.getTagname());
    }

    public void closeMarkup(Markup markup) {
      Markup firstToClose = openMarkup.peek();
      if (graph.getTokenVertexListForMarkup(firstToClose).isEmpty()) {
        // add milestone
        addNewToken("");
      }
      openMarkup.pop();
      parentXPath = buildParentXPath();
      String closingTag = markup.getTagname();
      String expectedTag = firstToClose.getTagname();
      if (!expectedTag.equals(closingTag)) {
        throw new RuntimeException("XML error: expected </" + expectedTag + ">, got </" + closingTag + ">");
      }
      if (isVariationStartingMarkup(markup)) {
        unconnectedVertices.push(lastTokenVertex);
      } else if (isVariationEndingMarkup(markup)) {
        variationEndVertices.push(lastTokenVertex);
      }
    }

    public void addNewToken(String content) {
      MarkedUpToken token = new MarkedUpToken()//
          .setContent(content)//
          .setWitness(witness)//
          .setIndexNumber(tokenCounter++)//
          .setParentXPath(parentXPath)//
          .setNormalizedContent(normalizer.apply(content));
      SimpleTokenVertex tokenVertex = new SimpleTokenVertex(token);
      graph.addOutgoingTokenVertexToTokenVertex(lastTokenVertex, tokenVertex);
      this.openMarkup.descendingIterator()//
          .forEachRemaining(markup -> graph.addMarkupToTokenVertex(tokenVertex, markup));
      checkUnconnectedVertices(tokenVertex);
      lastTokenVertex = tokenVertex;
    }

    private void checkUnconnectedVertices(SimpleTokenVertex tokenVertex) {
      if (!variationEndVertices.isEmpty() && lastTokenVertex.equals(variationEndVertices.peek())) {
        variationEndVertices.pop();
        TokenVertex unconnectedVertex = unconnectedVertices.pop();
        graph.addOutgoingTokenVertexToTokenVertex(unconnectedVertex, tokenVertex);
        checkUnconnectedVertices(tokenVertex);
      }
    }

    public void closeDocument() {
      graph.addOutgoingTokenVertexToTokenVertex(lastTokenVertex, graph.getEndTokenVertex());
    }

    private String buildParentXPath() {
      return "/" + stream(openMarkup.descendingIterator()).map(Markup::getTagname).collect(joining("/"));
    }
  }
}