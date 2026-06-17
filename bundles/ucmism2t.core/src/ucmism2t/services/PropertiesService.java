package ucmism2t.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.uml2.uml.Model;

/**
 * AQL service providing property lookup for Acceleo 4 generation.
 *
 * Two initialisation modes are supported:
 *
 * 1. Automatic path derivation (Acceleo 4 IDE launch and CLI without
 *    -config):
 *    - <projectFolder>/property/generic.properties (mandatory)
 *    - <modelFolder>/<modelName>.properties (optional, overrides
 *      generic)
 *    The project folder is derived from the model path:
 *      <projectFolder>/model/<modelName>.uml
 *    Triggered by initProperties(aModel) from m2tMaster.mtl.
 *
 * 2. Explicit paths (CLI with -config):
 *    - Property files are loaded in the order specified
 *    - Later files override earlier ones
 *    - All specified files are mandatory
 *    Triggered by init(projectFolder, modelPath, configPaths).
 *
 * When the CLI calls init() directly before generation, the
 * alreadyInitialised flag is set so that the subsequent call to
 * initProperties(aModel) from m2tMaster.mtl is a no-op. This
 * prevents a second initialisation with a potentially different
 * (unresolved) path derived from the model resource URI.
 *
 * Each property value is stored together with the source file it was
 * loaded from, enabling the configuration report to show which file
 * each value originates from.
 *
 * Three initialisation paths are supported:
 *   - Acceleo 4 IDE launch: initProperties(aModel) called from
 *     m2tMaster.mtl
 *   - CLI launch (automatic): init(projectFolder, modelPath)
 *   - CLI launch (explicit):  init(projectFolder, modelPath,
 *                                  configPaths)
 */
public class PropertiesService {

    private static final String PREFIX = "[PropertiesService] ";

    /** Source indicator for the generic properties file. */
    private static final String INDICATOR_GENERIC = "G";

    /** Source indicator for the model-specific properties file. */
    private static final String INDICATOR_MODEL = "M";

    // -----------------------------------------------------------------
    // Internal storage
    // -----------------------------------------------------------------

    /**
     * Stores a property value together with the source file path and
     * the single-letter indicator used in the configuration report.
     */
    private static class PropertyEntry {
        final String value;
        final String source;
        final String indicator;

        PropertyEntry(String value, String source, String indicator) {
            this.value = value;
            this.source = source;
            this.indicator = indicator;
        }
    }

    /**
     * Merged property map. Later files override earlier ones.
     * Each entry records the source file for reporting purposes.
     */
    private static final Map<String, PropertyEntry> propertyMap =
        new HashMap<>();

    /**
     * Absolute path of the first properties file loaded.
     * Used for the configuration report legend.
     */
    private static String genericPropsPath = "";

    /**
     * Absolute path of the second properties file loaded.
     * Empty if the file was not found or not specified.
     * Used for the configuration report legend.
     */
    private static String modelPropsPath = "";

    /**
     * True if any init() overload has already run successfully.
     * Prevents initProperties(aModel) from running a redundant
     * second initialisation when called from m2tMaster.mtl during
     * CLI generation, where init() has already been called before
     * generation started.
     */
    private static boolean alreadyInitialised = false;

    // -----------------------------------------------------------------
    // Logging
    // -----------------------------------------------------------------

    private static void log(String message) {
        System.out.println(PREFIX + message);
        System.out.flush();
    }

    private static void logError(String message) {
        System.out.println(PREFIX + "ERROR: " + message);
        System.out.flush();
    }

    public String getDiagnosticInfo(EObject context) {
        ResourceSet rs = context.eResource().getResourceSet();
        StringBuilder sb = new StringBuilder();
        sb.append("Resources loaded:\n");
        rs.getResources().forEach(r ->
            sb.append("  ").append(r.getURI()).append("\n"));
        sb.append("\nURI mappings containing 'Primitive':\n");
        rs.getURIConverter().getURIMap()
            .forEach((k, v) -> {
                if (k.toString().contains("Primitive")
                        || v.toString().contains("Primitive")) {
                    sb.append("  ").append(k)
                      .append(" -> ").append(v).append("\n");
                }
            });
        return sb.toString();
    }
    
    // -----------------------------------------------------------------
    // Initialisation from MTL — Acceleo 4 IDE launch
    // -----------------------------------------------------------------

