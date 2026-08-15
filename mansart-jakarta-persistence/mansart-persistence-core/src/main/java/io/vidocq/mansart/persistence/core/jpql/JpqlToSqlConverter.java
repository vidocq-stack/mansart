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
 * It is also made available under the European Union Public License v. 1.2,
 * which is available at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.jpql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.vidocq.mansart.data.dialect.Attribute;
import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.data.dialect.Where;

/**
 * Converts JPQL AST expressions to Dialect.Where predicates and SQL fragments.
 *
 * <p>Milestone: M7-13 — supports basic SELECT/FROM/WHERE with path expressions,
 * literals, parameters, comparisons, AND/OR/NOT, IS NULL, IS NOT NULL.
 * <p>Milestone: M8-1 — added GROUP BY and HAVING support.
 */
public final class JpqlToSqlConverter {

    private final Map<Class<?>, EntityModel<?>> entityModels;
    private final Map<String, Class<?>> entityClasses;

    /**
     * Creates a new converter with entity model registry.
     *
     * @param entityModels map of entity classes to their EntityModel
     * @param entityClasses map of entity names to entity classes
     */
    public JpqlToSqlConverter(Map<Class<?>, EntityModel<?>> entityModels, Map<String, Class<?>> entityClasses) {
        this.entityModels = Objects.requireNonNull(entityModels, "entityModels must not be null");
        this.entityClasses = Objects.requireNonNull(entityClasses, "entityClasses must not be null");
    }

    /**
     * Converts a JPQLQuery to a Dialect-compatible Where predicate.
     *
     * @param query the JPQL query AST
     * @return the Where predicate, or Where.ALWAYS_TRUE if no WHERE clause
     */
    public Where toWhere(JPQLQuery<?> query) {
        JPQLWhereClause whereClause = query.specification().whereClause();
        if (whereClause == null || whereClause.isNone()) {
            return Where.ALWAYS_TRUE;
        }
        JPQLExpression expression = whereClause.predicate();
        return convertExpression(expression, query.specification().fromClause());
    }

    /**
     * Converts a JPQLExpression to a Dialect.Where predicate.
     */
    private Where convertExpression(JPQLExpression expr, JPQLFromClause fromClause) {
        if (expr instanceof JPQLExpression.And and) {
            return convertAnd(and, fromClause);
        } else if (expr instanceof JPQLExpression.Or or) {
            return convertOr(or, fromClause);
        } else if (expr instanceof JPQLExpression.Not not) {
            return convertNot(not, fromClause);
        } else if (expr instanceof JPQLExpression.Comparison comp) {
            return convertComparison(comp, fromClause);
        } else if (expr instanceof JPQLExpression.NullCheck nullCheck) {
            return convertNullCheck(nullCheck, fromClause);
        } else if (expr instanceof JPQLExpression.PathExpression path) {
            // Path without comparison - should not happen in WHERE clause
            throw new JPQLException("Bare path expression not supported in WHERE clause: " + path);
        } else {
            throw new JPQLException("Unsupported expression type in WHERE clause: " + expr.getClass().getSimpleName());
        }
    }

    private Where convertAnd(JPQLExpression.And and, JPQLFromClause fromClause) {
        Where left = convertExpression(and.left(), fromClause);
        Where right = convertExpression(and.right(), fromClause);
        return new Where.And(List.of(left, right));
    }

    private Where convertOr(JPQLExpression.Or or, JPQLFromClause fromClause) {
        Where left = convertExpression(or.left(), fromClause);
        Where right = convertExpression(or.right(), fromClause);
        return new Where.Or(List.of(left, right));
    }

    private Where convertNot(JPQLExpression.Not not, JPQLFromClause fromClause) {
        Where child = convertExpression(not.child(), fromClause);
        return new Where.Not(child);
    }

