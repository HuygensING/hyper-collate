package nl.knaw.huygens.hypercollate.rest;

/*-
 * #%L
 * hyper-collate-rest
 * =======
 * Copyright (C) 2017 - 2019 Huygens ING (KNAW)
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
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class CachedCollationStoreTest {

  private static final Logger LOG = LoggerFactory.getLogger(CachedCollationStore.class);
  TestConfiguration config = new TestConfiguration();

  static class TestConfiguration implements HyperCollateConfiguration {
    TestConfiguration() {
      try {
        Files.createDirectories(getCollationsDir().toPath());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public String getBaseURI() {
      return "https://test.hypercollate.com";
    }

    @Override
    public File getProjectDir() {
      return new File(System.getProperty("java.io.tmpdir") + "/.hypercollate");
    }

    @Override
    public File getCollationsDir() {
      return new File(getProjectDir(), "collations");
    }

    @Override
    public boolean hasPathToDotExecutable() {
      return false;
    }

    @Override
    public String getPathToDotExecutable() {
      return "";
    }
  }

  @Before
  public void before() {
    assertThat(config.getCollationsDir()).isDirectory();
  }

  @After
  public void after() throws IOException {
    FileUtils.deleteDirectory(config.getProjectDir());
  }

  @Test
  public void testAddSampleCollations() {
    CachedCollationStore store = new CachedCollationStore(config);
    Set<String> collationIds = store.getCollationIds();
    LOG.info("collationIds={}", collationIds);
    assertThat(collationIds).isNotEmpty();
    assertThat(collationIds).allSatisfy(id -> id.startsWith("sample-"));
  }

}