    /**
     * AQL service method — called from the main MTL template as:
     *   [aModel.initProperties()/]
     *
     * If any init() overload has already run (CLI context), this
     * method is a no-op to prevent overwriting the already-loaded
     * configuration with a potentially unresolved path derived from
     * the model resource URI.
     *
     * Otherwise derives the project folder from the model resource
     * URI:
     *   model is at <projectFolder>/model/<modelName>.uml
     *   so projectFolder = modelPath.getParent().getParent()
     *
     * @param aModel The UML model being generated
     */
    public void initProperties(Model aModel) {
        if (alreadyInitialised) {
            log("Skipping automatic initialisation"
                + " — properties already loaded.");
            return;
        }

        URI modelURI = aModel.eResource().getURI();
        Path modelPath = resolveToFilesystemPath(modelURI);
        Path projectFolder = modelPath.getParent().getParent();
        init(projectFolder, modelPath);
    }

    // -----------------------------------------------------------------
    // Initialisation from Java — CLI launch, automatic paths
    // -----------------------------------------------------------------

    /**
     * Called directly from the CLI launcher before generation when
     * no explicit config files are specified. Derives both property
     * file paths from the model path:
     *   1. <projectFolder>/property/generic.properties (mandatory)
     *   2. <modelFolder>/<modelName>.properties (optional)
     *
     * Sets alreadyInitialised = true so that the subsequent call to
     * initProperties(aModel) from m2tMaster.mtl is a no-op.
     *
     * @param projectFolder Absolute path to the project root
     * @param modelPath     Absolute path to the model file
     */
    public static void init(Path projectFolder, Path modelPath) {
        alreadyInitialised = true;
        propertyMap.clear();
        genericPropsPath = "";
        modelPropsPath = "";

        log("=== Initialisation ===");
        log("Project folder : " + projectFolder.toAbsolutePath());
        log("Model file     : " + modelPath.toAbsolutePath());

        // 1. Generic properties — mandatory
        Path genericProps = projectFolder.resolve(
            Paths.get("property", "generic.properties"));
        log("Generic properties (expected)       : "
            + genericProps.toAbsolutePath());
        genericPropsPath = genericProps.toAbsolutePath().toString();
        loadFile(genericProps, INDICATOR_GENERIC, true);

        // 2. Model-specific properties — optional
        String modelFilename = modelPath.getFileName().toString();
        String modelBasename = modelFilename.contains(".")
            ? modelFilename.substring(0, modelFilename.lastIndexOf('.'))
            : modelFilename;
        Path modelProps = modelPath.getParent()
            .resolve(modelBasename + ".properties");
        log("Model-specific properties (expected): "
            + modelProps.toAbsolutePath());
        if (Files.exists(modelProps)) {
            modelPropsPath = modelProps.toAbsolutePath().toString();
        }
        loadFile(modelProps, INDICATOR_MODEL, false);

        log("=== Initialisation complete — "
            + propertyMap.size() + " properties loaded ===");
    }

    // -----------------------------------------------------------------
    // Initialisation from Java — CLI launch, explicit paths
    // -----------------------------------------------------------------

    /**
     * Called from the CLI launcher when explicit property file paths
     * are provided via the -config argument. Files are loaded in the
     * order specified — later files override earlier ones. All
     * specified files are mandatory.
     *
     * Sets alreadyInitialised = true so that the subsequent call to
     * initProperties(aModel) from m2tMaster.mtl is a no-op.
     *
     * The source indicators in the configuration report are:
     *   C1 for the first file, C2 for the second, etc.
     *
     * @param projectFolder Absolute path to the project root
     * @param modelPath     Absolute path to the model file
     * @param configPaths   Explicit property file paths in load order
     * @throws IOException  If any specified file cannot be read
     */
    public static void init(
            Path projectFolder,
            Path modelPath,
            List<String> configPaths) throws IOException {
        alreadyInitialised = true;
        propertyMap.clear();
        genericPropsPath = "";
        modelPropsPath = "";

        log("=== Initialisation (explicit config) ===");
        log("Project folder : " + projectFolder.toAbsolutePath());
        log("Model file     : " + modelPath.toAbsolutePath());

        for (int i = 0; i < configPaths.size(); i++) {
            Path configPath = Paths.get(configPaths.get(i))
                .toAbsolutePath().normalize();
            String indicator = "C" + (i + 1);
            log("Config [" + (i + 1) + "] (expected): " + configPath);

            // Store first two paths for the report legend
            if (i == 0) genericPropsPath = configPath.toString();
            if (i == 1) modelPropsPath   = configPath.toString();

            loadFile(configPath, indicator, true);
        }

        log("=== Initialisation complete — "
            + propertyMap.size() + " properties loaded ===");
    }

