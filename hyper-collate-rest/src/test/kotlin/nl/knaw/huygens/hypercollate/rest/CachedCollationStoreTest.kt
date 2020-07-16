package nl.knaw.huygens.hypercollate.rest


/*-
 * #%L
 * hyper-collate-rest
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

import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Files

class CachedCollationStoreTest {
    private var config = TestConfiguration()

    class TestConfiguration : HyperCollateConfiguration {
        override fun getBaseURI(): String {
            return "https://test.hypercollate.com"
        }

        override fun getProjectDir(): File {
            return File(System.getProperty("java.io.tmpdir") + "/.hypercollate")
        }

        override fun getCollationsDir(): File {
            return File(projectDir, "collations")
        }

        override fun hasPathToDotExecutable(): Boolean {
            return false
        }

        override fun getPathToDotExecutable(): String {
            return ""
        }

        init {
            try {
                Files.createDirectories(collationsDir.toPath())
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }

    @Before
    fun before() {
        Assertions.assertThat(config.collationsDir).isDirectory()
    }

    @After
    @Throws(IOException::class)
    fun after() {
        FileUtils.deleteDirectory(config.projectDir)
    }

    @Test
    fun testAddSampleCollations() {
        val store = CachedCollationStore(config)
        val collationIds = store.collationIds
        LOG.info("collationIds={}", collationIds)
        Assertions.assertThat(collationIds).isNotEmpty
        Assertions.assertThat(collationIds).allSatisfy { id: String -> id.startsWith("sample-") }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(CachedCollationStore::class.java)
    }
}
