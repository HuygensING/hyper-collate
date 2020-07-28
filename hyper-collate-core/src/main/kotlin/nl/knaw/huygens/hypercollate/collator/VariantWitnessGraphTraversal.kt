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

import nl.knaw.huygens.hypercollate.model.TokenVertex
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph
import java.util.*

class VariantWitnessGraphTraversal private constructor(private val graph: VariantWitnessGraph) : Iterable<TokenVertex> {
    override fun iterator(): Iterator<TokenVertex> =
            object : Iterator<TokenVertex> {
                private val encountered: MutableMap<TokenVertex, Long> = HashMap()
                private val queue: Queue<TokenVertex> = ArrayDeque()
                private var next = Optional.of(graph.startTokenVertex)

                override fun hasNext(): Boolean =
                        next.isPresent

                override fun next(): TokenVertex {
                    val next = next.get()
                    next.outgoingTokenVertexStream
                            .forEach { outgoing: TokenVertex ->
                                val endEncountered = Optional.ofNullable(encountered[outgoing]).orElse(0L)
                                val endIncoming = outgoing.incomingTokenVertexStream.count()
                                check(endIncoming != endEncountered) { String.format("Encountered cycle traversing %s to %s", next, outgoing) }
                                if (endIncoming - endEncountered == 1L) {
                                    queue.add(outgoing)
                                }
                                encountered[outgoing] = endEncountered + 1
                            }
                    this.next = Optional.ofNullable(queue.poll())
                    return next
                }
            }

    companion object {
        @JvmStatic
        fun of(graph: VariantWitnessGraph): VariantWitnessGraphTraversal =
                VariantWitnessGraphTraversal(graph)
    }

}
