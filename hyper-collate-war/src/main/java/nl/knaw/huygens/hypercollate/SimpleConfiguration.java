package nl.knaw.huygens.hypercollate;

/*-
 * #%L
 * hyper-collate-war
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

import nl.knaw.huygens.hypercollate.rest.HyperCollateConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SimpleConfiguration implements HyperCollateConfiguration {
  private String baseURI;
  private File projectDir;
  private File collationsDir;
  private String pathToDotExecutable;

  @Override
  public String getBaseURI() {
    return baseURI;
  }

  @Override
  public File getProjectDir() {
    checkProjectDirIsInitialized();
    return projectDir;
  }

  @Override
  public File getCollationsDir() {
    return collationsDir;
  }

  @Override
  public boolean hasPathToDotExecutable() {
    return pathToDotExecutable != null;
  }

  @Override
  public String getPathToDotExecutable() {
    return pathToDotExecutable;
  }

  public SimpleConfiguration setBaseURI(String baseURI) {
    this.baseURI = baseURI;
    return this;
  }

  public SimpleConfiguration setProjectDir(String projectDir) {
    this.projectDir = new File(projectDir);
    collationsDir = new File(projectDir, "collations");
    try {
      Files.createDirectories(collationsDir.toPath());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return this;
  }

  public SimpleConfiguration setPathToDotExecutable(String pathToDotExecutable) {
    this.pathToDotExecutable = pathToDotExecutable;
    return this;
  }

  private void checkProjectDirIsInitialized() {
    if (this.projectDir == null) {
      setProjectDir(Paths.get(System.getProperty("user.home"), ".hypercollate").toString());
    }
  }
}