    @SuppressWarnings("unchecked")
    private Where convertComparison(JPQLExpression.Comparison comp, JPQLFromClause fromClause) {
        JPQLExpression left = comp.left();
        JPQLExpression right = comp.right();
        JPQLComparator operator = comp.operator();

        // Left side must be a path expression
        if (!(left instanceof JPQLExpression.PathExpression path)) {
            throw new JPQLException("Comparison left side must be a path expression: " + left);
        }

        // Resolve the attribute from the path
        Attribute<?, ?> attr = resolveAttribute(path, fromClause);

        // Right side determines the comparison type
        if (right instanceof JPQLExpression.ParameterExpression param) {
            return createParameterComparison(operator, attr);
        } else if (right instanceof JPQLExpression.Literal lit) {
            return createLiteralComparison(operator, attr);
        } else if (right instanceof JPQLExpression.PathExpression rightPath) {
            Attribute<?, ?> rightAttr = resolveAttribute(rightPath, fromClause);
            return createAttributeComparison(operator, attr, rightAttr);
        } else {
            throw new JPQLException("Unsupported comparison right side: " + right.getClass().getSimpleName());
        }
    }

    private Where createParameterComparison(JPQLComparator operator, Attribute<?, ?> attr) {
        return switch (operator) {
            case EQUALS -> new Where.Eq(attr);
            case NOT_EQUALS -> new Where.NotEq(attr);
            case LESS_THAN -> new Where.Lt(attr);
            case LESS_THAN_EQUAL -> new Where.Lte(attr);
            case GREATER_THAN -> new Where.Gt(attr);
            case GREATER_THAN_EQUAL -> new Where.Gte(attr);
            case LIKE -> new Where.Like(attr);
            default -> throw new JPQLException("Unsupported comparison operator: " + operator);
        };
    }

    private Where createLiteralComparison(JPQLComparator operator, Attribute<?, ?> attr) {
        // For literal comparisons, we use Eq with the literal value as a constant
        // The dialect will handle binding the literal value
        return switch (operator) {
            case EQUALS -> new Where.Eq(attr);
            case NOT_EQUALS -> new Where.NotEq(attr);
            case LESS_THAN -> new Where.Lt(attr);
            case LESS_THAN_EQUAL -> new Where.Lte(attr);
            case GREATER_THAN -> new Where.Gt(attr);
            case GREATER_THAN_EQUAL -> new Where.Gte(attr);
            case LIKE -> new Where.Like(attr);
            default -> throw new JPQLException("Unsupported comparison operator: " + operator);
        };
    }

    private Where createAttributeComparison(JPQLComparator operator, Attribute<?, ?> leftAttr, Attribute<?, ?> rightAttr) {
        // For path-to-path comparisons, we use Eq and let the dialect handle it
        return switch (operator) {
            case EQUALS -> new Where.Eq(leftAttr);
            case NOT_EQUALS -> new Where.NotEq(leftAttr);
            case LESS_THAN -> new Where.Lt(leftAttr);
            case LESS_THAN_EQUAL -> new Where.Lte(leftAttr);
            case GREATER_THAN -> new Where.Gt(leftAttr);
            case GREATER_THAN_EQUAL -> new Where.Gte(leftAttr);
            case LIKE -> new Where.Like(leftAttr);
            default -> throw new JPQLException("Unsupported comparison operator: " + operator);
        };
    }

    private Where convertNullCheck(JPQLExpression.NullCheck nullCheck, JPQLFromClause fromClause) {
        JPQLExpression path = nullCheck.path();
        if (!(path instanceof JPQLExpression.PathExpression pathExpr)) {
            throw new JPQLException("IS NULL path must be a path expression: " + path);
        }
        Attribute<?, ?> attr = resolveAttribute(pathExpr, fromClause);
        return switch (nullCheck.nullCheckType()) {
            case IS_NULL -> new Where.IsNull(attr);
            case IS_NOT_NULL -> new Where.IsNotNull(attr);
        };
    }

