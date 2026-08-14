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
package io.vidocq.mansart.persistence.processor;

import java.io.IOException;
import java.io.Writer;
import javax.tools.FileObject;

/**
 * Abstraction for source file output during annotation processing.
 *
 * <p>This interface abstracts the file creation mechanism, allowing
 * the same code generation logic to work with different backends
 * (e.g., Filer during APT, or file system in Maven plugins).
 */
public interface SourceSink {

    /**
     * Creates a new source file with the given fully-qualified name.
     *
     * @param fqn the fully-qualified name of the class being generated (e.g., {@code com.example._Book})
     * @return a {@link Writer} for writing the source file content
     * @throws IOException if the file cannot be created
     */
    Writer createSource(String fqn) throws IOException;
}
