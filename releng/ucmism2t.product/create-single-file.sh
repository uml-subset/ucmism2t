#!/bin/bash
#
# Creates platform-specific self-extracting archives
# Supports: Linux (bash), macOS (bash), Windows (PowerShell)
#

set -e

PRODUCT_DIR="$1"
OUTPUT_FILE="$2"

if [ -z "$PRODUCT_DIR" ] || [ -z "$OUTPUT_FILE" ]; then
    echo "Usage: $0 <product-dir> <output-file-base>"
    exit 1
fi

# Detect platform from product directory structure
if [ -d "$PRODUCT_DIR/linux" ]; then
    PLATFORM="linux"
    PLATFORM_DIR="$PRODUCT_DIR/linux/gtk/x86_64"
    EXE_NAME="ucmism2t"
elif [ -d "$PRODUCT_DIR/win32" ]; then
    PLATFORM="windows"
    PLATFORM_DIR="$PRODUCT_DIR/win32/win32/x86_64"
    EXE_NAME="ucmism2t.exe"
elif [ -d "$PRODUCT_DIR/macosx" ]; then
    PLATFORM="macos"
    PLATFORM_DIR=$(find "$PRODUCT_DIR/macosx" -type d -name "x86_64" -o -name "aarch64" | head -1)
    EXE_NAME="ucmism2t"
else
    echo "Could not detect platform in $PRODUCT_DIR"
    exit 1
fi

if [ ! -d "$PLATFORM_DIR" ]; then
    echo "Platform directory not found: $PLATFORM_DIR"
    exit 1
fi

echo "Detected platform: $PLATFORM"
echo "Creating single-file executable from: $PLATFORM_DIR"

# Create temporary directory for archive
TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

