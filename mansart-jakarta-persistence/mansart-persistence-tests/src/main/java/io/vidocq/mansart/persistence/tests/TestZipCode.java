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

import jakarta.persistence.Embeddable;

/**
 * Nestable embeddable used by {@code TestEmbeddable} to exercise
 * nested embeddable generation in the APT processor.
 */
@Embeddable
public class TestZipCode {

    protected String zip;
    protected String plusFour;

    public TestZipCode() {
    }

    public String getZip() { return zip; }
    public void setZip(String zip) { this.zip = zip; }

    public String getPlusFour() { return plusFour; }
    public void setPlusFour(String plusFour) { this.plusFour = plusFour; }
}
