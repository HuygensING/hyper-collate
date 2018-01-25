package nl.knaw.huygens.hypercollate;

/*-
 * #%L
 * hyper-collate-server-war
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
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import nl.knaw.huygens.hypercollate.dropwizard.ServerApplication;
import nl.knaw.huygens.hypercollate.dropwizard.ServerConfiguration;
import org.eclipse.jetty.server.Server;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

// based on wizard-in-a-box
public class HyperCollateWebApplication extends Application<ServerConfiguration> implements ServletContextListener {
  private static ServletContext theServletContext;
  private final Application<ServerConfiguration> dropwizardApplication = new ServerApplication();
  private final String[] args;
  private Environment dropwizardEnvironment;
  private ConfigurationBridge configurationBridge;

  public HyperCollateWebApplication(String configurationFileLocation) {
    this(new String[]{"server", configurationFileLocation});
  }

  public HyperCollateWebApplication(String[] args) {
    this.args = args;
  }

  public void setConfigurationBridge(ConfigurationBridge configurationBridge) {
    this.configurationBridge = configurationBridge;
  }

  public ConfigurationBridge getConfigurationBridge() {
    return configurationBridge;
  }

  @Override
  public void initialize(Bootstrap<ServerConfiguration> bootstrap) {
    if (configurationBridge != null) {
      bootstrap.setConfigurationFactoryFactory(new BridgedConfigurationFactoryFactory<ServerConfiguration>(configurationBridge));
    }
    // Swaps the default FileConfigurationSourceProvider
    bootstrap.setConfigurationSourceProvider(new ClasspathConfigurationSourceProvider());
    dropwizardApplication.initialize(bootstrap);
  }

  @Override
  public String getName() {
    return dropwizardApplication.getName() + "-war";
  }

  @Override
  public void run(ServerConfiguration configuration, Environment environment) throws Exception {
    dropwizardEnvironment = environment;
    dropwizardApplication.run(configuration, environment);
  }

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    if (theServletContext != null) {
      throw new IllegalStateException("Multiple WebListeners extending WebApplication detected. Only one is allowed!");
    }
    theServletContext = sce.getServletContext();
    try {
      run(args);
    } catch (Exception e) {
      throw new RuntimeException("Initialization of Dropwizard failed ...", e);
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    Server server = (Server) theServletContext.getAttribute("fakeJettyServer");
    if (server != null) {
      try {
        server.stop();
      } catch (Exception e) {
        throw new RuntimeException("Shutdown of Dropwizard failed ...", e);
      }
    }
    theServletContext = null;
  }
}
