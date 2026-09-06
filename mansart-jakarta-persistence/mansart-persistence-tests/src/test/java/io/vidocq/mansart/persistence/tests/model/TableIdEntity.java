package io.vidocq.mansart.persistence.tests.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

@Entity
@Table(name = "table_id_entities")
@TableGenerator(name = "tblGen", table = "id_gen",
    pkColumnName = "GEN_NAME", valueColumnName = "GEN_VAL",
    pkColumnValue = "tblGen", initialValue = 0, allocationSize = 1)
public class TableIdEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "tblGen")
    private Long id;

    private String name;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
