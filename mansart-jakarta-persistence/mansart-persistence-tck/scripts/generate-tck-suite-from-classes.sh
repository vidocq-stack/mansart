#!/bin/bash
# Generate a TestNG suite XML from unpacked test-classes directory
set -euo pipefail

TEST_CLASSES_DIR="$1"
OUTPUT="$2"

if [ ! -d "$TEST_CLASSES_DIR" ]; then
    echo "ERROR: Test classes directory not found: $TEST_CLASSES_DIR"
    exit 1
fi

mkdir -p "$(dirname "$OUTPUT")"

{
    echo '<?xml version="1.0" encoding="UTF-8"?>'
    echo '<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">'
    echo '<suite name="Mansart Persistence TCK Suite" parallel="none">'
    echo '    <test name="Jakarta Persistence 3.2 TCK">'
    echo '        <classes>'
    find "$TEST_CLASSES_DIR" -name 'Client.class' -type f | while read -r classfile; do
        # Convert file path to class name: remove .class, replace / with ., remove prefix
        classname="${classfile%.class}"
        classname="${classname#$TEST_CLASSES_DIR/}"
        classname="${classname//\//.}"
        echo "            <class name=\"$classname\"/>"
    done
    echo '        </classes>'
    echo '    </test>'
    echo '</suite>'
} > "$OUTPUT"

count=$(grep -c '<class name=' "$OUTPUT" || true)
echo "Generated $OUTPUT with $count test classes"
