/*-
 * #%L
 * hyper-collate-jupyter
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
package nl.knaw.huygens.hypercollate.jupyter

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import nl.knaw.huygens.hypercollate.model.CollationGraph
import nl.knaw.huygens.hypercollate.tools.CollationGraphVisualizer
import org.slf4j.LoggerFactory

class LibraryConfig {

    companion object {
        fun init() {
            (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).level = Level.WARN
            println("Welcome to hyper-collate")
        }

        fun initCell() {
            println("initCell")
        }

        fun shutdown() {
            println("Goodbye from hyper-collate")
        }

        fun renderCollationGraph(collationGraph: CollationGraph): String =
                CollationGraphVisualizer.toTableASCII(collationGraph, false)
    }
}
