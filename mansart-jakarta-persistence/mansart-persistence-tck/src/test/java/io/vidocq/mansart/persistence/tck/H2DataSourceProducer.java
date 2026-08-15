/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tck;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;

/**
 * Produces an H2 in-memory DataSource for TCK test deployments.
 * Uses the same database name as the TCK expects (default is often "testdb").
 */
@ApplicationScoped
public class H2DataSourceProducer {

    @Produces
    @ApplicationScoped
    public DataSource produceDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }
}
