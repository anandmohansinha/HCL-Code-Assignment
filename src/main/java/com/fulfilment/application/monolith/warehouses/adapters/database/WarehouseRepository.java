package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import org.jboss.logging.Logger;

@ApplicationScoped
@Transactional
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  private static final Logger LOGGER = Logger.getLogger(WarehouseRepository.class);

  @Override
  public List<Warehouse> getAll() {
    return this.listAll().stream().map(DbWarehouse::toWarehouse).toList();
  }

  public List<Warehouse> search(
      String location,
      Integer minCapacity,
      Integer maxCapacity,
      String sortBy,
      String sortOrder,
      int page,
      int pageSize) {
    StringBuilder query = new StringBuilder("archivedAt is null");
    Parameters parameters = new Parameters();

    if (location != null) {
      query.append(" and location = :location");
      parameters.and("location", location);
    }

    if (minCapacity != null) {
      query.append(" and capacity >= :minCapacity");
      parameters.and("minCapacity", minCapacity);
    }

    if (maxCapacity != null) {
      query.append(" and capacity <= :maxCapacity");
      parameters.and("maxCapacity", maxCapacity);
    }

    Sort.Direction direction = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.Descending
        : Sort.Direction.Ascending;

    List<Warehouse> results =
        find(query.toString(), Sort.by(sortBy, direction), parameters)
        .page(Page.of(page, pageSize))
        .list()
        .stream()
        .map(DbWarehouse::toWarehouse)
        .toList();

    LOGGER.debugf(
        "Warehouse search returned %d results for query='%s' on page=%d with pageSize=%d",
        results.size(),
        query,
        page,
        pageSize);

    return results;
  }

  @Override
  public void create(Warehouse warehouse) {
    DbWarehouse dbWarehouse = new DbWarehouse();
    dbWarehouse.businessUnitCode = warehouse.businessUnitCode;
    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.createdAt = warehouse.createdAt;
    dbWarehouse.archivedAt = warehouse.archivedAt;
    
    this.persist(dbWarehouse);
  }

  @Override
  public void update(Warehouse warehouse) {
    DbWarehouse dbWarehouse = find("businessUnitCode", warehouse.businessUnitCode).firstResult();
    if (dbWarehouse == null) {
      throw new IllegalArgumentException(
          "Warehouse with business unit code '" + warehouse.businessUnitCode + "' does not exist");
    }

    dbWarehouse.location = warehouse.location;
    dbWarehouse.capacity = warehouse.capacity;
    dbWarehouse.stock = warehouse.stock;
    dbWarehouse.archivedAt = warehouse.archivedAt;

    getEntityManager().flush();
    LOGGER.debugf("Updated warehouse businessUnitCode=%s", warehouse.businessUnitCode);
  }

  @Override
  public void remove(Warehouse warehouse) {
    DbWarehouse dbWarehouse = find("businessUnitCode", warehouse.businessUnitCode).firstResult();
    if (dbWarehouse == null) {
      throw new IllegalArgumentException(
          "Warehouse with business unit code '" + warehouse.businessUnitCode + "' does not exist");
    }

    if (dbWarehouse.archivedAt == null) {
      throw new IllegalArgumentException(
          "Warehouse with business unit code '" + warehouse.businessUnitCode
              + "' must be archived before removal");
    }

    delete(dbWarehouse);
    getEntityManager().flush();
    LOGGER.infof("Removed archived warehouse businessUnitCode=%s", warehouse.businessUnitCode);
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    DbWarehouse dbWarehouse = find("businessUnitCode", buCode).firstResult();
    return dbWarehouse != null ? dbWarehouse.toWarehouse() : null;
  }
}
