/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.dialect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.PersistenceException;

import io.vidocq.mansart.persistence.core.runtime.MansartCallback;
import io.vidocq.mansart.data.dialect.Attribute;
import io.vidocq.mansart.data.dialect.Dialect;
import io.vidocq.mansart.data.dialect.OrderBy;
import io.vidocq.mansart.data.dialect.Pagination;
import io.vidocq.mansart.data.dialect.SqlFragment;
import io.vidocq.mansart.data.dialect.Where;
import io.vidocq.mansart.data.dialect.attribute.VersionAttribute;

/**
 * Maps between entity instances and database rows using a Dialect.
 * <p>
 * Uses the Dialect SPI to generate SQL and the persistence SPI to access entity state.
 */
public final class EntityMapper {

    private static final String MSG_CONN_NULL = "conn must not be null";

    private final Dialect dialect;
    private final MansartCallback callback;
    private final DialectEntityModelAdapter adapter;

    public EntityMapper(Dialect dialect, MansartCallback callback, DialectEntityModelAdapter adapter) {
        this.dialect = Objects.requireNonNull(dialect, "dialect must not be null");
        this.callback = Objects.requireNonNull(callback, "callback must not be null");
        this.adapter = Objects.requireNonNull(adapter, "adapter must not be null");
    }

