package nl.knaw.huygens.hypercollate.rest.resources

import org.assertj.core.api.AssertionsForClassTypes
import org.junit.Test

/*-
 * #%L
 * hyper-collate-rest
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
 */   class HomePageResourceTest {
    @Test
    fun testNoRobots() {
        val resource = HomePageResource()
        val noRobots = resource.noRobots()
        AssertionsForClassTypes.assertThat(noRobots).isEqualTo("""
    User-agent: *
    Disallow: /
    
    """.trimIndent())
    }
}
