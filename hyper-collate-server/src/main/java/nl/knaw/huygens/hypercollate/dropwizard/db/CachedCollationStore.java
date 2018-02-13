package nl.knaw.huygens.hypercollate.dropwizard.db;

/*-
 * #%L
 * HyperCollate server
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import nl.knaw.huygens.hypercollate.dropwizard.ServerConfiguration;
import nl.knaw.huygens.hypercollate.dropwizard.api.CollationStore;
import nl.knaw.huygens.hypercollate.dropwizard.db.CollationInfo.State;
import nl.knaw.huygens.hypercollate.importer.XMLImporter;
import nl.knaw.huygens.hypercollate.model.CollationGraph;
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph;

import java.io.File;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class CachedCollationStore implements CollationStore {
  private final Set<String> collationIds = new LinkedHashSet<>();
  private static String baseURI;
  private static final Cache<String, CollationInfo> CollationInfoCache = CacheBuilder.newBuilder()//
      .maximumSize(100)//
      .build();
  private static final Cache<String, CollationGraph> CollationGraphCache = CacheBuilder.newBuilder()//
      .maximumSize(100)//
      .build();
  private final File projectDir;
  private final File collationsDir;

  public CachedCollationStore(ServerConfiguration config) {
    baseURI = config.getBaseURI();
    projectDir = config.getProjectDir();
    collationsDir = config.getCollationsDir();
    readCollationIds();
  }

  @Override
  public Optional<CollationGraph> getCollationGraph(String collationId) {
    if (collationIds.contains(collationId)) {
      try {
        CollationGraph document = CollationGraphCache.get(collationId, readCollationGraph(collationId));
        return Optional.of(document);
      } catch (ExecutionException e) {
        e.printStackTrace();
      }
    }
    return Optional.empty();
  }

  @Override
  public void addCollation(String collationId) {
    CollationInfo collationInfo = getCollationInfo(collationId)//
        .orElseGet(() -> newCollationInfo(collationId));
    collationInfo.setModified(Instant.now());
    CollationInfoCache.put(collationId, collationInfo);
    collationIds.add(collationId);
    persist(collationId);
  }

  @Override
  public void setCollation(CollationInfo collationInfo, CollationGraph collationGraph) {
    String collationId = collationInfo.getId();
    CollationGraphCache.put(collationId, collationGraph);
    collationInfo.setModified(Instant.now());
    CollationInfoCache.put(collationId, collationInfo);
    persist(collationId);
  }

  // public void addWitness(String name, String sigil, String xml) {
  // CollationInfo docInfo = getCollationInfo(name)//
  // .orElseGet(() -> newCollationInfo(name));
  // docInfo.addWitness(sigil, xml);
  // docInfo.setModified(Instant.now());
  // CollationInfoCache.put(name, docInfo);
  // }

  @Override
  public Set<String> getCollationIds() {
    return collationIds;
  }

  @Override
  public Optional<CollationInfo> getCollationInfo(String collationId) {
    if (collationIds.contains(collationId)) {
      try {
        CollationInfo CollationInfo = CollationInfoCache.get(collationId, () -> readCollationInfo(collationId).orElse(null));
        return Optional.ofNullable(CollationInfo);
      } catch (ExecutionException e) {
        e.printStackTrace();
      }
    }
    return Optional.empty();
  }

  @Override
  public void removeCollation(String collationId) {
    CollationInfoCache.invalidate(collationId);
    collationIds.remove(collationId);
    storeCollationIds();
    File file = getCollationInfoFile(collationId);
    file.delete();
  }

  @Override
  public void persist(String collationId) {
    storeCollationIds();
    storeCollationInfo(getCollationInfo(collationId).get());
    // CollationInfoCache.asMap()//
    // .values()//
    // .forEach(this::storeCollationInfo);
  }

  @Override
  public boolean idInUse(String collationId) {
    return collationIds.contains(collationId);
  }

  private void storeCollationIds() {
    try {
      File file = getCollationIndexFile();
      new ObjectMapper().writeValue(file, collationIds);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private File getCollationIndexFile() {
    return new File(projectDir, "collationIds.json");
  }

  private void readCollationIds() {
    File file = getCollationIndexFile();
    if (file.exists()) {
      try {
        collationIds.addAll(objectMapper().readValue(file, Set.class));
      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }
  }

  private ObjectMapper objectMapper() {
    return new ObjectMapper()//
        .registerModule(new ParameterNamesModule())//
        .registerModule(new Jdk8Module())//
        .registerModule(new JavaTimeModule());
  }

  private void storeCollationInfo(CollationInfo collationInfo) {
    try {
      File file = getCollationInfoFile(collationInfo.getId());
      objectMapper().writeValue(file, collationInfo);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private Optional<CollationInfo> readCollationInfo(String id) {
    File file = getCollationInfoFile(id);
    try {
      CollationInfo collationInfo = objectMapper().readValue(file, CollationInfo.class);
      initialize(collationInfo);
      return Optional.of(collationInfo);
    } catch (Exception e) {
      e.printStackTrace();
      // throw new RuntimeException(e);
    }
    return Optional.empty();
  }

  private void initialize(CollationInfo collationInfo) {
    collationInfo.setUriBase(baseURI);
    if (collationInfo.getCollationState().equals(State.is_collated)) {
      collationInfo.collationState = State.ready_to_collate;
    }
    collationInfo.getWitnesses().forEach((sigil, xml) -> {
      VariantWitnessGraph variantWitnessGraph = new XMLImporter().importXML(sigil, xml);
      collationInfo.addWitnessGraph(sigil, variantWitnessGraph);
    });
  }

  private File getCollationInfoFile(String id) {
    return new File(collationsDir, id + ".json");
  }

  private static CollationInfo newCollationInfo(String name) {
    return new CollationInfo(name, baseURI)//
        .setCreated(Instant.now())//
        .setModified(Instant.now());
  }

  private static Callable<CollationGraph> readCollationGraph(String name) {
    return () -> null;
  }
}
