package nl.knaw.huygens.hypercollate.collator;

/*-
 * #%L
 * HyperCollate core
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class QuantumCollatedMatchListTest {

  @Test
  public void testBranchPathsOverlap(){
    List<Integer> matchBranchPath = Arrays.asList(0,1,3);
    List<Integer> nodeBranchPath = Arrays.asList(0,1);
    List<Integer> matchBranchPath1 = Arrays.asList(0,4,6);

    boolean branchPathsOverlap1 = QuantumCollatedMatchList.branchPathsOverlap(matchBranchPath,nodeBranchPath);
    assertThat(branchPathsOverlap1).isTrue();

    boolean branchPathsOverlap2 = QuantumCollatedMatchList.branchPathsOverlap(nodeBranchPath,matchBranchPath);
    assertThat(branchPathsOverlap2).isTrue();

    boolean branchPathsOverlap3 = QuantumCollatedMatchList.branchPathsOverlap(matchBranchPath1,nodeBranchPath);
    assertThat(branchPathsOverlap3).isFalse();
  }

}
