# ucmism2t Usage Guide

## Quick Start

### 1. Basic Transformation

Transform a UML model to text:

```bash
ucmism2t -input mymodel.uml -output ./generated
```

This will:
- Load `mymodel.uml`
- Execute the main template (`m2t.mtl`)
- Write output files to `./generated` directory

### 2. Using Configuration Files

Provide runtime configuration:

```bash
ucmism2t -input mymodel.uml \
         -output ./generated \
         -config config.properties
```

**config.properties:**
```properties
output.encoding=UTF-8
project.name=MyProject
project.version=1.0.0
max.depth=10
```

### 3. Multiple Configuration Files

Override defaults with environment-specific configuration:

```bash
ucmism2t -input mymodel.uml \
         -output ./generated \
         -config defaults.properties,production.properties
```

Configuration precedence (later files override earlier):
1. `defaults.properties` - Base configuration
2. `production.properties` - Production overrides

## Command-Line Reference

### Required Arguments

| Argument | Description | Example |
|----------|-------------|---------|
| `-input <path>` | Path to UML model file | `-input model.uml` |
| `-output <path>` | Output directory path | `-output ./generated` |

### Optional Arguments

| Argument | Description | Example |
|----------|-------------|---------|
| `-config <files>` | Comma-separated config files | `-config a.properties,b.properties` |

### Exit Codes

| Code | Meaning |
|------|---------|
| 0 | Success |
| 1 | Error (invalid arguments, file not found, transformation failed) |

## Configuration File Format

Configuration files use standard Java properties format:

```properties
# Comments start with #

# Simple key-value pairs
output.encoding=UTF-8
project.name=MyProject

# Multiline values (use backslash)
long.description=This is a very long \
description that spans \
multiple lines

# Property references (not directly supported, use templates)
base.path=/usr/local
output.path=${base.path}/output

# Special characters must be escaped
windows.path=C\:\\Users\\Documents
```

### Common Configuration Properties

```properties
# Output Settings
output.encoding=UTF-8
output.line.separator=\n
output.file.prefix=generated_
output.file.suffix=.txt

# Generation Control
max.depth=10
include.documentation=true
include.stereotypes=true
generate.getters=true
generate.setters=true

# Project Metadata
project.name=MyProject
project.version=1.0.0
project.author=John Doe
project.company=Example Corp

# Code Generation
java.package.prefix=com.example
java.indent.size=4
java.indent.char=space
```

## Template Usage Examples

### Example 1: Generate Java Classes

**Template (generateJava.mtl):**
```mtl
[module generateJava('http://www.eclipse.org/uml2/5.0.0/UML')]

[template public generateModel(model : Model)]
[comment @main /]
[for (cls : Class | model.allOwnedElements()->filter(Class))]
[cls.generateJavaClass()/]
[/for]
[/template]

[template public generateJavaClass(cls : Class)]
[file (cls.name + '.java', false, 'UTF-8')]
// Generated on [cls.getCurrentDateTime()/]
// Copyright (c) [cls.getCurrentYear()/] [cls.getProperty('project.company')/]

package [cls.getJavaPackageName()/];

public class [cls.name/] {
    
[for (attr : Property | cls.ownedAttribute)]
    private [attr.type.name/] [attr.toCamelCase()/];
[/for]

[for (attr : Property | cls.ownedAttribute)]
    public [attr.type.name/] get[attr.toPascalCase()/]() {
        return [attr.toCamelCase()/];
    }
    
    public void set[attr.toPascalCase()/]([attr.type.name/] [attr.toCamelCase()/]) {
        this.[attr.toCamelCase()/] = [attr.toCamelCase()/];
    }
[/for]
}
[/file]
[/template]
```

**Run:**
```bash
ucmism2t -input model.uml -output ./src -config java.properties
```

### Example 2: Generate Database Schema

**Template (generateSQL.mtl):**
```mtl
[template public generateSchema(model : Model)]
[comment @main /]
[file ('schema.sql', false, 'UTF-8')]
-- Generated on [model.getCurrentDateTime()/]
-- Database Schema for [model.getProperty('project.name')/]

[for (cls : Class | model.allOwnedElements()->filter(Class))]
[if (cls.hasStereotype('Entity'))]
CREATE TABLE [cls.toSnakeCase()/] (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
[for (attr : Property | cls.ownedAttribute) separator(',\n')]
    [attr.toSnakeCase()/] [attr.getSQLType()/][if (attr.isRequired())] NOT NULL[/if]
[/for]
);

[/if]
[/for]
[/file]
[/template]
```

### Example 3: Generate Documentation

