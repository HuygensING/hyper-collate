package nl.knaw.huygens.hypercollate;

/*-
 * #%L
 * hyper-collate-war
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

import io.federecio.dropwizard.swagger.SwaggerOAuth2Configuration;
import io.federecio.dropwizard.swagger.SwaggerResource;
import io.federecio.dropwizard.swagger.SwaggerViewConfiguration;
import nl.knaw.huygens.hypercollate.dropwizard.ServerConfiguration;
import nl.knaw.huygens.hypercollate.dropwizard.api.CollationStore;
import nl.knaw.huygens.hypercollate.dropwizard.db.CachedCollationStore;
import nl.knaw.huygens.hypercollate.dropwizard.resources.*;

import javax.ws.rs.core.Application;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HyperCollateApplication extends Application {
  private static final Logger LOG = Logger.getLogger(HyperCollateApplication.class.getName());

  @Override
  public Set<Object> getSingletons() {
    ServerConfiguration configuration = getServerConfiguration();

    Set<Object> singletons = new HashSet<>();
    singletons.add(new HomePageResource());
    singletons.add(new AboutResource(configuration, "HyperCollate Server"));
    CollationStore collationStore = new CachedCollationStore(configuration);
    singletons.add(new CollationsResource(configuration, collationStore));
    singletons.add(new XMLStreamExceptionMapper());
    singletons.add(new RuntimeExceptionMapper());
//    SwaggerResource swaggerResource = getSwaggerResource(configuration);
//    singletons.add(swaggerResource);
    return singletons;
  }

  private ServerConfiguration getServerConfiguration() {
    ServerConfiguration configuration = new ServerConfiguration();
    configuration.setBaseURI("");
    configuration.setPathToDotExecutable(detectDotPath());
    return configuration;
  }

  private SwaggerResource getSwaggerResource(ServerConfiguration configuration) {
    SwaggerOAuth2Configuration oAuth2Configuration = null;
    SwaggerViewConfiguration swaggerViewConfiguration = new SwaggerViewConfiguration();
    swaggerViewConfiguration.setTemplateUrl(configuration.getBaseURI() + "swagger.json");
    return new SwaggerResource("/", swaggerViewConfiguration, oAuth2Configuration);
  }

  private static String detectDotPath() {
    for (String detectionCommand : new String[]{"which dot", "where dot.exe"}) {
      try {
        final Process process = Runtime.getRuntime().exec(detectionCommand);
        try (BufferedReader processReader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
          final CompletableFuture<Optional<String>> path = CompletableFuture.supplyAsync(() -> processReader.lines()
              .map(String::trim)
              .filter(l -> l.toLowerCase().contains("dot"))
              .findFirst());
          process.waitFor();
          final String dotPath = path.get().get();
          LOG.info(() -> "Detected GraphViz' dot at '" + dotPath + "'");
          return dotPath;
        }
      } catch (Throwable t) {
        LOG.log(Level.FINE, detectionCommand, t);
      }
    }
    return null;
  }

}
