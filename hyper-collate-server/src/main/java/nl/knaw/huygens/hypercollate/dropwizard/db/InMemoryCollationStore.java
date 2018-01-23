package nl.knaw.huygens.hypercollate.dropwizard.db;

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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import nl.knaw.huygens.hypercollate.api.CollationInput;
import nl.knaw.huygens.hypercollate.api.WitnessInput;
import nl.knaw.huygens.hypercollate.dropwizard.ServerConfiguration;
import nl.knaw.huygens.hypercollate.dropwizard.api.CollationStore;
import nl.knaw.huygens.hypercollate.model.CollationGraph;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class InMemoryCollationStore implements CollationStore {
  private final Set<UUID> uuids = new LinkedHashSet<>();
  private static String baseURI;

  public InMemoryCollationStore(ServerConfiguration config) {
    baseURI = config.getBaseURI();
  }

  private static final Cache<UUID, CollationGraph> CollationGraphCache = CacheBuilder.newBuilder()//
      .maximumSize(100)//
      .build();

  @Override
  public Optional<CollationGraph> getCollationGraph(UUID uuid) {
    if (uuids.contains(uuid)) {
      try {
        CollationGraph document = CollationGraphCache.get(uuid, readCollationGraph(uuid));
        return Optional.of(document);
      } catch (ExecutionException e) {
        e.printStackTrace();
      }
    }
    return Optional.empty();
  }

  private static Callable<CollationGraph> readCollationGraph(UUID uuid) {
    return () -> null;
  }

  @Override
  public void setCollation(UUID collationId, CollationGraph collationGraph, CollationInput collationInput, long collationDurationInMilliseconds) {
    CollationGraphCache.put(collationId, collationGraph);

    CollationInfo docInfo = getCollationInfo(collationId)//
        .orElseGet(() -> newCollationInfo(collationId, collationInput));
    docInfo.setModified(Instant.now());
    docInfo.setCollationDurationInMilliseconds(collationDurationInMilliseconds);
    CollationInfoCache.put(collationId, docInfo);

    uuids.add(collationId);
  }

  @Override
  public void addWitness(UUID collationId, String sigil, String xml) {
    CollationInput collationInput = new CollationInput();
    CollationInfo docInfo = getCollationInfo(collationId)//
        .orElseGet(() -> newCollationInfo(collationId, collationInput));
    WitnessInput witnessInput = new WitnessInput().setSigil(sigil).setXml(xml);
    docInfo.getInput().addWitness(witnessInput);
    docInfo.setModified(Instant.now());
    CollationInfoCache.put(collationId, docInfo);
  }

  @Override
  public Set<UUID> getCollationUUIDs() {
    return uuids;
  }

  private static final Cache<UUID, CollationInfo> CollationInfoCache = CacheBuilder.newBuilder()//
      .maximumSize(100)//
      .build();

  @Override
  public Optional<CollationInfo> getCollationInfo(UUID uuid) {
    if (uuids.contains(uuid)) {
      try {
        CollationInfo CollationInfo = CollationInfoCache.get(uuid, () -> null);
        return Optional.ofNullable(CollationInfo);
      } catch (ExecutionException e) {
        e.printStackTrace();
      }
    }
    return Optional.empty();
  }

  // private static Callable<? extends CollationInfo> readCollationInfo(UUID uuid, CollationInput collationInput) {
  // return () -> newCollationInfo(uuid, collationInput);
  // }

  private static CollationInfo newCollationInfo(UUID uuid, CollationInput collationInput) {
    return new CollationInfo(uuid, baseURI, collationInput)//
        .setCreated(Instant.now())//
        .setModified(Instant.now());
  }

  public Collection<UUID> getDocumentUUIDs() {
    return uuids;
  }

}
