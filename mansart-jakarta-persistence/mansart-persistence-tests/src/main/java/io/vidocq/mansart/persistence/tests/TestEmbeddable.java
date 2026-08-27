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

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test embeddable used by {@code MansartPersistenceProcessorTest} to verify
 * that the APT processor generates metamodel for {@code @Embeddable} types
 * and {@code @ElementCollection} fields.
 */
@Embeddable
public class TestEmbeddable {

    protected String street;
    protected String city;
    protected String state;

    @ElementCollection
    protected Set<TestZipCode> sZipcode = new HashSet<>();

    @ElementCollection
    protected List<TestZipCode> lZipcode = new ArrayList<>();

    @ElementCollection
    protected Map<TestZipCode, String> mZipcode = new HashMap<>();

    public TestEmbeddable() {
    }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public Set<TestZipCode> getSZipcode() { return sZipcode; }
    public void setSZipcode(Set<TestZipCode> sZipcode) { this.sZipcode = sZipcode; }

    public List<TestZipCode> getLZipcode() { return lZipcode; }
    public void setLZipcode(List<TestZipCode> lZipcode) { this.lZipcode = lZipcode; }

    public Map<TestZipCode, String> getMZipcode() { return mZipcode; }
    public void setMZipcode(Map<TestZipCode, String> mZipcode) { this.mZipcode = mZipcode; }
}
