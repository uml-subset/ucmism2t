package ucmism2t.services;

import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Property;

/**
 * Custom Utility Services for UML Model Processing
 * 
 * Provides additional helper methods for working with UML2 models
 * in Acceleo templates. These complement the standard UML2 metamodel
 * operations with commonly needed transformations and queries.
 * 
 * Usage in Acceleo templates (AQL syntax):
 *   [class.toCamelCase()/]
 *   [class.isPersistent()/]
 * 
 * @author ucmism2t generator
 * @version 1.0.0
 * @since Java 21
 */
public class CustomServices {
    
    /**
     * Converts a UML element name to camelCase.
     * 
     * Examples:
     *   "MyClass" -> "myClass"
     *   "UserAccount" -> "userAccount"
     *   "ID" -> "id"
     * 
     * Usage in template:
     *   private [property.type.name/] [property.toCamelCase()/];
     * 
     * @param element The named UML element
     * @return Name in camelCase, or empty string if name is null/empty
     */
    public String toCamelCase(NamedElement element) {
        String name = element.getName();
        if (name == null || name.isEmpty()) {
            return "";
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
    
    /**
     * Converts a UML element name to PascalCase.
     * 
     * Examples:
     *   "myClass" -> "MyClass"
     *   "userAccount" -> "UserAccount"
     * 
     * @param element The named UML element
     * @return Name in PascalCase, or empty string if name is null/empty
     */
    public String toPascalCase(NamedElement element) {
        String name = element.getName();
        if (name == null || name.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
    
    /**
     * Converts a UML element name to snake_case.
     * 
     * Examples:
     *   "MyClass" -> "my_class"
     *   "UserAccount" -> "user_account"
     * 
     * Useful for database table/column names.
     * 
     * @param element The named UML element
     * @return Name in snake_case, or empty string if name is null/empty
     */
    public String toSnakeCase(NamedElement element) {
        String name = element.getName();
        if (name == null || name.isEmpty()) {
            return "";
        }
        return name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
    
    /**
     * Converts a UML element name to UPPER_SNAKE_CASE.
     * 
     * Examples:
     *   "MyClass" -> "MY_CLASS"
     *   "UserAccount" -> "USER_ACCOUNT"
     * 
     * Useful for constants and database constraints.
     * 
     * @param element The named UML element
     * @return Name in UPPER_SNAKE_CASE
     */
    public String toUpperSnakeCase(NamedElement element) {
        return toSnakeCase(element).toUpperCase();
    }
    
    /**
     * Checks if a UML class has a specific stereotype applied.
     * 
     * Stereotypes extend UML with domain-specific semantics.
     * Common stereotypes: «Entity», «Service», «Repository», etc.
     * 
     * Example in template:
     *   [if (class.hasStereotype('Entity'))]
     *     @Entity
     *   [/if]
     * 
     * @param clazz The UML class to check
     * @param stereotypeName Name of the stereotype (case-insensitive)
     * @return true if stereotype is applied, false otherwise
     */
    public boolean hasStereotype(Class clazz, String stereotypeName) {
        if (stereotypeName == null) {
            return false;
        }
        return clazz.getAppliedStereotypes().stream()
                .anyMatch(s -> stereotypeName.equalsIgnoreCase(s.getName()));
    }
    
    /**
     * Checks if a UML class has the «Persistent» stereotype.
     * 
     * Convenience method for checking persistence stereotypes.
     * 
     * @param clazz The UML class to check
     * @return true if class is marked as persistent
     */
    public boolean isPersistent(Class clazz) {
        return hasStereotype(clazz, "persistent") || 
               hasStereotype(clazz, "entity");
    }
    
    /**
     * Gets the fully qualified name with custom separator.
     * 
     * UML qualified names use "::" separator by default.
     * This method allows custom separators for different languages.
     * 
     * Examples:
     *   Java: Model::Package::Class -> Model.Package.Class
     *   C++: Model::Package::Class -> Model::Package::Class
     *   Path: Model::Package::Class -> Model/Package/Class
     * 
     * Usage in template:
     *   package [class.getQualifiedName('.')/];
     * 
     * @param element The named UML element
     * @param separator Custom separator string
     * @return Qualified name with custom separator
     */
    public String getQualifiedName(NamedElement element, String separator) {
        String qualifiedName = element.getQualifiedName();
        if (qualifiedName == null) {
            return element.getName() != null ? element.getName() : "";
        }
        return qualifiedName.replace("::", separator);
    }
    
    /**
     * Gets the package name of a UML element (for Java package declarations).
     * 
     * Extracts the package portion of a qualified name and converts
     * to Java package format (dot-separated, lowercase).
     * 
     * Example:
     *   Model::com::example::MyClass -> com.example
     * 
     * @param element The named UML element
     * @return Java package name, or empty string if no package
     */
    public String getJavaPackageName(NamedElement element) {
        String qualifiedName = element.getQualifiedName();
        if (qualifiedName == null || !qualifiedName.contains("::")) {
            return "";
        }
        // Remove the element name (last segment)
        int lastSeparator = qualifiedName.lastIndexOf("::");
        String packageName = qualifiedName.substring(0, lastSeparator);
        return packageName.replace("::", ".").toLowerCase();
    }
    
    /**
     * Checks if a property is multivalued (collection).
     * 
     * A property is multivalued if its upper bound is greater than 1
     * or unbounded (*).
     * 
     * Usage in template:
     *   [if (property.isMultivalued())]
     *     private List<[property.type.name/]> [property.name/];
     *   [else]
     *     private [property.type.name/] [property.name/];
     *   [/if]
     * 
     * @param property The UML property to check
     * @return true if property represents a collection
     */
    public boolean isMultivalued(Property property) {
        return property.getUpper() > 1 || property.getUpper() == -1;
    }
    
    /**
     * Checks if a property is required (lower bound >= 1).
     * 
     * @param property The UML property to check
     * @return true if property is required
     */
    public boolean isRequired(Property property) {
        return property.getLower() >= 1;
    }
}
