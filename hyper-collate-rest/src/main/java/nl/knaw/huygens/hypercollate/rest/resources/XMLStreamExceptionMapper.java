package nl.knaw.huygens.hypercollate.rest.resources;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.xml.stream.XMLStreamException;

@Provider
public class XMLStreamExceptionMapper implements ExceptionMapper<XMLStreamException> {

  @Override
  public Response toResponse(XMLStreamException exception) {
    return Response.status(Status.BAD_REQUEST)//
        .entity(exception.getMessage())//
        .type(MediaType.TEXT_PLAIN)//
        .build();
  }

}
