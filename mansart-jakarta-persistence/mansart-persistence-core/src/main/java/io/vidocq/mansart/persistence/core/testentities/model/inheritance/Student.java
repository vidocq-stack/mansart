/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.testentities.model.inheritance;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;

/**
 * Student entity extending Person with JOINED inheritance.
 */
@Entity
@PrimaryKeyJoinColumn(name = "person_id")
public class Student extends Person {

    private String university;

    private String major;

    public Student() {}

    public Student(String name, String university, String major) {
        super(name);
        this.university = university;
        this.major = major;
    }

    public String getUniversity() {
        return university;
    }

    public void setUniversity(String university) {
        this.university = university;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    @Override
    public String toString() {
        return "Student{id=" + getId() + ", name='" + getName() + 
               "', university='" + university + "', major='" + major + "'}";
    }
}
