package ucmism2t.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import org.eclipse.acceleo.query.runtime.ServiceUtils;
import org.eclipse.acceleo.query.runtime.impl.namespace.ClassLoaderQualifiedNameResolver;
import org.eclipse.acceleo.query.runtime.impl.namespace.JavaLoader;
import org.eclipse.acceleo.query.runtime.namespace.IQualifiedNameQueryEnvironment;
import org.eclipse.acceleo.query.runtime.namespace.IQualifiedNameResolver;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
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

import ucmism2t.services.ConfigurationService;
import ucmism2t.services.CustomServices;
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
 *    - Configuration files list (-config)
 * 
 * 2. EMF/UML2 Initialization
 *    - Registers UML2 5.0.0 metamodel
 *    - Configures resource factories
 * 
 * 3. Model Loading
 *    - Loads .uml files using EMF resource framework
 *    - Validates model structure
 * 
 * 4. Configuration Loading
 *    - Loads multiple properties files in order
 *    - Later files override earlier ones
 * 
 * 5. Acceleo Execution
 *    - Parses .mtl templates
 *    - Registers custom Java services
 *    - Executes transformation
 *    - Writes generated files to output directory
 * 
 * Usage:
 *   ucmism2t -input model.uml -output ./generated -config config1.properties,config2.properties
 * 
 * Exit Codes:
 *   0 = Success
 *   1 = Error (invalid arguments, file not found, transformation failed)
 * 
 * @author ucmism2t generator
 * @version 1.0.0
 * @since Java 21
 */
public class Main implements IApplication {
    
    // Command-line argument names
    private static final String ARG_INPUT = "-input";
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
        String[] args = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
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
            // Print banner
            printBanner();
            
            // Parse command-line arguments
            Arguments arguments = parseArguments(args);
            if (arguments == null) {
                printUsage();
                return Integer.valueOf(1);
            }
            
            // Display configuration
            System.out.println("Configuration:");
            System.out.println("  Input model: " + arguments.inputModel);
            System.out.println("  Output path: " + arguments.outputPath);
            System.out.println("  Config files: " + arguments.configFiles);
            System.out.println();
            
            // Initialize EMF and UML2
            System.out.println("Initializing EMF and UML2 framework...");
            initializeEMF();
            
            // Load the UML model
            System.out.println("Loading UML model: " + arguments.inputModel);
            EObject model = loadModel(arguments.inputModel);
            System.out.println("Model loaded successfully: " + model.eClass().getName());
            
            // Load configuration files
            System.out.println("Loading configuration files...");
            ConfigurationService configService = new ConfigurationService();
            if (!arguments.configFiles.isEmpty()) {
                configService.loadConfigurationFiles(
                    arguments.configFiles.toArray(new String[0])
                );
                System.out.println("Loaded " + configService.getPropertyCount() + " configuration properties");
            }
            
            // Execute Acceleo transformation
            System.out.println("Executing Acceleo transformation...");
            executeTransformation(model, arguments.outputPath, configService);
            
