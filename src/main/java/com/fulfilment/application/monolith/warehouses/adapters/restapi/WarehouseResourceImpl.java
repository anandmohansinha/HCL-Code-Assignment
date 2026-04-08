package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.Warehouse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import java.math.BigInteger;
import java.util.List;
import org.jboss.logging.Logger;

@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  private static final Logger LOGGER = Logger.getLogger(WarehouseResourceImpl.class);

  @Inject private WarehouseRepository warehouseRepository;
  @Inject private CreateWarehouseOperation createWarehouseOperation;
  @Inject private ArchiveWarehouseOperation archiveWarehouseOperation;
  @Inject private ReplaceWarehouseOperation replaceWarehouseOperation;

  @Override
  public List<Warehouse> listAllWarehousesUnits() {
    LOGGER.debug("Listing all warehouse units");
    return warehouseRepository.getAll().stream().map(this::toWarehouseResponse).toList();
  }

  @Override
  public List<Warehouse> searchWarehouses(
      String location,
      BigInteger minCapacity,
      BigInteger maxCapacity,
      String sortBy,
      String sortOrder,
      BigInteger page,
      BigInteger pageSize) {
    String effectiveSortBy = sortBy != null ? sortBy : "createdAt";
    String effectiveSortOrder = sortOrder != null ? sortOrder : "asc";
    int effectivePage = page != null ? page.intValueExact() : 0;
    int effectivePageSize = pageSize != null ? pageSize.intValueExact() : 10;
    Integer effectiveMinCapacity = minCapacity != null ? minCapacity.intValueExact() : null;
    Integer effectiveMaxCapacity = maxCapacity != null ? maxCapacity.intValueExact() : null;

    LOGGER.infof(
        "Searching warehouses with location=%s, minCapacity=%s, maxCapacity=%s, sortBy=%s, sortOrder=%s, page=%d, pageSize=%d",
        location,
        effectiveMinCapacity,
        effectiveMaxCapacity,
        effectiveSortBy,
        effectiveSortOrder,
        effectivePage,
        effectivePageSize);

    if (!"createdAt".equals(effectiveSortBy) && !"capacity".equals(effectiveSortBy)) {
      throw new WebApplicationException("sortBy must be either 'createdAt' or 'capacity'", 400);
    }

    if (!"asc".equalsIgnoreCase(effectiveSortOrder) && !"desc".equalsIgnoreCase(effectiveSortOrder)) {
      throw new WebApplicationException("sortOrder must be either 'asc' or 'desc'", 400);
    }

    if (effectivePage < 0) {
      throw new WebApplicationException("page must be >= 0", 400);
    }

    if (effectivePageSize < 1 || effectivePageSize > 100) {
      throw new WebApplicationException("pageSize must be between 1 and 100", 400);
    }

    if (effectiveMinCapacity != null
        && effectiveMaxCapacity != null
        && effectiveMinCapacity > effectiveMaxCapacity) {
      throw new WebApplicationException("minCapacity must be <= maxCapacity", 400);
    }

    return warehouseRepository.search(
            location,
            effectiveMinCapacity,
            effectiveMaxCapacity,
            effectiveSortBy,
            effectiveSortOrder,
            effectivePage,
            effectivePageSize)
        .stream()
        .map(this::toWarehouseResponse)
        .toList();
  }

  @Override
  @Transactional
  public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    // Convert API model to domain model
    var domainWarehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    domainWarehouse.businessUnitCode = data.getBusinessUnitCode();
    domainWarehouse.location = data.getLocation();
    domainWarehouse.capacity = data.getCapacity();
    domainWarehouse.stock = data.getStock() != null ? data.getStock() : 0;

    try {
      // Create warehouse through use case (includes validations)
      createWarehouseOperation.create(domainWarehouse);
      LOGGER.infof(
          "Created warehouse businessUnitCode=%s at location=%s with capacity=%d and stock=%d",
          domainWarehouse.businessUnitCode,
          domainWarehouse.location,
          domainWarehouse.capacity,
          domainWarehouse.stock);
      
      // Return the created warehouse
      return toWarehouseResponse(domainWarehouse);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    }
  }

  @Override
  public Warehouse getAWarehouseUnitByID(String id) {
    // Find warehouse by business unit code
    LOGGER.debugf("Fetching warehouse businessUnitCode=%s", id);
    var domainWarehouse = warehouseRepository.findByBusinessUnitCode(id);
    
    if (domainWarehouse == null) {
      throw new WebApplicationException("Warehouse with business unit code '" + id + "' not found", 404);
    }
    
    return toWarehouseResponse(domainWarehouse);
  }

  @Override
  @Transactional
  public void archiveAWarehouseUnitByID(String id) {
    // Find warehouse by business unit code
    var domainWarehouse = warehouseRepository.findByBusinessUnitCode(id);

    if (domainWarehouse == null) {
      throw new WebApplicationException("Warehouse with business unit code '" + id + "' not found", 404);
    }

    try {
      // Archive warehouse through use case (includes validations)
      archiveWarehouseOperation.archive(domainWarehouse);
      LOGGER.infof("Archived warehouse businessUnitCode=%s", id);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    }
  }

  @Override
  @Transactional
  public Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull Warehouse data) {
    // Convert API model to domain model
    var domainWarehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    domainWarehouse.businessUnitCode = businessUnitCode; // Use businessUnitCode from path
    domainWarehouse.location = data.getLocation();
    domainWarehouse.capacity = data.getCapacity();
    domainWarehouse.stock = data.getStock() != null ? data.getStock() : 0;

    try {
      // Replace warehouse through use case (includes validations)
      replaceWarehouseOperation.replace(domainWarehouse);

      // Return the updated warehouse
      var updated = warehouseRepository.findByBusinessUnitCode(businessUnitCode);
      LOGGER.infof(
          "Replaced warehouse businessUnitCode=%s with location=%s, capacity=%d, stock=%d",
          businessUnitCode,
          updated.location,
          updated.capacity,
          updated.stock);
      return toWarehouseResponse(updated);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e.getMessage(), 400);
    }
  }

  private Warehouse toWarehouseResponse(
      com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
    var response = new Warehouse();
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);

    return response;
  }
}
