package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class StoreEndpointTest {

  @Inject EntityManager em;

  private LegacyStoreManagerGateway legacyGateway;
  private Long storeId;

  @BeforeEach
  @Transactional
  public void setup() {
    legacyGateway = Mockito.mock(LegacyStoreManagerGateway.class);
    QuarkusMock.installMockForType(legacyGateway, LegacyStoreManagerGateway.class);

    em.createQuery("DELETE FROM Store").executeUpdate();

    Store store = new Store();
    store.name = "Primary Store";
    store.quantityProductsInStock = 10;
    em.persist(store);
    storeId = store.id;

    Store secondStore = new Store();
    secondStore.name = "Secondary Store";
    secondStore.quantityProductsInStock = 5;
    em.persist(secondStore);
  }

  @Test
  public void testListAndGetSingleStore() {
    given()
        .when()
        .get("/store")
        .then()
        .statusCode(200)
        .body("$", hasSize(2));

    given()
        .when()
        .get("/store/" + storeId)
        .then()
        .statusCode(200)
        .body("name", equalTo("Primary Store"))
        .body("quantityProductsInStock", equalTo(10));
  }

  @Test
  public void testGetMissingStoreReturns404() {
    given()
        .when()
        .get("/store/999999")
        .then()
        .statusCode(404)
        .body("code", equalTo(404));
  }

  @Test
  public void testCreateStoreRejectsPresetId() {
    given()
        .contentType("application/json")
        .body(
            """
            {
              "id": 123,
              "name": "Preset Id Store",
              "quantityProductsInStock": 4
            }
            """)
        .when()
        .post("/store")
        .then()
        .statusCode(422)
        .body("code", equalTo(422));
  }

  @Test
  public void testUpdateStoreAndDeleteMissingStore() {
    given()
        .contentType("application/json")
        .body(
            """
            {
              "name": "Updated Store",
              "quantityProductsInStock": 22
            }
            """)
        .when()
        .put("/store/" + storeId)
        .then()
        .statusCode(200)
        .body("name", equalTo("Updated Store"))
        .body("quantityProductsInStock", equalTo(22));

    verify(legacyGateway, times(1)).updateStoreOnLegacySystem(any(Store.class));

    given()
        .when()
        .delete("/store/999999")
        .then()
        .statusCode(404)
        .body("code", equalTo(404));
  }

  @Test
  public void testUpdateStoreRejectsMissingName() {
    given()
        .contentType("application/json")
        .body("{\"quantityProductsInStock\": 2}")
        .when()
        .put("/store/" + storeId)
        .then()
        .statusCode(422)
        .body("code", equalTo(422));
  }
}
