/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.math.BigDecimal;

/**
 * Test entity with various @Column properties for M5-JP-31.
 */
@Entity
public class ColumnTestEntity {

    @Id
    private Long id;

    @Column(name = "full_name", length = 100, nullable = false)
    private String name;

    @Column(name = "description", length = 500, insertable = false)
    private String description;

    @Column(name = "amount", precision = 12, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "code", columnDefinition = "VARCHAR(20)")
    private String code;

    @Column(length = 50, insertable = true, updatable = true)
    private String label;

    private String plain;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getPlain() { return plain; }
    public void setPlain(String plain) { this.plain = plain; }
}