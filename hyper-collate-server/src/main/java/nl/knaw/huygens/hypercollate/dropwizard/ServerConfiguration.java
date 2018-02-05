package nl.knaw.huygens.hypercollate.dropwizard;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import org.hibernate.validator.constraints.NotEmpty;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/*
 * #%L
 * HyperCollate server
 * =======
 * Copyright (C) 2017 - 2018 Huygens ING (KNAW)
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

public class ServerConfiguration extends Configuration {
  @NotEmpty
  private String baseURI;

  private final File projectDir;
  private final File collationsDir;

  ServerConfiguration() {
    super();
    projectDir = Paths.get(System.getProperty("user.home"), ".hypercollate").toFile();
    collationsDir = new File(projectDir,"collations");
    try {
      Files.createDirectories(collationsDir.toPath());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void setBaseURI(String baseURI) {
    this.baseURI = baseURI.replaceFirst("/$", "");
  }

  public String getBaseURI() {
    return baseURI;
  }

  @JsonProperty("swagger")
  public SwaggerBundleConfiguration swaggerBundleConfiguration;

  public File getProjectDir() {
    return this.projectDir;
  }

  public File getCollationsDir() {
    return collationsDir;
  }
}
