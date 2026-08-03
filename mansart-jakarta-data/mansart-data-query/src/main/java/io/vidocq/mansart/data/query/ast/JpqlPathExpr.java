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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */

package io.vidocq.mansart.data.query.ast;

import java.util.List;

/**
 * JPQL path expression node.
 *
 * <p>Represents a navigation through entity attributes in JPQL.</p>
 *
 * <p>Example JPQL:</p>
 * <pre>
 * b                    (simple identifier)
 * b.title              (single-valued attribute)
 * b.author            (single-valued association)
 * b.author.name       (navigation through association)
 * b.items             (collection-valued association)
 * b.items.size        (collection size)
 * </pre>
 *
 * @param path the path as a string (e.g., "b.author.name")
 * @param parts the path parts (e.g., ["b", "author", "name"])
 * @since 0.3.0-SNAPSHOT
 */
public record JpqlPathExpr(String path, List<String> parts) implements JpqlExpr {

    /**
     * Creates a new path expression.
     *
     * @param path the path as a string
     * @param parts the path parts
     */
    public JpqlPathExpr {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path cannot be null or blank");
        }
        if (parts == null || parts.isEmpty()) {
            throw new IllegalArgumentException("parts cannot be null or empty");
        }
        path = path.intern();
        parts = List.copyOf(parts);
    }

    @Override
    public <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter) {
        return visitor.visitPathExpr(this, parameter);
    }

    /**
     * Creates a path expression from a string path.
     *
     * @param path the path (e.g., "b.author.name")
     * @return a new path expression
     */
    public static JpqlPathExpr of(String path) {
        List<String> parts = List.of(path.split("\\."));
        return new JpqlPathExpr(path, parts);
    }

    /**
     * Creates a path expression from a single identifier.
     *
     * @param identifier the identifier (e.g., "b")
     * @return a new simple path expression
     */
    public static JpqlPathExpr ofIdentifier(String identifier) {
        return new JpqlPathExpr(identifier, List.of(identifier));
    }

    /**
     * Returns true if this path is a simple identifier (no dots).
     *
     * @return true if simple identifier
     */
    public boolean isSimple() {
        return parts().size() == 1;
    }

    /**
     * Returns the first part (identifier/alias).
     *
     * @return the first part
     */
    public String identifier() {
        return parts().get(0);
    }

    /**
     * Returns the last part (attribute name).
     *
     * @return the last part
     */
    public String attribute() {
        return parts().get(parts().size() - 1);
    }

    /**
     * Returns the parent path (all parts except the last).
     *
     * @return the parent path, or empty if this is a simple identifier
     */
    public JpqlPathExpr parent() {
        if (parts().size() <= 1) {
            return null;
        }
        List<String> parentParts = parts().subList(0, parts().size() - 1);
        String parentPath = String.join(".", parentParts);
        return new JpqlPathExpr(parentPath, parentParts);
    }

    @Override
    public String toString() {
        return path();
    }
}
