package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ProductEndpointTest {

  @Inject EntityManager em;

  private Long productId;

  @BeforeEach
  @Transactional
  public void setup() {
    em.createQuery("DELETE FROM Product").executeUpdate();

    Product product = new Product();
    product.name = "TONSTAD";
    product.description = "Seed product";
    product.price = new BigDecimal("10.50");
    product.stock = 10;
    em.persist(product);
    productId = product.id;

    Product secondProduct = new Product();
    secondProduct.name = "KALLAX";
    secondProduct.description = "Second seed product";
    secondProduct.price = new BigDecimal("20.00");
    secondProduct.stock = 5;
    em.persist(secondProduct);
  }

  @Test
  public void testListProducts() {
    given()
        .when()
        .get("/product")
        .then()
        .statusCode(200)
        .body("$", hasSize(2));
  }

  @Test
  public void testGetSingleProduct() {
    given()
        .when()
        .get("/product/" + productId)
        .then()
        .statusCode(200)
        .body("name", equalTo("TONSTAD"))
        .body("description", equalTo("Seed product"))
        .body("price", notNullValue())
        .body("stock", equalTo(10));
  }

  @Test
  public void testGetMissingProductReturns404() {
    given()
        .when()
        .get("/product/999999")
        .then()
        .statusCode(404)
        .body("code", equalTo(404));
  }

  @Test
  public void testCreateProduct() {
    given()
        .contentType("application/json")
        .body(
            """
            {
              "name": "BESTA",
              "description": "New product",
              "price": 12.99,
              "stock": 3
            }
            """)
        .when()
        .post("/product")
        .then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("name", equalTo("BESTA"))
        .body("stock", equalTo(3));
  }

  @Test
  public void testCreateProductRejectsPresetId() {
    given()
        .contentType("application/json")
        .body(
            """
            {
              "id": 123,
              "name": "PRESET-ID",
              "stock": 1
            }
            """)
        .when()
        .post("/product")
        .then()
        .statusCode(422)
        .body("code", equalTo(422));
  }

  @Test
  public void testUpdateProduct() {
    given()
        .contentType("application/json")
        .body(
            """
            {
              "name": "TONSTAD UPDATED",
              "description": "Updated product",
              "price": 14.25,
              "stock": 17
            }
            """)
        .when()
        .put("/product/" + productId)
        .then()
        .statusCode(200)
        .body("name", equalTo("TONSTAD UPDATED"))
        .body("description", equalTo("Updated product"))
        .body("stock", equalTo(17));
  }

  @Test
  public void testUpdateProductRejectsMissingName() {
    given()
        .contentType("application/json")
        .body(
            """
            {
              "description": "Missing name",
              "price": 11.00,
              "stock": 4
            }
            """)
        .when()
        .put("/product/" + productId)
        .then()
        .statusCode(422)
        .body("code", equalTo(422));
  }

  @Test
  public void testDeleteProductAndMissingDeleteReturns404() {
    given()
        .when()
        .delete("/product/" + productId)
        .then()
        .statusCode(204);

    given()
        .when()
        .delete("/product/" + productId)
        .then()
        .statusCode(404)
        .body("code", equalTo(404));
  }
}
