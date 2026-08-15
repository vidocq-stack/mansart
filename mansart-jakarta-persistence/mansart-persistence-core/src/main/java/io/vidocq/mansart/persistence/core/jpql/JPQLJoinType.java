/*
 * Copyright (c) 2026 Vidocq contributors
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.jpql;

/**
 * JOIN types supported in JPQL.
 *
 * <p>Milestone: M8-2 — JOIN syntax support.
 */
public enum JPQLJoinType {
    INNER,
    LEFT,
    RIGHT,
    // LEFT OUTER and RIGHT OUTER are aliases
    LEFT_OUTER,
    RIGHT_OUTER
}