    // -----------------------------------------------------------------
    // Internal loading
    // -----------------------------------------------------------------

    private static void loadFile(
            Path path, String indicator, boolean mandatory) {
        if (!Files.exists(path)) {
            if (mandatory) {
                logError("Mandatory properties file not found: "
                    + path.toAbsolutePath());
                throw new IllegalStateException(
                    "Mandatory properties file not found: "
                    + path.toAbsolutePath());
            }
            log("Optional file not found (skipped): "
                + path.toAbsolutePath());
            return;
        }
        try (InputStream is = Files.newInputStream(path)) {
            Properties props = new Properties();
            props.load(is);
            String source = path.toAbsolutePath().toString();
            int added = 0;
            int overridden = 0;
            for (String key : props.stringPropertyNames()) {
                if (propertyMap.containsKey(key)) {
                    overridden++;
                } else {
                    added++;
                }
                propertyMap.put(key, new PropertyEntry(
                    props.getProperty(key), source, indicator));
            }
            log("Read     : " + source);
            log("Added    : " + added + " new properties");
            if (overridden > 0) {
                log("Overrode : " + overridden
                    + " properties from earlier files");
            }
        } catch (IOException e) {
            logError("Failed to load properties file: "
                + path.toAbsolutePath() + " — " + e.getMessage());
            throw new IllegalStateException(
                "Failed to load properties file: "
                + path.toAbsolutePath(), e);
        }
    }

    // -----------------------------------------------------------------
    // URI resolution
    // -----------------------------------------------------------------

    /**
     * Resolves an EMF URI to a filesystem Path.
     * Supports file:/ URIs directly.
     * For platform:/resource/ URIs (Eclipse workspace), resolves
     * via the workspace root using reflection to avoid a hard
     * compile dependency on org.eclipse.core.resources — absent
     * in the CLI context.
     *
     * @param uri The EMF resource URI of the model
     * @return    The corresponding filesystem Path
     */
    private static Path resolveToFilesystemPath(URI uri) {
        log("Resolving model URI: " + uri);
        if (uri.isFile()) {
            Path path = Paths.get(uri.toFileString());
            log("Resolved as file URI: " + path.toAbsolutePath());
            return path;
        }
        if (uri.isPlatformResource()) {
            String platformPath = uri.toPlatformString(true);
            try {
                Class<?> rpClass = Class.forName(
                    "org.eclipse.core.resources.ResourcesPlugin");
                Object workspace = rpClass
                    .getMethod("getWorkspace")
                    .invoke(null);
                Object root = workspace.getClass()
                    .getMethod("getRoot")
                    .invoke(workspace);
                Class<?> pathClass = Class.forName(
                    "org.eclipse.core.runtime.Path");
                Object iPath = pathClass
                    .getConstructor(String.class)
                    .newInstance(platformPath);
                Class<?> iPathInterface = Class.forName(
                    "org.eclipse.core.runtime.IPath");
                Object iFile = root.getClass()
                    .getMethod("getFile", iPathInterface)
                    .invoke(root, iPath);
                Object location = iFile.getClass()
                    .getMethod("getLocation")
                    .invoke(iFile);
                Path path = Paths.get(location.toString());
                log("Resolved as platform URI: "
                    + path.toAbsolutePath());
                return path;
            } catch (Exception e) {
                logError("Cannot resolve platform URI: " + uri
                    + " — " + e.getMessage());
                throw new IllegalStateException(
                    "Cannot resolve platform URI to filesystem"
                    + " path: " + uri, e);
            }
        }
        throw new IllegalStateException(
            "Unsupported model URI scheme — expected file:/ or "
            + "platform:/resource/: " + uri);
    }

    // -----------------------------------------------------------------
    // AQL service methods — callable from MTL templates
    // -----------------------------------------------------------------

