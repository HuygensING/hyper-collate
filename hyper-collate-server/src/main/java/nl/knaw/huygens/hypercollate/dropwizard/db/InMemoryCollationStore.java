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
import nl.knaw.huygens.hypercollate.dropwizard.ServerConfiguration;
import nl.knaw.huygens.hypercollate.dropwizard.api.CollationStore;
import nl.knaw.huygens.hypercollate.model.CollationGraph;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class InMemoryCollationStore implements CollationStore {
  private final Set<String> names = new LinkedHashSet<>();
  private static String baseURI;
  private static final Cache<String, CollationInfo> CollationInfoCache = CacheBuilder.newBuilder()//
      .maximumSize(100)//
      .build();
  private static final Cache<String, CollationGraph> CollationGraphCache = CacheBuilder.newBuilder()//
      .maximumSize(100)//
      .build();

  public InMemoryCollationStore(ServerConfiguration config) {
    baseURI = config.getBaseURI();
  }

  @Override
  public Optional<CollationGraph> getCollationGraph(String collationId) {
    if (names.contains(collationId)) {
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
    names.add(collationId);
  }

  @Override
  public void setCollation(CollationInfo collationInfo, CollationGraph collationGraph) {
    String collationId = collationInfo.getId();
    CollationGraphCache.put(collationId, collationGraph);
    collationInfo.setModified(Instant.now());
    CollationInfoCache.put(collationId, collationInfo);
  }

//  public void addWitness(String name, String sigil, String xml) {
//    CollationInfo docInfo = getCollationInfo(name)//
//        .orElseGet(() -> newCollationInfo(name));
//    docInfo.addWitness(sigil, xml);
//    docInfo.setModified(Instant.now());
//    CollationInfoCache.put(name, docInfo);
//  }

  @Override
  public Set<String> getCollationIds() {
    return names;
  }

  @Override
  public Optional<CollationInfo> getCollationInfo(String collationId) {
    if (names.contains(collationId)) {
      try {
        CollationInfo CollationInfo = CollationInfoCache.get(collationId, () -> null);
        return Optional.ofNullable(CollationInfo);
      } catch (ExecutionException e) {
        e.printStackTrace();
      }
    }
    return Optional.empty();
  }

  @Override
  public void persist() {
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
