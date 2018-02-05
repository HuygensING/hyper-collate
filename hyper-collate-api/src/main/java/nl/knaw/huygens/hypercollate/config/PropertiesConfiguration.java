package nl.knaw.huygens.hypercollate.config;

/*-
 * #%L
 * HyperCollate API
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class PropertiesConfiguration {
  private static final Logger LOG = LoggerFactory.getLogger(PropertiesConfiguration.class);
  private PropertyResourceBundle propertyResourceBundle;

  public PropertiesConfiguration(String propertiesFile, boolean isResource) {
    try {
      InputStream inputStream = isResource //
          ? Thread.currentThread().getContextClassLoader().getResourceAsStream(propertiesFile)//
          : new FileInputStream(new File(propertiesFile));
      propertyResourceBundle = new PropertyResourceBundle(inputStream);
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException("Couldn't read properties file " + propertiesFile + ": " + e.getMessage());
    }
  }

  public synchronized Optional<String> getProperty(String key) {
    return Optional.ofNullable(getValue(key));
  }

  public synchronized String getProperty(String key, String defaultValue) {
    String value = getValue(key);
    return value != null ? value : defaultValue;
  }

  private String getValue(String key) {
    String value = null;
    try {
      value = propertyResourceBundle.getString(key);
    } catch (MissingResourceException e) {
      LOG.warn("Missing expected resource: [{}]", key);
    } catch (ClassCastException e) {
      LOG.warn("Property value for key [{}] cannot be transformed to String", key);
    }
    return value;
  }

  public List<String> getKeys() {
    return Collections.list(propertyResourceBundle.getKeys());
  }

}
