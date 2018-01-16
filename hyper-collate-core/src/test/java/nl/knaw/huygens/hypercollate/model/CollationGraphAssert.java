package nl.knaw.huygens.hypercollate.model;

/*-
 * #%L
 * hyper-collate-core
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
import org.assertj.core.api.AbstractObjectAssert;

public class CollationGraphAssert extends AbstractObjectAssert<CollationGraphAssert, CollationGraph> {

  public CollationGraphAssert(CollationGraph collationGraph) {
    super(collationGraph, CollationGraphAssert.class);
  }

  public CollationGraphAssert aligned(String... tokens) {
    isNotNull();

    // we overrides the default error message with a more explicit one
//    String errorMessage = "\nExpected jobTitle of:\n  <%s>\nto be:\n  <%s>\n but was:\n  <%s>";

    // check
//    if (!actual.jobTitle.equals(jobTitle)) {
//      failWithMessage(errorMessage, actual, jobTitle, actual.jobTitle);
//    }

    return myself;
  }

  public CollationGraphAssert nonAligned(String... tokens) {
    isNotNull();
    return myself;
  }
}
