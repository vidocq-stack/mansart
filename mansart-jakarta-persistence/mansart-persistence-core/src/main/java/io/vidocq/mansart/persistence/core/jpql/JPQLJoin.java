/*
 * Copyright (c) 2026 Vidocq contributors
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.jpql;

import java.util.Objects;

/**
 * Represents a JOIN clause in JPQL.
 *
 * <p>Milestone: M8-2 — JOIN syntax support.
 */
public final class JPQLJoin {

    private final JPQLJoinType joinType;
    private final JPQLExpression entityExpression;
    private final String identificationVariable;
    private final JPQLExpression onExpression;

    /**
     * Creates a new JOIN.
     *
     * @param joinType the type of join (INNER, LEFT, RIGHT)
     * @param entityExpression the entity or collection expression to join
     * @param identificationVariable the alias for the joined entity
     * @param onExpression the ON condition (may be null for implicit joins)
     */
    public JPQLJoin(JPQLJoinType joinType, JPQLExpression entityExpression, 
                    String identificationVariable, JPQLExpression onExpression) {
        this.joinType = Objects.requireNonNull(joinType, "joinType must not be null");
        this.entityExpression = Objects.requireNonNull(entityExpression, "entityExpression must not be null");
        this.identificationVariable = identificationVariable;
        this.onExpression = onExpression;
    }

    public JPQLJoinType joinType() { return joinType; }
    public JPQLExpression entityExpression() { return entityExpression; }
    public String identificationVariable() { return identificationVariable; }
    public JPQLExpression onExpression() { return onExpression; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (joinType != JPQLJoinType.INNER) {
            sb.append(joinType).append(" ");
        }
        sb.append("JOIN ").append(entityExpression);
        if (identificationVariable != null) {
            sb.append(" ").append(identificationVariable);
        }
        if (onExpression != null) {
            sb.append(" ON ").append(onExpression);
        }
        return sb.toString();
    }
}
