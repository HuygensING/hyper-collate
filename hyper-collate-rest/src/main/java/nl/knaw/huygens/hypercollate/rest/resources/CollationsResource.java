package nl.knaw.huygens.hypercollate.rest.resources;

/*-
 * #%L
 * hyper-collate-rest
 * =======
 * Copyright (C) 2017 - 2021 Huygens ING (KNAW)
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

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Stopwatch;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import nl.knaw.huygens.graphviz.DotEngine;
import nl.knaw.huygens.hypercollate.api.ResourcePaths;
import nl.knaw.huygens.hypercollate.api.UTF8MediaType;
import nl.knaw.huygens.hypercollate.collator.HyperCollator;
import nl.knaw.huygens.hypercollate.importer.XMLImporter;
import nl.knaw.huygens.hypercollate.model.CollationGraph;
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph;
import nl.knaw.huygens.hypercollate.rest.CollationInfo;
import nl.knaw.huygens.hypercollate.rest.CollationStore;
import nl.knaw.huygens.hypercollate.rest.HyperCollateConfiguration;
import nl.knaw.huygens.hypercollate.tools.CollationGraphNodeJoiner;
import nl.knaw.huygens.hypercollate.tools.CollationGraphVisualizer;
import nl.knaw.huygens.hypercollate.tools.DotFactory;
import nl.knaw.huygens.hypercollate.tools.TokenMerger;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

@Api(ResourcePaths.COLLATIONS)
@Path(ResourcePaths.COLLATIONS)
@Produces(UTF8MediaType.APPLICATION_JSON)
public class CollationsResource {
  private static final String PATHPARAM_NAME = "name";
  private static final String PATHPARAM_SIGIL = "sigil";

  private static final String COLLATION_PATH = "{" + PATHPARAM_NAME + "}";
  private static final String COLLATION_SUBPATH = COLLATION_PATH + "/";
  private static final String COLLATION_FORMATPATH = COLLATION_PATH + ".";
  private static final String COLLATION_ASCII_TABLE_PATH =
      COLLATION_SUBPATH + ResourcePaths.COLLATIONS_ASCII_TABLE;
  private static final String COLLATION_DOT_PATH =
      COLLATION_FORMATPATH + ResourcePaths.COLLATIONS_DOT;
  private static final String COLLATION_SVG_PATH =
      COLLATION_FORMATPATH + ResourcePaths.COLLATIONS_SVG;
  private static final String COLLATION_PNG_PATH =
      COLLATION_FORMATPATH + ResourcePaths.COLLATIONS_PNG;
  private static final String COLLATION_WITNESS_PATH =
      COLLATION_SUBPATH + ResourcePaths.WITNESSES + "/{" + PATHPARAM_SIGIL + "}";
  private static final String COLLATION_WITNESS_XML_PATH = COLLATION_WITNESS_PATH + ".xml";
  private static final String COLLATION_WITNESS_SVG_PATH = COLLATION_WITNESS_PATH + ".svg";
  private static final String COLLATION_WITNESS_PNG_PATH = COLLATION_WITNESS_PATH + ".png";
  private static final String COLLATION_WITNESS_DOT_PATH = COLLATION_WITNESS_PATH + ".dot";

  private static final String APIPARAM_NAME = "Collation name";
  private static final String APIPARAM_SIGIL = "Witness sigil";
  private static final String APIPARAM_XML = "Witness Source (XML)";
  private static final String EMPHASIZE_WHITESPACE = "emphasize-whitespace";
  private static final String HIDE_MARKUP = "hide-markup";
  private static final String JOIN_TOKENS = "join-tokens";

  private static final String IMAGE_PNG = "image/png";
  private static final String IMAGE_SVG = "image/svg+xml";
  private static final String PNG = "png";
  private static final String SVG = "svg";
  private static final String FALSE = "false";

  private final HyperCollateConfiguration configuration;
  private final HyperCollator hypercollator = new HyperCollator();
  private final CollationStore collationStore;
  private final DotEngine dotEngine;
  private final boolean dotEngineAvailable;

  public CollationsResource(
      HyperCollateConfiguration configuration, CollationStore collationStore) {
    this.configuration = configuration;
    this.collationStore = collationStore;
    if (configuration.hasPathToDotExecutable()) {
      this.dotEngine = new DotEngine(configuration.getPathToDotExecutable());
      this.dotEngineAvailable = true;
    } else {
      this.dotEngine = null;
      this.dotEngineAvailable = false;
    }
  }

  @GET
  @Timed
  @ApiOperation(value = "List all collation names")
  public List<String> getCollationNames() {
    return collationStore.getCollationIds().stream()
        .map(
            id ->
                format("%s/%s/%s", configuration.getBaseURI(), ResourcePaths.COLLATIONS, id))
        .sorted()
        .collect(toList());
  }

  @PUT
  @Timed
  @Path(COLLATION_PATH)
  @ApiOperation(value = "Create a new collation with the given name")
  @Produces(UTF8MediaType.TEXT_PLAIN)
  public Response addCollation(
      @ApiParam(APIPARAM_NAME) @PathParam(PATHPARAM_NAME) final String name) {
    if (collationStore.idInUse(name)) {
      throw new BadRequestException(
          format("%s '%s' is already in use.", APIPARAM_NAME, name));
    }
    try {
      collationStore.addCollation(name);
      return Response.created(collationURI(name)).build();

    } catch (Exception e) {
      e.printStackTrace();
      throw new BadRequestException(e.getMessage());
    }
  }

  @DELETE
  @Timed
  @Path(COLLATION_PATH)
  @ApiOperation(value = "Delete the collation with the given name")
  public Response deleteCollation(
      @ApiParam(APIPARAM_NAME) @PathParam(PATHPARAM_NAME) final String name) {
    if (!collationStore.idInUse(name)) {
      throw collationNotFoundException(name);
    }
    try {
      collationStore.removeCollation(name);
      return Response.noContent().build();

    } catch (Exception e) {
      e.printStackTrace();
      throw new BadRequestException(e.getMessage());
    }
  }

  @GET
  @Path(COLLATION_PATH)
  @Timed
  @Produces(UTF8MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get information about the collation")
  public Response getCollationInfo(
      @ApiParam(APIPARAM_NAME) @PathParam(PATHPARAM_NAME) final String name) {
    CollationInfo collationInfo = getExistingCollationInfo(name);
    return Response.ok(collationInfo).build();
  }

  @PUT
  @Path(COLLATION_WITNESS_PATH)
  @Timed
  @Consumes(UTF8MediaType.TEXT_XML)
  @ApiOperation(value = "Add a witness to the collation")
  public Response addXMLWitness(
      @ApiParam(APIPARAM_NAME) @PathParam(PATHPARAM_NAME) @NotNull final String name,
      @ApiParam(APIPARAM_SIGIL) @PathParam(PATHPARAM_SIGIL) @NotNull final String sigil,
      @ApiParam(APIPARAM_XML) @NotNull @Valid String xml) {
    CollationInfo collationInfo = getExistingCollationInfo(name);
    VariantWitnessGraph variantWitnessGraph = new XMLImporter().importXML(sigil, xml);
    collationInfo.addWitness(sigil, xml);
    collationInfo.addWitnessGraph(sigil, variantWitnessGraph);
    collationStore.persist(name);
    return Response.noContent().build();
  }

  @GET
  @Path(COLLATION_WITNESS_XML_PATH)
  @Timed
  @Produces(UTF8MediaType.TEXT_XML)
  @ApiOperation(value = "Return the XML source of the witness")
  public Response getWitnessXML(
      @ApiParam(APIPARAM_NAME) @PathParam(PATHPARAM_NAME) final String name,
      @ApiParam(APIPARAM_SIGIL) @PathParam(PATHPARAM_SIGIL) final String sigil) {
    CollationInfo collationInfo = getExistingCollationInfo(name);
    String xml =
        collationInfo
            .getWitness(sigil)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        format("No witness '%s' found for collation '%s'.", sigil, name)));
    return Response.ok(xml).build();
  }

  @GET
  @Path(COLLATION_WITNESS_DOT_PATH)
  @Timed
  @Produces(UTF8MediaType.TEXT_PLAIN)
  @ApiOperation(
      value =
          "Get a .dot visualization of the witness graph, with optional emphasizing of whitespace and optional joining of tokens.")
  public Response getWitnessDot(
      @ApiParam(APIPARAM_NAME) @PathParam(PATHPARAM_NAME) final String name,
      @ApiParam(APIPARAM_SIGIL) @PathParam(PATHPARAM_SIGIL) final String sigil,
      @DefaultValue(FALSE) @QueryParam(EMPHASIZE_WHITESPACE) final boolean emphasizeWhitespace,
      @DefaultValue(FALSE) @QueryParam(JOIN_TOKENS) final boolean joinTokens) {
    String dot = getDot(name, sigil, emphasizeWhitespace, joinTokens);
    return Response.ok(dot).build();
  }

  @GET
  @Path(COLLATION_WITNESS_SVG_PATH)
  @Timed
  @Produces(IMAGE_SVG)
  @ApiOperation(
      value =
          "Return an SVG visualization of the witness graph, with optional emphasizing of whitespace and optional joining of tokens.")
  public Response getWitnessSVG(
      @ApiParam(APIPARAM_NAME) @PathParam(PATHPARAM_NAME) final String name,
      @ApiParam(APIPARAM_SIGIL) @PathParam(PATHPARAM_SIGIL) final String sigil,
      @DefaultValue(FALSE) @QueryParam(EMPHASIZE_WHITESPACE) boolean emphasizeWhitespace,
      @DefaultValue(FALSE) @QueryParam(JOIN_TOKENS) final boolean joinTokens) {
    return renderWitnessGraphAs(name, sigil, emphasizeWhitespace, joinTokens, SVG);
  }

  @GET
  @Path(COLLATION_WITNESS_PNG_PATH)
  @Timed
  @Produces(IMAGE_PNG)
  @ApiOperation(
      value =
          "Return a PNG visualization of the witness graph, with optional emphasizing of whitespace and optional joining of tokens.")
  public Response getWitnessPNG(
      @ApiParam(APIPARAM_NAME) @PathParam(PATHPARAM_NAME) final String name,
      @ApiParam(APIPARAM_SIGIL) @PathParam(PATHPARAM_SIGIL) final String sigil,
      @DefaultValue(FALSE) @QueryParam(EMPHASIZE_WHITESPACE) boolean emphasizeWhitespace,
      @DefaultValue(FALSE) @QueryParam(JOIN_TOKENS) final boolean joinTokens) {
    return renderWitnessGraphAs(name, sigil, emphasizeWhitespace, joinTokens, PNG);
  }

  @GET
  @Path(COLLATION_DOT_PATH)
  @Timed
  @Produces(UTF8MediaType.TEXT_PLAIN)
  @ApiOperation(
      value =
          "Get a .dot visualization of the collation graph, with optional emphasizing of whitespace and optional hiding of markup.")
  public Response getDotVisualization(
      @ApiParam(APIPARAM_NAME) @PathParam(PATHPARAM_NAME) final String name,
      @DefaultValue(FALSE) @QueryParam(EMPHASIZE_WHITESPACE) final boolean emphasizeWhitespace,
      @DefaultValue(FALSE) @QueryParam(HIDE_MARKUP) final boolean hideMarkup) {
    String dot = getDot(name, emphasizeWhitespace, hideMarkup);
    return Response.ok(dot).build();
  }

  @GET
  @Path(COLLATION_SVG_PATH)
  @Timed
  @Produces(IMAGE_SVG)
  @ApiOperation(
      value =
          "Get an SVG visualization of the collation graph, with optional emphasizing of whitespace and optional hiding of markup.")
  public Response getSVGVisualization(
      @ApiParam(APIPARAM_NAME) @PathParam(PATHPARAM_NAME) final String name,
      @DefaultValue(FALSE) @QueryParam(EMPHASIZE_WHITESPACE) final boolean emphasizeWhitespace,
      @DefaultValue(FALSE) @QueryParam(HIDE_MARKUP) final boolean hideMarkup) {
    return getCollationGraphVisualization(name, emphasizeWhitespace, hideMarkup, SVG);
  }

  @GET
  @Path(COLLATION_PNG_PATH)
  @Timed
  @Produces(IMAGE_PNG)
  @ApiOperation(
      value =
          "Get a PNG visualization of the collation graph, with optional emphasizing of whitespace and optional hiding of markup.")
  public Response getPNGVisualization(
      @ApiParam(APIPARAM_NAME) @PathParam(PATHPARAM_NAME) final String name,
      @DefaultValue(FALSE) @QueryParam(EMPHASIZE_WHITESPACE) final boolean emphasizeWhitespace,
      @DefaultValue(FALSE) @QueryParam(HIDE_MARKUP) final boolean hideMarkup) {
    return getCollationGraphVisualization(name, emphasizeWhitespace, hideMarkup, PNG);
  }

  @GET
  @Path(COLLATION_ASCII_TABLE_PATH)
  @Timed
  @Produces(UTF8MediaType.TEXT_PLAIN)
  @ApiOperation(
      value =
          "Get an ASCII table visualization of the collation graph, with optional emphasizing of whitespace.")
  public Response getAsciiTableVisualization(
      @ApiParam(APIPARAM_NAME) @PathParam(PATHPARAM_NAME) final String name,
      @DefaultValue(FALSE) @QueryParam(EMPHASIZE_WHITESPACE) final boolean emphasizeWhitespace) {
    CollationGraph collation = getExistingCollationGraph(name);
    String table = CollationGraphVisualizer.toTableASCII(collation, emphasizeWhitespace);
    return Response.ok(table).build();
  }

  private URI collationURI(String collationId) {
    return URI.create(
        format(
            "%s/%s/%s", configuration.getBaseURI(), ResourcePaths.COLLATIONS, collationId));
  }

  private CollationInfo getExistingCollationInfo(final String name) {
    return collationStore
        .getCollationInfo(name)
        .orElseThrow(() -> collationNotFoundException(name));
  }

  private CollationGraph getExistingCollationGraph(final String name) {
    CollationInfo collationInfo = getExistingCollationInfo(name);
    CollationInfo.State collationState = collationInfo.getCollationState();
    if (collationState.equals(CollationInfo.State.needs_witness)) {
      throw new BadRequestException(
          format("Collation '%s' has no witnesses yet. Please add them first.", name));
    }
    if (collationState.equals(CollationInfo.State.ready_to_collate)) {
      collate(collationInfo);
    }
    return collationStore
        .getCollationGraph(name)
        .orElseThrow(() -> collationNotFoundException(name));
  }

  private NotFoundException collationNotFoundException(String name) {
    return new NotFoundException(format("No collation '%s' found.", name));
  }

  private void collate(CollationInfo collationInfo) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    VariantWitnessGraph[] variantWitnessGraphs =
        collationInfo.getWitnessGraphMap().values().toArray(new VariantWitnessGraph[]{});

    CollationGraph collationGraph = hypercollator.collate(variantWitnessGraphs);
    if (collationInfo.getJoin()) {
      collationGraph = CollationGraphNodeJoiner.join(collationGraph);
    }
    stopwatch.stop();
    collationInfo.setCollationDurationInMilliseconds(stopwatch.elapsed(TimeUnit.MILLISECONDS));
    collationStore.setCollation(collationInfo, collationGraph);
  }

  private Response getCollationGraphVisualization(
      String name, boolean emphasizeWhitespace, boolean hideMarkup, String format) {
    String dot = getDot(name, emphasizeWhitespace, hideMarkup);
    return renderDotAs(dot, format);
  }

  private Response renderDotAs(String dot, String format) {
    if (!dotEngineAvailable) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("Cannot render, pathToDotExecutable not set in server config file.")
          .type(MediaType.TEXT_PLAIN)
          .build();
    }

    StreamingOutput stream = outputStream -> dotEngine.renderAs(format, dot, outputStream);
    return Response.ok(stream).build();
  }

  private String getDot(String name, boolean emphasizeWhitespace, boolean hideMarkup) {
    CollationGraph collation = getExistingCollationGraph(name);
    return CollationGraphVisualizer.toDot(collation, emphasizeWhitespace, hideMarkup);
  }

  private Response renderWitnessGraphAs(
      String name, String sigil, boolean emphasizeWhitespace, boolean joinTokens, String format) {
    String dot = getDot(name, sigil, emphasizeWhitespace, joinTokens);
    return renderDotAs(dot, format);
  }

  private String getDot(
      String name, String sigil, boolean emphasizeWhitespace, boolean joinTokens) {
    CollationInfo collationInfo = getExistingCollationInfo(name);
    VariantWitnessGraph variantWitnessGraph = collationInfo.getWitnessGraphMap().get(sigil);
    if (joinTokens) {
      variantWitnessGraph = TokenMerger.merge(variantWitnessGraph);
    }
    return new DotFactory(emphasizeWhitespace).fromVariantWitnessGraphColored(variantWitnessGraph);
  }
}
