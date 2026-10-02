package ru.moysklad.intern.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    @Column(name = "name", nullable = false) // cannot be null means required for db
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Column(name = "description", nullable = true)
    private String description;

    // unit of measurement
    @Column(name = "uom")
    private String uomId = "BASE";

    // TODO: узнать насчет того, как подтягивать величины

    // пример: 10.000.000.000.000.000,00 - то есть
    // десять квадраллионов и 0 копеек
    // в 10 раз меньше, чем долг гугла перед Россией
    @Column(name = "price", precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "archived")
    private Boolean archived = false; // as default
}
