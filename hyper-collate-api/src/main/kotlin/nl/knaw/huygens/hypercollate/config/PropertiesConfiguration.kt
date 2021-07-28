package nl.knaw.huygens.hypercollate.config

/*-
 * #%L
 * hyper-collate-api
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

import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*

class PropertiesConfiguration(propertiesFile: String, isResource: Boolean) {
    private var propertyResourceBundle: PropertyResourceBundle? = null

    @Synchronized
    fun getProperty(key: String): Optional<String> =
            Optional.ofNullable(getValue(key))

    @Synchronized
    fun getProperty(key: String, defaultValue: String): String =
            getValue(key) ?: defaultValue!!

    private fun getValue(key: String): String? {
        var value: String? = null
        try {
            value = propertyResourceBundle!!.getString(key)
        } catch (e: MissingResourceException) {
            LOG.warn("Missing expected resource: [{}]", key)
        } catch (e: ClassCastException) {
            LOG.warn("Property value for key [{}] cannot be transformed to String", key)
        }
        return value
    }

    val keys: List<String>
        get() = Collections.list(propertyResourceBundle!!.keys)

    companion object {
        private val LOG = LoggerFactory.getLogger(PropertiesConfiguration::class.java)
    }

    init {
        propertyResourceBundle = try {
            val inputStream = if (isResource) Thread.currentThread().contextClassLoader.getResourceAsStream(propertiesFile) else FileInputStream(File(propertiesFile))
            PropertyResourceBundle(inputStream)
        } catch (e: IOException) {
            e.printStackTrace()
            throw RuntimeException(
                    "Couldn't read properties file " + propertiesFile + ": " + e.message)
        }
    }
}
