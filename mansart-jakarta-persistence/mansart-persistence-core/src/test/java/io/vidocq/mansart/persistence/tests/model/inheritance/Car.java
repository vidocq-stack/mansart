/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests.model.inheritance;

import jakarta.persistence.Entity;

/**
 * Car entity extending Vehicle with TABLE_PER_CLASS inheritance.
 */
@Entity
public class Car extends Vehicle {

    private String model;

    private int year;

    public Car() {}

    public Car(String manufacturer, String model, int year) {
        super(manufacturer);
        this.model = model;
        this.year = year;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    @Override
    public String toString() {
        return "Car{id=" + getId() + ", manufacturer='" + getManufacturer() + 
               "', model='" + model + "', year=" + year + "}";
    }
}
