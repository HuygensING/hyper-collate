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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Files

class CachedCollationStoreTest {
    private var config = TestConfiguration()

    class TestConfiguration : HyperCollateConfiguration {
        override fun getBaseURI(): String = "https://test.hypercollate.com"

        override fun getProjectDir(): File = File(System.getProperty("java.io.tmpdir") + "/.hypercollate")

        override fun getCollationsDir(): File = File(projectDir, "collations")

        override fun hasPathToDotExecutable(): Boolean = false

        override fun getPathToDotExecutable(): String = ""

        init {
            try {
                Files.createDirectories(collationsDir.toPath())
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }

    @BeforeEach
    fun before() {
        assertThat(config.collationsDir).isDirectory
    }

    @AfterEach
    @Throws(IOException::class)
    fun after() {
        FileUtils.deleteDirectory(config.projectDir)
    }

    @Test
    fun testAddSampleCollations() {
        val store = CachedCollationStore(config)
        val collationIds = store.collationIds
        LOG.info("collationIds={}", collationIds)
        assertThat(collationIds).isNotEmpty
        assertThat(collationIds).allSatisfy { id: String -> id.startsWith("sample-") }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(CachedCollationStore::class.java)
    }
}