    /**
     * Inserts the given entity into the database.
     *
     * @param conn the database connection
     * @param entity the entity to insert
     * @param <T> the entity type
     * @return the generated id if database-generated, otherwise the assigned id from the entity
     * @throws PersistenceException if a database error occurs
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> Object insert(Connection conn, T entity) {
        Objects.requireNonNull(conn, MSG_CONN_NULL);
        Objects.requireNonNull(entity, "entity must not be null");

        try {
            if (conn.isClosed()) {
                throw new PersistenceException("Connection is closed");
            }

            Class<T> entityClass = (Class<T>) entity.getClass();
            io.vidocq.mansart.persistence.spi.EntityModel<T> spiModel = callback.getEntityModel(entityClass);
            @SuppressWarnings("unchecked")
            io.vidocq.mansart.data.dialect.EntityModel dialectModel = adapter.adapt(spiModel);

            boolean generated = dialectModel.id() != null && dialectModel.id().generated();

            SqlFragment sqlFragment = dialect.insert(dialectModel, generated);

            PreparedStatement ps;
            if (generated) {
                ps = conn.prepareStatement(sqlFragment.sql(), Statement.RETURN_GENERATED_KEYS);
            } else {
                ps = conn.prepareStatement(sqlFragment.sql());
            }

            try (ps) {
                int paramIndex = 1;
                List<Attribute<?, ?>> attrs = dialectModel.attributes();
                for (Attribute<?, ?> attr : attrs) {
                    if (generated && attr == dialectModel.id()) {
                        continue; // Skip id when generated
                    }
                    Object value = callback.getAccessor(entityClass).get(entity, attr.name());
                    dialect.bind(ps, paramIndex++, value, attr.javaType());
                }

                int affected = ps.executeUpdate();
                if (affected != 1) {
                    throw new PersistenceException("Insert affected " + affected + " rows, expected 1");
                }

                if (generated) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            Object generatedKey = dialect.extract(rs, 1, dialectModel.id().javaType());
                            callback.getAccessor(entityClass).set(entity, dialectModel.id().name(), generatedKey);
                            return generatedKey;
                        }
                    }
                }

                return callback.getAccessor(entityClass).get(entity, dialectModel.id().name());
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    /**
     * Selects an entity by its id.
     *
     * @param conn the database connection
     * @param entityClass the entity class
     * @param id the id value
     * @param <T> the entity type
     * @return the entity instance, or null if not found
     * @throws PersistenceException if a database error occurs
     */
    public <T> T select(Connection conn, Class<T> entityClass, Object id) {
        Objects.requireNonNull(conn, MSG_CONN_NULL);
        Objects.requireNonNull(entityClass, "entityClass must not be null");
        Objects.requireNonNull(id, "id must not be null");

        try {
            if (conn.isClosed()) {
                throw new PersistenceException("Connection is closed");
            }

            io.vidocq.mansart.persistence.spi.EntityModel<T> spiModel = callback.getEntityModel(entityClass);
            @SuppressWarnings("unchecked")
            io.vidocq.mansart.data.dialect.EntityModel dialectModel = adapter.adapt(spiModel);

            Where where = new Where.Eq(dialectModel.id());
            SqlFragment sqlFragment = dialect.select(dialectModel, where, OrderBy.NONE, Pagination.NONE);

            PreparedStatement ps = conn.prepareStatement(sqlFragment.sql());
            try (ps) {
                dialect.bind(ps, 1, id, dialectModel.id().javaType());

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        T entity = callback.instantiate(entityClass);
                        List<Attribute<?, ?>> attrs = dialectModel.attributes();
                        for (Attribute<?, ?> attr : attrs) {
                            Object value = dialect.extract(rs, rs.findColumn(attr.columnName()), attr.javaType());
                            callback.getAccessor(entityClass).set(entity, attr.name(), value);
                        }
                        return entity;
                    }
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
        return null;
    }

    /**
     * Updates the given entity in the database.
     *
     * @param conn the database connection
     * @param entity the entity to update
     * @param <T> the entity type
     * @return the number of affected rows (expected 1)
     * @throws PersistenceException if a database error occurs
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> int update(Connection conn, T entity) {
        Objects.requireNonNull(conn, MSG_CONN_NULL);
        Objects.requireNonNull(entity, "entity must not be null");

        try {
            if (conn.isClosed()) {
                throw new PersistenceException("Connection is closed");
            }

            Class<T> entityClass = (Class<T>) entity.getClass();
            io.vidocq.mansart.persistence.spi.EntityModel<T> spiModel = callback.getEntityModel(entityClass);
            @SuppressWarnings("unchecked")
            io.vidocq.mansart.data.dialect.EntityModel dialectModel = adapter.adapt(spiModel);

            Where where = new Where.Eq(dialectModel.id());
            SqlFragment sqlFragment = dialect.update(dialectModel, where);

            PreparedStatement ps = conn.prepareStatement(sqlFragment.sql());
            try (ps) {
                String idName = dialectModel.id().name();
                Optional<VersionAttribute<?, ?>> versionOpt = dialectModel.version();
                String versionName = versionOpt.map(VersionAttribute::name).orElse(null);

                int paramIndex = 1;
                List<Attribute<?, ?>> attrs = dialectModel.attributes();
                for (Attribute<?, ?> attr : attrs) {
                    String fieldName = attr.name();
                    if (!fieldName.equals(idName) && !fieldName.equals(versionName)) {
                        Object value = callback.getAccessor(entityClass).get(entity, fieldName);
                        dialect.bind(ps, paramIndex++, value, attr.javaType());
                    }
                }

                Object idValue = callback.getAccessor(entityClass).get(entity, idName);
                dialect.bind(ps, paramIndex, idValue, dialectModel.id().javaType());

                int affected = ps.executeUpdate();
                if (affected != 1) {
                    throw new PersistenceException("Update affected " + affected + " rows, expected 1");
                }
                return affected;
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    /**
     * Deletes the entity with the given id from the database.
     *
     * @param conn the database connection
     * @param entityClass the entity class
     * @param id the id value
     * @param <T> the entity type
     * @return the number of affected rows (expected 1)
     * @throws PersistenceException if a database error occurs
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> int delete(Connection conn, Class<T> entityClass, Object id) {
        Objects.requireNonNull(conn, MSG_CONN_NULL);
        Objects.requireNonNull(entityClass, "entityClass must not be null");
        Objects.requireNonNull(id, "id must not be null");

        try {
            if (conn.isClosed()) {
                throw new PersistenceException("Connection is closed");
            }

            io.vidocq.mansart.persistence.spi.EntityModel<T> spiModel = callback.getEntityModel(entityClass);
            @SuppressWarnings("unchecked")
            io.vidocq.mansart.data.dialect.EntityModel dialectModel = adapter.adapt(spiModel);

            Where where = new Where.Eq(dialectModel.id());
            SqlFragment sqlFragment = dialect.delete(dialectModel, where);

            PreparedStatement ps = conn.prepareStatement(sqlFragment.sql());
            try (ps) {
                dialect.bind(ps, 1, id, dialectModel.id().javaType());

                int affected = ps.executeUpdate();
                if (affected != 1) {
                    throw new PersistenceException("Delete affected " + affected + " rows, expected 1");
                }
                return affected;
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }
}
