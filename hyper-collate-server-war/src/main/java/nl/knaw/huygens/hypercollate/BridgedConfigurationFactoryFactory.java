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
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Configuration;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.ConfigurationFactoryFactory;

import javax.validation.Validator;

public class BridgedConfigurationFactoryFactory<T extends Configuration> implements ConfigurationFactoryFactory<T> {
  private final ConfigurationBridge configurationBridge;

  public BridgedConfigurationFactoryFactory(ConfigurationBridge configurationBridge) {
    this.configurationBridge = configurationBridge;
  }

  @Override
  public ConfigurationFactory<T> create(Class<T> klass, Validator validator, ObjectMapper objectMapper, String propertyPrefix) {
    JsonFactory parserFactory = null;
    String formatName= "";
    return new BridgedConfigurationFactory<T>(configurationBridge, klass, validator, objectMapper, propertyPrefix);
  }
}
