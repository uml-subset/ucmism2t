# ucmism2t - UML to Text Model-to-Text Transformation Tool

## Overview

`ucmism2t` is a command-line tool for transforming UML 2.5 models into text artifacts using Acceleo 4.2 templates. It provides a flexible, configuration-driven approach to model-to-text transformation suitable for code generation, documentation generation, and continuous integration pipelines.

## Technology Stack

- **Eclipse Modeling Tools**: 2025-12
- **Acceleo**: 4.2 (R202602100910)
- **UML2**: 5.0.0
- **Tycho**: 5.0
- **Java**: 21
- **Maven**: 3.9+

## Features

- ✅ UML 2.5 model support with standard profiles
- ✅ Acceleo 4.2 MTL templates with AQL queries
- ✅ Custom Java services callable from templates
- ✅ Cascading configuration via properties files
- ✅ Multi-platform executables (Linux, Windows, macOS)
- ✅ CI/CD ready command-line interface
- ✅ ISO 8601 date/time formatting
- ✅ Comprehensive error handling and logging

## Project Structure

```
ucmism2t/
├── bundles/
│   ├── ucmism2t.core/       # Templates and services
│   └── ucmism2t.cli/        # Command-line interface
├── features/
│   └── ucmism2t.feature/    # Feature definition
├── releng/
│   ├── ucmism2t.target/     # Target platform
│   └── ucmism2t.product/    # Product packaging
└── repository/
    └── ucmism2t.repository/ # P2 repository
```

## Prerequisites

- **Java Development Kit (JDK)**: 21 or later
- **Apache Maven**: 3.9 or later
- **UML Model**: Valid .uml file (UML 2.5 format)

## Building

### Full Build

```bash
# Build all modules and create executables
mvn clean verify

# Build output location:
# releng/ucmism2t.product/target/products/
```

### Quick Build (Skip Tests)

```bash
mvn clean verify -DskipTests
```

### Build Specific Module

```bash
# Build only core bundle
cd bundles/ucmism2t.core
mvn clean install
```

## Installation

After building, executables are located in:

```
releng/ucmism2t.product/target/products/
├── ucmism2t.product-linux.gtk.x86_64.tar.gz
├── ucmism2t.product-win32.win32.x86_64.zip
├── ucmism2t.product-macosx.cocoa.x86_64.tar.gz
└── ucmism2t.product-macosx.cocoa.aarch64.tar.gz
```

### Linux/macOS Installation

```bash
# Extract archive
tar -xzf ucmism2t.product-linux.gtk.x86_64.tar.gz

# Optional: Add to PATH
export PATH=$PATH:/path/to/ucmism2t
```

### Windows Installation

1. Extract the ZIP file
2. Add the installation directory to your PATH environment variable
3. Run `ucmism2t.exe` from command prompt

## Usage

### Recommended: Use Wrapper Script

For clean output without Eclipse launcher messages, use the wrapper:

```bash
./ucmism2t-wrapper.sh -input model.uml -output ./generated
```

### Direct Launcher (shows verbose output)

```bash
./ucmism2t -input model.uml -output ./generated
```

**Note:** The direct launcher may show Eclipse framework messages on stderr.
Use the wrapper script for production use.

### Basic Usage

```bash
ucmism2t-wrapper.sh -input model.uml -output ./generated
```

### With Configuration Files

```bash
ucmism2t-wrapper.sh -input model.uml \
         -output ./generated \
         -config defaults.properties,production.properties
```

### Command-Line Options

| Option | Required | Description |
|--------|----------|-------------|
| `-input <path>` | Yes | Path to input UML model file (.uml) |
| `-output <path>` | Yes | Path to output directory for generated files |
| `-config <files>` | No | Comma-separated list of configuration files |

### Configuration Files

Configuration files use Java properties format:

```properties
# defaults.properties

# Output settings
output.encoding=UTF-8
output.line.separator=\n

# Generation settings
max.depth=10
include.documentation=true

# Project metadata
project.name=MyProject
project.version=1.0.0
```

**Configuration Precedence:**

When multiple configuration files are specified, later files override earlier ones:

```bash
# defaults.properties is loaded first
# production.properties overrides defaults
# local.properties overrides both
ucmism2t -input model.uml -output ./generated \
         -config defaults.properties,production.properties,local.properties
```

## Acceleo Template Development

### Template Structure

Templates are located in:
```
bundles/ucmism2t.core/src/ucmism2t/templates/
```

