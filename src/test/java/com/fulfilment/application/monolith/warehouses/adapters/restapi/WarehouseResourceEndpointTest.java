package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class WarehouseResourceEndpointTest {

  @Inject EntityManager em;
  @Inject WarehouseRepository warehouseRepository;

  @BeforeEach
  @Transactional
  public void setup() {
    em.createQuery("DELETE FROM DbWarehouse").executeUpdate();

    persistWarehouse("MWH.001", "AMSTERDAM-001", 50, 10, LocalDateTime.of(2024, 1, 1, 10, 0), null);
    persistWarehouse("MWH.002", "TILBURG-001", 30, 5, LocalDateTime.of(2024, 1, 2, 10, 0), null);
  }

  @Test
  public void testListWarehouses() {
    given()
        .when()
        .get("/warehouse")
        .then()
        .statusCode(200)
        .body("$", hasSize(2));
  }

  @Test
  public void testGetWarehouseByBusinessUnitCode() {
    given()
        .when()
        .get("/warehouse/MWH.001")
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo("MWH.001"))
        .body("location", equalTo("AMSTERDAM-001"))
        .body("capacity", equalTo(50))
        .body("stock", equalTo(10));
  }

  @Test
  public void testGetMissingWarehouseReturns404() {
    given()
        .when()
        .get("/warehouse/MISSING")
        .then()
        .statusCode(404)
        .body("code", equalTo(404));
  }

  @Test
  public void testCreateWarehouse() {
    given()
        .contentType("application/json")
        .body(
            """
            {
              "businessUnitCode": "MWH.003",
              "location": "AMSTERDAM-002",
              "capacity": 60,
              "stock": 12
            }
            """)
        .when()
        .post("/warehouse")
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo("MWH.003"))
        .body("location", equalTo("AMSTERDAM-002"))
        .body("stock", equalTo(12));
  }

  @Test
  public void testCreateWarehouseDefaultsMissingStockToZero() {
    given()
        .contentType("application/json")
        .body(
            """
            {
              "businessUnitCode": "MWH.005",
              "location": "AMSTERDAM-002",
              "capacity": 60
            }
            """)
        .when()
        .post("/warehouse")
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo("MWH.005"))
        .body("stock", equalTo(0));
  }

  @Test
  public void testCreateWarehouseRejectsInvalidRequest() {
    given()
        .contentType("application/json")
        .body(
            """
            {
              "businessUnitCode": "MWH.004",
              "location": "UNKNOWN",
              "capacity": 20,
              "stock": 2
            }
            """)
        .when()
        .post("/warehouse")
        .then()
        .statusCode(400)
        .body("code", equalTo(400));
  }

  @Test
  public void testArchiveWarehouseSetsArchivedAt() {
    given()
        .when()
        .delete("/warehouse/MWH.001")
        .then()
        .statusCode(204);

    var archived = warehouseRepository.findByBusinessUnitCode("MWH.001");
    org.junit.jupiter.api.Assertions.assertNotNull(archived.archivedAt);
  }

  @Test
  public void testArchiveMissingWarehouseReturns404() {
    given()
        .when()
        .delete("/warehouse/MISSING")
        .then()
        .statusCode(404)
        .body("code", equalTo(404));
  }

  @Test
  public void testArchiveAlreadyArchivedWarehouseReturns400() {
    given().when().delete("/warehouse/MWH.001").then().statusCode(204);

    given()
        .when()
        .delete("/warehouse/MWH.001")
        .then()
        .statusCode(400)
        .body("code", equalTo(400));
  }

  @Test
  public void testReplaceWarehouse() {
    given()
        .contentType("application/json")
        .body(
            """
            {
              "location": "AMSTERDAM-002",
              "capacity": 70,
              "stock": 15
            }
            """)
        .when()
        .post("/warehouse/MWH.001/replacement")
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo("MWH.001"))
        .body("location", equalTo("AMSTERDAM-002"))
        .body("capacity", equalTo(70))
        .body("stock", equalTo(15));
  }

  @Test
  public void testReplaceWarehouseDefaultsMissingStockToZero() {
    given()
        .contentType("application/json")
        .body(
            """
            {
              "location": "AMSTERDAM-002",
              "capacity": 70
            }
            """)
        .when()
        .post("/warehouse/MWH.001/replacement")
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo("MWH.001"))
        .body("stock", equalTo(0));
  }

  @Test
  public void testReplaceWarehouseRejectsInvalidCapacity() {
    given()
        .contentType("application/json")
        .body(
            """
            {
              "location": "TILBURG-001",
              "capacity": 100,
              "stock": 15
            }
            """)
        .when()
        .post("/warehouse/MWH.001/replacement")
        .then()
        .statusCode(400)
        .body("code", equalTo(400));
  }

  private void persistWarehouse(
      String businessUnitCode,
      String location,
      int capacity,
      int stock,
      LocalDateTime createdAt,
      LocalDateTime archivedAt) {
    DbWarehouse warehouse = new DbWarehouse();
    warehouse.businessUnitCode = businessUnitCode;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    warehouse.createdAt = createdAt;
    warehouse.archivedAt = archivedAt;
    em.persist(warehouse);
  }
}
