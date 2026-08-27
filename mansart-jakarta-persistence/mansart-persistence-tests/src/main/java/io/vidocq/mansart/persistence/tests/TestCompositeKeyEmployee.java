/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

import java.io.Serializable;

/**
 * Entity with a composite primary key, used by
 * {@code MansartPersistenceProcessorTest} to verify that the APT
 * processor generates {@code SingularAttribute} fields for each
 * key attribute when {@code @IdClass} is present.
 */
@Entity(name = "CompositeKeyEmployee")
@IdClass(CompositeKeyEmployeeId.class)
public class TestCompositeKeyEmployee {

    @Id
    private String firstName;

    @Id
    private String lastName;

    private int empNo;

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public int getEmpNo() { return empNo; }
    public void setEmpNo(int empNo) { this.empNo = empNo; }
}

/**
 * Composite primary key class for {@link TestCompositeKeyEmployee}.
 * Must be {@code Serializable} with matching field names and types.
 */
class CompositeKeyEmployeeId implements Serializable {

    private String firstName;
    private String lastName;

    public CompositeKeyEmployeeId() {
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
}
