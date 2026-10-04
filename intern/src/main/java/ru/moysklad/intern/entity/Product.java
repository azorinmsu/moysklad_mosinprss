package ru.moysklad.intern.entity;

import jakarta.persistence.*;
import ru.moysklad.intern.entity.meta.Currency;
import ru.moysklad.intern.entity.meta.UnitOfMeasurement;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products", indexes = @Index(columnList = "name"))
public class Product {
    public Product() {}

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
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // unit of measurement
    // на будущее как парсить json-объект:
    // ID: obj["rows"][INDEX]["id"] | имя: obj["rows"][INDEX]["name"]
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uom_id")
    private UnitOfMeasurement uom;
    public UnitOfMeasurement getUom() { return uom; }
    public void setUom(UnitOfMeasurement uom) { this.uom = uom; }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id")
    private Currency currency;
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }

    // пример: 10.000.000.000.000.000,00 - то есть
    // десять квадраллионов и 0 копеек
    @Column(name = "price", precision = 19, scale = 2)
    private BigDecimal price;
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; } // note: строку цены обработать в DTO и перенести его в BD формат

    @Column(name = "archived")
    private boolean archived = false; // as default
    public boolean getArchived() { return archived; }
    public void setArchived(boolean status) { this.archived = status; }

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Modifier> modifiers = new ArrayList<>();
    // honestly idk is there a reason to use them there?
    public List<Modifier> getModifiers() { return modifiers; }
    public void setModifiers(List<Modifier> list) { this.modifiers = list; }

    // additional methods for connecting products and modifiers
    public void addModifier(Modifier modifier) {
        modifiers.add(modifier);
        modifier.setProduct(this);
    }
    public void removeModifier(Modifier modifier) {
        modifiers.remove(modifier);
        modifier.setProduct(null);
    }

    // Maybe I will do logic for updating modifiers. Maybe.
}
