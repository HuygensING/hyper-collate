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
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Configuration;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.configuration.JsonConfigurationFactory;

import javax.validation.Validator;
import java.io.File;
import java.io.IOException;

public class BridgedConfigurationFactory<T extends Configuration> extends JsonConfigurationFactory<T> {
  private ConfigurationBridge<T> configurationBridge;

  public BridgedConfigurationFactory(ConfigurationBridge bridge,
                                     Class<T> klass,
                                     Validator validator,
                                     ObjectMapper objectMapper,
                                     String propertyPrefix) {
    super(klass, validator, objectMapper, propertyPrefix);
    configurationBridge = bridge;
  }

  @Override
  public T build(File file) throws IOException, ConfigurationException {
    T configuration = super.build(file);
    configurationBridge.load(configuration);
    return configuration;
  }

  @Override
  public T build(ConfigurationSourceProvider provider, String path) throws IOException, ConfigurationException {
    T configuration = super.build(provider, path);
    configurationBridge.load(configuration);
    return configuration;
  }

  @Override
  public T build() throws IOException, ConfigurationException {
    T configuration = super.build();
    configurationBridge.load(configuration);
    return configuration;
  }
}
