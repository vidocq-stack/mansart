#!/bin/bash

# Jakarta Persistence 3.2 TCK Installation Script

set -e

echo "========================================="
echo " Jakarta Persistence 3.2 TCK Installer"
echo "========================================="
echo ""

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven (mvn) is not installed or not in PATH"
    exit 1
fi

# Check if we're in the right directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ ! -f "$SCRIPT_DIR/pom.xml" ]; then
    echo "ERROR: This script must be run from the mansart-persistence-tck directory"
    exit 1
fi

echo "Running from: $SCRIPT_DIR"
echo ""

TCK_VERSION="3.2.0"
TCK_GROUP="jakarta.persistence"
TCK_ARTIFACT="jakarta.persistence-tck"

echo "Checking if TCK is already installed..."
if mvn dependency:tree -Dincludes=$TCK_GROUP:$TCK_ARTIFACT:$TCK_VERSION -q 2>/dev/null | grep -q "$TCK_ARTIFACT"; then
    echo "TCK $TCK_VERSION is already installed!"
    echo ""
    echo "You can now run the TCK tests with:"
    echo "  mvn clean test -Ptck-run"
    exit 0
fi

echo "TCK $TCK_VERSION is NOT installed"
echo ""

echo "Trying to install from Maven Central..."

# Create a temporary pom to install the TCK
cat > /tmp/tck-installer.pom << 'EOF'
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>tck.installer</groupId>
  <artifactId>tck-installer</artifactId>
  <version>1.0</version>
  <dependencies>
    <dependency>
      <groupId>jakarta.persistence</groupId>
      <artifactId>jakarta.persistence-tck</artifactId>
      <version>3.2.0</version>
    </dependency>
  </dependencies>
</project>
EOF

cd /tmp
mvn -f tck-installer.pom dependency:go-offline -q
if [ $? -eq 0 ]; then
    echo "TCK successfully installed from Maven Central"
    rm /tmp/tck-installer.pom
    cd "$SCRIPT_DIR"
else
    echo "Failed to install TCK from Maven Central"
    rm /tmp/tck-installer.pom
    cd "$SCRIPT_DIR"
    echo ""
    echo "Alternative: Add Eclipse repository to your Maven settings.xml:"
    echo "  <repository>"
    echo "    <id>eclipse-jakarta</id>"
    echo "    <url>https://repo.eclipse.org/content/repositories/jakarta-releases/</url>"
    echo "  </repository>"
fi
