/*
 * Copyright (c) ${year} Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */

package io.vidocq.mansart.persistence.cdi;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;

/**
 * Mansart Persistence CDI Build Compatible Extension.
 * This extension provides build-time integration between Mansart JPA and CDI containers.
 * For M1, this is a minimal implementation. Full BCE functionality will be added in later milestones.
 */
public class MansartPersistenceExtension implements BuildCompatibleExtension {

    /**
     * Default constructor.
     */
    public MansartPersistenceExtension() {
    }
}
