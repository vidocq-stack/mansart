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

package io.vidocq.mansart.persistence.core;

import io.vidocq.mansart.data.query.ast.*;

import java.util.*;

/**
 * Extracts parameters from JPQL AST and builds argument arrays for query execution.
 * 
 * <p>Handles both named parameters (:name) and positional parameters (?1, ?2, ...).
 * Positional parameters are converted to named parameters internally for Mansart Data compatibility.</p>
 * 
 * <p>M6 — Parameter binding support for JPQL queries.</p>
 */
public final class ParameterExtractor {

    private ParameterExtractor() {}

    /**
     * Extracts parameters from a JPQL statement and builds the argument array.
     * 
     * @param stmt the JPQL statement
     * @param namedParameters map of named parameter values
     * @param positionalParameters map of positional parameter values (1-based)
     * @return array of parameter values in the order they appear in the query
     */
    public static Object[] extractParameters(JpqlStmt stmt, 
                                            Map<String, Object> namedParameters,
                                            Map<Integer, Object> positionalParameters) {
        List<Object> args = new ArrayList<>();
        List<String> parameterOrder = new ArrayList<>();
        
        collectParameters(stmt, parameterOrder);
        
        for (String paramRef : parameterOrder) {
            if (paramRef.startsWith(":")) {
                String paramName = paramRef.substring(1);
                Object value = namedParameters.get(paramName);
                if (value != null) {
                    args.add(value);
                } else {
                    throw new IllegalArgumentException("Named parameter '" + paramName + "' not bound");
                }
            } else if (paramRef.startsWith("?")) {
                int position = Integer.parseInt(paramRef.substring(1));
                Object value = positionalParameters.get(position);
                if (value != null) {
                    args.add(value);
                } else {
                    throw new IllegalArgumentException("Positional parameter ?" + position + " not bound");
                }
            }
        }
        
        return args.toArray();
    }

    private static void collectParameters(JpqlNode node, List<String> parameterOrder) {
        if (node instanceof JpqlParameterExpr paramExpr) {
            if (paramExpr.isNamed()) {
                parameterOrder.add(":" + paramExpr.getName());
            } else if (paramExpr.isPositional()) {
                parameterOrder.add("?" + paramExpr.getPosition());
            }
        } else if (node instanceof JpqlPredicate predicate) {
            collectParametersFromPredicate(predicate, parameterOrder);
        } else if (node instanceof JpqlSelectStmt selectStmt) {
            collectParametersFromSelectStmt(selectStmt, parameterOrder);
        } else if (node instanceof JpqlWhereClause whereClause) {
            collectParameters(whereClause.predicate(), parameterOrder);
        } else if (node instanceof JpqlOrderByClause orderByClause) {
            for (JpqlOrderByItem item : orderByClause.items()) {
                collectParametersFromExpr(item.expression(), parameterOrder);
            }
        } else if (node instanceof JpqlSelectClause selectClause) {
            for (JpqlExpr expr : selectClause.expressions()) {
                collectParametersFromExpr(expr, parameterOrder);
            }
        } else if (node instanceof JpqlFromClause fromClause) {
            // FROM clause typically doesn't have parameters
        } else if (node instanceof JpqlExpr expr) {
            collectParametersFromExpr(expr, parameterOrder);
        }
    }

    private static void collectParametersFromSelectStmt(JpqlSelectStmt selectStmt, List<String> parameterOrder) {
        if (selectStmt.selectClause() != null) {
            collectParameters(selectStmt.selectClause(), parameterOrder);
        }
        if (selectStmt.fromClause() != null) {
            collectParameters(selectStmt.fromClause(), parameterOrder);
        }
        selectStmt.whereClause().ifPresent(clause -> collectParameters(clause, parameterOrder));
        selectStmt.groupByClause().ifPresent(clause -> collectParameters(clause, parameterOrder));
        selectStmt.havingClause().ifPresent(clause -> collectParameters(clause, parameterOrder));
        selectStmt.orderByClause().ifPresent(clause -> collectParameters(clause, parameterOrder));
    }

