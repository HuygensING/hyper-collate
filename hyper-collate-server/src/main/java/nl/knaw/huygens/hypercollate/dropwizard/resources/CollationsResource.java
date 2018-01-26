package nl.knaw.huygens.hypercollate.dropwizard.resources;

/*-
 * #%L
 * hyper-collate-server
 * =======
 * Copyright (C) 2017 - 2018 Huygens ING (KNAW)
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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import static java.util.stream.Collectors.toList;
import jersey.repackaged.com.google.common.base.Stopwatch;
import nl.knaw.huygens.hypercollate.api.ResourcePaths;
import nl.knaw.huygens.hypercollate.api.UTF8MediaType;
import nl.knaw.huygens.hypercollate.collator.HyperCollator;
import nl.knaw.huygens.hypercollate.dropwizard.ServerConfiguration;
import nl.knaw.huygens.hypercollate.dropwizard.api.CollationStore;
import nl.knaw.huygens.hypercollate.dropwizard.db.CollationInfo;
import nl.knaw.huygens.hypercollate.importer.XMLImporter;
import nl.knaw.huygens.hypercollate.model.CollationGraph;
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph;
import nl.knaw.huygens.hypercollate.tools.CollationGraphNodeJoiner;
import nl.knaw.huygens.hypercollate.tools.CollationGraphVisualizer;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Api(ResourcePaths.COLLATIONS)
@Path(ResourcePaths.COLLATIONS)
@Produces(UTF8MediaType.APPLICATION_JSON)
public class CollationsResource {
  private static final String APIPARAM_NAME = "Collation name";
  private static final String APIPARAM_SIGIL = "Witness sigil";
  private static final String APIPARAM_XML = "Witness Source (XML)";
  private final ServerConfiguration configuration;
  private final HyperCollator hypercollator = new HyperCollator();
  private final CollationStore collationStore;

  public CollationsResource(ServerConfiguration configuration, CollationStore collationStore) {
    this.configuration = configuration;
    this.collationStore = collationStore;
  }

  @GET
  @Timed
  @ApiOperation(value = "List all collation names")
  public List<String> getCollationNames() {
    return collationStore.getCollationIds()//
        .stream()//
        .map(id -> String.format("%s/%s/%s", configuration.getBaseURI(), ResourcePaths.COLLATIONS, id))//
        .sorted()//
        .collect(toList());
  }

  @PUT
  @Timed
  @Path("{name}")
  @ApiOperation(value = "Create a new collation with the given name")
  @Produces(UTF8MediaType.TEXT_PLAIN)
  public Response addCollation(@ApiParam(APIPARAM_NAME) @PathParam("name") final String name) {
    try {
      collationStore.addCollation(name);
      return Response.created(documentURI(name)).build();

    } catch (Exception e) {
      e.printStackTrace();
      throw new BadRequestException(e.getMessage());
    }
  }

  @GET
  @Path("{name}")
  @Timed
  @Produces(UTF8MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get information about the collation")
  public Response getCollationInfo(@ApiParam(APIPARAM_NAME) @PathParam("name") final String name) {
    CollationInfo collationInfo = getExistingCollationInfo(name);
    return Response.ok(collationInfo).build();
  }

  @PUT
  @Path("{name}/" + ResourcePaths.WITNESSES + "/{sigil}")
  @Timed
  @Consumes(UTF8MediaType.TEXT_XML)
  @ApiOperation(value = "Add a witness to the collation")
  public Response addXMLWitness(@ApiParam(APIPARAM_NAME) @PathParam("name") final String name, @ApiParam(APIPARAM_SIGIL) @PathParam("sigil") final String sigil,
                                @ApiParam(APIPARAM_XML) @NotNull @Valid String xml) {
    CollationInfo collationInfo = getExistingCollationInfo(name);
    collationInfo.addWitness(sigil, xml);
    collationStore.persist();
    return Response.noContent().build();
  }

  @GET
  @Path("{name}/" + ResourcePaths.WITNESSES + "/{sigil}")
  @Timed
  @Produces(UTF8MediaType.TEXT_XML)
  @ApiOperation(value = "Return the XML source of the witness")
  public Response getWitnessXML(@ApiParam(APIPARAM_NAME) @PathParam("name") final String name, @ApiParam(APIPARAM_SIGIL) @PathParam("sigil") final String sigil) {
    CollationInfo collationInfo = getExistingCollationInfo(name);
    String xml = collationInfo.getWitness(sigil).orElseThrow(NotFoundException::new);
    return Response.ok(xml).build();
  }

  @GET
  @Path("{name}/" + ResourcePaths.COLLATIONS_DOT)
  @Timed
  @Produces(UTF8MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get a .dot visualization of the collation graph")
  public Response getDotVisualization(@ApiParam(APIPARAM_NAME) @PathParam("name") final String name) {
    CollationGraph collation = getExistingCollationGraph(name);
    String dot = CollationGraphVisualizer.toDot(collation);
    return Response.ok(dot).build();
  }

  @GET
  @Path("{name}/" + ResourcePaths.COLLATIONS_ASCII_TABLE)
  @Timed
  @Produces(UTF8MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get an ASCII table visualization of the collation graph")
  public Response getAsciiTableVisualization(@ApiParam(APIPARAM_NAME) @PathParam("name") final String name) {
    CollationGraph collation = getExistingCollationGraph(name);
    String table = CollationGraphVisualizer.toTableASCII(collation);
    return Response.ok(table).build();
  }

  private URI documentURI(String collationId) {
    return URI.create(String.format("%s/%s/%s", configuration.getBaseURI(), ResourcePaths.COLLATIONS, collationId));
  }

  private CollationInfo getExistingCollationInfo(final String name) {
    return collationStore.getCollationInfo(name)//
        .orElseThrow(NotFoundException::new);
  }

  private CollationGraph getExistingCollationGraph(final String name) {
    CollationInfo collationInfo = getExistingCollationInfo(name);
    CollationInfo.State collationState = collationInfo.getCollationState();
    if (collationState.equals(CollationInfo.State.needs_witness)) {
      throw new BadRequestException("This collation has no witnesses yet. Add them first;");
    }
    if (collationState.equals(CollationInfo.State.ready_to_collate)) {
      collate(collationInfo);
    }
    return collationStore.getCollationGraph(name)//
        .orElseThrow(NotFoundException::new);
  }

  private void collate(CollationInfo collationInfo) {
    XMLImporter importer = new XMLImporter();
    Stopwatch stopwatch = Stopwatch.createStarted();
    VariantWitnessGraph[] variantWitnessGraphs = collationInfo.getWitnesses().entrySet().stream().map(e -> importer.importXML(e.getKey(), e.getValue())).toArray(VariantWitnessGraph[]::new);

    CollationGraph collationGraph = hypercollator.collate(variantWitnessGraphs);
    if (collationInfo.getJoin()) {
      collationGraph = CollationGraphNodeJoiner.join(collationGraph);
    }
    stopwatch.stop();
    collationInfo.setCollationDurationInMilliseconds(stopwatch.elapsed(TimeUnit.MILLISECONDS));
    collationStore.setCollation(collationInfo, collationGraph);
  }

}
