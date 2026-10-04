package ru.moysklad.intern.entity.meta;

import jakarta.persistence.*;
import ru.moysklad.intern.entity.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "currency")
public class Currency {
    private UUID id;
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    @Column(name = "name", nullable = false)
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // даже если тип удален, нужно ОСТАВИТЬ сам товар
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = false)
    // todo: доделать завтра все связи и смержить в бд
    private List<Product> products = new ArrayList<>();
    public List<Product> getProducts() { return products; }
    public void setProducts(List<Product> products) { this.products = products; }

    // для сверки с api МоегоСклада
    @Column(name = "external_id", nullable = false)
    private UUID externalId;
    public UUID getExternalId() { return externalId; }
    public void setExternalId(UUID externalId) { this.externalId = externalId; }
}
