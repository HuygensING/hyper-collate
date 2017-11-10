package nl.knaw.huygens.hypercollate.api;

/*-
 * #%L
 * hyper-collate-api
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

import java.util.ArrayList;
import java.util.List;

public class CollationInput {

  List<WitnessInput> witnesses = new ArrayList<>();
  Boolean join = true;

  public List<WitnessInput> getWitnesses() {
    return witnesses;
  }

  public void addWitness(WitnessInput witnessInput) {
    witnesses.add(witnessInput);
  }

  public void setJoin(Boolean join) {
    this.join = join;
  }

  public Boolean getJoin() {
    return this.join;
  }

}
