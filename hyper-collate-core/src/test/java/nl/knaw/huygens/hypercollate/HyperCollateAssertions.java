package nl.knaw.huygens.hypercollate;

/*-
 * #%L
 * hyper-collate-core
 * =======
 * Copyright (C) 2017 - 2020 Huygens ING (KNAW)
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
import nl.knaw.huygens.hypercollate.model.CollationGraphAssert;
import org.assertj.core.api.Assertions;

public class HyperCollateAssertions extends Assertions {

  protected HyperCollateAssertions() {
  }

  /**
   * Creates a new instance of <code>{@link nl.knaw.huygens.hypercollate.model.CollationGraphAssert}</code>.
   *
   * @param actual the actual value.
   * @return the created assertion object.
   */
  @org.assertj.core.util.CheckReturnValue
  public static CollationGraphAssert assertThat(CollationGraph actual) {
    return new CollationGraphAssert(actual);
  }
}
