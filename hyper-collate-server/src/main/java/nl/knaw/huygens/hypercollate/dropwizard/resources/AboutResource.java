package nl.knaw.huygens.hypercollate.dropwizard.resources;

/*
 * #%L
 * hyper-collate-server
 * =======
 * Copyright (C) 2017 Huygens ING (KNAW)
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

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import nl.knaw.huygens.hypercollate.dropwizard.api.AboutInfo;
import nl.knaw.huygens.hypercollate.dropwizard.api.ResourcePaths;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.time.Instant;

@Api(ResourcePaths.ABOUT)
@Path(ResourcePaths.ABOUT)
@Produces(MediaType.APPLICATION_JSON)
public class AboutResource {

  private final AboutInfo about = new AboutInfo();

  public AboutResource() {
    this("appName");
  }

  public AboutResource(String appName) {
    about.setAppName(appName);
    about.setStartedAt(Instant.now().toString());
    about.setVersion("version");
  }

  @GET
  @Timed
  @ApiOperation(value = "Get some info about the server", response = AboutInfo.class)
  public AboutInfo getAbout() {
    return about;
  }
}
