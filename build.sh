#!/bin/bash

################################################################################
# Build Script for ucmism2t
#
# This script builds the complete ucmism2t project using Maven/Tycho.
# It handles all build phases from compilation to product packaging.
################################################################################

set -e  # Exit on error

echo "╔════════════════════════════════════════════════════════════╗"
echo "║           Building ucmism2t Project                        ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Check Java version
echo "Checking Java version..."
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "Error: Java 21 or later is required (found Java $JAVA_VERSION)"
    exit 1
fi
echo "✓ Java version: $(java -version 2>&1 | head -n 1)"
echo ""

# Check Maven version
echo "Checking Maven version..."
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven not found. Please install Maven 3.9 or later."
    exit 1
fi
echo "✓ Maven version: $(mvn -version | head -n 1)"
echo ""

# Clean build
echo "Cleaning previous build artifacts..."
mvn clean
echo ""

# Build
echo "Building project..."
mvn verify

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║              Build Completed Successfully!                 ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""
echo "Products are available in:"
echo "  releng/ucmism2t.product/target/products/"
echo ""
