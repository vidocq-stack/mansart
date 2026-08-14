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
package io.vidocq.mansart.persistence.processor.parse;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;

/**
 * Parser for {@code @jakarta.persistence.Id} annotation.
 *
 * <p>This class extracts primary key metadata from annotated fields or properties.
 */
public final class IdParser {

    /**
     * Parses the {@code @Id} annotation from a field.
     *
     * @param element the field element annotated with {@code @Id}
     * @return an IdInfo containing name and type, or null if not annotated with @Id
     */
    public IdInfo parse(Element element) {
        if (!element.getKind().isField()) {
            return null;
        }

        VariableElement varElement = (VariableElement) element;

        // Extract the field name
        String name = varElement.getSimpleName().toString();
        
        // Extract the type
        String type = varElement.asType().toString();

        return new IdInfo(name, type);
    }

    /**
     * Records for ID information extracted from {@code @Id} annotation.
     */
    public record IdInfo(String name, String type) {
    }
}
