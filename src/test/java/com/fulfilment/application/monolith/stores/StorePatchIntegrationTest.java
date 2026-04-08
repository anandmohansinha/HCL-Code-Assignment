package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusMock;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class StorePatchIntegrationTest {

  LegacyStoreManagerGateway legacyGateway;

  private Long zeroStockStoreId;
  private Long namedStoreId;

  @BeforeEach
  @Transactional
  public void setup() {
    legacyGateway = Mockito.mock(LegacyStoreManagerGateway.class);
    QuarkusMock.installMockForType(legacyGateway, LegacyStoreManagerGateway.class);
    Store.deleteAll();

    Store zeroStockStore = new Store();
    zeroStockStore.name = "Zero Stock Store";
    zeroStockStore.quantityProductsInStock = 0;
    zeroStockStore.persist();
    zeroStockStoreId = zeroStockStore.id;

    Store namedStore = new Store();
    namedStore.name = "Original Store Name";
    namedStore.quantityProductsInStock = 7;
    namedStore.persist();
    namedStoreId = namedStore.id;
  }

  @Test
  public void testPatchAllowsStockOnlyUpdateWhenCurrentStockIsZero() {
    given()
        .contentType("application/json")
        .body("{\"quantityProductsInStock\": 5}")
        .when()
        .patch("/store/" + zeroStockStoreId)
        .then()
        .statusCode(200)
        .body("name", equalTo("Zero Stock Store"))
        .body("quantityProductsInStock", equalTo(5));

    verify(legacyGateway, times(1)).updateStoreOnLegacySystem(any(Store.class));
  }

  @Test
  public void testPatchAllowsNameOnlyUpdateWithoutChangingStock() {
    given()
        .contentType("application/json")
        .body("{\"name\": \"Renamed Store\"}")
        .when()
        .patch("/store/" + namedStoreId)
        .then()
        .statusCode(200)
        .body("name", equalTo("Renamed Store"))
        .body("quantityProductsInStock", equalTo(7));

    verify(legacyGateway, times(1)).updateStoreOnLegacySystem(any(Store.class));
  }
}
