package io.vidocq.mansart.persistence.core.jpql;

/**
 * JPQL parser implementation for Jakarta Persistence 3.2.
 * <p>
 * Provides a recursive-descent parser that produces an AST from JPQL queries.
 * The AST is rooted at {@link JpqlStatement} and contains sealed interfaces for
 * SELECT, UPDATE, and DELETE statements with their clauses and expressions.
 */
