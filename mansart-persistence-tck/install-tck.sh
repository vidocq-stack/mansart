#!/bin/bash
/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */

# Jakarta Persistence 3.2 TCK Installation Script
# This script helps download and install the official Jakarta Persistence TCK
# which is required to run the Mansart TCK tests.

set -e

echo "========================================="
echo " Jakarta Persistence 3.2 TCK Installer"
echo "========================================="
echo ""

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven (mvn) is not installed or not in PATH"
    echo "Please install Maven 3.9+ before running this script"
    exit 1
fi

# Check if we're in the right directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ ! -f "$SCRIPT_DIR/pom.xml" ]; then
    echo "ERROR: This script must be run from the mansart-persistence-tck directory"
    echo "Current directory: $PWD"
    echo "Script directory: $SCRIPT_DIR"
    exit 1
fi

echo "✓ Running from: $SCRIPT_DIR"
echo ""

# Default TCK version
TCK_VERSION="3.2.0"
TCK_GROUP="jakarta.persistence"
TCK_ARTIFACT="jakarta.persistence-tck"

# Check if TCK is already installed
echo "Checking if TCK is already installed in local Maven repository..."
if mvn dependency:tree -Dincludes=$TCK_GROUP:$TCK_ARTIFACT:$TCK_VERSION -q 2>/dev/null | grep -q "$TCK_ARTIFACT"; then
    echo "✓ TCK $TCK_VERSION is already installed!"
    echo ""
    echo "You can now run the TCK tests with:"
    echo "  mvn clean test -Ptck-run"
    exit 0
fi

echo "✗ TCK $TCK_VERSION is NOT installed in local Maven repository"
echo ""

# Try to find the TCK on Maven Central
echo "Searching for TCK on Maven Central..."
TCK_URL="https://repo1.maven.org/maven2/$TCK_GROUP/$TCK_ARTIFACT/$TCK_VERSION/"

if curl --output /dev/null --silent --head --fail "$TCK_URL$TCK_ARTIFACT-$TCK_VERSION.jar"; then
    echo "✓ Found TCK on Maven Central"
    echo ""
    echo "Installing TCK from Maven Central..."
    
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
        echo "✓ TCK successfully installed from Maven Central"
        rm /tmp/tck-installer.pom
        cd "$SCRIPT_DIR"
        echo ""
        echo "You can now run the TCK tests with:"
        echo "  mvn clean test -Ptck-run"
    else
        echo "✗ Failed to install TCK from Maven Central"
        rm /tmp/tck-installer.pom
        cd "$SCRIPT_DIR"
    fi
else
    echo "✗ TCK not found on Maven Central"
fi

echo ""
echo "Alternative options:"
echo ""
echo "1. Manual download from Eclipse Foundation:"
echo "   wget https://download.eclipse.org/jakarta/persistence/3.2.0/jakarta.persistence-tck-3.2.0.zip"
echo "   unzip jakarta.persistence-tck-3.2.0.zip"
echo "   cd jakarta.persistence-tck-3.2.0"
echo "   mvn install -DskipTests"
echo ""
echo "2. Add Eclipse repository to your Maven settings.xml:"
echo "   See README.md for configuration details"
echo ""
echo "3. Check if TCK is available at a different location"
echo ""
