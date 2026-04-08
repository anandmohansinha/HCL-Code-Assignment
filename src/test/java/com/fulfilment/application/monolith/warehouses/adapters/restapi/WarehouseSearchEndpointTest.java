package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class WarehouseSearchEndpointTest {

  @Inject EntityManager em;

  @BeforeEach
  @Transactional
  void setup() {
    em.createQuery("DELETE FROM DbWarehouse").executeUpdate();

    persistWarehouse("SEARCH-AMS-LOW", "AMSTERDAM-001", 20, 5, LocalDateTime.of(2024, 1, 1, 10, 0), null);
    persistWarehouse("SEARCH-AMS-HIGH", "AMSTERDAM-001", 80, 10, LocalDateTime.of(2024, 1, 2, 10, 0), null);
    persistWarehouse("SEARCH-ZWO", "ZWOLLE-001", 30, 8, LocalDateTime.of(2024, 1, 3, 10, 0), null);
    persistWarehouse(
        "SEARCH-ARCHIVED",
        "AMSTERDAM-001",
        60,
        7,
        LocalDateTime.of(2024, 1, 4, 10, 0),
        LocalDateTime.of(2024, 2, 1, 10, 0));
  }

  @Test
  void testSearchFiltersExcludeArchivedAndApplyAndLogic() {
    given()
        .queryParam("location", "AMSTERDAM-001")
        .queryParam("minCapacity", 50)
        .when()
        .get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("$", hasSize(1))
        .body("[0].businessUnitCode", equalTo("SEARCH-AMS-HIGH"))
        .body("[0].location", equalTo("AMSTERDAM-001"))
        .body("[0].capacity", equalTo(80));
  }

  @Test
  void testSearchSupportsSortingAndPagination() {
    given()
        .queryParam("sortBy", "capacity")
        .queryParam("sortOrder", "desc")
        .queryParam("page", 0)
        .queryParam("pageSize", 2)
        .when()
        .get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("$", hasSize(2))
        .body("[0].businessUnitCode", equalTo("SEARCH-AMS-HIGH"))
        .body("[1].businessUnitCode", equalTo("SEARCH-ZWO"));

    given()
        .queryParam("sortBy", "capacity")
        .queryParam("sortOrder", "desc")
        .queryParam("page", 1)
        .queryParam("pageSize", 2)
        .when()
        .get("/warehouse/search")
        .then()
        .statusCode(200)
        .body("$", hasSize(1))
        .body("[0].businessUnitCode", equalTo("SEARCH-AMS-LOW"));
  }

  @Test
  void testSearchRejectsInvalidPageSize() {
    given()
        .queryParam("pageSize", 101)
        .when()
        .get("/warehouse/search")
        .then()
        .statusCode(400);
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
