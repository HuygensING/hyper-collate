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

import io.swagger.jaxrs.config.BeanConfig;
import nl.knaw.huygens.hypercollate.rest.CachedCollationStore;
import nl.knaw.huygens.hypercollate.rest.CollationStore;
import nl.knaw.huygens.hypercollate.rest.HyperCollateConfiguration;
import nl.knaw.huygens.hypercollate.rest.Util;
import nl.knaw.huygens.hypercollate.rest.resources.AboutResource;
import nl.knaw.huygens.hypercollate.rest.resources.CollationsResource;
import nl.knaw.huygens.hypercollate.rest.resources.RuntimeExceptionMapper;
import nl.knaw.huygens.hypercollate.rest.resources.XMLStreamExceptionMapper;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.core.Application;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public class HyperCollateApplication extends Application {
  private static final Logger LOG = Logger.getLogger(HyperCollateApplication.class.getName());
  private final HyperCollateConfiguration configuration;

  public HyperCollateApplication() {
    super();
    configuration = getServerConfiguration();
    BeanConfig beanConfig = new BeanConfig();
    beanConfig.setVersion("1.0");
    beanConfig.setSchemes(new String[]{"http"});
    beanConfig.setTitle("HyperCollate API");
    beanConfig.setBasePath("");
    beanConfig.setResourcePackage("nl.knaw.huygens.hypercollate.rest.resources");
    beanConfig.setScan(true);
  }

  @Override
  public Set<Class<?>> getClasses() {
    Set<Class<?>> set = new HashSet<>();
    set.add(io.swagger.jaxrs.listing.ApiListingResource.class);
    set.add(io.swagger.jaxrs.listing.SwaggerSerializers.class);
    return set;
  }

  @Override
  public Set<Object> getSingletons() {
    Set<Object> singletons = new HashSet<>();
    String[] strings = {"about", "collations", "swagger.json", "swagger.yaml"};
    List<String> endpoints = new ArrayList<>(asList(strings));
    List<String> rootEndpointURLs =
        endpoints.stream().map(e -> configuration.getBaseURI() + "/" + e).collect(toList());
    singletons.add(new SimpleHomePageResource(rootEndpointURLs));
    singletons.add(new AboutResource(configuration, "HyperCollate Server"));
    CollationStore collationStore = new CachedCollationStore(configuration);
    singletons.add(new CollationsResource(configuration, collationStore));
    singletons.add(new XMLStreamExceptionMapper());
    singletons.add(new RuntimeExceptionMapper());
    return singletons;
  }

  private HyperCollateConfiguration getServerConfiguration() {
    try {
      InitialContext initialContext = new InitialContext();
      String baseURI = (String) initialContext.lookup("java:comp/env/baseURI");
      String projectDir = (String) initialContext.lookup("java:comp/env/projectDir");
      return new SimpleConfiguration()
          .setBaseURI(baseURI)
          .setProjectDir(projectDir)
          .setPathToDotExecutable(Util.detectDotPath());

    } catch (NamingException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
}
