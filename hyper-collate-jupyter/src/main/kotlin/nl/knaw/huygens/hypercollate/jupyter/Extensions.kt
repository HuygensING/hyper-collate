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

import nl.knaw.huygens.hypercollate.model.CollationGraph
import nl.knaw.huygens.hypercollate.model.VariantWitnessGraph
import nl.knaw.huygens.hypercollate.tools.CollationGraphVisualizer
import nl.knaw.huygens.hypercollate.tools.DotFactory

fun CollationGraph.asASCIITable(emphasizeWhitespace: Boolean = false): String =
        CollationGraphVisualizer.toTableASCII(this, emphasizeWhitespace)

fun CollationGraph.asDot(emphasizeWhitespace: Boolean = false, hideMarkup: Boolean = false): String =
        CollationGraphVisualizer.toDot(this, emphasizeWhitespace, hideMarkup)

fun CollationGraph.asHTML(): String =
        CollationGraphVisualizer.toTableHTML(this)

fun VariantWitnessGraph.asDot(emphasizeWhitespace: Boolean = false): String =
        DotFactory(emphasizeWhitespace).fromVariantWitnessGraphSimple(this)

fun VariantWitnessGraph.asColoredDot(emphasizeWhitespace: Boolean = false): String =
        DotFactory(emphasizeWhitespace).fromVariantWitnessGraphColored(this)
