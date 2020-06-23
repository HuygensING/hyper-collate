package nl.knaw.huygens.hypercollate.collator

import org.assertj.core.api.AssertionsForClassTypes
import org.junit.Test
import java.util.*

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
 */   class QuantumCollatedMatchListTest {
    @Test
    fun testBranchPathsOverlap() {
        val matchBranchPath = Arrays.asList(0, 1, 3)
        val nodeBranchPath = Arrays.asList(0, 1)
        val matchBranchPath1 = Arrays.asList(0, 4, 6)
        val branchPathsOverlap1 = QuantumCollatedMatchList.branchPathsOverlap(matchBranchPath, nodeBranchPath)
        AssertionsForClassTypes.assertThat(branchPathsOverlap1).isTrue()
        val branchPathsOverlap2 = QuantumCollatedMatchList.branchPathsOverlap(nodeBranchPath, matchBranchPath)
        AssertionsForClassTypes.assertThat(branchPathsOverlap2).isTrue()
        val branchPathsOverlap3 = QuantumCollatedMatchList.branchPathsOverlap(matchBranchPath1, nodeBranchPath)
        AssertionsForClassTypes.assertThat(branchPathsOverlap3).isFalse()
    }
}