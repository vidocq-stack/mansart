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

import jakarta.persistence.MappedSuperclass;

/**
 * Test mapped superclass used by {@code MansartPersistenceProcessorTest} to verify
 * that the APT processor generates metamodel for {@code @MappedSuperclass} types.
 */
@MappedSuperclass
public class TestMappedSuperclass {

    protected String createdAt;

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
