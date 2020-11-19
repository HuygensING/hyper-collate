package nl.knaw.huygens.hypercollate.model

/*-
* #%L
 * hyper-collate-core
 * =======
 * Copyright (C) 2017 - 2020 Huygens ING (KNAW)
 * =======
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *       http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * #L%
*/

import eu.interedition.collatex.Token
import java.util.*
import java.util.stream.Collectors

open class TextNode(vararg tokens: Token) : Node {
    private val tokenMap: MutableMap<String, Token> = HashMap()
    private val branchPaths: MutableMap<String, List<Int>> = HashMap()

    fun addToken(token: Token) {
        if (token.witness != null) {
            tokenMap[token.witness.sigil] = token
        }
    }

    fun tokenForSigil(sigil: String): Token? =
            tokenMap[sigil]

    val sigils: Set<String>
        get() = tokenMap.keys

    override fun toString(): String {
        val tokensString = sigils.stream()
                .sorted()
                .map { key: String -> tokenMap[key]!! }
                .map { obj: Token -> obj.toString() }
                .collect(Collectors.joining(", "))
        return "($tokensString)"
    }

    fun branchPathForSigil(s: String): List<Int> =
            branchPaths[s]!!

    fun addBranchPath(sigil: String, branchPath: List<Int>) {
        branchPaths[sigil] = branchPath
    }

    companion object {
        const val LABEL = "Text"
    }

    init {
        for (token in tokens) {
            addToken(token)
        }
    }
}