# Copy all files to temp directory
cp -r "$PLATFORM_DIR"/* "$TEMP_DIR/"

# Platform-specific self-extractor
case "$PLATFORM" in
    linux|macos)
        OUTPUT="${OUTPUT_FILE}"
        echo "Output: $OUTPUT"
        
        # Create tar.gz archive
        cd "$TEMP_DIR"
        tar czf "${OUTPUT}.tar.gz" .
        cd - > /dev/null

        # Create bash self-extracting script
        cat > "$OUTPUT" << 'EOF'
#!/bin/bash
# ucmism2t - Self-Extracting Executable
# This file contains the entire application and will extract itself on first run

set -e

# Check if we're being extracted or run
if [ "$1" = "--extract" ]; then
    EXTRACT_DIR="${2:-.}"
    echo "Extracting to: $EXTRACT_DIR"
    mkdir -p "$EXTRACT_DIR"
    sed '1,/^__ARCHIVE_BELOW__$/d' "$0" | tar xz -C "$EXTRACT_DIR"
    echo "Extracted successfully to: $EXTRACT_DIR"
    echo "Run with: $EXTRACT_DIR/ucmism2t"
    exit 0
fi

# Run from temporary location
TEMP_RUN_DIR=$(mktemp -d)
trap "rm -rf $TEMP_RUN_DIR" EXIT

# Extract archive to temp location
sed '1,/^__ARCHIVE_BELOW__$/d' "$0" | tar xz -C "$TEMP_RUN_DIR"

# Execute the application  
exec "$TEMP_RUN_DIR/ucmism2t" "$@"

__ARCHIVE_BELOW__
EOF
        # Append the archive
        cat "${OUTPUT}.tar.gz" >> "$OUTPUT"
        chmod +x "$OUTPUT"
        rm "${OUTPUT}.tar.gz"
        
        echo "Created self-extracting executable: $OUTPUT"
        echo "Size: $(du -h "$OUTPUT" | cut -f1)"
        ;;
        
    windows)
        OUTPUT="${OUTPUT_FILE}.ps1"
        echo "Output: $OUTPUT"
        
        # Create zip archive for Windows
        cd "$TEMP_DIR"
        zip -q -r "${OUTPUT_FILE}.zip" .
        cd - > /dev/null

        # Convert zip to base64 for embedding
        BASE64_DATA=$(base64 -w 0 "${OUTPUT_FILE}.zip")

        # Create PowerShell self-extracting script
        cat > "$OUTPUT" << PSEOF
<#
.SYNOPSIS
    ucmism2t - Self-Extracting Executable for Windows
    
.DESCRIPTION
    This PowerShell script contains the entire ucmism2t application
    and will extract itself on first run.
    
.PARAMETER Extract
    Extract to a directory instead of running
    
.EXAMPLE
    .\ucmism2t.ps1 -input model.xmi -output gen
    
.EXAMPLE
    .\ucmism2t.ps1 -Extract -Path C:\Tools\ucmism2t
#>

param(
    [switch]\$Extract,
    [string]\$Path = ".",
    [Parameter(ValueFromRemainingArguments=\$true)]
    [string[]]\$Arguments
)

# Embedded archive (base64 encoded ZIP)
\$archiveData = @"
${BASE64_DATA}
"@

if (\$Extract) {
    Write-Host "Extracting to: \$Path"
    
    # Create extraction directory
    New-Item -ItemType Directory -Force -Path \$Path | Out-Null
    
    # Decode base64 and save as zip
    \$zipPath = Join-Path \$env:TEMP "ucmism2t-extract.zip"
    \$bytes = [System.Convert]::FromBase64String(\$archiveData)
    [System.IO.File]::WriteAllBytes(\$zipPath, \$bytes)
    
    # Extract zip
    Expand-Archive -Path \$zipPath -DestinationPath \$Path -Force
    Remove-Item \$zipPath
    
    Write-Host "Extracted successfully to: \$Path"
    Write-Host "Run with: \$Path\\ucmism2t.exe"
    exit 0
}

# Run from temporary location
\$tempDir = Join-Path \$env:TEMP ("ucmism2t-run-" + [System.Guid]::NewGuid().ToString())
New-Item -ItemType Directory -Force -Path \$tempDir | Out-Null

try {
    # Extract archive to temp location
    \$zipPath = Join-Path \$env:TEMP "ucmism2t-temp.zip"
    \$bytes = [System.Convert]::FromBase64String(\$archiveData)
    [System.IO.File]::WriteAllBytes(\$zipPath, \$bytes)
    
    Expand-Archive -Path \$zipPath -DestinationPath \$tempDir -Force
    Remove-Item \$zipPath
    
    # Execute the application
    \$exePath = Join-Path \$tempDir "${EXE_NAME}"
    & \$exePath @Arguments
    \$exitCode = \$LASTEXITCODE
    
} finally {
    # Cleanup temp directory
    Remove-Item -Recurse -Force \$tempDir -ErrorAction SilentlyContinue
}

exit \$exitCode
PSEOF

        # Also create a .bat launcher for convenience
        cat > "${OUTPUT_FILE}.bat" << BATEOF
@echo off
REM ucmism2t launcher - calls PowerShell self-extractor
PowerShell.exe -ExecutionPolicy Bypass -File "%~dp0${OUTPUT_FILE##*/}.ps1" %*
BATEOF

        rm "${OUTPUT_FILE}.zip"
        
        echo "Created Windows self-extracting scripts:"
        echo "  PowerShell: $OUTPUT"
        echo "  Batch:      ${OUTPUT_FILE}.bat"
        echo "  Size:       $(du -h "$OUTPUT" | cut -f1)"
        ;;
esac

echo ""
echo "Usage:"
case "$PLATFORM" in
    linux|macos)
        echo "  Run directly:        $OUTPUT [args]"
        echo "  Extract to dir:      $OUTPUT --extract [directory]"
        ;;
    windows)
        echo "  Run directly:        .\\${OUTPUT_FILE##*/}.bat [args]"
        echo "  Or:                  .\\${OUTPUT_FILE##*/}.ps1 [args]"
        echo "  Extract to dir:      .\\${OUTPUT_FILE##*/}.ps1 -Extract -Path [directory]"
        ;;
esac

