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

package io.vidocq.mansart.persistence.core.bootstrap;

import jakarta.persistence.spi.ProviderUtil;
import jakarta.persistence.spi.LoadState;

/**
 * Mansart implementation of the Jakarta Persistence ProviderUtil interface.
 * This class provides utility methods for determining if classes are managed
 * by the persistence provider.
 */
public class MansartProviderUtil implements ProviderUtil {

    /**
     * Checks if the given object is a managed entity that is loaded.
     *
     * @param proxy the object to check
     * @return the LoadState of the object
     */
    @Override
    public LoadState isLoaded(Object proxy) {
        // TODO: Implement check for loaded managed class
        // This should check if the given object is a managed entity instance
        return LoadState.LOADED;
    }

    /**
     * Checks if the given object is loaded without reference.
     *
     * @param proxy the object to check
     * @param attributeName the attribute name
     * @return the LoadState of the object
     */
    @Override
    public LoadState isLoadedWithoutReference(Object proxy, String attributeName) {
        // TODO: Implement check for loaded without reference
        return LoadState.LOADED;
    }

    /**
     * Checks if the given object is loaded with reference.
     *
     * @param proxy the object to check
     * @param attributeName the attribute name
     * @return the LoadState of the object
     */
    @Override
    public LoadState isLoadedWithReference(Object proxy, String attributeName) {
        // TODO: Implement check for loaded with reference
        return LoadState.LOADED;
    }


}
