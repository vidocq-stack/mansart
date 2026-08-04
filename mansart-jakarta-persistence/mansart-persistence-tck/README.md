# Mansart Jakarta Persistence 3.2 :: TCK

Module d'integration et d'execution du **Jakarta Persistence 3.2 Technology Compatibility Kit** pour l'implementation Mansart.

## Contexte

Le **Jakarta Persistence TCK 3.2.0** est le jeu de tests officiel de conformite pour la specification Jakarta Persistence 3.2. Il n'est pas disponible sur Maven Central et doit donc etre construit localement depuis les sources.

## Pre-requis

- Java 25+
- Maven 3.9+
- Git
- Base de donnees relationnelle (H2 est utilise par defaut)

## Configuration

### 1. Telecharger et installer le TCK

Executer le script de configuration pour cloner, compiler et installer le TCK Jakarta Persistence dans votre repository Maven local :

```bash
cd mansart-jakarta-persistence/mansart-persistence-tck
./setup-tck.sh
```

**Options :**
- `./setup-tck.sh clean` - Supprime le clone local avant de re-telecharger

**Ce que fait le script :**
1. Clone le repository [jakartaee/persistence](https://github.com/jakartaee/persistence) (branche main)
2. Execute `mvn clean install -DskipTests` depuis le dossier `tck`
3. Installe tous les artefacts TCK dans `~/.m2/repository`

**Durée estimée :** 5-15 minutes (selon la vitesse de votre connexion et de votre machine)

### 2. Verifier l'installation

Une fois le script termine, verifiez que les artefacts sont bien installs :

```bash
ls ~/.m2/repository/jakarta/persistence/persistence-tck-*/
```

Vous devriez voir les artefacts suivants :
- `persistence-tck-parent`
- `persistence-tck-common`
- `persistence-tck-spec-tests`
- `persistence-tck-dbprocedures`

## Execution des tests

### Smoke Test (par defaut)

Le profil `smoke` est active par defaut et execute un test simple pour verifier l'integration :

```bash
mvn -pl mansart-persistence-tck test
```

### Execution complete du TCK

Pour executer tous les tests du TCK Jakarta Persistence :

```bash
mvn -pl mansart-persistence-tck -Ptck test
```

### Avec une base de donnees PostgreSQL

```bash
mvn -pl mansart-persistence-tck -Ptck,pgsql test
```

**Pre-requis PostgreSQL :**
- Base de donnees `tck_db` creee
- Utilisateur `tck_user` avec mot de passe `tck_password`
- Droits suffisants sur la base

### Configuration personnalisee

Vous pouvez surcharger les proprietes par defaut via la ligne de commande :

```bash
mvn -pl mansart-persistence-tck test \
    -Dtck.db.url=jdbc:h2:mem:custom-db \
    -Dtck.db.user=custom_user \
    -Dtck.db.password=custom_pass \
    -Dmansart.provider=io.vidocq.mansart.persistence.core.bootstrap.MansartPersistenceProvider
```

## Structure du module

```
mansart-persistence-tck/
├── pom.xml                    # Configuration Maven
├── setup-tck.sh               # Script de configuration du TCK
├── README.md                  # Ce fichier
└── src/
    └── test/
        └── java/
            └── io/vidocq/mansart/persistence/tck/
                └── MansartPersistenceTckSmokeTest.java  # Test d'integration
```

## Configuration Maven

### Dependances

Le module depend des artefacts suivants :

**Implementation Mansart :**
- `mansart-persistence-api`
- `mansart-persistence-core`
- `mansart-persistence-cdi`
- `mansart-data-dialect-h2`
- `mansart-data-dialect-postgresql`

**Jakarta Persistence TCK :**
- `persistence-tck-common` (version 4.0.0-SNAPSHOT)
- `persistence-tck-spec-tests` (version 4.0.0-SNAPSHOT)
- `persistence-tck-dbprocedures` (version 4.0.0-SNAPSHOT)

**Autres :**
- `jakarta.persistence-api` (version 3.2.0)
- `jakarta.enterprise.cdi-api` (version 4.1.0)
- `h2` (pour les tests avec H2)
- `postgresql` (pour les tests avec PostgreSQL)

### Profils Maven

| Profil | Description |
|--------|-------------|
| `smoke` (par defaut) | Execute uniquement les tests d'integration simples |
| `tck` | Execute tous les tests du TCK Jakarta Persistence |
| `pgsql` | Configure l'utilisation de PostgreSQL au lieu de H2 |

## Depannage

### Erreur : "Cannot resolve jakarta.persistence:persistence-tck-*"

**Solution :** Executer `./setup-tck.sh` pour installer le TCK localement.

### Erreur : "No Persistence provider for EntityManager"

**Solution :** Verifier que la propriete `jakarta.persistence.provider` est bien configuree :

```bash
mvn -pl mansart-persistence-tck test \
    -Djakarta.persistence.provider=io.vidocq.mansart.persistence.core.bootstrap.MansartPersistenceProvider
```

### Erreur de connection a la base de donnees

**Solution :** Verifier que la base de donnees est en cours d'execution et que les identifiants sont corrects. Pour H2, aucune configuration n'est necessaire (base en memoire).

## Contribuer

Pour contribuer a l'implementation Mansart Jakarta Persistence :

1. Forker le repository [Vidocq/mansart](https://codeberg.org/Vidocq/mansart)
2. Creer une branche de fonctionnalite
3. Implémenter les fonctionnalites manquantes (identifier via les echecs du TCK)
4. Executer le TCK pour verifier la conformite
5. Ouvrir une Pull Request

## Liens utiles

- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/)
- [Jakarta Persistence TCK Repository](https://github.com/jakartaee/persistence/tree/main/tck)
- [Mansart Project](https://codeberg.org/Vidocq/mansart)
- [Vidocq Ecosystem](https://vidocq.dev)
