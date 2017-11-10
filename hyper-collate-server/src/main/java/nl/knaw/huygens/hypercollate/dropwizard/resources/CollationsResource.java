package nl.knaw.huygens.hypercollate.dropwizard.resources;

/*-
 * #%L
 * hyper-collate-server
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

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Stopwatch;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import nl.knaw.huygens.hypercollate.api.CollationInput;
import nl.knaw.huygens.hypercollate.api.ResourcePaths;
import nl.knaw.huygens.hypercollate.api.UTF8MediaType;
import nl.knaw.huygens.hypercollate.api.WitnessInput;
import nl.knaw.huygens.hypercollate.collater.HyperCollater;
import nl.knaw.huygens.hypercollate.collater.OptimalMatchSetAlgorithm2;
import nl.knaw.huygens.hypercollate.dropwizard.ServerConfiguration;
import nl.knaw.huygens.hypercollate.dropwizard.api.CollationStore;
import nl.knaw.huygens.hypercollate.dropwizard.db.CollationInfo;
import nl.knaw.huygens.hypercollate.importer.XMLImporter;
import nl.knaw.huygens.hypercollate.model.CollationGraph;
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph;
import nl.knaw.huygens.hypercollate.tools.CollationGraphNodeJoiner;
import nl.knaw.huygens.hypercollate.tools.CollationGraphVisualizer;

@Api(ResourcePaths.COLLATIONS)
@Path(ResourcePaths.COLLATIONS)
@Produces(UTF8MediaType.APPLICATION_JSON)
public class CollationsResource {
  public static final String APIPARAM_UUID = "collation UUID";
  public static final String APIPARAM_COLLATION_INPUT = "Collation input";
  private final ServerConfiguration configuration;
  private final HyperCollater hypercollater = new HyperCollater(new OptimalMatchSetAlgorithm2());
  private final CollationStore collationStore;

  public CollationsResource(ServerConfiguration configuration, CollationStore collationStore) {
    this.configuration = configuration;
    this.collationStore = collationStore;
  }

  @GET
  @Timed
  @ApiOperation(value = "List all collation URIs")
  public List<URI> getCollationURIs() {
    return collationStore.getCollationUUIDs()//
        .stream()//
        .map(this::documentURI)//
        .collect(Collectors.toList());
  }

  @POST
  @Consumes(UTF8MediaType.APPLICATION_JSON)
  @Timed
  @ApiOperation(value = "Create a new collation")
  @Produces(UTF8MediaType.TEXT_PLAIN)
  public Response addCollation(@ApiParam(APIPARAM_COLLATION_INPUT) @NotNull @Valid CollationInput collationInput) {
    UUID collationId = UUID.randomUUID();
    try {
      process(collationInput, collationId);
      return Response.created(documentURI(collationId)).build();

    } catch (Exception e) {
      e.printStackTrace();
      throw new BadRequestException(e.getMessage());
    }
  }

  @GET
  @Path("{uuid}")
  @Timed
  @Produces(UTF8MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get information about the collation")
  public Response getCollationInfo(@ApiParam(APIPARAM_UUID) @PathParam("uuid") final UUID uuid) {
    CollationInfo collationInfo = getExistingCollationInfo(uuid);
    return Response.ok(collationInfo).build();
  }

  @GET
  @Path("{uuid}/" + ResourcePaths.COLLATIONS_DOT)
  @Timed
  @Produces(UTF8MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get a .dot visualization of the collation graph")
  public Response getDotVisualization(@ApiParam(APIPARAM_UUID) @PathParam("uuid") final UUID uuid) {
    CollationGraph collation = getExistingCollationGraph(uuid);
    String dot = CollationGraphVisualizer.toDot(collation);
    return Response.ok(dot).build();
  }

  @GET
  @Path("{uuid}/" + ResourcePaths.COLLATIONS_ASCII_TABLE)
  @Timed
  @Produces(UTF8MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get an ASCII table visualization of the collation graph")
  public Response getAsciiTableVisualization(@ApiParam(APIPARAM_UUID) @PathParam("uuid") final UUID uuid) {
    CollationGraph collation = getExistingCollationGraph(uuid);
    String table = CollationGraphVisualizer.toTableASCII(collation);
    return Response.ok(table).build();
  }

  private void process(CollationInput collationInput, UUID collationId) {
    List<WitnessInput> witnesses = collationInput.getWitnesses();
    WitnessInput wi1 = witnesses.get(0);
    WitnessInput wi2 = witnesses.get(1);
    XMLImporter importer = new XMLImporter();
    VariantWitnessGraph w1 = importer.importXML(wi1.getSigil(), wi1.getXml());
    VariantWitnessGraph w2 = importer.importXML(wi2.getSigil(), wi2.getXml());
    Stopwatch stopwatch = Stopwatch.createStarted();
    CollationGraph collationGraph = hypercollater.collate(w1, w2);
    if (collationInput.getJoin()) {
      collationGraph = CollationGraphNodeJoiner.join(collationGraph);
    }
    stopwatch.stop();
    collationStore.setCollation(collationId, collationGraph, collationInput, stopwatch.elapsed(TimeUnit.MILLISECONDS));
  }

  private URI documentURI(UUID collationId) {
    return URI.create(configuration.getBaseURI() + "/" + ResourcePaths.COLLATIONS + "/" + collationId);
  }

  private CollationGraph getExistingCollationGraph(final UUID uuid) {
    return collationStore.getCollationGraph(uuid)//
        .orElseThrow(NotFoundException::new);
  }

  private CollationInfo getExistingCollationInfo(final UUID uuid) {
    return collationStore.getCollationInfo(uuid)//
        .orElseThrow(NotFoundException::new);
  }

}
