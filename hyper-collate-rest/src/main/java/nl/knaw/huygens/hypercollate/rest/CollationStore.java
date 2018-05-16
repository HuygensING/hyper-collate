package nl.knaw.huygens.hypercollate.rest;

/*-
 * #%L
 * hyper-collate-rest
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

import nl.knaw.huygens.hypercollate.model.CollationGraph;

import java.util.Optional;
import java.util.Set;

public interface CollationStore {

  CollationInfo addCollation(String collationId);

  void setCollation(CollationInfo collationInfo, CollationGraph collationGraph);

  void persist(String collationId);

  boolean idInUse(String collationId);

  Set<String> getCollationIds();

  Optional<CollationGraph> getCollationGraph(String collationId);

  Optional<CollationInfo> getCollationInfo(String collationId);

  void removeCollation(String collationId);
}