    /**
     * Returns the value of the given property key.
     * Returns an empty string if the key is not defined, to prevent
     * NullPointerExceptions in template string concatenations.
     *
     * Called in MTL as: 'key'.getPropertyValue()
     *
     * @param key The property key
     * @return    The property value, or '' if not found
     */
    public String getPropertyValue(String key) {
        PropertyEntry entry = propertyMap.get(key);
        return entry != null ? entry.value : "";
    }

    /**
     * Returns the value of the given property key.
     * Returns an error marker string if the key is not defined,
     * making misconfiguration visible in generated output.
     * Use for mandatory properties only.
     *
     * Called in MTL as: 'key'.getRequiredPropertyValue()
     *
     * @param key The property key
     * @return The property value, or 'ERROR: <key> is undefined' if not found
     */
    public String getRequiredPropertyValue(String key) {
        PropertyEntry entry = propertyMap.get(key);
        return entry != null ? entry.value : "ERROR: " + key + " is undefined";
    }
    
    /**
     * Returns true if the given property key has the value 'true'
     * (case-insensitive). Replaces the Acceleo 3.7 pattern:
     *   getProperty('key').trim().equalsIgnoreCase('true')
     *
     * Called in MTL as: 'key'.isPropertyTrue()
     *
     * @param key The property key
     * @return    true if the property value is 'true'
     *            (case-insensitive)
     */
    public boolean isPropertyTrue(String key) {
        PropertyEntry entry = propertyMap.get(key);
        return entry != null
            && entry.value.trim().equalsIgnoreCase("true");
    }

    /**
     * Returns a formatted string summarising the loaded configuration.
     * Includes a legend explaining the source indicators, followed by
     * an aligned table of indicator, key, and value sorted by key.
     * Intended for writing to a generated diagnostic file.
     *
     * Called in MTL as: [aModel.getConfigurationReport()/]
     *
     * @param context Any EObject — used as receiver for AQL dispatch
     * @return        A multi-line configuration report string
     */
    public String getConfigurationReport(EObject context) {
        StringBuilder sb = new StringBuilder();

        // legend — derive indicators from stored source paths
        String firstIndicator = genericPropsPath.isEmpty()
            ? "G"
            : propertyMap.values().stream()
                .filter(e -> e.source.equals(genericPropsPath))
                .map(e -> e.indicator)
                .findFirst().orElse("G");
        String secondIndicator = modelPropsPath.isEmpty()
            ? "M"
            : propertyMap.values().stream()
                .filter(e -> e.source.equals(modelPropsPath))
                .map(e -> e.indicator)
                .findFirst().orElse("M");

        sb.append("Source indicators:\n");
        sb.append("  ")
          .append(firstIndicator)
          .append("  =  ")
          .append(genericPropsPath.isEmpty()
              ? "(first properties file not loaded)"
              : genericPropsPath)
          .append("\n");
        sb.append("  ")
          .append(secondIndicator)
          .append("  =  ")
          .append(modelPropsPath.isEmpty()
              ? "(second properties file not found)"
              : modelPropsPath)
          .append("\n\n");

        sb.append("Properties loaded: ")
          .append(propertyMap.size())
          .append("\n\n");

        // compute column widths for alignment
        int keyWidth = propertyMap.keySet().stream()
            .mapToInt(String::length).max().orElse(20);
        int valWidth = propertyMap.values().stream()
            .mapToInt(e -> e.value.length()).max().orElse(20);

        // header
        sb.append("S   ")
          .append(pad("Key", keyWidth))
          .append("  ")
          .append(pad("Value", valWidth))
          .append("\n");
        sb.append("--  ")
          .append("-".repeat(keyWidth))
          .append("  ")
          .append("-".repeat(valWidth))
          .append("\n");

        // rows sorted by key
        propertyMap.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> sb
                .append(pad(e.getValue().indicator, 2))
                .append("  ")
                .append(pad(e.getKey(), keyWidth))
                .append("  ")
                .append(e.getValue().value)
                .append("\n"));

        return sb.toString();
    }

    // -----------------------------------------------------------------
    // Utility
    // -----------------------------------------------------------------

    private static String pad(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s;
        return s + " ".repeat(width - s.length());
    }
}
