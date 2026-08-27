/*
 * Copyright (c) 2007, 2024 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package com.sun.ts.tests.common.vehicle.ejb3share;

import jakarta.persistence.EntityManager;

/**
 * Stub interface for Jakarta EE TCK vehicle sharing.
 * Mirrors the interface from the Jakarta EE Platform TCK.
 */
public interface UseEntityManager {
    /**
     * Returns the EntityManager for the current test.
     * @return the EntityManager
     */
    EntityManager getEntityManager();

    /**
     * Returns the EntityManager for the current test, with optional reinitialization.
     * @param reInit whether to reinitialize the EntityManager
     * @return the EntityManager
     */
    EntityManager getEntityManager(boolean reInit);
}
