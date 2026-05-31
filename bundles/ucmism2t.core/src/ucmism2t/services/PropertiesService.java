package ucmism2t.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.eclipse.emf.common.util.URI;
import org.eclipse.uml2.uml.Model;

/**
 * AQL service providing property lookup for Acceleo 4 generation.
 *
 * Two property files are loaded in order:
 *   1. <projectFolder>/property/generic.properties — always loaded, provides defaults
 *   2. <modelFolder>/<modelName>.properties        — model-specific, optional, overrides generic
 *
 * The project folder is derived from the model path as:
 *   <projectFolder>/model/<modelName>.uml
 *   <projectFolder>/property/generic.properties
 *
 * Two initialisation paths are supported:
 *   - Acceleo 4 IDE launch: initProperties(aModel) called from m2tMaster.mtl
 *   - CLI launch:           init(projectFolder, modelPath) called from the CLI launcher
 */
public class PropertiesService {

    private static final Properties properties = new Properties();

    // -------------------------------------------------------------------------
    // Initialisation from MTL — Acceleo 4 IDE launch
    // -------------------------------------------------------------------------

    /**
     * AQL service method — called from the main MTL template as:
     *   [aModel.initProperties()/]
     *
     * Derives the project folder from the model resource URI:
     *   model is at <projectFolder>/model/<modelName>.uml
     *   so projectFolder = modelPath.getParent().getParent()
     *
     * @param aModel the UML root model element being generated
     */
    public void initProperties(Model aModel) {
        URI modelURI = aModel.eResource().getURI();
        Path modelPath = resolveToFilesystemPath(modelURI);
        Path projectFolder = modelPath.getParent().getParent();
        init(projectFolder, modelPath);
    }

    // -------------------------------------------------------------------------
    // Initialisation from Java — CLI launch
    // -------------------------------------------------------------------------

    /**
     * Called directly from the CLI launcher before generation.
     * The CLI has program arguments so both paths are passed explicitly.
     *
     * @param projectFolder absolute path to the project root (ucmism2t/)
     * @param modelPath     absolute path to the model file
     */
    public static void init(Path projectFolder, Path modelPath) {
        properties.clear();

        // 1. Generic properties — mandatory
        Path genericProps = projectFolder.resolve(
                Paths.get("property", "generic.properties"));
        loadFile(genericProps, true);

        // 2. Model-specific properties — optional, overrides generic
        String modelFilename = modelPath.getFileName().toString();
        String modelBasename = modelFilename.contains(".")
                ? modelFilename.substring(0, modelFilename.lastIndexOf('.'))
                : modelFilename;
        Path modelProps = modelPath.getParent()
                .resolve(modelBasename + ".properties");
        loadFile(modelProps, false);
    }

    // -------------------------------------------------------------------------
    // URI resolution
    // -------------------------------------------------------------------------

    /**
     * Resolves an EMF URI to a filesystem Path.
     * Supports file:/ URIs directly.
     * For platform:/resource/ URIs (Eclipse workspace), resolves via the
     * workspace root using reflection to avoid a hard compile dependency on
     * org.eclipse.core.resources — absent in the CLI context.
     *
     * @param uri the EMF resource URI of the model
     * @return    the corresponding filesystem Path
     */
    private static Path resolveToFilesystemPath(URI uri) {
        if (uri.isFile()) {
            return Paths.get(uri.toFileString());
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
                return Paths.get(location.toString());
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Cannot resolve platform URI to filesystem path: "
                        + uri, e);
            }
        }
        throw new IllegalStateException(
                "Unsupported model URI scheme — expected file:/ or "
                + "platform:/resource/: " + uri);
    }

    // -------------------------------------------------------------------------
    // Internal loading
    // -------------------------------------------------------------------------

    private static void loadFile(Path path, boolean mandatory) {
        if (!Files.exists(path)) {
            if (mandatory) {
                throw new IllegalStateException(
                        "Mandatory properties file not found: "
                        + path.toAbsolutePath());
            }
            return;
        }
        try (InputStream is = Files.newInputStream(path)) {
            properties.load(is);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load properties file: "
                    + path.toAbsolutePath(), e);
        }
    }

    // -------------------------------------------------------------------------
    // AQL service methods — callable from MTL templates
    // -------------------------------------------------------------------------

    /**
     * Returns the value of the given property key.
     * Returns an empty string if the key is not defined, to prevent
     * NullPointerExceptions in template string concatenations.
     *
     * Called in MTL as: 'key'.getProperty()
     *
     * @param key the property key
     * @return    the property value, or '' if not found
     */
    public String getPropertyValue(String key) {
        return properties.getProperty(key, "");
    }

    /**
     * Returns true if the given property key has the value 'true'
     * (case-insensitive). Replaces the Acceleo 3.7 pattern:
     *   getProperty('key').trim().equalsIgnoreCase('true')
     *
     * Called in MTL as: 'key'.isPropertyTrue()
     *
     * @param key the property key
     * @return    true if the property value is 'true' (case-insensitive)
     */
    public boolean isPropertyTrue(String key) {
        return properties.getProperty(key, "")
                .trim()
                .equalsIgnoreCase("true");
    }
}