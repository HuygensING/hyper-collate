package nl.knaw.huygens.hypercollate.collater;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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