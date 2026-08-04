#!/bin/bash
#
# Mansart Jakarta Persistence 3.2 - TCK Setup Script
#
# Ce script télécharge, compile et installe le Jakarta Persistence TCK 3.2.0
# dans le repository Maven local, afin de permettre son utilisation par
# le module mansart-persistence-tck.
#
# Le TCK officiel n'est pas disponible sur Maven Central, il faut donc
# le builder depuis les sources GitHub.
#
# Usage:
#   ./setup-tck.sh [clean]
#
# Options:
#   clean  - Supprime le clone local avant de re-télécharger
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TCK_REPO="https://github.com/jakartaee/persistence.git"
TCK_BRANCH="3.2-TCK"
TCK_CLONE_DIR="${SCRIPT_DIR}/target/jakarta-persistence-tck"

echo "============================================"
echo "Mansart Jakarta Persistence TCK Setup"
echo "============================================"

# Vérifier Maven
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven (mvn) est requis mais n'est pas trouvé dans le PATH"
    exit 1
fi

# Vérifier Java
if ! command -v java &> /dev/null; then
    echo "ERROR: Java est requis mais n'est pas trouvé dans le PATH"
    exit 1
fi

echo "Java version: $(java -version 2>&1 | head -1)"
echo "Maven version: $(mvn -version 2>&1 | head -1)"

# Clean si demandé
if [ "$1" = "clean" ]; then
    echo "Suppression du clone TCK existant..."
    rm -rf "${TCK_CLONE_DIR}"
fi

# Cloner ou mettre à jour le repository TCK
if [ ! -d "${TCK_CLONE_DIR}/.git" ]; then
    echo "Clonage du repository Jakarta Persistence TCK..."
    git clone --depth 1 --branch "${TCK_BRANCH}" "${TCK_REPO}" "${TCK_CLONE_DIR}"
else
    echo "Mise à jour du repository Jakarta Persistence TCK..."
    cd "${TCK_CLONE_DIR}"
    git fetch origin
    git checkout "${TCK_BRANCH}"
    git pull origin "${TCK_BRANCH}"
    cd "${SCRIPT_DIR}"
fi

# Builder et installer le TCK
cd "${TCK_CLONE_DIR}/tck"

echo ""
echo "Construction et installation du Jakarta Persistence TCK..."
echo "Cela peut prendre plusieurs minutes..."
echo ""

# Builder avec le profil pour sauter les tests (on veut juste installer les artefacts)
mvn clean install -DskipTests -B -q

cd "${SCRIPT_DIR}"

echo ""
echo "============================================"
echo "TCK installé avec succès!"
echo "============================================"
echo ""
echo "Les artefacts TCK sont maintenant disponibles dans votre repository Maven local."
echo "Vous pouvez exécuter les tests TCK avec:"
echo "  mvn -pl mansart-persistence-tck test"
echo ""
echo "Ou avec le profil TCK:"
echo "  mvn -Ptck test"
echo ""
