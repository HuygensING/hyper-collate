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
import nl.knaw.huygens.hypercollate.collator.HyperCollator
import nl.knaw.huygens.hypercollate.importer.XMLImporter
import nl.knaw.huygens.hypercollate.model.CollationGraph
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph
import org.slf4j.LoggerFactory
import java.io.File

object HC {

    fun init() {
        (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).level = Level.WARN
        val version = this.javaClass.getResource("version.txt").openStream().bufferedReader().use { it.readText() }
        println("Welcome to HyperCollate $version")
    }

    fun initCell() {
        println("initCell")
    }

    fun shutdown() {
        println("Goodbye from hyper-collate")
    }

    fun importVariantWitnessGraphFromXML(sigil: String, xml: String): VariantWitnessGraph =
            XMLImporter().importXML(sigil, xml)

    fun importVariantWitnessGraphFromXML(sigil: String, xmlFile: File): VariantWitnessGraph =
            XMLImporter().importXML(sigil, xmlFile)

    fun collate(vararg witnesses: VariantWitnessGraph): CollationGraph =
            HyperCollator().collate(*witnesses)
}
