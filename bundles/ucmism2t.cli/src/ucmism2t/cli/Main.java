package ucmism2t.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.acceleo.Module;
import org.eclipse.acceleo.Template;
import org.eclipse.acceleo.aql.AcceleoUtil;
import org.eclipse.acceleo.aql.evaluation.AcceleoEvaluator;
import org.eclipse.acceleo.aql.evaluation.GenerationResult;
import org.eclipse.acceleo.aql.evaluation.strategy.DefaultGenerationStrategy;
import org.eclipse.acceleo.aql.evaluation.strategy.DefaultWriterFactory;
import org.eclipse.acceleo.aql.evaluation.strategy.IAcceleoGenerationStrategy;
import org.eclipse.acceleo.aql.parser.AcceleoParser;
import org.eclipse.acceleo.aql.parser.ModuleLoader;
import org.eclipse.acceleo.query.AQLUtils;
import org.eclipse.acceleo.query.ast.EClassifierTypeLiteral;
import org.eclipse.acceleo.query.runtime.ServiceUtils;
import org.eclipse.acceleo.query.runtime.impl.namespace.ClassLoaderQualifiedNameResolver;
import org.eclipse.acceleo.query.runtime.impl.namespace.JavaLoader;
import org.eclipse.acceleo.query.runtime.namespace.IQualifiedNameQueryEnvironment;
import org.eclipse.acceleo.query.runtime.namespace.IQualifiedNameResolver;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;

import ucmism2t.services.PropertiesService;
//import ucmism2t.services.CustomServices;
import ucmism2t.services.DateTimeService;

/**
 * Main CLI Application for ucmism2t
 *
 * This class implements the command-line interface for the UML to Text
 * transformation tool. It handles:
 *
 * 1. Argument Parsing
 *    - Input UML model path (-input)
 *    - Output directory path (-output)
 *    - Optional explicit property files (-config)
 *
 * 2. EMF/UML2 Initialization
 *    - Registers UML2 5.0.0 metamodel
 *    - Configures resource factories
 *
 * 3. Model Loading
 *    - Creates resource set via AQLUtils.createResourceSetForModels()
 *      with a dedicated generationKey object
 *    - Loads .uml/.xmi files using EMF resource framework
 *
 * 4. Configuration Loading
 *    - Without -config: derives property file paths from model path:
 *        <projectFolder>/property/generic.properties (mandatory)
 *        <modelFolder>/<modelName>.properties (optional)
 *    - With -config: loads the specified files in order, later files
 *      override earlier ones, all files are mandatory
 *
 * 5. Acceleo Execution
 *    - Uses the ucmism2t.core bundle classloader (via PropertiesService)
 *      so the resolver can find all MTL files packaged in that bundle
 *    - Pre-loads all transitive module dependencies before generation
 *      so their queries are registered in the query environment
 *    - Follows the official Acceleo 4 MainGenerator pattern:
 *      AQLUtils.createResourceSetForModels with generationKey,
 *      AQLUtils.getAllNeededEPackages + registerEPackages,
 *      AQLUtils.cleanResourceSetForModels + cleanServices on exit
 *
 * Usage:
 *   ucmism2t -application ucmism2t.cli.app \
 *       -input model.uml -output ./generated
 *   ucmism2t -application ucmism2t.cli.app \
 *       -input model.uml -output ./generated \
 *       -config generic.properties,model.properties
 *
 * Exit Codes:
 *   0 = Success
 *   1 = Error (invalid arguments, file not found, transformation
 *       failed)
 *
 * @author ucmism2t generator
 * @version 1.0.0
 * @since Java 21
 */
public class Main implements IApplication {

    // Command-line argument names
    private static final String ARG_INPUT  = "-input";
    private static final String ARG_OUTPUT = "-output";
    private static final String ARG_CONFIG = "-config";

    /**
     * Application entry point for Eclipse runtime.
     *
     * @param context Application context from Eclipse
     * @return Exit code (SUCCESS or error)
     */
    @Override
    public Object start(IApplicationContext context) throws Exception {
        String[] args = (String[]) context.getArguments()
            .get(IApplicationContext.APPLICATION_ARGS);
        return run(args);
    }

    /**
     * Application cleanup (no-op for this application).
     */
    @Override
    public void stop() {
        // Nothing to clean up
    }

