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
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.google.common.base.Stopwatch
import nl.knaw.huygens.hypercollate.collator.HyperCollator
import nl.knaw.huygens.hypercollate.importer.XMLImporter
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph
import nl.knaw.huygens.hypercollate.tools.CollationGraphNodeJoiner
import nl.knaw.huygens.hypercollate.tools.CollationGraphVisualizer
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

fun main() {
    (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).level = Level.WARN
    for (i in 1..100) testCollationWithManyMatches()
}

fun testCollationWithManyMatches() {
    val importer = XMLImporter()
    val xml1 = ("<seg>Ik had een buurvrouw, een paar deuren verder,"
            + " en <del>ze</del><add>het</add> was zo'n type dat naar het muse<del>im</del>um ging en "
            + "cappuc<add>c</add>i<del>o</del>no's dronk<del>l</del>, dus ik<del>i k</del>kon er weinig mee, en zij kon weinig"
            + " m<del>netr</del>et mij<del>,</del><add>;</add> we <del>lk</del> knikten alleen naar elkaar, en als ik"
            + " Rock<del>u</del>y bij me had, <del>knikte</del>maakte ze van het knikken iets dat nog wat sneller "
            + "a<del >g</del>fgehandeld moest<del>r</del> worden dan anders.</seg>")
    val w1 = importer.importXML("W1", xml1)
    val xml2 = ("<seg><del>Ik had een buurvrouw, </del><add>Die "
            + "buurvrouw woonde </add>een paar deuren verder, en het was zo'n type <del>dat naar het museum ging en "
            + "cappuccino's dronk, dus ik kon er</del><add>waar ik</add> weinig mee<add> ko<del>m</del>n</add>, en zij kon "
            + "weinig met mij; we knikten alleen naar elkaar, en als ik Rocky bij me had, maakte ze van het knikken iets dat"
            + " nog wat sneller afgehandeld moest worden dan anders.</seg>")
    val w2 = importer.importXML("W2", xml2)
    hyperCollate(w1, w2)
}

private val hyperCollator = HyperCollator()

private fun hyperCollate(
        witness1: VariantWitnessGraph,
        witness2: VariantWitnessGraph
) {
    val collation0 = time("collation") {
        hyperCollator.collate(witness1, witness2)
    }

    val collation = time("joining") {
        CollationGraphNodeJoiner.join(collation0)
    }

    time("toDot") {
        CollationGraphVisualizer.toDot(
                collation,
                emphasizeWhitespace = true,
                hideMarkup = false
        )
    }

    time("toTableASCII") { CollationGraphVisualizer.toTableASCII(collation, false) }
}

private fun <A> time(title: String, func: () -> A): A {
    val stopwatch = Stopwatch.createStarted()
    val result = func()
    stopwatch.stop()
    val duration = stopwatch.elapsed(TimeUnit.MILLISECONDS)
    println("> $title took $duration ms")
    return result
}
