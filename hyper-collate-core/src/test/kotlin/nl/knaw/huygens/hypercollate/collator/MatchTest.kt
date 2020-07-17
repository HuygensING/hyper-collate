package nl.knaw.huygens.hypercollate.collator

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

import eu.interedition.collatex.Token
import eu.interedition.collatex.Witness
import nl.knaw.huygens.hypercollate.HyperCollateTest
import nl.knaw.huygens.hypercollate.model.TokenVertex
import org.assertj.core.api.Assertions
import org.junit.Test
import java.util.*
import java.util.stream.Stream

class MatchTest : HyperCollateTest() {

    @Test
    fun testGetLowestRankForWitnessesOtherThan() {
        val v1 = mockVertexWithSigil("A")
        val v2 = mockVertexWithSigil("B")
        val v3 = mockVertexWithSigil("C")
        val v4 = mockVertexWithSigil("D")
        val match = Match(v1, v2, v3, v4).setRank("A", 1).setRank("B", 2).setRank("C", 3).setRank("D", 4)
        val lowestRankForWitnessesOtherThan = match.getLowestRankForWitnessesOtherThan("A")
        Assertions.assertThat(lowestRankForWitnessesOtherThan).isEqualTo(2)
    }

    class DummyWitness : Witness {
        override fun getSigil(): String = "S"
    }

    class DummyToken : Token {
        override fun getWitness(): Witness = DummyWitness()
    }

    val dummyToken: Token = DummyToken()

    private fun mockVertexWithSigil(sigil: String): TokenVertex =
            object : TokenVertex {
                override val token: Token
                    get() = dummyToken

                override fun addIncomingTokenVertex(incoming: TokenVertex) {}

                override val incomingTokenVertexStream: Stream<TokenVertex>
                    get() = Stream.empty()

                override fun addOutgoingTokenVertex(outgoing: TokenVertex) {}

                override val outgoingTokenVertexStream: Stream<TokenVertex>
                    get() = Stream.empty()

                override val sigil: String
                    get() = sigil

                override val branchPath: List<Int>
                    get() = ArrayList()

            }
}