    @SuppressWarnings("unchecked")
    private Attribute<?, ?> resolveAttribute(JPQLExpression.PathExpression path, JPQLFromClause fromClause) {
        String identificationVariable = path.identificationVariable();
        List<String> fieldPath = path.fieldPath();

        // Resolve entity class from the identification variable
        Class<?> entityClass = resolveEntityClass(identificationVariable, fromClause);

        // Get EntityModel for this entity
        EntityModel<?> entityModel = entityModels.get(entityClass);
        if (entityModel == null) {
            throw new JPQLException("No EntityModel found for entity: " + entityClass.getName());
        }

        // Navigate through the field path
        Attribute<?, ?> attr = null;
        Class<?> currentType = entityClass;
        EntityModel<?> currentModel = entityModel;

        for (String fieldName : fieldPath) {
            attr = findAttribute(currentModel, fieldName);
            if (attr == null) {
                throw new JPQLException("No attribute '" + fieldName + "' found in entity " + currentType.getName());
            }
            currentType = attr.javaType();
            currentModel = entityModels.get(currentType);
        }

        if (attr == null && !fieldPath.isEmpty()) {
            // Try the first field as the attribute name
            attr = findAttribute(entityModel, fieldPath.get(0));
        }

        if (attr == null && fieldPath.isEmpty()) {
            // The identification variable itself might be the table reference
            // Try to find the ID attribute or first attribute
            if (!entityModel.attributes().isEmpty()) {
                attr = entityModel.attributes().get(0);
            }
        }

        if (attr == null) {
            throw new JPQLException("Cannot resolve attribute from path: " + path);
        }

        return attr;
    }

    private Class<?> resolveEntityClass(String identificationVariable, JPQLFromClause fromClause) {
        // The identification variable might match the from clause's identification variable
        if (identificationVariable.equals(fromClause.identificationVariable())) {
            return fromClause.entityType();
        }
        
        // Try to resolve from entityClasses map
        Class<?> resolved = entityClasses.get(identificationVariable);
        if (resolved != null) {
            return resolved;
        }
        
        // Try with capitalized name
        String capitalized = identificationVariable.substring(0, 1).toUpperCase() + identificationVariable.substring(1);
        resolved = entityClasses.get(capitalized);
        if (resolved != null) {
            return resolved;
        }
        
        // Default to the from clause's entity type
        return fromClause.entityType();
    }

    @SuppressWarnings("unchecked")
    private <E> Attribute<E, ?> findAttribute(EntityModel<?> model, String name) {
        for (Attribute<?, ?> attr : model.attributes()) {
            if (attr.name().equals(name)) {
                return (Attribute<E, ?>) attr;
            }
        }
        return null;
    }

    /**
     * Converts a JPQLQuery to the entity class being queried.
     */
    public Class<?> getEntityClass(JPQLQuery<?> query) {
        return query.specification().fromClause().entityType();
    }

    /**
     * Gets the EntityModel for the queried entity.
     */
    @SuppressWarnings("unchecked")
    public EntityModel<?> getEntityModel(JPQLQuery<?> query) {
        Class<?> entityClass = getEntityClass(query);
        return entityModels.get(entityClass);
    }