    /**
     * Main entry point for standalone JAR execution.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        Main app = new Main();
        try {
            Object result = app.run(args);
            System.exit(result == IApplication.EXIT_OK ? 0 : 1);
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Main application logic.
     *
     * @param args Command-line arguments
     * @return EXIT_OK on success, error code otherwise
     */
    private Object run(String[] args) {
        try {
            printBanner();

            Arguments arguments = parseArguments(args);
            if (arguments == null) {
                printUsage();
                return Integer.valueOf(1);
            }

            System.out.println("Configuration:");
            System.out.println(
                "  Input model  : " + arguments.inputModel);
            System.out.println(
                "  Output path  : " + arguments.outputPath);
            if (!arguments.configFiles.isEmpty()) {
                System.out.println(
                    "  Config files : " + arguments.configFiles);
            } else {
                System.out.println(
                    "  Config files : (derived from model path)");
            }
            System.out.println();

            System.out.println(
                "Initializing EMF and UML2 framework...");
            initializeEMF();

            System.out.println("Loading configuration files...");
            PropertiesService configService = new PropertiesService();
            Path modelPath = Paths.get(arguments.inputModel)
                .toAbsolutePath().normalize();
            Path projectFolder = modelPath.getParent().getParent();

            if (arguments.configFiles.isEmpty()) {
                PropertiesService.init(projectFolder, modelPath);
            } else {
                PropertiesService.init(
                    projectFolder, modelPath,
                    arguments.configFiles);
            }

            System.out.println("Executing Acceleo transformation...");
            executeTransformation(
                arguments.inputModel,
                arguments.outputPath,
                configService);

            System.out.println();
            System.out.println(
                "\u2713 Transformation completed successfully!");
            return IApplication.EXIT_OK;

        } catch (Exception e) {
            System.err.println();
            System.err.println("\u2717 Error: " + e.getMessage());
            e.printStackTrace();
            return Integer.valueOf(1);
        }
    }

    /**
     * Prints application banner.
     */
    private void printBanner() {
        System.out.println(
            "\u2554\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550"
            + "\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550"
            + "\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550"
            + "\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550"
            + "\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550"
            + "\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550"
            + "\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2557");
        System.out.println(
            "\u2551               ucmism2t - Version 1.0.0"
            + "                      \u2551");
        System.out.println(
            "\u2551     UML to Text Model-to-Text Transformation Tool"
            + "           \u2551");
        System.out.println(
            "\u2551                                                   "
            + "          \u2551");
        System.out.println(
            "\u2551  Eclipse 2026-03 | Acceleo 4.2 | Tycho 5 | Java 21"
            + "          \u2551");
        System.out.println(
            "\u255a\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550"
            + "\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550"
            + "\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550"
            + "\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550"
            + "\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550"
            + "\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550"
            + "\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u255d");
        System.out.println();
    }

