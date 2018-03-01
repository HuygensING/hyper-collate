package nl.knaw.huygens.hypercollate.rest.resources;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class RuntimeExceptionMapper implements ExceptionMapper<RuntimeException> {

  @Override
  public Response toResponse(RuntimeException exception) {
    return Response.status(Status.BAD_REQUEST)//
        .entity(exception.getMessage())//
        .type(MediaType.TEXT_PLAIN)//
        .build();
  }

}