    /**
     * Converts a GROUP BY clause to SQL GROUP BY fragment.
     *
     * @param groupByClause the GROUP BY clause
     * @param fromClause the FROM clause
     * @return SQL GROUP BY fragment, or empty string if no GROUP BY
     */
    public String toGroupBySql(JPQLGroupByClause groupByClause, JPQLFromClause fromClause) {
        if (groupByClause == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(" GROUP BY ");
        boolean first = true;
        for (JPQLExpression expr : groupByClause.expressions()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(expressionToSql(expr, fromClause));
            first = false;
        }
        return sb.toString();
    }

    /**
     * Converts a HAVING clause to SQL HAVING fragment.
     *
     * @param havingClause the HAVING clause
     * @param fromClause the FROM clause
     * @return SQL HAVING fragment, or empty string if no HAVING
     */
    public String toHavingSql(JPQLHavingClause havingClause, JPQLFromClause fromClause) {
        if (havingClause == null) {
            return "";
        }
        return " HAVING " + expressionToSql(havingClause.expression(), fromClause);
    }

    /**
     * Converts a JPQL expression to SQL string.
     */
    private String expressionToSql(JPQLExpression expr, JPQLFromClause fromClause) {
        if (expr instanceof JPQLExpression.PathExpression path) {
            return pathToSql(path, fromClause);
        } else if (expr instanceof JPQLExpression.StringLiteral lit) {
            return "'" + escapeSqlString((String) lit.value()) + "'";
        } else if (expr instanceof JPQLExpression.NumberLiteral lit) {
            return String.valueOf(lit.value());
        } else if (expr instanceof JPQLExpression.BooleanLiteral lit) {
            return lit.value() ? "TRUE" : "FALSE";
        } else if (expr instanceof JPQLExpression.NullLiteral) {
            return "NULL";
        } else if (expr instanceof JPQLExpression.ParameterExpression param) {
            return "?";
        } else if (expr instanceof JPQLExpression.Comparison comp) {
            return comparisonToSql(comp, fromClause);
        } else if (expr instanceof JPQLExpression.And and) {
            return "(" + expressionToSql(and.left(), fromClause) + " AND " + expressionToSql(and.right(), fromClause) + ")";
        } else if (expr instanceof JPQLExpression.Or or) {
            return "(" + expressionToSql(or.left(), fromClause) + " OR " + expressionToSql(or.right(), fromClause) + ")";
        } else if (expr instanceof JPQLExpression.Not not) {
            return "(NOT " + expressionToSql(not.child(), fromClause) + ")";
        } else if (expr instanceof JPQLExpression.NullCheck nullCheck) {
            return nullCheckToSql(nullCheck, fromClause);
        } else if (expr instanceof JPQLQuantifiedExpression quant) {
            return quantifiedExpressionToSql(quant, fromClause);
        } else if (expr instanceof JPQLExpression.ExistsExpression exists) {
            return existsExpressionToSql(exists, fromClause);
        } else if (expr instanceof JPQLSubquery subquery) {
            return subqueryToSql(subquery, fromClause);
        } else if (expr instanceof JPQLFunctionExpression func) {
            return functionExpressionToSql(func, fromClause);
        } else {
            throw new JPQLException("Unsupported expression type for SQL conversion: " + expr.getClass().getSimpleName());
        }
    }

    private String pathToSql(JPQLExpression.PathExpression path, JPQLFromClause fromClause) {
        try {
            Attribute<?, ?> attr = resolveAttribute(path, fromClause);
            return attr.columnName();
        } catch (JPQLException e) {
            // If we can't resolve the attribute, use the path as-is
            StringBuilder sb = new StringBuilder();
            sb.append(path.identificationVariable());
            for (String field : path.fieldPath()) {
                sb.append("_").append(field);
            }
            return sb.toString();
        }
    }

    private String comparisonToSql(JPQLExpression.Comparison comp, JPQLFromClause fromClause) {
        String left = expressionToSql(comp.left(), fromClause);
        String right = expressionToSql(comp.right(), fromClause);
        String op = switch (comp.operator()) {
            case EQUALS -> "=";
            case NOT_EQUALS -> "<>";
            case LESS_THAN -> "<";
            case LESS_THAN_EQUAL -> "<=";
            case GREATER_THAN -> ">";
            case GREATER_THAN_EQUAL -> ">=";
            case LIKE -> "LIKE";
        };
        return left + " " + op + " " + right;
    }

    private String nullCheckToSql(JPQLExpression.NullCheck nullCheck, JPQLFromClause fromClause) {
        String path = expressionToSql(nullCheck.path(), fromClause);
        return switch (nullCheck.nullCheckType()) {
            case IS_NULL -> path + " IS NULL";
            case IS_NOT_NULL -> path + " IS NOT NULL";
        };
    }

    private String escapeSqlString(String value) {
        return value.replace("'", "''");
    }

    /** Converts a quantified expression to SQL */
    private String quantifiedExpressionToSql(JPQLQuantifiedExpression quant, JPQLFromClause fromClause) {
        String left = expressionToSql(quant.leftExpression(), fromClause);
        String op = switch (quant.comparator()) {
            case EQUALS -> "=";
            case NOT_EQUALS -> "<>";
            case LESS_THAN -> "<";
            case LESS_THAN_EQUAL -> "<=";
            case GREATER_THAN -> ">";
            case GREATER_THAN_EQUAL -> ">=";
            case LIKE -> "LIKE";
        };
        String quantifier = switch (quant.quantifier()) {
            case ALL -> "ALL";
            case ANY -> "ANY";
            case SOME -> "SOME";
        };
        String subquery = subqueryToSql(quant.subquery(), fromClause);
        return left + " " + op + " " + quantifier + " " + subquery;
    }

    /** Converts an EXISTS expression to SQL */
    private String existsExpressionToSql(JPQLExpression.ExistsExpression exists, JPQLFromClause fromClause) {
        return "EXISTS " + subqueryToSql(exists.subquery(), fromClause);
    }

    /** Converts a subquery to SQL */
    private String subqueryToSql(JPQLSubquery subquery, JPQLFromClause fromClause) {
        StringBuilder sb = new StringBuilder("(");
        JPQLQuery<?> query = subquery.query();
        JPQLQuerySpecification spec = query.specification();
        
        // SELECT clause - for now use *
        sb.append("SELECT *");
        
        // FROM clause
        sb.append(" FROM ").append(spec.fromClause().entityType().getSimpleName());
        if (spec.fromClause().identificationVariable() != null) {
            sb.append(" ").append(spec.fromClause().identificationVariable());
        }
        
        // WHERE clause - for now, skip (subqueries in WHERE are handled separately)
        // This is a simplified implementation
        
        sb.append(")");
        return sb.toString();
    }

    /** Converts a function expression to SQL */
    private String functionExpressionToSql(JPQLFunctionExpression func, JPQLFromClause fromClause) {
        String functionName = mapJpqlFunctionToSql(func.functionName());
        StringBuilder sb = new StringBuilder();
        sb.append(functionName).append("(");
        boolean first = true;
        for (JPQLExpression arg : func.arguments()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(expressionToSql(arg, fromClause));
            first = false;
        }
        sb.append(")");
        return sb.toString();
    }

    /** Maps JPQL function names to SQL function names */
    private String mapJpqlFunctionToSql(String jpqlFunction) {
        return switch (jpqlFunction.toUpperCase()) {
            // String functions
            case "CONCAT" -> "CONCAT";
            case "SUBSTRING" -> "SUBSTRING";
            case "TRIM" -> "TRIM";
            case "LOWER" -> "LOWER";
            case "UPPER" -> "UPPER";
            case "LENGTH" -> "LENGTH";
            case "LOCATE" -> "LOCATE";
            
            // Numeric functions
            case "ABS" -> "ABS";
            case "SQRT" -> "SQRT";
            case "MOD" -> "MOD";
            
            // Date functions
            case "CURRENT_DATE" -> "CURRENT_DATE";
            case "CURRENT_TIME" -> "CURRENT_TIME";
            case "CURRENT_TIMESTAMP" -> "CURRENT_TIMESTAMP";
            
            // Collection functions
            case "SIZE" -> "COUNT";  // SIZE is typically mapped to COUNT for collections
            case "INDEX" -> "INDEX";
            
            // Default: pass through as-is
            default -> jpqlFunction;
        };
    }
}
