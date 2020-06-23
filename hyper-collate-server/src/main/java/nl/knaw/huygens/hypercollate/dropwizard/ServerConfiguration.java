package nl.knaw.huygens.hypercollate.dropwizard;

/*-
 * #%L
 * hyper-collate-server
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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.logging.DefaultLoggingFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import nl.knaw.huygens.hypercollate.rest.HyperCollateConfiguration;
import nl.knaw.huygens.hypercollate.rest.Util;
import nl.knaw.huygens.hypercollate.rest.resources.AboutResource;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ServerConfiguration extends Configuration implements HyperCollateConfiguration {
  // By initializing the properties with default values, a config file is not necessary.

  private static final String HTTP_LOCALHOST = "http://localhost:";
  private String baseURI;

  private String pathToDotExecutable;

  private File projectDir;
  private File collationsDir;

  @JsonProperty("swagger")
  public SwaggerBundleConfiguration swaggerBundleConfiguration = new SwaggerBundleConfiguration();

  public ServerConfiguration() {
    super();
    setDefaults();
  }

  private void setDefaults() {
    pathToDotExecutable = Util.detectDotPath();

    String name = AboutResource.class.getPackage().getName();
    swaggerBundleConfiguration.setResourcePackage(name);

    int port = availablePort();
    baseURI = HTTP_LOCALHOST + port;

    DefaultServerFactory serverFactory = (DefaultServerFactory) getServerFactory();
    HttpConnectorFactory httpConnectorFactory = (HttpConnectorFactory) serverFactory.getApplicationConnectors().get(0);
    httpConnectorFactory.setPort(port);

    DefaultLoggingFactory loggingFactory = (DefaultLoggingFactory) getLoggingFactory();
    loggingFactory.setLevel("INFO");
  }

  public void setBaseURI(String baseURI) {
    this.baseURI = baseURI.replaceFirst("/$", "");
  }

  public String getBaseURI() {
    return baseURI;
  }

  public File getProjectDir() {
    checkProjectDirIsInitialized();
    return this.projectDir;
  }

  public File getCollationsDir() {
    checkProjectDirIsInitialized();
    return collationsDir;
  }

  public String getPathToDotExecutable() {
    return pathToDotExecutable;
  }

  public void setPathToDotExecutable(String pathToDotExecutable) {
    this.pathToDotExecutable = pathToDotExecutable;
  }

  public boolean hasPathToDotExecutable() {
    return pathToDotExecutable != null;
  }

  public void setProjectDir(String projectDir) {
    this.projectDir = new File(projectDir);
    collationsDir = new File(projectDir, "collations");
    try {
      Files.createDirectories(collationsDir.toPath());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void checkProjectDirIsInitialized() {
    if (this.projectDir == null) {
      setProjectDir(Paths.get(System.getProperty("user.home"), ".hypercollate").toString());
    }
  }

  private int availablePort() {
    try {
      ServerSocket s = new ServerSocket(0);
      int port = s.getLocalPort();
      s.close();
      return port;
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

}