            System.out.println();
            System.out.println("✓ Transformation completed successfully!");
            return IApplication.EXIT_OK;
            
        } catch (Exception e) {
            System.err.println();
            System.err.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
            return Integer.valueOf(1);
        }
    }
    
    /**
     * Prints application banner.
     */
    private void printBanner() {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║               ucmism2t - Version 1.0.0                     ║");
        System.out.println("║     UML to Text Model-to-Text Transformation Tool          ║");
        System.out.println("║                                                            ║");
        System.out.println("║  Eclipse 2025-12 | Acceleo 4.2 | Tycho 5 | Java 21        ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
    }
    
    /**
     * Initializes EMF and UML2 frameworks.
     * 
     * Registers:
     * - UML2 5.0.0 metamodel packages
     * - UML resource factory for .uml files
     * - Standard library types
     */
    private void initializeEMF() {
        // Register UML2 5.0.0 package
        EPackage.Registry.INSTANCE.put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
        
        // Register UML resource factory for .uml file extension
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap()
            .put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);
        
        // Register UML resource factory for .uml namespace
        Resource.Factory.Registry.INSTANCE.getContentTypeToFactoryMap()
            .put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);
    }
    
    /**
     * Loads a UML model from file.
     * 
     * Creates an EMF ResourceSet, loads the .uml file, and resolves
     * all cross-references and proxies.
     * 
     * @param modelPath Path to .uml file
     * @return Root model element
     * @throws IOException If model cannot be loaded
     */
    private EObject loadModel(String modelPath) throws IOException {
        // Check file exists
        Path path = Paths.get(modelPath);
        if (!Files.exists(path)) {
            throw new IOException("Model file not found: " + modelPath);
        }
        
        // Create resource set and load model
        ResourceSet resourceSet = new ResourceSetImpl();
        URI uri = URI.createFileURI(new File(modelPath).getAbsolutePath());
        Resource resource = resourceSet.getResource(uri, true);
        
        // Check model loaded successfully
        if (resource == null || resource.getContents().isEmpty()) {
            throw new IOException("Failed to load model or model is empty: " + modelPath);
        }
        
        // Resolve all proxies (cross-references)
        EcoreUtil.resolveAll(resourceSet);
        
        return resource.getContents().get(0);
    }
    
    /**
     * Executes the Acceleo transformation.
     * 
     * Steps:
     * 1. Create AQL query environment
     * 2. Register UML2 metamodel packages
     * 3. Register custom Java services with proper Acceleo 4.x API
     * 4. Parse Acceleo module (m2t.mtl)
     * 5. Execute main template
     * 6. Write generated files to output directory
     * 
     * IMPORTANT: Acceleo 4.x API Changes
     * -------------------------------------
     * 1. Service Registration:
     *    ServiceUtils.registerServices() expects: Set<IService<?>>
     *    ServiceUtils.getServices() converts POJOs to IService wrappers
     * 
     *    Pattern:
     *      ServiceUtils.registerServices(queryEnvironment, 
     *          ServiceUtils.getServices(queryEnvironment, serviceInstance));
     * 
     * 2. Template Parsing:
     *    AcceleoParser.parse() returns: AcceleoAstResult (not Module directly!)
     *    Must call parseResult.getModule() to get the Module
     *    Check parseResult.getErrors() for parsing errors
     *    
     *    Signature: parse(InputStream, String qualifiedName, String charset)
     *    Example qualifiedName: "ucmism2t::templates::m2t"
     * 
     * @param model Root UML model element
     * @param outputPath Output directory path
     * @param configService Configuration service instance
     * @throws Exception If transformation fails
     */
    private void executeTransformation(EObject model, String outputPath, 
            ConfigurationService configService) throws Exception {
        
        // Create resource set for Acceleo environment
        Map<String, String> options = new HashMap<>();
        
        // Create resolver with correct 3-parameter constructor
        IQualifiedNameResolver resolver = new ClassLoaderQualifiedNameResolver(
            getClass().getClassLoader(),
            EPackage.Registry.INSTANCE,
            AcceleoParser.QUALIFIER_SEPARATOR
        );
        
        // CRITICAL: Use the model's EXISTING resource set, don't create a new one!
        // The model is already loaded in a resource set, we need to use that same one
        ResourceSet resourceSetForModels = model.eResource().getResourceSet();
        
        if (resourceSetForModels == null) {
            throw new Exception("Model's resource has no ResourceSet!");
        }
        
        System.out.println("Using model's resource set: " + resourceSetForModels.getClass().getName());
        System.out.println("Model resource URI: " + model.eResource().getURI());
        System.out.println("Model in resource contents: " + model.eResource().getContents().contains(model));
        
        // Create qualified query environment
        IQualifiedNameQueryEnvironment queryEnvironment = 
            AcceleoUtil.newAcceleoQueryEnvironment(options, resolver, resourceSetForModels, false);
        
        // Register UML2 5.0.0 metamodel packages  
        queryEnvironment.registerEPackage(UMLPackage.eINSTANCE);
        
        // Create evaluator
        AcceleoEvaluator evaluator = new AcceleoEvaluator(
            queryEnvironment.getLookupEngine(), 
            System.lineSeparator()
        );
        
        // Add loaders to resolver - THIS IS THE CRITICAL STEP
        resolver.addLoader(new ModuleLoader(new AcceleoParser(), evaluator));
        resolver.addLoader(new JavaLoader(AcceleoParser.QUALIFIER_SEPARATOR, false));
        
        // Register custom Java services (makes them callable from templates)
        ServiceUtils.registerServices(queryEnvironment, 
            ServiceUtils.getServices(queryEnvironment, configService));
        ServiceUtils.registerServices(queryEnvironment, 
            ServiceUtils.getServices(queryEnvironment, new DateTimeService()));
        ServiceUtils.registerServices(queryEnvironment, 
            ServiceUtils.getServices(queryEnvironment, new CustomServices()));
        
        // Load the Acceleo module using the resolver
        System.out.println("Loading Acceleo template: m2t.mtl");
        
        // Module qualified name (matches the path)
        String moduleQualifiedName = "ucmism2t::templates::m2t";
        
        System.out.println("Attempting to resolve module: " + moduleQualifiedName);
        System.out.println("Resolver class: " + resolver.getClass().getName());
        System.out.println("Loaders registered: " + 
            (resolver.toString().contains("ModuleLoader") ? "ModuleLoader found" : "No ModuleLoader"));
        
        // Resolve the module using the qualified name resolver
        // This is the correct way in Acceleo 4.x - resolver handles parsing via ModuleLoader
        Object resolved = resolver.resolve(moduleQualifiedName);
        
        System.out.println("Resolved object type: " + 
            (resolved != null ? resolved.getClass().getName() : "null"));
        
        Module module;
        if (resolved instanceof Module) {
            module = (Module) resolved;
            System.out.println("✓ Module resolved successfully: " + module.getName());
        } else {
            System.err.println("✗ Failed to resolve module!");
            System.err.println("  Expected: Module");
            System.err.println("  Got: " + (resolved != null ? resolved.getClass().getName() : "null"));
            
            // Try to get more info
            if (resolved != null) {
                System.err.println("  Object: " + resolved.toString());
            }
            
            throw new Exception("Failed to resolve module: " + moduleQualifiedName + 
                " (got " + (resolved != null ? resolved.getClass().getName() : "null") + ")");
        }
        
        System.out.println("Template parsed successfully: " + module.getName());
        
        // Check if we actually got any templates
        long templateCount = module.getModuleElements().stream()
            .filter(elem -> elem instanceof Template)
            .count();
        
        System.out.println("Module has " + module.getModuleElements().size() + 
            " elements (" + templateCount + " templates)");
        
        if (templateCount == 0) {
            throw new Exception("No executable templates in module - check MTL syntax");
        }
        
        // List found templates
        System.out.println("Found templates:");
        module.getModuleElements().stream()
            .filter(elem -> elem instanceof Template)
            .map(elem -> (Template)elem)
            .forEach(tmpl -> System.out.println("  - " + tmpl.getName()));
        
        // Create output directory if it doesn't exist
        File outputDir = new File(outputPath);
        if (!outputDir.exists()) {
            System.out.println("Creating output directory: " + outputPath);
            outputDir.mkdirs();
        }
        
        // Execute transformation
        System.out.println("Generating output files...");
        System.out.println("Output directory: " + outputDir.getAbsolutePath());
        
        try {
            // Create generation strategy
            IAcceleoGenerationStrategy strategy = new DefaultGenerationStrategy(
                resourceSetForModels.getURIConverter(),
                new DefaultWriterFactory()
            );
            
            // Convert output path to absolute File
            File outputDirFile = new File(outputPath);
            if (!outputDirFile.isAbsolute()) {
                // If relative path, resolve against current working directory
                outputDirFile = new File(System.getProperty("user.dir"), outputPath);
            }
            
            // Create output directory if it doesn't exist
            if (!outputDirFile.exists()) {
                outputDirFile.mkdirs();
                System.out.println("Created output directory: " + outputDirFile.getAbsolutePath());
            }
            
            // CRITICAL: Use file:// URI scheme for proper file path handling
            // IMPORTANT: Add trailing slash to ensure it's treated as a directory!
            String absolutePath = outputDirFile.getAbsolutePath();
            if (!absolutePath.endsWith(File.separator)) {
                absolutePath += File.separator;
            }
            URI destinationURI = URI.createFileURI(absolutePath);
            URI logURI = AcceleoUtil.getlogURI(destinationURI, options.get(AcceleoUtil.LOG_URI_OPTION));
            
            System.out.println("Generating output files...");
            System.out.println("Output directory (absolute): " + outputDirFile.getAbsolutePath());
            System.out.println("Destination URI: " + destinationURI);
            
            // Get main templates from the module
            List<Template> mainTemplates = AcceleoUtil.getMainTemplates(module);
            
            System.out.println("Found " + mainTemplates.size() + " main template(s)");
            
            if (mainTemplates.isEmpty()) {
                System.err.println("No @main templates found!");
                return;
            }
            
            // For each main template, call it with matching model elements
            for (Template template : mainTemplates) {
                System.out.println("Executing template: " + template.getName());
                
                // Get the template's parameter name and type
                String parameterName = template.getParameters().get(0).getName();
                
                System.out.println("  Parameter: " + parameterName);
                System.out.println("  Model type: " + model.eClass().getName());
                
                // Create variables map with the model
                Map<String, Object> variables = new LinkedHashMap<>();
                variables.put(parameterName, model);
                
                // Call the template with the model element
                // Signature: generate(Template, Map<String,Object> variables, AcceleoEvaluator, 
                //                     IQualifiedNameQueryEnvironment, IAcceleoGenerationStrategy,
                //                     URI destination, URI logURI, Monitor)
                AcceleoUtil.generate(
                    template,
                    variables,
                    evaluator,
                    queryEnvironment,
                    strategy,
                    destinationURI,
                    logURI,
                    new BasicMonitor()
                );
                
                System.out.println("  ✓ Template executed");
            }
            
            // CHECK GENERATION RESULT FOR ERRORS
            GenerationResult generationResult = evaluator.getGenerationResult();
            if (generationResult != null) {
                System.out.println("\n=== Generation Result ===");
                System.out.println("Generated files: " + generationResult.getGeneratedFiles().size());
                
                // Print actual file paths
                if (!generationResult.getGeneratedFiles().isEmpty()) {
                    System.out.println("\nGenerated file URIs:");
                    for (URI fileURI : generationResult.getGeneratedFiles()) {
                        System.out.println("  - " + fileURI.toString());
                        // Convert to file path if it's a file URI
                        if (fileURI.isFile()) {
                            File file = new File(fileURI.toFileString());
                            System.out.println("    File path: " + file.getAbsolutePath());
                            System.out.println("    Exists: " + file.exists());
                            if (file.exists()) {
                                System.out.println("    Size: " + file.length() + " bytes");
                            }
                        }
                    }
                }
                
                System.out.println("Lost files: " + generationResult.getLostFiles().size());
                
                Diagnostic diagnostic = generationResult.getDiagnostic();
                System.out.println("Diagnostic severity: " + diagnostic.getSeverity());
                
                if (diagnostic.getSeverity() >= Diagnostic.WARNING) {
                    System.out.println("\n=== Diagnostics ===");
                    printDiagnostic(diagnostic, "");
                }
                
                // Print summary
                System.out.println("\n=== Summary ===");
                System.out.println("Files: " + generationResult.getGeneratedFiles().size() + 
                    ", Lost Files: " + generationResult.getLostFiles().size() +
                    ", Diagnostics: " + diagnostic.getChildren().size());
            }
            
        } catch (Exception e) {
            System.err.println("  ✗ Error executing template: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Check generation result for errors
        if (evaluator.getGenerationResult() != null) {
            Diagnostic diagnostic = evaluator.getGenerationResult().getDiagnostic();
            if (diagnostic.getSeverity() >= Diagnostic.ERROR) {
                System.err.println("\nGeneration errors:");
                for (Diagnostic child : diagnostic.getChildren()) {
                    System.err.println("  " + child.getMessage());
                }
            }
        }
        
        // List generated files
        File[] files = outputDir.listFiles();
        if (files != null && files.length > 0) {
            System.out.println("\nGenerated " + files.length + " file(s):");
            for (File file : files) {
                if (file.isFile()) {
                    System.out.println("  - " + file.getName() + " (" + file.length() + " bytes)");
                }
            }
        } else {
            System.out.println("\nNote: No files were generated.");
            System.out.println("Check that your template contains [file] blocks.");
        }
        
        System.out.println("\nTemplate processing completed");
    }
    
    private void printDiagnostic(Diagnostic diagnostic, String indent) {
        if (diagnostic.getMessage() != null && !diagnostic.getMessage().isEmpty()) {
            String severity = "";
            switch (diagnostic.getSeverity()) {
                case Diagnostic.ERROR: severity = "ERROR"; break;
                case Diagnostic.WARNING: severity = "WARNING"; break;
                case Diagnostic.INFO: severity = "INFO"; break;
                default: severity = "OK"; break;
            }
            System.out.println(indent + "[" + severity + "] " + diagnostic.getMessage());
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
        
        // Parse arguments in pairs (flag + value)
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case ARG_INPUT:
                    if (i + 1 < args.length) {
                        arguments.inputModel = args[++i];
                    } else {
                        System.err.println("Missing value for " + ARG_INPUT);
                        return null;
                    }
                    break;
                    
                case ARG_OUTPUT:
                    if (i + 1 < args.length) {
                        arguments.outputPath = args[++i];
                    } else {
                        System.err.println("Missing value for " + ARG_OUTPUT);
                        return null;
                    }
                    break;
                    
                case ARG_CONFIG:
                    if (i + 1 < args.length) {
                        String configs = args[++i];
                        // Split comma-separated list
                        arguments.configFiles = Arrays.asList(configs.split(","));
                    } else {
                        System.err.println("Missing value for " + ARG_CONFIG);
                        return null;
                    }
                    break;
                    
                default:
                    System.err.println("Unknown argument: " + args[i]);
                    return null;
            }
        }
        
        // Validate required arguments
        if (arguments.inputModel == null) {
            System.err.println("Required argument missing: " + ARG_INPUT);
            return null;
        }
        
        if (arguments.outputPath == null) {
            System.err.println("Required argument missing: " + ARG_OUTPUT);
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
        System.out.println("  -input <path>      Path to input UML model file (.uml)");
        System.out.println("  -output <path>     Path to output directory for generated files");
        System.out.println();
        System.out.println("Optional Options:");
        System.out.println("  -config <files>    Comma-separated list of configuration files");
        System.out.println("                     (Java properties format)");
        System.out.println("                     Later files override earlier ones");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  Basic usage:");
        System.out.println("    ucmism2t -input model.uml -output ./generated");
        System.out.println();
        System.out.println("  With configuration:");
        System.out.println("    ucmism2t -input model.uml -output ./generated \\");
        System.out.println("             -config defaults.properties,production.properties");
        System.out.println();
        System.out.println("Configuration File Format:");
        System.out.println("  # Comment lines start with #");
        System.out.println("  output.encoding=UTF-8");
        System.out.println("  max.depth=10");
        System.out.println("  project.name=MyProject");
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
