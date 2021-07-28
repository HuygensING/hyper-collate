package nl.knaw.huygens.hypercollate.rest.resources;

/*
 * #%L
 * hyper-collate-rest
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

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;

@Api("/")
@Path("/")
public class HomePageResource {

  /**
   * Shows the homepage for the backend
   *
   * @return HTML representation of the homepage
   */
  @GET
  @Timed
  @Produces(MediaType.TEXT_HTML)
  @ApiOperation(value = "Show the server homepage")
  public Response getHomePage() {
    InputStream resourceAsStream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("index.html");
    return Response.ok(resourceAsStream)
        .header("Pragma", "public")
        .header("Cache-Control", "public")
        .build();
  }

  @GET
  @Path("favicon.ico")
  @ApiOperation(value = "Placeholder for favicon.ico")
  public Response getFavIcon() {
    return Response.noContent().build();
  }

  @GET
  @Path("robots.txt")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Placeholder for robots.txt")
  public String noRobots() {
    return "User-agent: *\nDisallow: /\n";
  }
}
