package nl.knaw.huygens.hypercollate.dropwizard;

/*-
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

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import nl.knaw.huygens.hypercollate.dropwizard.api.CollationStore;
import nl.knaw.huygens.hypercollate.dropwizard.db.CachedCollationStore;
import nl.knaw.huygens.hypercollate.dropwizard.health.DotHealthCheck;
import nl.knaw.huygens.hypercollate.dropwizard.health.ServerHealthCheck;
import nl.knaw.huygens.hypercollate.dropwizard.resources.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicBoolean;

class ServerApplication extends Application<ServerConfiguration> {
  private final Logger LOG = LoggerFactory.getLogger(getClass());

  public static void main(final String[] args) throws Exception {
    new ServerApplication().run(args);
  }

  @Override
  public String getName() {
    return "HyperCollate Server";
  }

  @Override
  public void initialize(final Bootstrap<ServerConfiguration> bootstrap) {
    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(//
        new SubstitutingSourceProvider(//
            bootstrap.getConfigurationSourceProvider(), //
            new EnvironmentVariableSubstitutor()));
    bootstrap.addBundle(new SwaggerBundle<ServerConfiguration>() {
      @Override
      protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(ServerConfiguration configuration) {
        return configuration.swaggerBundleConfiguration;
      }
    });
  }

  @Override
  public void run(final ServerConfiguration configuration, final Environment environment) {
    environment.jersey().register(new HomePageResource());
    environment.jersey().register(new AboutResource(configuration, getName()));
    CollationStore collationStore = new CachedCollationStore(configuration);
    environment.jersey().register(new CollationsResource(configuration, collationStore));
    environment.jersey().register(new XMLStreamExceptionMapper());
    environment.jersey().register(new RuntimeExceptionMapper());

    environment.healthChecks().register("server", new ServerHealthCheck());
    environment.healthChecks().register("dot", new DotHealthCheck(configuration));

    SortedMap<String, HealthCheck.Result> results = environment.healthChecks().runHealthChecks();
    AtomicBoolean healthy = new AtomicBoolean(true);
    LOG.info("Health checks:");
    results.forEach((name, result) -> {
      LOG.info("{}: {}, message='{}'",//
          name,//
          result.isHealthy() ? "healthy" : "unhealthy",//
          StringUtils.defaultIfBlank(result.getMessage(), ""));
      healthy.set(healthy.get() && result.isHealthy());
    });
    if (!healthy.get()) {
      throw new RuntimeException("Failing health check(s)");
    }

  }

}
