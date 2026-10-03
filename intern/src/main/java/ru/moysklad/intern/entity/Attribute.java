package ru.moysklad.intern.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "attributes")
public class Attribute {
    // its a table with key-values pairs
    public Attribute() {}

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    @Column(name = "name", nullable = false) // cannot be null means required for db
    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Column(name = "value", nullable = true)
    private String value;
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modifier_id")
    private Modifier modifier;
    public Modifier getModifier() { return modifier; }
    public void setModifier(Modifier modifier) { this.modifier = modifier; }
}
