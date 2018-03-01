package nl.knaw.huygens.hypercollate.rest.resources;

/*-
 * #%L
 * HyperCollate REST resources
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

import org.junit.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class HomePageResourceTest {
  @Test
  public void testNoRobots() {
    HomePageResource resource = new HomePageResource();
    String noRobots = resource.noRobots();
    assertThat(noRobots).isEqualTo("User-agent: *\n" +
        "Disallow: /\n");
  }
}
