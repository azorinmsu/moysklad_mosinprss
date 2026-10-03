package ru.moysklad.intern.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "modifiers", indexes = @Index(columnList = "name"))
public class Modifier {
    public Modifier() {}

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    @Column(name = "name", nullable = false)
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Column(name = "article", nullable = false)
    private String article;
    public String getArticle() { return article; }
    public void setArticle(String article) { this.article = article; }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    @OneToMany(mappedBy = "modifier", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attribute> attributes = new ArrayList<>();
    public List<Attribute> getAttributes() { return attributes; }
    public void setAttributes(List<Attribute> attrs) { this.attributes = attrs; }

    public void addAttribute(Attribute attr) {
        attributes.add(attr);
        attr.setModifier(this);
    }
    public void removeAttribute(Attribute attr) {
        attributes.remove(attr);
        attr.setModifier(null);
    }
}
