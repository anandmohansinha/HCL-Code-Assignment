package com.fulfilment.application.monolith.stores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

public class StoreErrorMapperTest {

  @Test
  void testMapsWebApplicationExceptionWithMessage() {
    StoreResource.ErrorMapper mapper = new StoreResource.ErrorMapper();
    mapper.objectMapper = new ObjectMapper();

    Response response = mapper.toResponse(new WebApplicationException("Bad store request", 422));
    ObjectNode body = (ObjectNode) response.getEntity();

    assertEquals(422, response.getStatus());
    assertEquals(422, body.get("code").asInt());
    assertEquals("Bad store request", body.get("error").asText());
  }

  @Test
  void testMapsGenericExceptionWithoutMessageTo500() {
    StoreResource.ErrorMapper mapper = new StoreResource.ErrorMapper();
    mapper.objectMapper = new ObjectMapper();

    Response response = mapper.toResponse(new RuntimeException());
    ObjectNode body = (ObjectNode) response.getEntity();

    assertEquals(500, response.getStatus());
    assertEquals(500, body.get("code").asInt());
    assertFalse(body.has("error"));
  }
}