    private static void collectParametersFromPredicate(JpqlPredicate predicate, List<String> parameterOrder) {
        if (predicate instanceof JpqlAndPredicate and) {
            and.predicates().forEach(p -> collectParametersFromPredicate(p, parameterOrder));
        } else if (predicate instanceof JpqlOrPredicate or) {
            or.predicates().forEach(p -> collectParametersFromPredicate(p, parameterOrder));
        } else if (predicate instanceof JpqlNotPredicate not) {
            collectParametersFromPredicate(not.operand(), parameterOrder);
        } else if (predicate instanceof JpqlComparisonPredicate comp) {
            collectParametersFromExpr(comp.left(), parameterOrder);
            if (comp.right() != null) {
                collectParametersFromExpr(comp.right(), parameterOrder);
            }
        } else if (predicate instanceof JpqlLikePredicate like) {
            collectParametersFromExpr(like.expression(), parameterOrder);
            collectParametersFromExpr(like.pattern(), parameterOrder);
        } else if (predicate instanceof JpqlInPredicate in) {
            collectParametersFromExpr(in.expression(), parameterOrder);
            collectParametersFromInPredicate(in, parameterOrder);
        } else if (predicate instanceof JpqlBetweenPredicate between) {
            collectParametersFromExpr(between.expression(), parameterOrder);
            collectParametersFromExpr(between.lower(), parameterOrder);
            collectParametersFromExpr(between.upper(), parameterOrder);
        } else if (predicate instanceof JpqlExistsPredicate exists) {
            collectParametersFromExistsPredicate(exists, parameterOrder);
        } else if (predicate instanceof JpqlAllAnySomePredicate allAnySome) {
            collectParametersFromAllAnySomePredicate(allAnySome, parameterOrder);
        }
    }

    private static void collectParametersFromInPredicate(JpqlInPredicate in, List<String> parameterOrder) {
        try {
            var items = in.getClass().getMethod("items").invoke(in);
            String itemsClassName = items.getClass().getName();
            
            if (itemsClassName.contains("ExpressionList")) {
                var expressions = (List<?>) items.getClass().getMethod("expressions").invoke(items);
                for (Object exprObj : expressions) {
                    if (exprObj instanceof JpqlExpr expr) {
                        collectParametersFromExpr(expr, parameterOrder);
                    }
                }
            } else if (itemsClassName.contains("Subquery")) {
                var query = (JpqlSelectStmt) items.getClass().getMethod("query").invoke(items);
                collectParameters(query, parameterOrder);
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    private static void collectParametersFromExistsPredicate(JpqlExistsPredicate exists, List<String> parameterOrder) {
        try {
            var subquery = (JpqlSelectStmt) exists.getClass().getMethod("subquery").invoke(exists);
            collectParameters(subquery, parameterOrder);
        } catch (Exception e) {
            // Ignore
        }
    }

    private static void collectParametersFromAllAnySomePredicate(JpqlAllAnySomePredicate predicate, List<String> parameterOrder) {
        try {
            collectParametersFromExpr(predicate.expression(), parameterOrder);
            // Try to get the subquery using the public getSubquery() method first
            JpqlSelectStmt subquery = predicate.getSubquery();
            if (subquery != null) {
                collectParameters(subquery, parameterOrder);
                return;
            }
            // Fallback to reflection for expression list case
            var subqueryObj = predicate.getClass().getMethod("subquery").invoke(predicate);
            if (subqueryObj != null) {
                String subqueryClassName = subqueryObj.getClass().getName();
                if (subqueryClassName.contains("Subquery")) {
                    var query = (JpqlSelectStmt) subqueryObj.getClass().getMethod("query").invoke(subqueryObj);
                    collectParameters(query, parameterOrder);
                } else if (subqueryClassName.contains("ExpressionList")) {
                    var expressions = (List<?>) subqueryObj.getClass().getMethod("expressions").invoke(subqueryObj);
                    for (Object exprObj : expressions) {
                        if (exprObj instanceof JpqlExpr expr) {
                            collectParametersFromExpr(expr, parameterOrder);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    private static void collectParametersFromExpr(JpqlExpr expr, List<String> parameterOrder) {
        if (expr == null) return;
        
        if (expr instanceof JpqlParameterExpr paramExpr) {
            if (paramExpr.isNamed()) {
                parameterOrder.add(":" + paramExpr.getName());
            } else if (paramExpr.isPositional()) {
                parameterOrder.add("?" + paramExpr.getPosition());
            }
        } else if (expr instanceof JpqlBinaryExpr binary) {
            collectParametersFromExpr(binary.left(), parameterOrder);
            collectParametersFromExpr(binary.right(), parameterOrder);
        } else if (expr instanceof JpqlFunctionExpr func) {
            for (JpqlExpr arg : func.arguments()) {
                collectParametersFromExpr(arg, parameterOrder);
            }
        } else if (expr instanceof JpqlUnaryExpr unary) {
            collectParametersFromExpr(unary.operand(), parameterOrder);
        }
    }

    public static Map<String, Object> convertPositionalToNamed(Map<Integer, Object> positionalParameters) {
        Map<String, Object> converted = new HashMap<>();
        for (Map.Entry<Integer, Object> entry : positionalParameters.entrySet()) {
            converted.put("__pos_" + entry.getKey(), entry.getValue());
        }
        return converted;
    }

    public static Map<String, Object> mergeParameters(Map<String, Object> namedParameters,
                                                     Map<Integer, Object> positionalParameters) {
        Map<String, Object> merged = new HashMap<>(namedParameters);
        merged.putAll(convertPositionalToNamed(positionalParameters));
        return merged;
    }
}
