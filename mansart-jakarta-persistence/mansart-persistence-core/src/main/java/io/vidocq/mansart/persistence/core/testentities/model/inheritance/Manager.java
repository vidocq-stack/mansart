/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.testentities.model.inheritance;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Manager entity extending Employee.
 * Part of SINGLE_TABLE inheritance hierarchy.
 */
@Entity
@DiscriminatorValue("MANAGER")
public class Manager extends Employee {

    private String team;

    private Integer bonus;

    public Manager() {}

    public Manager(String name, String department, String team, Integer bonus) {
        super(name, department);
        this.team = team;
        this.bonus = bonus;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public Integer getBonus() {
        return bonus;
    }

    public void setBonus(Integer bonus) {
        this.bonus = bonus;
    }

    @Override
    public String toString() {
        return "Manager{id=" + getId() + ", name='" + getName() + "', department='" + getDepartment() + 
               "', team='" + team + "', bonus=" + bonus + "'}";
    }
}
