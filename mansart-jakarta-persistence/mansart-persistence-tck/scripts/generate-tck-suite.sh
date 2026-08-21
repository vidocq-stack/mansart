#!/bin/bash
# Generate a TestNG suite XML from the persistence-tck-spec-tests jar
set -euo pipefail

TCK_JAR="$1"
OUTPUT="$2"

if [ ! -f "$TCK_JAR" ]; then
    echo "ERROR: TCK jar not found: $TCK_JAR"
    exit 1
fi

mkdir -p "$(dirname "$OUTPUT")"

{
    echo '<?xml version="1.0" encoding="UTF-8"?>'
    echo '<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">'
    echo '<suite name="Mansart Persistence TCK Suite" parallel="none">'
    echo '    <test name="Jakarta Persistence 3.2 TCK">'
    echo '        <classes>'
    # Extract class names from jar - unzip -l format: "  size  date time   path"
    unzip -l "$TCK_JAR" 2>/dev/null | awk '/Client\.class$/ {gsub(/\.class$/, "", $NF); print $NF}' | \
        while read classname; do
            echo "            <class name=\"$classname\"/>"
        done
    echo '        </classes>'
    echo '    </test>'
    echo '</suite>'
} > "$OUTPUT"

count=$(grep -c '<class name=' "$OUTPUT" || true)
echo "Generated $OUTPUT with $count test classes"