**Template (generateDocs.mtl):**
```mtl
[template public generateDocumentation(model : Model)]
[comment @main /]
[file ('documentation.md', false, 'UTF-8')]
# [model.name/] Documentation

*Generated on [model.getCurrentDateTime()/]*

## Overview

This document describes the [model.name/] model.

## Classes

[for (cls : Class | model.getAllClasses())]
### [cls.name/]

**Package:** `[cls.getQualifiedName('.')/]`

[if (not cls.ownedComment->isEmpty())]
[cls.ownedComment->first().body/]
[/if]

#### Attributes

| Name | Type | Multiplicity | Required |
|------|------|--------------|----------|
[for (attr : Property | cls.ownedAttribute)]
| [attr.name/] | [attr.type.name/] | [[attr.lower/]..[if (attr.upper = -1)]*[else][attr.upper/][/if]] | [if (attr.isRequired())]Yes[else]No[/if] |
[/for]

[/for]
[/file]
[/template]
```

## Advanced Usage

### Batch Processing

Process multiple models:

```bash
#!/bin/bash
for model in models/*.uml; do
    echo "Processing $model"
    ucmism2t -input "$model" \
             -output "./generated/$(basename "$model" .uml)" \
             -config config.properties
done
```

### CI/CD Integration

**Makefile:**
```makefile
.PHONY: generate
generate:
	ucmism2t -input model.uml \
	         -output ./src/generated \
	         -config config.properties

.PHONY: clean
clean:
	rm -rf ./src/generated

.PHONY: all
all: clean generate
```

**package.json (Node.js project):**
```json
{
  "scripts": {
    "generate": "ucmism2t -input model.uml -output ./src/generated -config config.properties",
    "pretest": "npm run generate"
  }
}
```

### Docker Integration

**Dockerfile:**
```dockerfile
FROM eclipse-temurin:21-jdk

COPY ucmism2t /opt/ucmism2t
ENV PATH="/opt/ucmism2t:${PATH}"

WORKDIR /workspace
ENTRYPOINT ["ucmism2t"]
```

**Usage:**
```bash
docker build -t ucmism2t:latest .
docker run -v $(pwd):/workspace ucmism2t:latest \
    -input model.uml \
    -output ./generated \
    -config config.properties
```

## Debugging

### Verbose Output

Enable detailed logging:

```bash
ucmism2t -input model.uml -output ./generated -console -consoleLog
```

### Check Template Syntax

Validate templates before running:

```bash
# Build only core bundle to check template syntax
cd bundles/ucmism2t.core
mvn clean compile
```

### Inspect Model

Use Eclipse UML2 Tools to inspect .uml files:

1. Open Eclipse Modeling Tools
2. Import model: `File > Open`
3. Right-click model element > Show Properties

## Performance Tips

### 1. Optimize Template Queries

**Inefficient:**
```mtl
[for (cls : Class | model.allOwnedElements()->filter(Class))]
  [for (otherCls : Class | model.allOwnedElements()->filter(Class))]
    [comment Nested loop - O(n²) /]
  [/for]
[/for]
```

**Efficient:**
```mtl
[let allClasses : Sequence(Class) = model.getAllClasses()]
[for (cls : Class | allClasses)]
  [for (otherCls : Class | allClasses)]
    [comment Single query - O(n) /]
  [/for]
[/for]
[/let]
```

### 2. Increase Memory

For large models:

```bash
# Increase JVM heap size
export JAVA_OPTS="-Xms512m -Xmx2048m"
ucmism2t -input large-model.uml -output ./generated
```

### 3. Parallel Execution

Process multiple models in parallel:

```bash
#!/bin/bash
for model in models/*.uml; do
    (ucmism2t -input "$model" -output "./generated/$(basename "$model" .uml)") &
done
wait
```

## Best Practices

1. **Version Control**: Store .uml files and templates in Git
2. **Configuration Management**: Use separate config files per environment
3. **Template Organization**: Split large templates into modules
4. **Documentation**: Document custom services and template usage
5. **Testing**: Validate generated output in CI/CD pipeline
6. **Backup**: Keep backups of models before generation

## Troubleshooting

### Model Loading Errors

**Error**: `Model file not found`
- Verify file path is correct
- Check file permissions
- Ensure .uml extension

**Error**: `Failed to load model or model is empty`
- Open model in Eclipse UML2 Tools to validate
- Check for XML syntax errors
- Verify UML namespace URIs

### Configuration Errors

**Error**: `Configuration file not found`
- Verify file path in `-config` argument
- Check file exists and is readable

**Error**: `Invalid property format`
- Check for proper key=value format
- Escape special characters

### Template Errors

**Error**: `Template not found`
- Verify .mtl file is in correct directory
- Check `build.properties` includes template
- Rebuild core bundle

**Error**: `Service method not found`
- Verify service is registered in `plugin.xml`
- Check method signature matches template call
- Rebuild core bundle

## Examples Repository

Complete working examples are available at:
- Basic model transformation
- Java code generation
- SQL schema generation
- Documentation generation
- Multi-template project

## Support

For additional help:
- Review README.md for setup instructions
- Check project documentation
- Examine example templates
- Review Acceleo 4.2 documentation
