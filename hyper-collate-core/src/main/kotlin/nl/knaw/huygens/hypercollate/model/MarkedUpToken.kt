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
import eu.interedition.collatex.Witness

class MarkedUpToken : Token {
    var content: String = ""
        internal set

    var normalizedContent: String = ""
        internal set

    private var witness: SimpleWitness? = null

    var indexNumber = 0L
        private set

    var parentXPath = ""
        private set

    var rdg: String? = null
        private set

    fun withContent(content: String): MarkedUpToken {
        this.content = content
        return this
    }

    fun withNormalizedContent(normalizedContent: String): MarkedUpToken {
        this.normalizedContent = normalizedContent
        return this
    }

    fun withWitness(witness: SimpleWitness): MarkedUpToken {
        this.witness = witness
        return this
    }

    fun withRdg(rdg: String?): MarkedUpToken {
        this.rdg = rdg
        return this
    }

    override fun getWitness(): Witness {
        return witness!!
    }

    fun withIndexNumber(index: Long): MarkedUpToken {
        indexNumber = index
        return this
    }

    fun withParentXPath(parentXPath: String): MarkedUpToken {
        this.parentXPath = parentXPath
        return this
    }

    override fun toString(): String {
        return (witness!!.sigil
                + indexNumber
                + ":"
                + parentXPath
                + "='"
                + content.replace("\n", "\\n")
                + "'")
    }

    fun clone(): MarkedUpToken =
            MarkedUpToken()
                    .withWitness(witness!!)
                    .withContent(content)
                    .withRdg(rdg!!)
                    .withNormalizedContent(normalizedContent)
                    .withIndexNumber(indexNumber)
                    .withParentXPath(parentXPath)
}
