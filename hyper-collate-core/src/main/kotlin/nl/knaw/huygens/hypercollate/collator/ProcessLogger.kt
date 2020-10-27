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
//        attribute("border", 1)
        val colspan = attribute("colspan", 2)
        "tr" {
            "td" {
                colspan
                -"collation took $durationInMilliSeconds ms"
            }
        }
        "tr" {
            "td" {
                colspan
                -"collation steps:"
            }
        }
        "tr" {
            "td" {
                colspan
                -"* initializing $startSigil as variantGraph"
            }
        }
        collationSteps.forEach { it.toTableRows()() }
    }.toString(prettyFormat = true)
}

data class CollationStep(val sigil: String) {
    val processSteps: MutableList<ProcessStep> = mutableListOf()
}

typealias NodeBlock = Node.() -> Unit

private fun CollationStep.toTableRows(): NodeBlock =
        {
            "tr" {
                "td" {
                    attribute("colspan", 2)
                    -"* collating $sigil with variantGraph"
                }
            }
            "tr" {
                "td"{ attribute("width", "10%") }
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
