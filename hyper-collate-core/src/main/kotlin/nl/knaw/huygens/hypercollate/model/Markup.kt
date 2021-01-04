package nl.knaw.huygens.hypercollate.model


/*-
* #%L
 * hyper-collate-core
 * =======
 * Copyright (C) 2017 - 2021 Huygens ING (KNAW)
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

import java.util.*

class Markup(val tagName: String) {
    val attributeMap: MutableMap<String, String> = TreeMap()

    var depth = 0
        private set

    fun addAttribute(key: String, value: String): Markup {
        attributeMap[key] = value
        return this
    }

    fun getAttributeValue(key: String): Optional<String> {
        return Optional.ofNullable(attributeMap[key])
    }

    override fun toString(): String {
        return String.format("<%s %s>", tagName, attributeMap)
    }

    fun setDepth(depth: Int): Markup {
        this.depth = depth
        return this
    }
}
