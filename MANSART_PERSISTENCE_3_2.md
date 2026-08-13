# Mansart Jakarta Persistence 3.2 :: Plan d'Implementation

**Date**: 2026-08-13  
**Statut**: PLANIFICATION  
**Version**: 1.0.0-DRAFT  

---

## Table des Matières

1. [Contexte et Objectifs](#contexte-et-objectifs)
2. [Architecture Globale](#architecture-globale)
3. [Structure du Projet](#structure-du-projet)
4. [Détails par Module](#détails-par-module)
5. [Intégration avec Mansart Existante](#intégration-avec-mansart-existante)
6. [Jakarta Persistence TCK 3.2](#jakarta-persistence-tck-32)
7. [Phases et Milestones](#phases-et-milestones)
8. [Risques et Mitigations](#risques-et-mitigations)
9. [Prochaines Étapes](#prochaines-étapes)

---

## Contexte et Objectifs

### Pourquoi ce projet ?

Mansart implémente **Jakarta Data 1.0** via `mansart-jakarta-data`. L'ajout de **Jakarta Persistence 3.2 (JPA)** permet une solution complète :
- **Jakarta Data** : Repository pattern, backend JDBC pur
- **Jakarta Persistence** : ORM complet avec EntityManager, JPQL, Criteria API, lifecycle

D'après `ROADMAP.md`, mansart-persistence était **suspendu (M7)** mais devient nécessaire pour :
- Conformité Jakarta EE complète
- Cas d'usage nécessitant EntityManager
- Intégration avec des frameworks existants

### Objectifs Principaux

| Objectif | Priorité |
|----------|----------|
| Implémenter Jakarta Persistence 3.2 API | Critique |
| Passer le TCK Jakarta Persistence 3.2 | Critique |
| Intégration avec les dialects existants (H2, PostgreSQL) | Haute |
| Génération statique de code (APT + Class-File API) | Haute |
| Support des Virtual Threads (JEP 444) | Haute |
| Zero runtime reflection | Moyenne |
| Intégration CDI 4.1 | Moyenne |

---

## Architecture Globale

### Positionnement dans l'écosystème Mansart

```
                    ┌──────────────────────────────┐
   user          →   │   mansart-jakarta-data       │   ← Jakarta Data 1.0
                    └──────────────┬───────────────┘
                                   │ peut déléguer à
                                   ▼
                    ┌──────────────────────────────┐
                    │   mansart-persistence        │   ← Jakarta Persistence 3.2 [NOUVEAU]
                    └──────────────┬───────────────┘
                                   │ utilise
                                   ▼
                    ┌──────────────────────────────┐
                    │   mansart-dialect-spi        │   ← SPI partagé
                    └──────────────┬───────────────┘
                                   │
          ┌────────────────────────┼────────────────────────┐
          ▼                         ▼                         ▼
        H2 (existant)          PostgreSQL (existant)        MySQL (futur)
```

### Réutilisation du code existant

- **Dialect SPI** : `mansart-data-dialect-spi` (déjà existe)
- **Dialect Implementations** : H2, PostgreSQL (déjà existent)
- **ConnectionScope** : Pattern de gestion des connexions
- **APT Patterns** : De `mansart-data-processor`

---

## Structure du Projet

### Arborescence complète

```
mansart/
├── pom.xml                                    # POM racine (mansart-root) - EXISTANT
├── mansart-jakarta-data/                     # EXISTANT
├── mansart-pool/                             # EXISTANT
├── mansart-transactions/                     # EXISTANT
└── mansart-jakarta-persistence/              # **NOUVEAU**
    ├── pom.xml                               # POM parent (modelVersion 4.0.0)
    ├── mansart-persistence-api/              # Re-export Jakarta Persistence API
    │   └── src/main/java/module-info.java
    ├── mansart-persistence-spi/              # SPI interne
    │   └── src/main/java/io/vidocq/mansart/persistence/spi/
    ├── mansart-persistence-core/              # Implémentation runtime
    │   ├── src/main/java/io/vidocq/mansart/persistence/core/
    │   │   ├── bootstrap/                     # PersistenceProvider, EMF
    │   │   │   └── MansartPersistenceProvider.java
    │   │   ├── runtime/                      # EntityManager, Query
    │   │   │   ├── MansartEntityManager.java
    │   │   │   ├── MansartQuery.java
    │   │   │   └── MansartEntityTransaction.java
    │   │   ├── cache/                        # L1 cache
    │   │   │   └── EntityCache.java
    │   │   ├── jpql/                         # JPQL implementation
    │   │   │   ├── JpqlParser.java
    │   │   │   ├── JpqlToRuntimeConverter.java
    │   │   │   └── QueryExecutionContext.java
    │   │   ├── mapping/                      # Entity mapping
    │   │   │   ├── EntityMapping.java
    │   │   │   └── AttributeMapping.java
    │   │   └── lifecycle/                   # Lifecycle callbacks
    │   │       └── CallbackExecutor.java
    │   └── module-info.java
    ├── mansart-persistence-processor/        # APT Annotation Processor
    │   ├── src/main/java/io/vidocq/mansart/persistence/processor/
    │   │   ├── MansartPersistenceProcessor.java
    │   │   ├── apt/                          # APT generators
    │   │   │   └── StaticMetamodelWriter.java
    │   │   └── bytecode/                     # Class-File API (last resort)
    │   │       └── EntityEnhancer.java
    │   └── module-info.java
    ├── mansart-persistence-cdi/              # CDI 4.1 integration
    │   └── src/main/java/io/vidocq/mansart/persistence/cdi/
    │       └── extension/
    │           └── MansartPersistenceExtension.java
    ├── mansart-persistence-tests/            # Unit & Integration tests
    │   └── src/test/java/io/vidocq/mansart/persistence/tests/
    │       ├── unit/
    │       └── integration/
    └── mansart-persistence-tck/              # **HORS REACTOR** - Model 4.0.0 standalone
        ├── pom.xml
        ├── README.md
        ├── setup-tck.sh
        ├── run-tck.sh
        └── src/test/resources/META-INF/
            └── persistence.xml
```

---

## Détails par Module

### 1. mansart-persistence-api

**Objectif**: Ré-exporter Jakarta Persistence 3.2 API sous groupId Mansart.

**module-info.java**:
```java
module io.vidocq.mansart.persistence.api {
    requires transitive jakarta.persistence;
    exports jakarta.persistence;
    exports jakarta.persistence.criteria;
    exports jakarta.persistence.metamodel;
    exports jakarta.persistence.spi;
}
```

### 2. mansart-persistence-spi

**Objectif**: SPI interne pour la communication entre modules.

**Classes clés**:
- `Bootstrap.java` - Interface pour le bootstrap
- `EntityState.java` - Enum NEW/MANAGED/DETACHED/REMOVED
- `EntityMetadata.java` - Metadata des entités

**module-info.java**:
```java
module io.vidocq.mansart.persistence.spi {
    requires io.vidocq.mansart.data.dialect.spi;
    requires jakarta.persistence;
    exports io.vidocq.mansart.persistence.spi;
}
```

### 3. mansart-persistence-core

**Objectif**: Implémentation runtime complète.

**Classes clés**:

**MansartPersistenceProvider.java** (implémente `jakarta.persistence.spi.PersistenceProvider`):
```java
public class MansartPersistenceProvider implements PersistenceProvider {
    @Override
    public EntityManagerFactory createEntityManagerFactory(String emName, Map<String, Object> properties) {
        // 1. Trouver PersistenceUnitInfo par nom
        // 2. Créer EntityManagerFactoryImpl
        // 3. Initialiser avec les propriétés
    }
    
    @Override
    public void generateSchema(PersistenceUnitInfo info, Map<String, Object> properties) {
        // Génération du schema SQL
    }
}
```

**MansartEntityManager.java** (implémente `jakarta.persistence.EntityManager`):
```java
public class MansartEntityManager implements EntityManager {
    private static final ScopedValue<MansartEntityManager> current = ScopedValue.newInstance();
    private final EntityCache entityCache;
    private final Dialect dialect;
    
    // CRUD Operations
    public <T> T persist(T entity) {
        return ScopedValue.where(current, this, () -> doPersist(entity));
    }
    
    public <T> T find(Class<T> entityClass, Object primaryKey) {
        return ScopedValue.where(current, this, () -> doFind(entityClass, primaryKey));
    }
    
    public <T> T merge(T entity) { /* ... */ }
    public void remove(Object entity) { /* ... */ }
    public void refresh(Object entity) { /* ... */ }
    
    // Query Operations
    public Query createQuery(String jpqlString) {
        return new MansartQuery(this, jpqlString);
    }
    
    public <T> TypedQuery<T> createQuery(String jpqlString, Class<T> resultClass) {
        return new MansartTypedQuery<>(this, jpqlString, resultClass);
    }
    
    public CriteriaBuilder getCriteriaBuilder() {
        return new MansartCriteriaBuilder(this);
    }
    
    public Metamodel getMetamodel() { /* ... */ }
    
    // Transaction
    public EntityTransaction getTransaction() {
        return new MansartEntityTransaction(this);
    }
    
    // Persistence Context
    public void clear() { entityCache.clear(); }
    public void detach(Object entity) { /* ... */ }
    public boolean contains(Object entity) { /* ... */ }
    public void close() { /* ... */ }
}
```

**EntityCache.java** (L1 Cache avec IdentityHashMap):
```java
public class EntityCache {
    private final Map<CacheKey, CacheEntry> cache = new IdentityHashMap<>();
    
    public <T> T get(Class<T> entityClass, Object id) { /* ... */ }
    public void put(Object entity, Object id) { /* ... */ }
    public void evict(Object entity) { /* ... */ }
    public void clear() { cache.clear(); }
}
```

**Intégration Dialect** (réutilisation de mansart-data-dialect-spi):
```java
public class MansartEntityManager {
    private final Dialect dialect;
    
    public MansartEntityManager(String dialectName, Map<String, Object> properties) {
        ServiceLoader<DialectFactory> loader = ServiceLoader.load(DialectFactory.class);
        DialectFactory factory = loader.findFirst().orElseThrow();
        this.dialect = factory.createDialect(dialectName, properties);
    }
}
```

**module-info.java**:
```java
module io.vidocq.mansart.persistence.core {
    requires transitive io.vidocq.mansart.persistence.api;
    requires transitive io.vidocq.mansart.persistence.spi;
    requires transitive io.vidocq.mansart.data.dialect.spi;
    requires transitive jakarta.transaction;
    requires java.sql;
    requires java.logging;
    
    exports io.vidocq.mansart.persistence.core.bootstrap;
    exports io.vidocq.mansart.persistence.core.runtime;
    exports io.vidocq.mansart.persistence.core.cache;
    exports io.vidocq.mansart.persistence.core.mapping;
    
    uses io.vidocq.mansart.data.dialect.DialectFactory;
    uses jakarta.transaction.Synchronization;
    
    provides jakarta.persistence.spi.PersistenceProvider 
        with io.vidocq.mansart.persistence.core.bootstrap.MansartPersistenceProvider;
}
```

### 4. mansart-persistence-processor

**Objectif**: Génération statique de code via APT.

**MansartPersistenceProcessor.java**:
```java
@SupportedAnnotationTypes({
    "jakarta.persistence.Entity",
    "jakarta.persistence.MappedSuperclass",
    "jakarta.persistence.Embeddable"
})
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class MansartPersistenceProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        processEntities(roundEnv);
        processMappedSuperclasses(roundEnv);
        processEmbeddables(roundEnv);
        return true;
    }
    
    private void processEntities(RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Entity.class)) {
            if (element instanceof TypeElement typeElement) {
                StaticMetamodelWriter.write(typeElement, processingEnv.getFiler());
                EntitySupportGenerator.generate(typeElement, processingEnv.getFiler());
                if (hasLazyRelationships(typeElement)) {
                    LazyLoadingProxyGenerator.generate(typeElement, processingEnv.getFiler());
                }
            }
        }
    }
}
```

**StaticMetamodelWriter.java**:
```java
public class StaticMetamodelWriter {
    public static void write(TypeElement entity, Filer filer) throws IOException {
        String className = entity.getSimpleName() + "_";
        try (Writer writer = filer.createSourceFile(packageName + "." + className).openWriter()) {
            writer.write("package " + packageName + ";\n\n");
            writer.write("@StaticMetamodel(" + entity.getSimpleName() + ".class)\n");
            writer.write("public abstract class " + className + " {\n\n");
            for (VariableElement field : getPersistentFields(entity)) {
                String attrType = isCollection(field.asType()) ? "PluralAttribute" : "SingularAttribute";
                writer.write("    public static volatile " + attrType + "<" + 
                    entity.getSimpleName() + ", " + field.asType() + "> " + 
                    field.getSimpleName() + ";\n");
            }
            writer.write("}\n");
        }
    }
}
```

**module-info.java**:
```java
module io.vidocq.mansart.persistence.processor {
    requires transitive io.vidocq.mansart.persistence.spi;
    requires transitive io.vidocq.mansart.persistence.api;
    requires java.compiler;
    requires jakarta.persistence;
}
```

### 5. mansart-persistence-cdi

**Objectif**: Intégration CDI 4.1 via **BuildCompatibleExtension** (BCE) avec Vauban.

**IMPORTANT**: Comme `mansart-data-cdi` et `mansart-transactions-cdi`, ce module **doit** implémenter une `BuildCompatibleExtension` pour que Vauban (runtime CDI de Vidocq) puisse découvrir et gérer les beans JPA.

**Structure**:
```
mansart-persistence-cdi/
├── pom.xml
├── src/main/java/io/vidocq/mansart/persistence/cdi/
│   └── extension/
│       ├── MansartPersistenceExtension.java    # BuildCompatibleExtension
│       ├── EntityManagerProducer.java
│       ├── EntityManagerFactoryProducer.java
│       └── PersistenceUnitResolver.java
└── src/main/resources/
    └── META-INF/services/
        └── jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension
```

**Fichier ServiceLoader obligatoire** (`META-INF/services/jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension`):
```
io.vidocq.mansart.persistence.cdi.extension.MansartPersistenceExtension
```

**MansartPersistenceExtension.java** (BuildCompatibleExtension pour Vauban):
```java
package io.vidocq.mansart.persistence.cdi.extension;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Discovery;
import jakarta.enterprise.inject.build.compatible.spi.Enhancement;
import jakarta.enterprise.inject.build.compatible.spi.ScannedClasses;
import jakarta.enterprise.inject.build.compatible.spi.Synthesis;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents;
import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Embeddable;

/**
 * CDI 4.1 BuildCompatibleExtension pour Mansart Persistence.
 * 
 * Comme MansartDataExtension, cette classe permet à Vauban de :
 * 1. Découvrir les classes @Entity, @MappedSuperclass, @Embeddable
 * 2. Enregistrer les producers (EntityManagerProducer, EntityManagerFactoryProducer)
 * 3. Créer des beans synthétiques si nécessaire
 * 
 * Listée dans META-INF/services/jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension
 */
public final class MansartPersistenceExtension implements BuildCompatibleExtension {
    
    /**
     * Phase @Discovery : Enregistre les classes à scanner pour Vauban.
     * Sans cela, Vauban ne verrait pas les producers dans ce JAR.
     */
    @Discovery
    public void registerScannedClasses(ScannedClasses scanned) {
        // Enregistrer les producers pour qu'ils soient visibles par Vauban
        // Vauban-processor indexe ces classes mais ne génère PAS de *_Factory.class
        // dans le module utilisateur (évite les split-packages Java Modules)
        // À l'exécution, Vauban utilise une factory rélective
        scanned.add(EntityManagerProducer.class.getName());
        scanned.add(EntityManagerFactoryProducer.class.getName());
    }
    
    /**
     * Phase @Enhancement : Découvre les entités JPA dans l'application.
     * Utilise types = Object.class + withSubtypes = true pour scanner toutes les classes.
     */
    @Enhancement(types = Object.class, withSubtypes = true)
    public void discoverJpaEntities(ClassInfo info) {
        // Découvrir les classes JPA pour les traiter si nécessaire
        // Par exemple : veto les classes qui ne doivent pas être des beans CDI
        if (info.hasAnnotation(Entity.class) ||
            info.hasAnnotation(MappedSuperclass.class) ||
            info.hasAnnotation(Embeddable.class)) {
            // Les entités JPA ne sont pas des beans CDI par défaut
            // Mais on peut les traiter si nécessaire
        }
    }
    
    /**
     * Phase @Synthesis : Crée des beans synthétiques si nécessaire.
     * Pour JPA, on peut créer des beans pour des services spécifiques.
     */
    @Synthesis
    public void registerPersistenceBeans(SyntheticComponents components) {
        // Pour l'instant, les producers sont suffisant pour la plupart des cas d'usage
        // On peut ajouter des beans synthétiques ici si nécessaire
        // Exemple : bean pour un EntityManager "default"
        
        // components.addBean(EntityManager.class)
        //     .type(EntityManager.class)
        //     .scope(RequestScoped.class)
        //     .createWith(DefaultEntityManagerCreator.class);
    }
}
```

**EntityManagerProducer.java**:
```java
package io.vidocq.mansart.persistence.cdi.extension;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;

/**
 * CDI Producer pour EntityManager.
 * Crée un EntityManager par requête, lié à une PersistenceUnit.
 */
public class EntityManagerProducer {
    
    @Inject
    private EntityManagerFactoryProducer emfProducer;
    
    /**
     * Crée un EntityManager pour la PersistenceUnit spécifiée.
     * Par défaut, utilise la PU "default" ou celle spécifiée via @PersistenceUnit.
     */
    @Produces
    @RequestScoped
    @PersistenceUnit
    public EntityManager createEntityManager(InjectionPoint injectionPoint) {
        String unitName = getPersistenceUnitName(injectionPoint);
        EntityManagerFactory emf = emfProducer.getEntityManagerFactory(unitName);
        return emf.createEntityManager();
    }
    
    /**
     * Récupère le nom de la PersistenceUnit depuis l'injection point.
     */
    private String getPersistenceUnitName(InjectionPoint injectionPoint) {
        PersistenceUnit pu = injectionPoint.getAnnotated()
            .getAnnotation(PersistenceUnit.class);
        return pu != null ? pu.name() : "default";
    }
    
    /**
     * Ferme l'EntityManager à la fin du scope RequestScoped.
     */
    public void closeEntityManager(@Disposes @PersistenceUnit EntityManager em) {
        if (em != null && em.isOpen()) {
            em.close();
        }
    }
}
```

**EntityManagerFactoryProducer.java**:
```java
package io.vidocq.mansart.persistence.cdi.extension;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;

/**
 * CDI Producer pour EntityManagerFactory.
 * Une factory par PersistenceUnit, scope ApplicationScoped.
 */
@ApplicationScoped
public class EntityManagerFactoryProducer {
    
    /**
     * Crée ou récupère une EntityManagerFactory pour la PersistenceUnit spécifiée.
     */
    @Produces
    @ApplicationScoped
    @PersistenceUnit
    public EntityManagerFactory createEntityManagerFactory(InjectionPoint injectionPoint) {
        String unitName = getPersistenceUnitName(injectionPoint);
        return PersistenceUnitResolver.getEntityManagerFactory(unitName);
    }
    
    /**
     * Récupère une EntityManagerFactory par nom (cache les instances).
     */
    public EntityManagerFactory getEntityManagerFactory(String unitName) {
        return PersistenceUnitResolver.getEntityManagerFactory(unitName);
    }
    
    private String getPersistenceUnitName(InjectionPoint injectionPoint) {
        PersistenceUnit pu = injectionPoint.getAnnotated()
            .getAnnotation(PersistenceUnit.class);
        return pu != null ? pu.name() : "default";
    }
}
```

**PersistenceUnitResolver.java**:
```java
package io.vidocq.mansart.persistence.cdi.extension;

import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Résout et cache les EntityManagerFactory pour les PersistenceUnit.
 * Utilise le MansartPersistenceProvider pour créer les factories.
 */
public final class PersistenceUnitResolver {
    
    private static final Map<String, EntityManagerFactory> factoryCache = new ConcurrentHashMap<>();
    
    private PersistenceUnitResolver() {
        // Utility class
    }
    
    /**
     * Récupère ou crée une EntityManagerFactory pour la PersistenceUnit donnée.
     */
    public static EntityManagerFactory getEntityManagerFactory(String unitName) {
        return factoryCache.computeIfAbsent(unitName, name -> {
            // Utilise le provider Mansart pour créer la factory
            // Les propriétés peuvent être lues depuis persistence.xml ou configurées
            return new io.vidocq.mansart.persistence.core.bootstrap.MansartPersistenceProvider()
                .createEntityManagerFactory(name, getPropertiesForUnit(name));
        });
    }
    
    private static Map<String, Object> getPropertiesForUnit(String unitName) {
        // Lire les propriétés depuis persistence.xml ou configuration
        // Pour l'instant, retourne une configuration par défaut
        // TODO: Implémenter la lecture de persistence.xml
        return Map.of();
    }
}
```

**module-info.java**:
```java
module io.vidocq.mansart.persistence.cdi {
    requires transitive io.vidocq.mansart.persistence.core;
    requires transitive io.vidocq.mansart.persistence.api;
    requires jakarta.cdi;
    requires jakarta.inject;
    requires jakarta.persistence;  // Nécessaire pour @Enhancement avec Entity.class, etc.
    
    exports io.vidocq.mansart.persistence.cdi.extension;
    
    // Vauban (ou tout conteneur CDI Lite conforme) instancie les beans de ce JAR
    // via MethodHandles.privateLookupIn. En mode module-path, cela nécessite opens-to.
    // On ouvre de manière restreinte à vauban-core uniquement — le package reste scellé
    // pour tous les autres. Justification: CDI container integration.
    opens io.vidocq.mansart.persistence.cdi.extension to io.vidocq.vauban.core;
    
    // Déclare la BuildCompatibleExtension pour ServiceLoader
    provides jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension
            with io.vidocq.mansart.persistence.cdi.extension.MansartPersistenceExtension;
}
```

**Exemple de pom.xml pour mansart-persistence-cdi** (suivant le pattern de mansart-transactions-cdi):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>io.vidocq.mansart</groupId>
        <artifactId>mansart-jakarta-persistence</artifactId>
        <version>0.3.0-SNAPSHOT</version>
    </parent>
    
    <artifactId>mansart-persistence-cdi</artifactId>
    <name>Mansart :: Persistence :: CDI bootstrap</name>
    <description>CDI 4.1 BuildCompatibleExtension pour l'intégration Jakarta Persistence avec Vauban.</description>
    
    <properties>
        <vauban.version>0.3.0-SNAPSHOT</vauban.version>
    </properties>
    
    <dependencies>
        <!-- Mansart Persistence -->
        <dependency>
            <groupId>io.vidocq.mansart</groupId>
            <artifactId>mansart-persistence-core</artifactId>
        </dependency>
        <dependency>
            <groupId>io.vidocq.mansart</groupId>
            <artifactId>mansart-persistence-api</artifactId>
        </dependency>
        
        <!-- Jakarta specs -->
        <dependency>
            <groupId>jakarta.enterprise</groupId>
            <artifactId>jakarta.enterprise.cdi-api</artifactId>
        </dependency>
        <dependency>
            <groupId>jakarta.inject</groupId>
            <artifactId>jakarta.inject-api</artifactId>
        </dependency>
        <dependency>
            <groupId>jakarta.persistence</groupId>
            <artifactId>jakarta.persistence-api</artifactId>
        </dependency>
        
        <!--
            vauban-api en scope provided : fournit le type VaubanComponentProvider référencé par
            l'APT. Compile-only, pas de dépendance runtime sur Vauban.
            Un conteneur CDI standard (Weld) ignore ce provider.
        -->
        <dependency>
            <groupId>io.vidocq.vauban</groupId>
            <artifactId>vauban-api</artifactId>
            <version>${vauban.version}</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>io.vidocq.vauban</groupId>
                            <artifactId>vauban-processor</artifactId>
                            <version>${vauban.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

**Dépendances Vauban en scope test** (à ajouter dans mansart-persistence-cdi/pom.xml pour les tests) :

```xml
<!-- Dans <dependencies> pour les tests -->
<dependency>
    <groupId>io.vidocq.vauban</groupId>
    <artifactId>vauban-core</artifactId>
    <version>${vauban.version}</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.vidocq.vauban</groupId>
    <artifactId>vauban-classloader-spi</artifactId>
    <version>${vauban.version}</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.vidocq.vauban</groupId>
    <artifactId>vauban-junit</artifactId>
    <version>${vauban.version}</version>
    <scope>test</scope>
</dependency>
```

**Note importante sur Vauban** :
> Comme expliqué dans les commentaires de `mansart-transactions-cdi/pom.xml` :
> - **vauban-api** (provided) : Fournit le type `VaubanComponentProvider` référencé par l'APT
> - **vauban-processor** (annotationProcessorPaths) : APT qui génère `_VaubanComponents`, `META-INF/vauban-beans.list`
> - **vauban-core** (test scope) : Runtime CDI pour les tests
> - Le code généré par APT est **build-time only** : pas de dépendance runtime sur Vauban, inert sous un conteneur CDI standard (Weld)

**Comparaison avec mansart-data-cdi** :

| Aspect | mansart-data-cdi | mansart-persistence-cdi |
|--------|------------------|--------------------------|
| **BCE** | `MansartDataExtension` | `MansartPersistenceExtension` |
| **Découverte** | Scanne `@Repository` interfaces | Scanne `@Entity`, `@MappedSuperclass`, `@Embeddable` |
| **Producers** | `MansartRuntimeProducer` | `EntityManagerProducer`, `EntityManagerFactoryProducer` |
| **Beans synthétiques** | Crée des beans pour les RepositoryImpl | Peut créer des beans pour EntityManager |
| **Approche** | Two paths: compile-time (APT) + runtime (ClassFile API) | Primairement runtime avec producers |
| **ServiceLoader** | `META-INF/services/...BuildCompatibleExtension` | `META-INF/services/...BuildCompatibleExtension` |

### 6. mansart-persistence-tests

**Objectif**: Tests unitaires et d'intégration.

**Structure**:
```
mansart-persistence-tests/
├── unit/
│   ├── entity/           # Entity mapping, lifecycle
│   ├── query/           # JPQL, Criteria API
│   └── cache/           # L1 cache tests
└── integration/
    ├── H2IntegrationTest.java
    └── PostgreSQLIntegrationTest.java
```

### 7. mansart-persistence-tck

**IMPORTANT**: Ce module doit être **HORS du réacteur parent** avec POM **Model 4.0.0 standalone** (sans `<parent>`).

**Raison**: ShrinkWrap Maven Resolver 3.3 (transitif du TCK officiel) ne sait pas parser les POM Model 4.1.0.

**Configuration**:
- **TCK Version**: 3.2.2-SNAPSHOT
- **Repository TCK**: https://github.com/jakartaee/persistence/tree/main/tck
- **Framework**: TestNG + Arquillian
- **Tests**: 1248+ tests

**pom.xml (Model 4.0.0 standalone)**:
```xml
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>io.vidocq.mansart</groupId>
    <artifactId>mansart-persistence-tck</artifactId>
    <version>0.3.0-SNAPSHOT</version>
    <packaging>jar</packaging>
    
    <properties>
        <tck.version>3.2.2-SNAPSHOT</tck.version>
        <mansart.provider>io.vidocq.mansart.persistence.core.bootstrap.MansartPersistenceProvider</mansart.provider>
        <tck.db.url>jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1</tck.db.url>
        <tck.db.user>sa</tck.db.user>
        <tck.db.password></tck.db.password>
        <tck.db.driver>org.h2.Driver</tck.db.driver>
    </properties>
    
    <dependencies>
        <!-- Mansart Persistence -->
        <dependency>
            <groupId>io.vidocq.mansart</groupId>
            <artifactId>mansart-persistence-api</artifactId>
            <version>0.3.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>io.vidocq.mansart</groupId>
            <artifactId>mansart-persistence-core</artifactId>
            <version>0.3.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>io.vidocq.mansart</groupId>
            <artifactId>mansart-persistence-cdi</artifactId>
            <version>0.3.0-SNAPSHOT</version>
        </dependency>
        
        <!-- Jakarta Persistence API -->
        <dependency>
            <groupId>jakarta.persistence</groupId>
            <artifactId>jakarta.persistence-api</artifactId>
            <version>3.2.0</version>
        </dependency>
        
        <!-- Mansart Data Dialects -->
        <dependency>
            <groupId>io.vidocq.mansart</groupId>
            <artifactId>mansart-data-dialect-h2</artifactId>
            <version>0.3.0-SNAPSHOT</version>
        </dependency>
        
        <!-- TCK ( profile tck) -->
        <dependency>
            <groupId>jakarta.tck</groupId>
            <artifactId>persistence-tck-spec-tests</artifactId>
            <version>${tck.version}</version>
            <scope>test</scope>
        </dependency>
        
        <!-- Test frameworks -->
        <dependency>
            <groupId>org.testng</groupId>
            <artifactId>testng</artifactId>
            <version>7.10.2</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.jboss.arquillian</groupId>
            <artifactId>arquillian-bom</artifactId>
            <version>1.9.1.Final</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
    
    <profiles>
        <profile>
            <id>tck</id>
            <dependencies>
                <dependency>
                    <groupId>jakarta.tck</groupId>
                    <artifactId>persistence-tck-spec-tests</artifactId>
                    <version>${tck.version}</version>
                    <scope>test</scope>
                </dependency>
            </dependencies>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-surefire-plugin</artifactId>
                        <configuration>
                            <systemPropertyVariables>
                                <jakarta.persistence.provider>${mansart.provider}</jakarta.persistence.provider>
                                <javax.persistence.provider>${mansart.provider}</javax.persistence.provider>
                                <javax.persistence.jdbc.url>${tck.db.url}</javax.persistence.jdbc.url>
                                <javax.persistence.jdbc.user>${tck.db.user}</javax.persistence.jdbc.user>
                                <javax.persistence.jdbc.password>${tck.db.password}</javax.persistence.jdbc.password>
                                <javax.persistence.jdbc.driver>${tck.db.driver}</javax.persistence.jdbc.driver>
                                <persistence.unit.name>JPATCK</persistence.unit.name>
                                <platform.mode>standalone</platform.mode>
                                <vehicle>standalone</vehicle>
                                <persistence.second.level.caching.supported>false</persistence.second.level.caching.supported>
                            </systemPropertyVariables>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
        </profile>
        <profile>
            <id>pgsql</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-surefire-plugin</artifactId>
                        <configuration>
                            <systemPropertyVariables>
                                <tck.db.url>jdbc:postgresql://localhost:5432/tck_db</tck.db.url>
                                <tck.db.user>tck_user</tck.db.user>
                                <tck.db.password>tck_password</tck.db.password>
                                <tck.db.driver>org.postgresql.Driver</tck.db.driver>
                            </systemPropertyVariables>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>
</project>
```

---

## Intégration avec Mansart Existante

### 1. Réutilisation des Dialects

```java
// Dans MansartEntityManager.java
private final Dialect dialect;

public MansartEntityManager(String dialectName, Map<String, Object> properties) {
    ServiceLoader<DialectFactory> loader = ServiceLoader.load(DialectFactory.class);
    DialectFactory factory = loader.findFirst().orElseThrow();
    this.dialect = factory.createDialect(dialectName, properties);
}
```

**Dépendance Maven** (dans mansart-persistence-core/pom.xml):
```xml
<dependency>
    <groupId>io.vidocq.mansart</groupId>
    <artifactId>mansart-data-dialect-spi</artifactId>
    <version>0.3.0-SNAPSHOT</version>
</dependency>
```

### 2. Réutilisation de ConnectionScope

Le pattern de `mansart-data-core/ConnectionScope.java` peut être réutilisé pour la gestion des connexions.

### 3. Intégration avec Mansart Transactions

Pour JTA support, créer un adapter:
```java
public class TransactionManagerAdapter implements JakartaTransactionManager {
    private final io.vidocq.mansart.transactions.api.TransactionManager tm;
    
    @Override
    public void begin() throws NotSupportedException, SystemException {
        tm.begin();
    }
    
    @Override
    public void commit() throws RollbackException, ... {
        tm.commit();
    }
    
    @Override
    public Transaction getTransaction() throws SystemException {
        return tm.getTransaction();
    }
    
    // ... autres méthodes
}
```

---

## Jakarta Persistence TCK 3.2

### Catégories de tests

| Catégorie | Tests | Priorité |
|-----------|-------|----------|
| Core | 200+ | Critique |
| Persistence Context | 100+ | Critique |
| Entity Mappings | 150+ | Critique |
| Relationships | 200+ | Haute |
| JPQL | 150+ | Critique |
| Criteria API | 100+ | Moyenne |
| Inheritance | 50+ | Moyenne |
| Named Queries | 50+ | Moyenne |
| Locking | 30+ | Moyenne |
| Caching | 30+ | Basse |
| Transactions | 50+ | Haute |
| Validation | 20+ | Basse |

### Objectifs par Phase

| Phase | Objectif | Tests Ciblés |
|-------|----------|--------------|
| **M7** | Core JPA | 400+ PASS |
| **M8** | JPQL Avancé + Criteria API | 1000+ PASS |
| **M9** | TCK Complet | 1248+ PASS |

### Commandes TCK

```bash
# Smoke test (vérification basique)
mvn -pl mansart-persistence-tck test

# TCK complet sur H2
mvn -pl mansart-persistence-tck -Ptck test

# TCK complet sur PostgreSQL
mvn -pl mansart-persistence-tck -Ptck,pgsql test

# Test spécifique
mvn -pl mansart-persistence-tck -Ptck test -Dtest=EntityTest

# Avec debugging
mvn -pl mansart-persistence-tck -Ptck test \
    -Dmansart.sql.log=true \
    -Dmansart.jpql.debug=true \
    -Dmansart.apt.debug=true
```

---

## Phases et Milestones

### Vue d'ensemble

| Milestone | Période | Objectifs | Statut |
|-----------|---------|-----------|--------|
| **M7** | 2026-08 - 2026-09 | Bootstrap, Core JPA, JPQL Basique | A faire |
| **M8** | 2026-10 - 2026-11 | JPQL Avancé, Criteria API, Inheritance | A faire |
| **M9** | 2026-12 - 2027-01 | L2 Cache, Validation, TCK Complet | A faire |

### M7 - Bootstrap et Core JPA (Critique)

| ID | Tâche | Priorité | Durée |
|----|-------|----------|-------|
| M7-1 | Créer structure mansart-jakarta-persistence | Critique | 2j |
| M7-2 | Implémenter mansart-persistence-api | Critique | 1j |
| M7-3 | Implémenter mansart-persistence-spi | Critique | 1j |
| M7-4 | Implémenter MansartPersistenceProvider | Critique | 2j |
| M7-5 | Implémenter EntityManagerFactory | Critique | 2j |
| M7-6 | Implémenter MansartEntityManager (CRUD) | Critique | 3j |
| M7-7 | Implémenter L1 Cache | Haute | 2j |
| M7-8 | Implémenter EntityState management | Haute | 2j |
| M7-9 | Intégrer Dialects existants | Critique | 1j |
| M7-10 | Implémenter JPA Annotations parsing | Critique | 3j |
| M7-11 | Implémenter @Id, @GeneratedValue | Critique | 2j |
| M7-12 | Implémenter Entity mappings | Critique | 2j |
| M7-13 | Implémenter JPQL basique (SELECT, WHERE) | Critique | 3j |
| M7-14 | Implémenter Simple Relationships | Haute | 3j |
| M7-15 | Implémenter Transaction management | Critique | 2j |
| M7-16 | Créer mansart-persistence-processor | Haute | 3j |
| M7-17 | Génération Static Metamodel | Moyenne | 2j |
| M7-18 | Créer mansart-persistence-tests | Moyenne | 2j |
| M7-19 | Configurer mansart-persistence-tck | Critique | 1j |
| M7-20 | Exécuter TCK Smoke | Critique | 1j |
| M7-21 | Corriger échecs TCK Core | Critique | Variable |

**Livrables M7**:
- Structure complète du projet
- Core JPA implémenté (80%)
- TCK : 400+ tests PASS

### M8 - JPQL Avancé et Criteria API

| ID | Tâche | Priorité | Durée |
|----|-------|----------|-------|
| M8-1 | GROUP BY et HAVING | Critique | 3j |
| M8-2 | JOIN syntax (INNER, LEFT, RIGHT) | Critique | 4j |
| M8-3 | Subqueries in FROM | Haute | 3j |
| M8-4 | ALL/ANY/SOME predicates | Haute | 2j |
| M8-5 | Additional JPQL functions | Moyenne | 3j |
| M8-6 | Criteria API | Haute | 5j |
| M8-7 | Inheritance (SINGLE_TABLE) | Moyenne | 3j |
| M8-8 | Inheritance (JOINED) | Moyenne | 2j |
| M8-9 | Inheritance (TABLE_PER_CLASS) | Moyenne | 2j |
| M8-10 | @OneToMany, @ManyToMany | Haute | 3j |
| M8-11 | Lazy Loading | Moyenne | 3j |
| M8-12 | Dirty Tracking | Moyenne | 2j |
| M8-13 | Compléter APT processor | Haute | 3j |
| M8-14 | Named Queries | Moyenne | 2j |
| M8-15 | Native Queries | Moyenne | 2j |
| M8-16 | Compléter tests | Moyenne | 3j |
| M8-17 | Exécuter TCK par catégorie | Critique | Variable |
| M8-18 | Corriger échecs TCK | Critique | Variable |

**Livrables M8**:
- JPQL complet implémenté
- Criteria API implémenté
- TCK : 1000+ tests PASS

### M9 - Finalisation

| ID | Tâche | Priorité | Durée |
|----|-------|----------|-------|
| M9-1 | L2 Cache (optionnel) | Basse | 3j |
| M9-2 | Bean Validation integration | Basse | 2j |
| M9-3 | Lifecycle Callbacks | Moyenne | 2j |
| M9-4 | Entity Listeners | Moyenne | 2j |
| M9-5 | Locking | Moyenne | 2j |
| M9-6 | Stored Procedures | Basse | 2j |
| M9-7 | mansart-persistence-cdi | Moyenne | 3j |
| M9-8 | Optimisations | Moyenne | Variable |
| M9-9 | Documentation complète | Moyenne | 3j |
| M9-10 | TCK complet | Critique | Variable |

**Livrables M9**:
- Implémentation complète
- TCK : 1248+ tests PASS (100% conformité)

---

## Risques et Mitigations

### Risques Techniques

| Risque | Probabilité | Impact | Mitigation |
|--------|-------------|--------|------------|
| Complexité de JPQL | Moyenne | Haut | Décomposer en petites tâches, tester progressivement |
| Intégration TCK | Haute | Haut | Suivre les skills mansart-persistence-tck |
| Performance Virtual Threads | Basse | Moyen | Benchmarker tôt |
| Compatibilité Dialects | Moyenne | Moyen | Tester avec H2 et PostgreSQL |
| Génération APT | Moyenne | Haut | Tester avec entités complexes |

### Stratégies

1. **Développement incrémental** - Valider chaque fonctionnalité
2. **Intégration continue** - Exécuter TCK régulièrement
3. **Réutilisation maximale** - Code existant (Dialects, patterns)
4. **Documentation** - Chaque décision architecturale
5. **Review** - PRs critiques reviewés par plusieurs contributeurs

---

## Prochaines Étapes

### Étapes Immédiates (M7-1 à M7-5)

1. **Créer la structure du projet**
   ```bash
   mkdir -p mansart-jakarta-persistence/mansart-persistence-{api,spi,core,processor,cdi,tests}
   mkdir -p mansart-jakarta-persistence/mansart-persistence-tck
   ```

2. **Créer les POMs**
   - `mansart-jakarta-persistence/pom.xml` (parent)
   - `mansart-persistence-api/pom.xml`
   - `mansart-persistence-spi/pom.xml`
   - `mansart-persistence-core/pom.xml`
   - `mansart-persistence-processor/pom.xml`
   - `mansart-persistence-cdi/pom.xml`
   - `mansart-persistence-tests/pom.xml`
   - `mansart-persistence-tck/pom.xml` (Model 4.0.0 standalone)

3. **Mettre à jour le POM racine**
   ```xml
   <!-- Dans mansart/pom.xml -->
   <modules>
       <module>mansart-pool</module>
       <module>mansart-jakarta-data</module>
       <module>mansart-transactions</module>
       <module>mansart-jakarta-persistence</module>  <!-- AJOUTER -->
   </modules>
   ```

4. **Implémenter les classes de base**
   - `MansartPersistenceProvider.java`
   - `MansartEntityManager.java` (version minimale)
   - `EntityCache.java`
   - `EntityState.java` (enum)

5. **Configurer le TCK**
   - `setup-tck.sh`
   - `run-tck.sh`
   - `persistence.xml`

### Commandes pour démarrer

```bash
# Créer la structure
mkdir -p mansart-jakarta-persistence/mansart-persistence-{api,spi,core,processor,cdi,tests,tck}

# Initialiser les POMs (à partir des templates ci-dessus)
# ...

# Bâtir le projet
cd mansart
mvn clean install -pl mansart-jakarta-persistence -am -DskipTests

# Exécuter les premiers tests
mvn test -pl mansart-jakarta-persistence/mansart-persistence-core

# Configurer et tester le TCK
cd mansart-jakarta-persistence/mansart-persistence-tck
./setup-tck.sh
mvn test  # smoke test
```

---

## Résumé Exécutif

### Points Clés

1. **Nouveau sous-réacteur**: `mansart-jakarta-persistence/` avec 7 modules
2. **Réutilisation maximale**: Dialects, ConnectionScope, patterns existants
3. **Génération de code**: APT first, Class-File API en dernier recours
4. **Virtual Threads**: ScopedValue, pas de ThreadLocal
5. **Zero Reflection**: Génération statique pour metamodel, dirty tracking, lazy loading
6. **TCK**: Module standalone Model 4.0.0
7. **Phases**: M7 (Core), M8 (JPQL Avancé), M9 (Finalisation)

### Estimation Globale

- **M7 (Core JPA)**: ~30-40 jours
- **M8 (JPQL Avancé)**: ~25-35 jours
- **M9 (Finalisation)**: ~15-20 jours
- **Total**: ~70-95 jours (3-4 mois) avec 1-2 développeurs

### Suivi

Ce plan sera mis à jour régulièrement. Créer un `PROGRESS.md` dans `mansart-jakarta-persistence/` pour suivre l'avancement détaillé.

---

## Références

- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/)
- [Jakarta Persistence TCK Repository](https://github.com/jakartaee/persistence/tree/main/tck)
- [ClassFile API (JEP 484)](https://openjdk.org/jeps/484)
- [ScopedValue API (JEP 444)](https://openjdk.org/jeps/444)
- [Virtual Threads (JEP 425)](https://openjdk.org/jeps/425)
- [Vidocq Website](https://vidocq.dev)
- [Mansart Repository](https://codeberg.org/Vidocq/mansart)

---

*Document généré par Mistral Vibe pour le projet Mansart/Vidocq*  
*Date: 2026-08-13*  
*Version: 1.0.0-DRAFT*
