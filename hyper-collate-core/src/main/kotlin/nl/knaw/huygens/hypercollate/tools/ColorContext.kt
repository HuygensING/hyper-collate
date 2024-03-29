package nl.knaw.huygens.hypercollate.tools

/*-
 * #%L
 * hyper-collate-core
 * =======
 * Copyright (C) 2017 - 2021 Huygens ING (KNAW)
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

class ColorContext {
    private val assignedColors: MutableMap<String, String> = HashMap()
    private val colors = arrayOf(
            "yellow",
            "orange",
            "#9aed7d",
            "lightblue",
            "grey",
            "#4286f4",
            "#ef10eb",
            "#9091D4",
            "#B190D4",
            "#B3D490",
            "#D49091",
            "#E62023",
            "#86E620",
            "#8020E6",
            "#20E6E3"
    )
    private var colorIndex = 0

    fun colorFor(tagName: String): String? {
        if (assignedColors.containsKey(tagName)) {
            return assignedColors[tagName]
        }
        val color = colors[colorIndex]
        assignedColors[tagName] = color
        colorIndex++
        if (colorIndex > colors.size - 1) {
            colorIndex = 0
        }
        return color
    }
}