    /**
     * Registers EMF and UML2 frameworks globally.
     */
    private void initializeEMF() {
        EPackage.Registry.INSTANCE.put(
            UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
        Resource.Factory.Registry.INSTANCE
            .getExtensionToFactoryMap()
            .put(UMLResource.FILE_EXTENSION,
                UMLResource.Factory.INSTANCE);
        Resource.Factory.Registry.INSTANCE
            .getContentTypeToFactoryMap()
            .put(UMLResource.FILE_EXTENSION,
                UMLResource.Factory.INSTANCE);
    }

    /**
     * Executes the Acceleo transformation.
     *
     * Key design decisions:
     *
     * Classloader: Uses PropertiesService.class.getClassLoader()
     * which is the ucmism2t.core bundle classloader. This gives the
     * resolver access to all MTL files packaged in that bundle.
     * Using getClass().getClassLoader() (the CLI bundle classloader)
     * would cause all MTL imports to resolve to null.
     *
     * Pre-loading: After resolving the main module, explicitly
     * resolves all transitive imports via getAllNeededQualifiedNames.
     * This forces the ModuleLoader to parse each imported module and
     * register its queries in the query environment before generation
     * begins. Without this step, imported queries are not found at
     * runtime even though the module names are known.
     *
     * EPackage registration: Uses AQLUtils.getAllNeededEPackages +
     * registerEPackages to register all metamodel packages needed by
     * the module and its transitive imports.
     *
     * @param modelPathStr  Path to the model file
     * @param outputPath    Output directory path
     * @param configService Configuration service instance
     * @throws Exception If transformation fails
     */
    private void executeTransformation(
            String modelPathStr,
            String outputPath,
            PropertiesService configService) throws Exception {

        Path modelPath = Paths.get(modelPathStr);
        if (!Files.exists(modelPath)) {
            throw new IOException(
                "Model file not found: " + modelPathStr);
        }

        final Map<String, String> options = new LinkedHashMap<>();
        options.put(AcceleoUtil.LOG_URI_OPTION, "acceleo.log");
        options.put(AcceleoUtil.NEW_LINE_OPTION,
            System.lineSeparator());

        // Dedicated generation key for resource set lifecycle
        final Object generationKey = new Object();
        final List<Exception> exceptions = new ArrayList<>();

        // Create resource set per official MainGenerator pattern
        final ResourceSet resourceSetForModels =
            AQLUtils.createResourceSetForModels(
                exceptions, generationKey,
                new ResourceSetImpl(), options);

        if (!exceptions.isEmpty()) {
            exceptions.forEach(e ->
                System.err.println(
                    "Resource set init warning: "
                    + e.getMessage()));
        }

        // Register UML factories on the integrated resource set
        resourceSetForModels.getResourceFactoryRegistry()
            .getExtensionToFactoryMap()
            .put(UMLResource.FILE_EXTENSION,
                UMLResource.Factory.INSTANCE);
        resourceSetForModels.getPackageRegistry()
            .put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);

        // Load model
        System.out.println(
            "Loading UML model: " + modelPathStr);
        URI modelURI = URI.createFileURI(
            new File(modelPathStr).getAbsolutePath());
        Resource resource =
            resourceSetForModels.getResource(modelURI, true);

        if (resource == null || resource.getContents().isEmpty()) {
            throw new IOException(
                "Failed to load model or model is empty: "
                + modelPathStr);
        }

        EcoreUtil.resolveAll(resourceSetForModels);

        EObject model = resource.getContents().get(0);
        System.out.println("Model loaded successfully: "
            + model.eClass().getName());

        // Create resolver using the ucmism2t.core classloader.
        // PropertiesService lives in ucmism2t.core which contains
        // all MTL files. Using the CLI classloader instead causes
        // all MTL imports to resolve to null.
        final IQualifiedNameResolver resolver =
            new ClassLoaderQualifiedNameResolver(
                PropertiesService.class.getClassLoader(),
                EPackage.Registry.INSTANCE,
                AcceleoParser.QUALIFIER_SEPARATOR
            );

        // Create query environment
        final IQualifiedNameQueryEnvironment queryEnvironment =
            AcceleoUtil.newAcceleoQueryEnvironment(
                options, resolver, resourceSetForModels, false);

        // Create evaluator and add loaders
        final AcceleoEvaluator evaluator = new AcceleoEvaluator(
            queryEnvironment.getLookupEngine(),
            System.lineSeparator()
        );

        resolver.addLoader(
            new ModuleLoader(new AcceleoParser(), evaluator));
        resolver.addLoader(
            new JavaLoader(AcceleoParser.QUALIFIER_SEPARATOR, false));

        // Register Java services
        ServiceUtils.registerServices(queryEnvironment,
            ServiceUtils.getServices(queryEnvironment, configService));
        ServiceUtils.registerServices(queryEnvironment,
            ServiceUtils.getServices(
                queryEnvironment, new DateTimeService()));
//        ServiceUtils.registerServices(queryEnvironment,
//            ServiceUtils.getServices(
//                queryEnvironment, new CustomServices()));

        // Resolve main module
        System.out.println("Loading Acceleo template: m2tMaster.mtl");
        final String moduleQualifiedName =
            "ucmism2t::templates::m2tMaster";

        Object resolved = resolver.resolve(moduleQualifiedName);

        Module module;
        if (resolved instanceof Module) {
            module = (Module) resolved;
            System.out.println(
                "\u2713 Module resolved: " + module.getName());
        } else {
            throw new Exception(
                "Failed to resolve module: " + moduleQualifiedName
                + " (got " + (resolved != null
                    ? resolved.getClass().getName() : "null") + ")");
        }

        // Pre-load all transitive imports so their queries are
        // registered in the query environment before generation.
        // resolver.resolve() on the main module only parses that
        // module — imported modules are known by name but not yet
        // parsed. Calling resolve() on each forces parsing and query
        // registration for the full dependency graph.
        System.out.println(
            "Pre-loading transitive module dependencies...");
        int loaded = 0;
        for (String name : AQLUtils.getAllNeededQualifiedNames(
                resolver, moduleQualifiedName)) {
            if (!name.equals(moduleQualifiedName)) {
                resolver.resolve(name);
                loaded++;
            }
        }
        System.out.println(
            "Pre-loaded " + loaded + " dependent modules.");

        // Register all EPackages needed by module and its imports
        final Set<String> nsURIs = AQLUtils.getAllNeededEPackages(
            resolver, moduleQualifiedName);
        final List<String> unregistered = AQLUtils.registerEPackages(
            queryEnvironment, EPackage.Registry.INSTANCE, nsURIs);
        System.out.println("Registered "
            + (nsURIs.size() - unregistered.size())
            + " EPackage(s).");
        if (!unregistered.isEmpty()) {
            unregistered.forEach(uri ->
                System.err.println(
                    "WARNING — could not register EPackage: "
                    + uri));
        }

        // Verify main templates
        List<Template> mainTemplates =
            AcceleoUtil.getMainTemplates(module);
        System.out.println("Found "
            + mainTemplates.size() + " main template(s):");
        mainTemplates.forEach(t ->
            System.out.println("  - " + t.getName()));

        if (mainTemplates.isEmpty()) {
            throw new Exception(
                "No @main templates found in module"
                + " — check MTL syntax");
        }

        // Prepare output directory
        File outputDirFile = new File(outputPath);
        if (!outputDirFile.isAbsolute()) {
            outputDirFile = new File(
                System.getProperty("user.dir"), outputPath);
        }
        if (!outputDirFile.exists()) {
            outputDirFile.mkdirs();
            System.out.println("Created output directory: "
                + outputDirFile.getAbsolutePath());
        }

        String absolutePath = outputDirFile.getAbsolutePath();
        if (!absolutePath.endsWith(File.separator)) {
            absolutePath += File.separator;
        }
        final URI destinationURI =
            URI.createFileURI(absolutePath);
        final URI logURI = AcceleoUtil.getlogURI(
            destinationURI,
            options.get(AcceleoUtil.LOG_URI_OPTION));

        System.out.println("Generating output files...");

        final IAcceleoGenerationStrategy strategy =
            new DefaultGenerationStrategy(
                resourceSetForModels.getURIConverter(),
                new DefaultWriterFactory()
            );

        // Generate per official MainGenerator pattern
        try {
            final Map<EClass, List<EObject>> valuesCache =
                new LinkedHashMap<>();
            for (Template template : mainTemplates) {
                System.out.println(
                    "Executing template: "
                    + template.getName());
                final EClassifierTypeLiteral typeLiteral =
                    (EClassifierTypeLiteral) template
                        .getParameters().get(0)
                        .getType().getAst();
                final List<EObject> values =
                    AcceleoUtil.getValues(
                        typeLiteral, queryEnvironment,
                        List.of(resource), valuesCache,
                        new BasicMonitor());
                final String parameterName =
                    template.getParameters().get(0).getName();
                final Map<String, Object> variables =
                    new LinkedHashMap<>();
                for (EObject value : values) {
                    variables.put(parameterName, value);
                    AcceleoUtil.generate(
                        template, variables, evaluator,
                        queryEnvironment, strategy,
                        destinationURI, logURI,
                        new BasicMonitor());
                }
                System.out.println(
                    "  \u2713 Template executed");
            }
        } finally {
            AQLUtils.cleanResourceSetForModels(
                generationKey, resourceSetForModels);
            AcceleoUtil.cleanServices(
                queryEnvironment, resourceSetForModels);
        }

        // Report results
        GenerationResult generationResult =
            evaluator.getGenerationResult();
        if (generationResult != null) {
            Diagnostic diagnostic =
                generationResult.getDiagnostic();

            if (diagnostic.getSeverity() >= Diagnostic.WARNING) {
                System.out.println("\n=== Diagnostics ===");
                printDiagnostic(diagnostic, "");
            }

            System.out.println("\n=== Summary ===");
            System.out.println("Files: "
                + generationResult.getGeneratedFiles().size()
                + ", Lost Files: "
                + generationResult.getLostFiles().size()
                + ", Errors: "
                + countBySeverity(diagnostic, Diagnostic.ERROR)
                + ", Warnings: "
                + countBySeverity(diagnostic, Diagnostic.WARNING)
                + ".");
        }
    }

