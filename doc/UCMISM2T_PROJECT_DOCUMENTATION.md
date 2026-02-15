# ucmism2t Project Documentation

**UML Class Model to Text - Model-to-Text Transformation Tool**

Version 1.0.0  
Last Updated: February 14, 2026

---

## Table of Contents

1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Project Architecture](#project-architecture)
4. [Technology Stack](#technology-stack)
5. [Module Structure](#module-structure)
6. [Template System](#template-system)
7. [Java Services](#java-services)
8. [Configuration](#configuration)
9. [Build System](#build-system)
10. [Distribution](#distribution)
11. [Development Guide](#development-guide)
12. [Extending the Project](#extending-the-project)
13. [Troubleshooting](#troubleshooting)
14. [API Reference](#api-reference)

---

## Overview

### What is ucmism2t?

ucmism2t (UML Class Model to Text) is a **model-to-text transformation tool** built on Eclipse and Acceleo 4.2 that generates text output from UML models.

**Key Capabilities:**
- ✅ Process UML 2.5 models (XMI format)
- ✅ Generate code, documentation, or any text format
- ✅ Use powerful Acceleo templates with AQL expressions
- ✅ Extend with custom Java services
- ✅ Run as Eclipse plugin or standalone CLI
- ✅ Cross-platform (Linux, macOS, Windows)

### Use Cases

```
Code Generation:
UML Model → Java Classes
          → Database Schema  
          → REST APIs
          → Test Code

Documentation:
UML Model → HTML Docs
          → Markdown
          → API Specs
          → PDF Reports

Data Exchange:
UML Model → JSON Schema
          → XML Schema
          → OpenAPI
          → Protocol Buffers
```

### Architecture Overview

```
┌──────────────────────────────────────────────┐
│           ucmism2t Application               │
│                                              │
│  CLI → Core Engine → Acceleo Templates      │
│          ↓              ↓                    │
│      UML Model     Java Services             │
│          ↓              ↓                    │
│      EMF/UML2    Configuration               │
│          ↓                                   │
│    Eclipse OSGi Platform                     │
└──────────────────────────────────────────────┘
```

---

## Quick Start

### Installation

**Option 1: Single-File Executable (Recommended)**

Download the standalone executable for your platform:

```bash
# Linux/macOS
chmod +x ucmism2t-standalone
./ucmism2t-standalone -input model.xmi -output generated/

# Windows
.\ucmism2t.bat -input model.xmi -output generated
```

**Option 2: Extract Full Installation**

```bash
# Linux/macOS
./ucmism2t-standalone --extract ~/ucmism2t
~/ucmism2t/ucmism2t -input model.xmi -output gen/

# Windows  
.\ucmism2t.ps1 -Extract -Path C:\Tools\ucmism2t
C:\Tools\ucmism2t\ucmism2t.exe -input model.xmi -output gen
```

### Basic Usage

```bash
# Minimal command
ucmism2t -input model.xmi -output generated/

# With configuration
ucmism2t -input model.xmi -output gen/ -config settings.properties

# Multiple configs (later files override earlier)
ucmism2t -input model.xmi -output gen/ \
  -config base.properties project.properties
```

### Example Workflow

**1. Create UML Model** (using any UML tool that exports XMI):
```
MyModel.xmi
├── Package: com.example.model
│   ├── Class: Customer
│   │   ├── id: String
│   │   ├── name: String
│   │   └── email: String
│   └── Class: Order
│       ├── id: String
│       ├── customer: Customer
│       └── total: Double
```

**2. Create Configuration** (optional):
```properties
# config.properties
project.name=MyProject
author=John Doe
package.prefix=com.example.generated
```

**3. Run Transformation:**
```bash
ucmism2t \
  -input MyModel.xmi \
  -output src/generated \
  -config config.properties
```

**4. View Generated Files:**
```
src/generated/
├── Customer.java
├── Order.java
└── README.md
```

---

## Project Architecture

### Directory Structure

```
ucmism2t/
├── bundles/                    # OSGi bundles (plugins)
│   ├── ucmism2t.core/         # Core transformation logic
│   │   ├── META-INF/
│   │   │   └── MANIFEST.MF    # OSGi manifest
│   │   ├── plugin.xml         # Extension points
│   │   ├── src/
│   │   │   └── ucmism2t/
│   │   │       ├── services/  # Java services
│   │   │       └── templates/ # Acceleo templates
│   │   └── pom.xml
│   │
│   └── ucmism2t.cli/          # Command-line interface
│       ├── META-INF/
│       │   └── MANIFEST.MF
│       ├── plugin.xml         # Application definition
│       ├── src/
│       │   └── ucmism2t/cli/
│       │       └── Main.java  # Entry point
│       └── pom.xml
│
├── features/                   # Feature definitions
│   └── ucmism2t.feature/
│       ├── feature.xml        # Groups bundles
│       └── pom.xml
│
├── releng/                     # Release engineering
│   ├── ucmism2t.target/       # Target platform
│   │   ├── ucmism2t.target    # Eclipse dependencies
│   │   └── pom.xml
│   │
│   └── ucmism2t.product/      # Product configuration
│       ├── ucmism2t.product   # Product definition
│       ├── create-single-file.sh  # Packaging script
│       └── pom.xml
│
└── pom.xml                     # Parent Maven POM
```

### Component Responsibilities

**ucmism2t.core**
- Load and validate UML models
- Execute Acceleo templates  
- Manage Java services
- Generate output files

**ucmism2t.cli**
- Parse command-line arguments
- Initialize Eclipse runtime
- Coordinate transformation
- Report results

**ucmism2t.feature**
- Group bundles for deployment
- Define feature dependencies

**ucmism2t.product**
- Create platform-specific executables
- Configure Eclipse launcher
- Package distributions

### OSGi Architecture

```
Bundle Dependencies:

ucmism2t.cli
    │
    ├──> ucmism2t.core
    │        │
    │        ├──> org.eclipse.acceleo.aql
    │        │        └──> org.eclipse.acceleo.query
    │        │
    │        └──> org.eclipse.uml2.uml
    │                 └──> org.eclipse.emf.ecore
    │
    ├──> org.eclipse.equinox.app
    └──> org.eclipse.core.runtime

Eclipse Platform provides:
├──> org.eclipse.osgi (OSGi runtime)
├──> org.eclipse.equinox.* (Platform services)
└──> org.eclipse.emf.* (Modeling framework)
```

---

## Technology Stack

### Core Technologies

**Eclipse Platform 2025-12**
- Application framework
- OSGi runtime (Equinox)
- Extension point mechanism

**EMF 2.35** (Eclipse Modeling Framework)
- Model representation (EObject, Resource)
- XMI serialization
- Metamodel definitions

**UML2 5.0.0**
- UML metamodel implementation
- Supports UML 2.5 standard
- Model element navigation

**Acceleo 4.2**
- Model-to-text transformation
- AQL query language
- Template execution engine

**Java 21 LTS**
- Modern Java features
- Records, pattern matching
- Text blocks

### Build Stack

**Maven 3.9+**
- Build orchestration
- Dependency management
- Multi-module projects

**Eclipse Tycho 5.0.0**
- OSGi/Eclipse integration
- P2 repository resolution
- Product packaging

---

## Module Structure

### ucmism2t.core Bundle

**Purpose:** Core transformation engine

**MANIFEST.MF:**
```
Bundle-SymbolicName: ucmism2t.core
Bundle-Version: 1.0.0.qualifier
Require-Bundle:
  org.eclipse.core.runtime,
  org.eclipse.emf.ecore,
  org.eclipse.uml2.uml,
  org.eclipse.acceleo.aql,
  org.eclipse.acceleo.query
Export-Package:
  ucmism2t.services
Bundle-RequiredExecutionEnvironment: JavaSE-21
```

**plugin.xml:**
```xml
<plugin>
   <!-- Register Java services -->
   <extension point="org.eclipse.acceleo.query.service">
      <service class="ucmism2t.services.ConfigurationService"/>
      <service class="ucmism2t.services.DateTimeService"/>
      <service class="ucmism2t.services.CustomServices"/>
   </extension>
</plugin>
```

**Directory Layout:**
```
ucmism2t.core/
├── src/
│   └── ucmism2t/
│       ├── services/
│       │   ├── ConfigurationService.java
│       │   ├── DateTimeService.java
│       │   └── CustomServices.java
│       │
│       ├── templates/
│       │   └── m2t.mtl (Acceleo template)
│       │
│       └── queries/
│           └── (query libraries)
```

### ucmism2t.cli Bundle

**Purpose:** Command-line interface

**MANIFEST.MF:**
```
Bundle-SymbolicName: ucmism2t.cli
Bundle-Version: 1.0.0.qualifier
Require-Bundle:
  org.eclipse.core.runtime,
  org.eclipse.equinox.app,
  org.eclipse.emf.ecore,
  org.eclipse.uml2.uml,
  ucmism2t.core
Bundle-RequiredExecutionEnvironment: JavaSE-21
```

**plugin.xml:**
```xml
<plugin>
   <!-- Define application -->
   <extension point="org.eclipse.core.runtime.applications">
      <application id="app" cardinality="singleton-global">
         <run class="ucmism2t.cli.Main"/>
      </application>
   </extension>
   
   <!-- Define product -->
   <extension point="org.eclipse.core.runtime.products">
      <product application="ucmism2t.cli.app" name="ucmism2t">
         <property name="appName" value="ucmism2t"/>
      </product>
   </extension>
</plugin>
```

**Main.java:**
```java
package ucmism2t.cli;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

public class Main implements IApplication {
    
    @Override
    public Object start(IApplicationContext context) throws Exception {
        // Parse arguments
        String[] args = (String[]) context.getArguments()
            .get(IApplicationContext.APPLICATION_ARGS);
        
        // Run transformation
        return run(args);
    }
    
    @Override
    public void stop() {
        // Cleanup if needed
    }
    
    private Integer run(String[] args) {
        // Implementation details...
        return IApplication.EXIT_OK; // 0 for success
    }
}
```

---

## Template System

### Acceleo Template Basics

**Module Structure:**
```mtl
[comment encoding = UTF-8 /]
[module m2t('http://www.eclipse.org/uml2/5.0.0/UML')]

[comment @main /]
[template public generateModel(aModel : uml::Model)]
  [file ('output.txt', overwrite)]
    Model: [aModel.name/]
  [/file]
[/template]
```

**Key Elements:**

1. **Module declaration**: Imports UML metamodel
2. **@main annotation**: Marks entry point
3. **Template**: Function that processes model
4. **File block**: Generates output file
5. **Expressions `[...]`**: AQL queries

### AQL Expressions

**Property Access:**
```mtl
[aModel.name/]              <!-- Get name -->
[aClass.isAbstract/]        <!-- Boolean property -->
```

**Navigation:**
```mtl
[aModel.nestedPackage/]     <!-- Collection of packages -->
[aClass.ownedAttribute/]    <!-- Collection of attributes -->
```

**Filtering:**
```mtl
[comment Get all classes /]
[aModel.eAllContents(Class)/]

[comment Filter by condition /]
[aClass.ownedOperation->select(op | op.visibility = VisibilityKind::public)/]
```

**Iteration:**
```mtl
[for (pkg : Package | aModel.nestedPackage)]
  Package: [pkg.name/]
[/for]
```

**Conditionals:**
```mtl
[if (aClass.isAbstract)]
  abstract class
[else]
  concrete class
[/if]
```

### Complete Example

**Generate Java Classes:**
```mtl
[comment encoding = UTF-8 /]
[module javaGen('http://www.eclipse.org/uml2/5.0.0/UML')]

[comment @main /]
[template public generateModel(aModel : uml::Model)]
  [for (pkg : Package | aModel.nestedPackage)]
    [for (cls : Class | pkg.ownedType->filter(Class))]
      [generateClass(cls)/]
    [/for]
  [/for]
[/template]

[template private generateClass(cls : Class)]
  [file (cls.name + '.java', overwrite)]
package [cls.package.name/];

[if (cls.isAbstract)]abstract [/if]class [cls.name/] {
    
    // Attributes
[for (attr : Property | cls.ownedAttribute)]
    private [attr.type.name/] [attr.name/];
[/for]

    // Getters and Setters
[for (attr : Property | cls.ownedAttribute)]
    public [attr.type.name/] get[attr.name.toUpperFirst()/]() {
        return this.[attr.name/];
    }
    
    public void set[attr.name.toUpperFirst()/]([attr.type.name/] [attr.name/]) {
        this.[attr.name/] = [attr.name/];
    }
[/for]
}
  [/file]
[/template]
```

---

## Java Services

### Overview

Java services extend templates with custom operations.

**Architecture:**
```
Template calls service:
[aClass.countAttributes()/]
        ↓
CustomServices.countAttributes(Class)
        ↓
Returns: int
        ↓
Template receives result
```

### Creating a Service

**1. Create Service Class:**
```java
package ucmism2t.services;

import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.NamedElement;

public class CustomServices {
    
    /**
     * Get qualified name with separator.
     * Usage: [element.getQualifiedName('.')/]
     */
    public String getQualifiedName(NamedElement element, String sep) {
        String qn = element.getQualifiedName();
        return qn != null ? qn.replace("::", sep) : element.getName();
    }
    
    /**
     * Count attributes.
     * Usage: [cls.countAttributes()/]
     */
    public int countAttributes(Class clazz) {
        return clazz.getOwnedAttributes().size();
    }
}
```

**2. Register in plugin.xml:**
```xml
<extension point="org.eclipse.acceleo.query.service">
   <service class="ucmism2t.services.CustomServices"/>
</extension>
```

**3. Use in Template:**
```mtl
[for (cls : Class | aModel.eAllContents(Class))]
  [cls.getQualifiedName('.')/] has [cls.countAttributes()/] attributes
[/for]
```

### Built-in Services

**ConfigurationService:**
```java
// Access configuration properties
public String getProperty(Object context, String key);
public String getProperty(Object context, String key, String default);
public boolean hasProperty(Object context, String key);
```

**Template Usage:**
```mtl
Author: [aModel.getProperty('author', 'Unknown')/]

[if (aModel.hasProperty('version'))]
  Version: [aModel.getProperty('version')/]
[/if]
```

**DateTimeService:**
```java
// Date/time formatting
public String getCurrentDate(Object context);
public String getCurrentTime(Object context);
public String formatDate(Object context, String pattern);
```

**Template Usage:**
```mtl
Generated on: [aModel.getCurrentDate()/] at [aModel.getCurrentTime()/]
```

---

## Configuration

### Configuration Files

**Format:** Java Properties
```properties
# config.properties
project.name=MyProject
author=John Doe
version=1.0.0
output.encoding=UTF-8
package.prefix=com.example.generated
```

### Loading Configuration

**Command Line:**
```bash
ucmism2t -input model.xmi -output gen/ -config settings.properties
```

**Multiple Files (override order):**
```bash
ucmism2t -input model.xmi -output gen/ \
  -config defaults.properties project.properties
```

### Using in Templates

```mtl
[comment Get property /]
Project: [aModel.getProperty('project.name')/]

[comment With default /]
Author: [aModel.getProperty('author', 'Unknown')/]

[comment Conditional /]
[if (aModel.hasProperty('debug'))]
  // Debug mode enabled
[/if]
```

---

## Build System

### Maven Build

**Build All:**
```bash
cd ucmism2t
mvn clean verify
```

**Platform Selection:**
```bash
# Linux (default)
mvn clean verify

# Windows
mvn clean verify -Dbuild.platform=windows

# macOS Intel
mvn clean verify -Dbuild.platform=macos-x86

# macOS ARM
mvn clean verify -Dbuild.platform=macos-arm

# All platforms
mvn clean verify -Dbuild.platform=all
```

### Build Outputs

```
releng/ucmism2t.product/target/
├── products/
│   ├── ucmism2t.product/          # Full installation
│   │   └── linux/gtk/x86_64/
│   │       ├── ucmism2t           # Launcher
│   │       ├── plugins/           # 100+ JAR files
│   │       └── configuration/
│   │
│   └── ucmism2t.product-linux.gtk.x86_64.tar.gz  # Archive
│
└── ucmism2t-standalone            # Single file executable!
```

### Build Phases

```
1. Compile        → Java sources to .class
2. Package        → Create JAR bundles
3. Resolve P2     → Download Eclipse dependencies
4. Materialize    → Assemble complete application
5. Archive        → Create tar.gz/zip
6. Package Single → Create self-extracting executable
```

---

## Distribution

### Option 1: Single-File Executable

**Linux/macOS:**
```bash
# Distribute one file
ucmism2t-standalone

# Users run directly
chmod +x ucmism2t-standalone
./ucmism2t-standalone -input model.xmi -output gen/
```

**Windows:**
```powershell
# Distribute two files
ucmism2t-standalone.ps1
ucmism2t-standalone.bat

# Users run
.\ucmism2t.bat -input model.xmi -output gen
```

**Advantages:**
- ✅ One file (or two for Windows)
- ✅ No installation needed
- ✅ Self-extracting
- ✅ Easy to distribute

### Option 2: Full Installation

**Extract Archive:**
```bash
tar -xzf ucmism2t.product-linux.gtk.x86_64.tar.gz
cd ucmism2t/
./ucmism2t -input model.xmi -output gen/
```

**Advantages:**
- ✅ Faster startup (no extraction)
- ✅ Traditional installation
- ✅ Can inspect plugins directory

### Option 3: Extract from Single File

```bash
# Extract once, use many times
./ucmism2t-standalone --extract ~/ucmism2t-install
~/ucmism2t-install/ucmism2t -input model.xmi -output gen/
```

---

## Development Guide

### Eclipse Setup

**1. Install Eclipse IDE for RCP Developers**
- Download from eclipse.org
- Version 2025-12 or later

**2. Import Project:**
```
File → Import → Maven → Existing Maven Projects
Browse to ucmism2t/
Select all modules
Finish
```

**3. Set Target Platform:**
```
Open: releng/ucmism2t.target/ucmism2t.target
Wait for resolution
Click: "Set as Active Target Platform"
```

### Running in Eclipse

**Create Launch Configuration:**
```
Run → Run Configurations...
Eclipse Application → New
Name: ucmism2t-dev
Main:
  Application: ucmism2t.cli.app
Arguments:
  Program arguments: -input test.xmi -output gen/
Apply → Run
```

### Development Workflow

**1. Edit Template:**
```mtl
<!-- ucmism2t.core/src/ucmism2t/templates/m2t.mtl -->
[template public generateModel(aModel : uml::Model)]
  [file ('new-output.txt', overwrite)]
    Modified content
  [/file]
[/template]
```

**2. Save** (Eclipse auto-compiles)

**3. Run Launch Configuration**

**4. Check Output:**
```
workspace/gen/new-output.txt
```

### Adding Features

**New Java Service:**
```java
// 1. Create class
public class StringUtils {
    public String toSnakeCase(NamedElement elem) {
        return elem.getName()
            .replaceAll("([A-Z])", "_$1")
            .toLowerCase();
    }
}

// 2. Register in plugin.xml
<service class="ucmism2t.services.StringUtils"/>

// 3. Use in template
[cls.toSnakeCase()/]
```

**New Template File:**
```mtl
// 1. Create ucmism2t.core/src/ucmism2t/templates/html.mtl
[module html('http://www.eclipse.org/uml2/5.0.0/UML')]

[comment @main /]
[template public generateHTML(aModel : Model)]
  [file ('index.html', overwrite)]
<!DOCTYPE html>
<html>
<head><title>[aModel.name/]</title></head>
<body>
  <h1>[aModel.name/]</h1>
</body>
</html>
  [/file]
[/template]

// 2. Update Main.java to use it
String moduleQualifiedName = "ucmism2t::templates::html";
```

---

## Extending the Project

### Custom Transformations

**Scenario:** Generate database schema from UML

**1. Create Template:**
```mtl
[module schema('http://www.eclipse.org/uml2/5.0.0/UML')]

[comment @main /]
[template public generateSchema(aModel : Model)]
  [file ('schema.sql', overwrite)]
-- Database Schema for [aModel.name/]

[for (cls : Class | aModel.eAllContents(Class))]
CREATE TABLE [cls.name.toUpperCase()/] (
    id INTEGER PRIMARY KEY,
[for (attr : Property | cls.ownedAttribute) separator(',\n')]
    [attr.name/] [attr.type.getSQLType()/]
[/for]
);

[/for]
  [/file]
[/template]
```

**2. Create Service:**
```java
public class SQLService {
    public String getSQLType(Type umlType) {
        return switch(umlType.getName()) {
            case "String" -> "VARCHAR(255)";
            case "Integer" -> "INTEGER";
            case "Boolean" -> "BOOLEAN";
            case "Date" -> "TIMESTAMP";
            default -> "TEXT";
        };
    }
}
```

### Integration Examples

**CI/CD Pipeline:**
```yaml
# .github/workflows/generate.yml
name: Generate Code
on: [push]
jobs:
  generate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Generate from UML
        run: |
          chmod +x ucmism2t
          ./ucmism2t -input model.xmi -output src/generated/
      
      - name: Commit generated code
        run: |
          git config user.name "GitHub Actions"
          git add src/generated/
          git commit -m "Generated code from UML model"
          git push
```

**Maven Plugin Integration:**
```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <goals><goal>exec</goal></goals>
            <configuration>
                <executable>ucmism2t</executable>
                <arguments>
                    <argument>-input</argument>
                    <argument>${project.basedir}/model/design.xmi</argument>
                    <argument>-output</argument>
                    <argument>${project.basedir}/src/generated</argument>
                </arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```

---

## Troubleshooting

### Common Issues

**Issue: "Model not found"**
```
Error: File not found: model.xmi
```
**Solution:** Check file path is correct and file exists

**Issue: "Template not found"**
```
Error: Could not resolve module: ucmism2t::templates::m2t
```
**Solution:** Verify m2t.mtl exists in ucmism2t.core/src/ucmism2t/templates/

**Issue: "No files generated"**
```
Generated files: 0
```
**Solution:** Check template has `@main` annotation and `[file]` blocks

**Issue: "Permission denied" (Linux/macOS)**
```
bash: ./ucmism2t: Permission denied
```
**Solution:** `chmod +x ucmism2t`

### Debug Mode

**Enable Verbose Logging:**
```bash
# Linux/macOS
export ACCELEO_DEBUG=true
./ucmism2t -input model.xmi -output gen/

# View Eclipse logs
tail -f workspace/.metadata/.log
```

---

## API Reference

### Main Entry Point

**ucmism2t.cli.Main**
```java
public class Main implements IApplication {
    public Object start(IApplicationContext context);
    public void stop();
}
```

### Java Services API

**ConfigurationService**
```java
String getProperty(Object context, String key)
String getProperty(Object context, String key, String defaultValue)
boolean hasProperty(Object context, String key)
void loadConfiguration(String filePath)
```

**DateTimeService**
```java
String getCurrentDate(Object context)
String getCurrentTime(Object context)
String getCurrentDateTime(Object context)
String formatDate(Object context, String pattern)
```

**CustomServices**
```java
String getQualifiedName(NamedElement element, String separator)
```

### Template API

**File Generation:**
```mtl
[file (fileName, overwrite)]
  content
[/file]
```

**Control Structures:**
```mtl
[for (var : Type | collection)]...[/for]
[if (condition)]...[elseif]...[else]...[/if]
[let var : Type = expr]...[/let]
```

**AQL Operations:**
```mtl
[element.property/]
[collection->select(x | condition)/]
[collection->collect(x | expression)/]
[collection->size()/]
[string.toUpperCase()/]
```

---

## Summary

ucmism2t provides a complete model-to-text transformation solution:

✅ **Easy to Use:** Simple command-line interface  
✅ **Powerful:** Full Acceleo 4.2 templating  
✅ **Extensible:** Java services and custom templates  
✅ **Professional:** Eclipse/OSGi architecture  
✅ **Portable:** Cross-platform, single-file distribution

**Next Steps:**
1. Download the executable
2. Try the examples
3. Create your own templates
4. Extend with custom services

**Resources:**
- Acceleo Documentation: https://eclipse.org/acceleo/
- EMF Documentation: https://eclipse.org/modeling/emf/
- UML2 Documentation: https://wiki.eclipse.org/MDT/UML2

---

**Version:** 1.0.0  
**Last Updated:** February 14, 2026
