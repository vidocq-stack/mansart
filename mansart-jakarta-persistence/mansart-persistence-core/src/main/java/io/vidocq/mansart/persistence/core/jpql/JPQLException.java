/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * or any later version, which is available at
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * It is also made available under the European Union Public Licence v. 1.2,
 * which is available at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.jpql;

/**
 * Exception thrown during JPQL parsing or query execution.
 *
 * <p>Milestone: M7-13 — used when queries cannot be parsed or executed.
 */
public class JPQLException extends RuntimeException {

    /** The query string being processed when the exception occurred. */
    private final String query;

    public JPQLException(String message) {
        super(message);
        this.query = null;
    }

    public JPQLException(String message, String query) {
        super(message);
        this.query = query;
    }

    public JPQLException(String message, Throwable cause) {
        super(message, cause);
        this.query = null;
    }

    public JPQLException(String message, String query, Throwable cause) {
        super(message, cause);
        this.query = query;
    }

    /**
     * Returns the query string that caused the error, or {@code null} if not available.
     *
     * @return the query string, or {@code null}
     */
    public String getQuery() {
        return query;
    }
}