    private int countBySeverity(
            Diagnostic diagnostic, int severity) {
        return (int) diagnostic.getChildren().stream()
            .filter(d -> d.getSeverity() == severity)
            .count();
    }

    private void printDiagnostic(
            Diagnostic diagnostic, String indent) {
        if (diagnostic.getMessage() != null
                && !diagnostic.getMessage().isEmpty()) {
            String severity;
            switch (diagnostic.getSeverity()) {
                case Diagnostic.ERROR:   severity = "ERROR";   break;
                case Diagnostic.WARNING: severity = "WARNING"; break;
                case Diagnostic.INFO:    severity = "INFO";    break;
                default:                 severity = "OK";      break;
            }
            System.out.println(indent + "[" + severity + "] "
                + diagnostic.getMessage());
        }
        for (Diagnostic child : diagnostic.getChildren()) {
            printDiagnostic(child, indent + "  ");
        }
    }

    /**
     * Parses command-line arguments.
     *
     * Expected format:
     *   -input <path> -output <path> [-config <file1,file2,...>]
     *
     * @param args Command-line arguments array
     * @return Parsed Arguments object, or null if invalid
     */
    private Arguments parseArguments(String[] args) {
        Arguments arguments = new Arguments();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case ARG_INPUT:
                    if (i + 1 < args.length) {
                        arguments.inputModel = args[++i];
                    } else {
                        System.err.println(
                            "Missing value for " + ARG_INPUT);
                        return null;
                    }
                    break;

                case ARG_OUTPUT:
                    if (i + 1 < args.length) {
                        arguments.outputPath = args[++i];
                    } else {
                        System.err.println(
                            "Missing value for " + ARG_OUTPUT);
                        return null;
                    }
                    break;

                case ARG_CONFIG:
                    if (i + 1 < args.length) {
                        arguments.configFiles =
                            Arrays.asList(args[++i].split(","));
                    } else {
                        System.err.println(
                            "Missing value for " + ARG_CONFIG);
                        return null;
                    }
                    break;

                default:
                    System.err.println(
                        "Unknown argument: " + args[i]);
                    return null;
            }
        }

        if (arguments.inputModel == null) {
            System.err.println(
                "Required argument missing: " + ARG_INPUT);
            return null;
        }

        if (arguments.outputPath == null) {
            System.err.println(
                "Required argument missing: " + ARG_OUTPUT);
            return null;
        }

        return arguments;
    }

    /**
     * Prints usage information.
     */
    private void printUsage() {
        System.out.println("Usage: ucmism2t [options]");
        System.out.println();
        System.out.println("Required Options:");
        System.out.println(
            "  -input <path>      Path to input UML model file"
            + " (.uml or .xmi)");
        System.out.println(
            "  -output <path>     Path to output directory for"
            + " generated files");
        System.out.println();
        System.out.println("Optional Options:");
        System.out.println(
            "  -config <files>    Comma-separated list of explicit"
            + " property files.");
        System.out.println(
            "                     Overrides automatic path"
            + " derivation.");
        System.out.println(
            "                     Later files override earlier"
            + " ones.");
        System.out.println(
            "                     All specified files are"
            + " mandatory.");
        System.out.println();
        System.out.println("Configuration (without -config):");
        System.out.println(
            "  Property files are derived automatically from the"
            + " model path:");
        System.out.println(
            "    Generic  : <projectFolder>/property/"
            + "generic.properties  (mandatory)");
        System.out.println(
            "    Specific : <modelFolder>/<modelName>.properties"
            + "  (optional)");
        System.out.println(
            "  The project folder is two levels above the model"
            + " file.");
        System.out.println();
        System.out.println("Examples:");
        System.out.println(
            "  Automatic property derivation:");
        System.out.println(
            "    ucmism2t -application ucmism2t.cli.app \\");
        System.out.println(
            "        -input /path/to/project/model/my.uml"
            + " -output ./generated");
        System.out.println();
        System.out.println(
            "  Explicit property files:");
        System.out.println(
            "    ucmism2t -application ucmism2t.cli.app \\");
        System.out.println(
            "        -input ./model/my.uml"
            + " -output ./generated \\");
        System.out.println(
            "        -config ./property/generic.properties"
            + ",./model/my.properties");
    }

    /**
     * Internal class to hold parsed command-line arguments.
     */
    private static class Arguments {
        String inputModel;
        String outputPath;
        List<String> configFiles = new ArrayList<>();
    }
}
