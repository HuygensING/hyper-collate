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
package nl.knaw.huygens.hypercollate.collator

import org.redundent.kotlin.xml.Node
import org.redundent.kotlin.xml.xml

class ProcessLogger(private val startSigil: String) {
    var durationInMilliSeconds: Long = 0L

    internal val collationSteps: MutableList<CollationStep> = mutableListOf()

    override fun toString(): String = """
        |collation took $durationInMilliSeconds ms
        |collation steps:
        |- initializing $startSigil as variantGraph 
        |- ${collationSteps.joinToString("\n- ") { step -> "collating ${step.sigil} with variantGraph" }}
        |""".trimMargin()

    fun toHTML(): String = xml("table") {
        attribute("border", 1)
        "tr" {
            "td" { -"collation took $durationInMilliSeconds ms" }
        }
        "tr" {
            "td" { -"collation steps:" }
        }
        "tr" {
            "td" { -"* initializing $startSigil as variantGraph" }
        }
        collationSteps.forEach { it.toTableRows()() }
    }.toString(prettyFormat = false)
}

data class CollationStep(val sigil: String) {
    val processSteps: MutableList<ProcessStep> = mutableListOf()
}

typealias NodeBlock = Node.() -> Unit

private fun CollationStep.toTableRows(): NodeBlock =
        {
            "tr" {
                "td" { -"* collating $sigil with variantGraph" }
            }
            "tr" {
                "td" {
                    "table" {
                        attribute("border", 1)
                        "tr" {
                            "th" { -"step" }
                            "th" { -"matches" }
                        }
                        processSteps.mapIndexed { index, processStep ->
                            processStep.toTableRow(index)()
                        }
                    }
                }
            }
        }

data class ProcessStep(val sigil: String)

private fun ProcessStep.toTableRow(index: Int): NodeBlock =
        {
            "tr" {
                "td" { -"$index" }
                "td" { -"${this@toTableRow}" }
            }
        }
