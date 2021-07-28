package nl.knaw.huygens.hypercollate.dropwizard.health;

/*
 * #%L
 * hyper-collate-server
 * =======
 * Copyright (C) 2017 - 2021 Huygens ING (KNAW)
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
import nl.knaw.huygens.hypercollate.dropwizard.ServerConfiguration;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class DotHealthCheck extends HealthCheck {

  private final String dotPath;

  public DotHealthCheck(ServerConfiguration config) {
    dotPath = config.getPathToDotExecutable();
  }

  @Override
  protected Result check() throws Exception {
    if (dotPath == null) {
      return Result.healthy();
    }
    File dotFile = new File(dotPath);
    if (!dotFile.exists()) {
      return Result.unhealthy("%s not found.", dotPath);
    }
    if (!(dotFile.isFile() && dotFile.canExecute())) {
      return Result.unhealthy("Cannot execute %s", dotPath);
    }
    final Process dotProc = new ProcessBuilder(dotPath, "-V").start();
    boolean terminated = dotProc.waitFor(2, TimeUnit.SECONDS);
    if (!terminated) {
      return Result.unhealthy("%s -V timed out.", dotPath);
    }

    return Result.healthy();
  }
}