Main template file: `m2t.mtl`

### Template Example

```mtl
[module m2t('http://www.eclipse.org/uml2/5.0.0/UML')]

[template public generateModel(model : Model)]
[comment @main /]
[file ('output.txt', false, 'UTF-8')]
// Generated on [model.getCurrentDateTime()/]

[for (cls : Class | model.getAllClasses())]
Class: [cls.name/]
  Package: [cls.getQualifiedName('.')/]
  camelCase: [cls.toCamelCase()/]
[/for]
[/file]
[/template]
```

### Available Services

#### DateTimeService

```mtl
[comment ISO 8601 date/time formatting /]
[model.getCurrentDateTime()/]          // 2025-02-13T14:30:45
[model.getCurrentDateTimeUTC()/]       // 2025-02-13T13:30:45
[model.getCurrentDate()/]              // 2025-02-13
[model.getCurrentTime()/]              // 14:30:45
[model.getCurrentYear()/]              // 2025
```

#### ConfigurationService

```mtl
[comment Access configuration properties /]
[model.getProperty('project.name')/]
[model.getProperty('max.retries', '3')/]  // With default
[if (model.hasProperty('debug.enabled'))]...[/if]
```

#### CustomServices

```mtl
[comment Name transformations /]
[class.toCamelCase()/]                 // myClass
[class.toPascalCase()/]                // MyClass
[class.toSnakeCase()/]                 // my_class
[class.toUpperSnakeCase()/]            // MY_CLASS

[comment Qualified names /]
[class.getQualifiedName('.')/]         // com.example.MyClass
[class.getJavaPackageName()/]          // com.example

[comment Stereotype checks /]
[if (class.hasStereotype('Entity'))]...[/if]
[if (class.isPersistent())]...[/if]

[comment Property checks /]
[if (property.isMultivalued())]...[/if]
[if (property.isRequired())]...[/if]
```

## Development

### Eclipse IDE Setup

1. Install Eclipse Modeling Tools 2025-12
2. Import project: `File > Import > Existing Maven Projects`
3. Select project root directory
4. Wait for Maven to resolve dependencies

### Adding Custom Services

1. Create Java class in `ucmism2t.services`
2. Implement public methods (will be callable from templates)
3. Register in `plugin.xml`:

```xml
<extension point="org.eclipse.acceleo.query.service">
   <service class="ucmism2t.services.YourService"/>
</extension>
```

4. Use in templates:

```mtl
[element.yourMethod()/]
```

### Running Tests

```bash
# Run all tests
mvn clean verify

# Run tests for specific module
cd bundles/ucmism2t.core
mvn test
```

## Continuous Integration

### GitHub Actions Example

```yaml
name: Build ucmism2t

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Build with Maven
        run: mvn clean verify
      
      - name: Upload artifacts
        uses: actions/upload-artifact@v3
        with:
          name: ucmism2t-products
          path: releng/ucmism2t.product/target/products/
```

### Jenkins Pipeline Example

```groovy
pipeline {
    agent any
    
    tools {
        jdk 'JDK-21'
        maven 'Maven-3.9'
    }
    
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean verify'
            }
        }
        
        stage('Archive') {
            steps {
                archiveArtifacts artifacts: 'releng/ucmism2t.product/target/products/*.tar.gz, releng/ucmism2t.product/target/products/*.zip'
            }
        }
    }
}
```

## Troubleshooting

### Common Issues

**Issue**: `Java version not supported`
- **Solution**: Ensure JDK 21 is installed and JAVA_HOME is set correctly

**Issue**: `Model file not found`
- **Solution**: Verify .uml file path is correct and file exists

**Issue**: `OutOfMemoryError during build`
- **Solution**: Increase Maven memory: `export MAVEN_OPTS="-Xmx2048m"`

**Issue**: `Template not found`
- **Solution**: Verify .mtl files are in `src/` directory and included in `build.properties`

### Debug Mode

Run with verbose output:

```bash
ucmism2t -input model.uml -output ./generated -console -consoleLog
```

## License

Copyright (c) 2025 Example Organization

This software is provided "as is" without warranty of any kind.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## Support

For issues, questions, or contributions, please visit the project repository.

## Version History

### 1.0.0 (2025-02-13)
- Initial release
- Eclipse 2025-12 support
- Acceleo 4.2 integration
- Java 21 compatibility
- Multi-platform executables
