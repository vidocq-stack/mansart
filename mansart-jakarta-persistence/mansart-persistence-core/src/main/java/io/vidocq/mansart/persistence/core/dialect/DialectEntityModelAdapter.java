/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.dialect;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.vidocq.mansart.data.dialect.Attribute;
import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.data.dialect.attribute.BooleanAttribute;
import io.vidocq.mansart.data.dialect.attribute.NumericAttribute;
import io.vidocq.mansart.data.dialect.attribute.ReferenceAttribute;
import io.vidocq.mansart.data.dialect.attribute.TextAttribute;
import io.vidocq.mansart.data.dialect.attribute.TemporalAttribute;

/**
 * Adapts a persistence-spi {@link io.vidocq.mansart.persistence.spi.EntityModel} to a data-dialect
 * {@link EntityModel} record so the Dialect can generate SQL.
 * <p>
 * Constructs data-dialect Attribute records from persistence-spi Attributes.
 * The MethodHandle getter/setter/constructor parameters are set to null since
 * the EntityMapper reads entity values via MansartCallback.getAccessor().
 */
public final class DialectEntityModelAdapter {

    /**
     * Adapts a persistence-spi EntityModel to a data-dialect EntityModel.
     *
     * @param spiModel the persistence-spi EntityModel
     * @param <T> the entity type
     * @return the data-dialect EntityModel
     * @throws IllegalArgumentException if spiModel is null
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public EntityModel adapt(io.vidocq.mansart.persistence.spi.EntityModel spiModel) {
        if (spiModel == null) {
            throw new IllegalArgumentException("spiModel must not be null");
        }

        // Build id attribute from first persistence Attribute
        @SuppressWarnings("unchecked")
        io.vidocq.mansart.persistence.spi.Attribute<?, ?> firstIdAttr = ((java.util.List<io.vidocq.mansart.persistence.spi.Attribute<?, ?>>) spiModel.getIdAttributes()).get(0);
        io.vidocq.mansart.data.dialect.attribute.IdAttribute idAttribute = buildIdAttribute(firstIdAttr);

        // Build version attribute if present
        Optional<io.vidocq.mansart.data.dialect.attribute.VersionAttribute<?, ?>> versionAttribute = spiModel.getVersionAttribute() == null
                ? Optional.empty()
                : Optional.of(buildVersionAttribute(spiModel.getVersionAttribute()));

        // Build all attributes
        List<Attribute<?, ?>> attributes = new ArrayList<>();
        @SuppressWarnings("rawtypes")
        List<io.vidocq.mansart.persistence.spi.Attribute<?, ?>> spiAttributes = spiModel.getAttributes();
        for (io.vidocq.mansart.persistence.spi.Attribute<?, ?> spiAttr : spiAttributes) {
            Attribute<?, ?> adapted;
            // Use the same idAttribute instance for the id attribute in the attributes list
            if (spiAttr.getName().equals(firstIdAttr.getName())) {
                adapted = idAttribute;
            } else {
                adapted = buildAttribute(spiAttr);
            }
            attributes.add(adapted);
        }

        // Build constructor MethodHandle (null for adapter)
        MethodHandle constructor = null;

        return new EntityModel(
                spiModel.getEntityClass(),
                spiModel.getTableName(),
                Optional.ofNullable(spiModel.getSchema()).orElse(""),
                idAttribute,
                versionAttribute,
                attributes,
                constructor
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private io.vidocq.mansart.data.dialect.attribute.IdAttribute buildIdAttribute(io.vidocq.mansart.persistence.spi.Attribute<?, ?> spiIdAttr) {
        // Determine if the id is generated based on the generation strategy
        // Only IDENTITY and AUTO should set generated=true (post-insert strategies)
        // SEQUENCE and TABLE allocate IDs before INSERT and must be included in the INSERT
        boolean generated = spiIdAttr instanceof io.vidocq.mansart.persistence.spi.IdAttribute<?, ?> idAttr
            && idAttr.getGenerationStrategy() != null
            && (idAttr.getGenerationStrategy() == io.vidocq.mansart.persistence.spi.IdAttribute.GenerationStrategy.IDENTITY
                || idAttr.getGenerationStrategy() == io.vidocq.mansart.persistence.spi.IdAttribute.GenerationStrategy.AUTO);
        Class<?> javaType = spiIdAttr.getJavaType();
        return new io.vidocq.mansart.data.dialect.attribute.IdAttribute(
                spiIdAttr.getName(),
                spiIdAttr.getColumnName(),
                javaType,
                spiIdAttr.getEntityModel().getEntityClass(),
                generated,
                null, // getter
                null  // setter
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private io.vidocq.mansart.data.dialect.attribute.VersionAttribute buildVersionAttribute(io.vidocq.mansart.persistence.spi.Attribute<?, ?> spiVersion) {
        Class<?> javaType = spiVersion.getJavaType();
        return new io.vidocq.mansart.data.dialect.attribute.VersionAttribute(
                spiVersion.getName(),
                spiVersion.getColumnName(),
                javaType,
                spiVersion.getEntityModel().getEntityClass(),
                null, // getter
                null  // setter
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Attribute<?, ?> buildAttribute(io.vidocq.mansart.persistence.spi.Attribute<?, ?> spiAttr) {
        if (spiAttr instanceof io.vidocq.mansart.persistence.spi.VersionAttribute) {
            // Handle version attribute
            io.vidocq.mansart.persistence.spi.VersionAttribute<?, Number> versionAttr = (io.vidocq.mansart.persistence.spi.VersionAttribute<?, Number>) spiAttr;
            return buildVersionAttribute(versionAttr);
        } else if (spiAttr instanceof io.vidocq.mansart.persistence.spi.NumericAttribute) {
            io.vidocq.mansart.persistence.spi.NumericAttribute<?, ?> numericAttr = (io.vidocq.mansart.persistence.spi.NumericAttribute<?, ?>) spiAttr;
            @SuppressWarnings("unchecked")
            Class<Number> javaType = (Class<Number>) (Class<?>) numericAttr.getJavaType();
            return new NumericAttribute(
                    numericAttr.getName(),
                    numericAttr.getColumnName(),
                    javaType,
                    numericAttr.getEntityModel().getEntityClass(),
                    numericAttr.isNullable(),
                    numericAttr.isUnique(),
                    numericAttr.getPrecision(),
                    numericAttr.getScale(),
                    null, // getter
                    null  // setter
            );
        } else if (spiAttr instanceof io.vidocq.mansart.persistence.spi.EnumAttribute) {
            io.vidocq.mansart.persistence.spi.EnumAttribute<?, ?> enumAttr = (io.vidocq.mansart.persistence.spi.EnumAttribute<?, ?>) spiAttr;
            @SuppressWarnings("unchecked")
            Class<Enum<?>> javaType = (Class<Enum<?>>) (Class<?>) enumAttr.getJavaType();
            Class<?> entityClass = enumAttr.getEntityModel().getEntityClass();
            return new io.vidocq.mansart.data.dialect.attribute.EnumAttribute(
                    enumAttr.getName(),
                    enumAttr.getColumnName(),
                    javaType,
                    entityClass,
                    enumAttr.isNullable(),
                    enumAttr.isUnique(),
                    io.vidocq.mansart.data.dialect.attribute.EnumStorage.STRING, // default storage
                    null, // getter
                    null  // setter
            );
        } else if (spiAttr instanceof io.vidocq.mansart.persistence.spi.TemporalAttribute) {
            io.vidocq.mansart.persistence.spi.TemporalAttribute<?, ?> temporalAttr = (io.vidocq.mansart.persistence.spi.TemporalAttribute<?, ?>) spiAttr;
            @SuppressWarnings("unchecked")
            Class<Object> javaType = (Class<Object>) (Class<?>) temporalAttr.getJavaType();
            Class<?> entityClass = temporalAttr.getEntityModel().getEntityClass();
            return new TemporalAttribute(
                    temporalAttr.getName(),
                    temporalAttr.getColumnName(),
                    javaType,
                    entityClass,
                    temporalAttr.isNullable(),
                    temporalAttr.isUnique(),
                    null, // getter
                    null  // setter
            );
        } else if (spiAttr instanceof io.vidocq.mansart.persistence.spi.ReferenceAttribute) {
            io.vidocq.mansart.persistence.spi.ReferenceAttribute<?, ?> refAttr = (io.vidocq.mansart.persistence.spi.ReferenceAttribute<?, ?>) spiAttr;
            @SuppressWarnings("unchecked")
            Class<Object> javaType = (Class<Object>) (Class<?>) refAttr.getJavaType();
            Class<?> entityClass = refAttr.getEntityModel().getEntityClass();
            return new ReferenceAttribute(
                    refAttr.getName(),
                    refAttr.getColumnName(),
                    javaType,
                    entityClass,
                    refAttr.isNullable(),
                    refAttr.isUnique(),
                    false, // lazy (simplified)
                    null, // referencedColumnName (not available in SPI)
                    null, // getter
                    null  // setter
            );
        } else {
            // Fallback for basic attributes (String, Integer, Boolean, etc.)
            // Check if it's a basic numeric type
            Class<?> javaType = spiAttr.getJavaType();
            if (Number.class.isAssignableFrom(javaType) || javaType == int.class || javaType == long.class || javaType == float.class || javaType == double.class) {
                // Treat as numeric
                @SuppressWarnings({"rawtypes", "unchecked"})
                Class<Number> numberType = (Class) javaType;
                return new NumericAttribute(
                        spiAttr.getName(),
                        spiAttr.getColumnName(),
                        numberType,
                        spiAttr.getEntityModel().getEntityClass(),
                        spiAttr.isNullable(),
                        spiAttr.isUnique(),
                        -1, // precision
                        -1, // scale
                        null, // getter
                        null  // setter
                );
            } else if (javaType == String.class) {
                return new TextAttribute(
                    spiAttr.getName(),
                    spiAttr.getColumnName(),
                    spiAttr.getEntityModel().getEntityClass(),
                    spiAttr.isNullable(),
                    spiAttr.isUnique(),
                    spiAttr.getLength(),
                    null, // getter
                    null  // setter
                );
            } else if (javaType == Boolean.class || javaType == boolean.class) {
                return new BooleanAttribute(
                        spiAttr.getName(),
                        spiAttr.getColumnName(),
                        spiAttr.getEntityModel().getEntityClass(),
                        spiAttr.isNullable(),
                        spiAttr.isUnique(),
                        null, // getter
                        null  // setter
                );
            } else {
                // Generic fallback to TextAttribute for other types
                return new TextAttribute(
                    spiAttr.getName(),
                    spiAttr.getColumnName(),
                    spiAttr.getEntityModel().getEntityClass(),
                    spiAttr.isNullable(),
                    spiAttr.isUnique(),
                    spiAttr.getLength(),
                    null, // getter
                    null  // setter
                );
            }
        }
    }
}
