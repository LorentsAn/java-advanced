// :NOTE:
// java-solutions\info\kgeorgiy\ja\lorents\implementor\Implementor.java:173: error: method does not override or implement a method from a supertype
//    @Override
//    ^
// java-solutions\info\kgeorgiy\ja\lorents\implementor\Implementor.java:303: error: method does not override or implement a method from a supertype
//    @Override
//    ^
package info.kgeorgiy.ja.lorents.implementor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.JarImpler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * Class implementing {@link JarImpler}, {@link Impler}.
 * Provides methods for writing interface implementations.
 *
 * @author Anna Lorents
 */
public class Implementor implements JarImpler {

    /**
     * Correct form of command line arguments
     */
    private static final String CORRECT_FORM_OF_ARGUMENTS = "use ([-jar]) [full interface name]";
    /**
     * Line feed character
     */
    private static final String NEW_LINE = System.lineSeparator();
    /**
     * Tab character
     */
    private static final String LINE_SKIP = NEW_LINE + NEW_LINE;
    /**
     * String with java extension
     */
    private static final String JAVA_EXTENSION = ".java";
    /**
     * String with class extension
     */
    private static final String CLASS_EXTENSION = ".class";
    /**
     * String with implemented class suffix
     */
    private static final String IMPL_SUFFIX = "Impl";

    /**
     * Check the correctness of the passed {@link Class}.
     * Returns an error if the class is private or not an interface.
     *
     * @param token {@link Class} which was passed as a command line argument and is checked for correctness.
     * @throws ImplerException if the class is private or not an interface.
     */
    private void correctToken(Class<?> token) throws ImplerException {
        // :NOTE: checks order - done
        if (Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("You can't implement a private clazz");
        }
        if (!token.isInterface()) {
            throw new ImplerException("The specified token is not an interface");
        }
    }

    /**
     * Pass the path to the implementation of the {@link Class},
     * which contains the name and {@code .java} extension of the class.
     *
     * @param token {@link Class} for which the class is implemented.
     * @param root  {@link Path} for implementation of class.
     * @return {@link Path} where should the generated class be located.
     */
    private Path getPathToOutputFile(Class<?> token, Path root) {
        return root.resolve(getTokenPath(token) + JAVA_EXTENSION);
    }

    /**
     * Pass the {@link Path} to the implementation of the {@link Class}, which contains the name of the class.
     * The path does not contain the class extension, only its name with the suffix {@value IMPL_SUFFIX}.
     *
     * @param token {@link Class} for which the class is implemented.
     * @return {@link Path} where should the generated class be located.
     */
    private Path getTokenPath(Class<?> token) {
        return Paths.get(token.getPackageName()
                .replace('.', File.separatorChar)).resolve(getSimpleNameAndSuffix(token));
    }

    /**
     * Converts the given string to unicode.
     * @param string {@link String} to convert to unicode.
     * @return unicode {@link String}
     */
    private static String toUnicode(String string) {
        StringBuilder str = new StringBuilder();
        for (char c : string.toCharArray()) {
            if (c < 128) {
                str.append(c);
            } else {
                str.append(String.format("\\u%04X", (int) c));
            }
        }
        return str.toString();
    }

    /**
     * Pass the {@link String} name of the implemented {@link Class}.
     * Returns the name of the implemented class, it is created from the name of the original interface
     * with the addition of a suffix {@value IMPL_SUFFIX}.
     *
     * @param token The {@link Class} from which the name is taken.
     * @return {@link String} the name of the implemented class.
     */
    private String getSimpleNameAndSuffix(Class<?> token) {
        return token.getSimpleName() + IMPL_SUFFIX;
    }

    /**
     * Creates the parent directory of the given {@link Path}.
     *
     * @param path {@link Path} to create directory.
     * @throws ImplerException throw if an error has occurred during an creating.
     */
    private void createParentDir(Path path) throws ImplerException {
        Path parentPath = path.toAbsolutePath().normalize().getParent();
        try {
            // :NOTE: NPE - done
            if (parentPath != null) {
                Files.createDirectories(parentPath);
            }
            // :NOTE: e - done
        } catch (IOException e) {
            throw new ImplerException("Could not create dir" + e.getMessage());
        }
    }

    /**
     * Main method interacting with command line arguments.
     * Usage: ([-jar]) [class name] [root directory]
     *
     * @param args command line arguments.
     * @see #implement(Class, Path)
     * @see #implementJar(Class, Path)
     */
    public static void main(String[] args) {
        try {
            if (args == null || !(args.length == 2 || (args.length == 3 && args[0].equals("-jar")))) {
                throw new ImplerException("Wrong arguments, " + CORRECT_FORM_OF_ARGUMENTS);
            }
            // :NOTE: formatting - done
            for (String arg : args) {
                if (arg == null) {
                    throw new ImplerException("All arguments should be not null, " + CORRECT_FORM_OF_ARGUMENTS);
                }
            }

            Implementor implementor = new Implementor();
            if (args.length == 2) {
                implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
            } else {
                implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
            }
        } catch (ImplerException e) {
            System.err.println("ImplerException: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("ClassNotFoundException: " + e.getMessage());
        } catch (InvalidPathException e) {
            System.err.println("InvalidPathException: " + e.getMessage());
        }
    }

    /**
     * Creates an implementation of the interface passed by {@code token} to the method
     * and writes it to the passed path {@code root}.
     *
     * @param token type token to create implementation for.
     * @param root  root directory.
     * @throws ImplerException if an error occurs while implementing and writing the file.
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        correctToken(token);
        Path pathToOutputFile = getPathToOutputFile(token, root);
        createParentDir(pathToOutputFile);

        try (BufferedWriter writer = Files.newBufferedWriter(pathToOutputFile)) {
            generateCode(token, root, writer);
        } catch (IOException e) {
            System.err.println("Failed to write in file" + e.getMessage());
        }
    }

    /**
     * A method that completely writes the implementation of a class.
     *
     * @param token  {@link Class} type token to create implementation for.
     * @param root   {@link Path} root directory.
     * @param writer given by {@link BufferedWriter}.
     * @throws IOException if an I/O exception occurs while writing to file.
     * @see #generatePackage(String)
     * @see #classHeadDeclaration(Class)
     * @see #generateMethods(Class)
     */
    private void generateCode(Class<?> token, Path root, Writer writer) throws IOException {
        try {
            writer.write(toUnicode(String.join(NEW_LINE,
                    generatePackage(token.getPackageName()),
                    classHeadDeclaration(token),
                    generateMethods(token),
                    "}"
            )));
        } catch (IOException e) {
            System.err.println("Unable to write to file " + getSimpleNameAndSuffix(token) + ".java" + e.getMessage());
        }
    }

    /**
     * Generates {@link String} implementation of all class {@link Method} excluding private.
     *
     * @param token {@link Class} type token to create implementation for.
     * @return a {@link String} with all class methods.
     */
    private String generateMethods(Class<?> token) {
        StringBuilder str = new StringBuilder();
        Arrays.stream(token.getMethods())
                .filter(method -> !Modifier.isPrivate(method.getModifiers()))
                .forEach(method -> str.append(methodBlock(method)));
        return str.toString();
    }

    /**
     * For each {@link Method} generates its implementation.
     *
     * @param method {@link Method} for which the implementation is generated.
     * @return {@link String} with method head and body and return value.
     */
    // :NOTE: methodBlock - done
    private String methodBlock(Method method) {
        return String.format("public %s %s (%s) {return %s;} %s",
                method.getReturnType().getCanonicalName(),
                method.getName(), getMethodArguments(method),
                getMethodReturnValue(method), LINE_SKIP);
    }

    /**
     * Passes the default return value for the {@link Method}.
     *
     * @param method {@link Method} for which the return value is generated.
     * @return {@link String} which represents the default value for the return type of the method.
     */
    private String getMethodReturnValue(Method method) {
        Class<?> token = method.getReturnType();
        if (!token.isPrimitive()) {
            return "null";
        }
        if (token.equals(boolean.class)) {
            return "false";
        }
        if (token.equals(void.class)) {
            return "";
        }
        return "0";
    }

    /**
     * Passes the arguments that the {@link Method} has.
     *
     * @param method given by {@link Method} for which the arguments is generated.
     * @return {@link String} which represents the method arguments separated by commas.
     */
    private String getMethodArguments(Method method) {
        return Arrays.stream(method.getParameters())
                .map(parameter -> parameter.getType().getCanonicalName() + " " + parameter.getName())
                .collect(Collectors.joining(", "));
    }

    /**
     * Passes a string that declares a {@link Class} with an implemented interface.
     *
     * @param token {@link Class} type token to create declaration of class for.
     * @return {@link String} with declaration of with class name declaration and interface implementation.
     */
    private String classHeadDeclaration(Class<?> token) {
        return String.format("public class %s implements %s { %s", getSimpleNameAndSuffix(token),
                token.getCanonicalName(), NEW_LINE);
    }

    /**
     * Declares the packages of the implemented {@link Class}, if any.
     *
     * @param packageName The {@link String} name of the class package if any, empty string otherwise.
     * @return {@link String} with class package declared.
     */
    private String generatePackage(String packageName) {
        return packageName.equals("") ? "" : String.format("package %s ;%s", packageName, LINE_SKIP);
    }

    /**
     * Generates a jar-file with the implementation of the corresponding interface.
     * The method on the passed arguments implements the class, compiles it and creates a jar file.
     *
     * @param token   type token to create implementation for.
     * @param jarFile jar-file that will be created as a result of {@link Class} implementation.
     * @throws ImplerException if an error occurs during the implementation of the {@link Class},
     *                         the inability to compile the class, or during I\O.
     * @see #implementJar(Class, Path)
     * @see #compile(Class, Path, Path)
     * @see #makeJarFile(Class, Path, Path)
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        Path pathToTmpDir = Paths.get(".");
        try {
            implement(token, pathToTmpDir);
            compile(token, jarFile, pathToTmpDir);
            makeJarFile(token, jarFile, pathToTmpDir);
        } catch (ImplerException e) {
            System.err.println("ImplerException: " + e.getMessage());
        }
    }

    /**
     * Creates a jar-file for the implemented {@link Class}.
     *
     * @param token   The {@link Class} token of implemented class.
     * @param jarFile The {@link Path} of the jar-file to be created.
     * @param dirPath {@link Path} to temporary directory.
     * @throws ImplerException if an I/O error occurred.
     */
    private void makeJarFile(Class<?> token, Path jarFile, Path dirPath) throws ImplerException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream writer = new JarOutputStream(Files.newOutputStream(jarFile))) {
            String classFilename = Paths.get(token.getPackageName().replace('.', File.separatorChar))
                    .resolve(getSimpleNameAndSuffix(token)).toString();
            writer.putNextEntry(new ZipEntry(classFilename.replace(File.separatorChar, '/') + CLASS_EXTENSION));
            Files.copy(Path.of(dirPath.resolve(classFilename) + CLASS_EXTENSION), writer);
        } catch (IOException e) {
            throw new ImplerException("Can not to write to jar file" + e.getMessage());
        }
    }

    /**
     * Compiles the implemented class.
     *
     * @param token   The {@link Class} token of implemented class.
     * @param jarFile The {@link Path} of the jar-file to be created.
     * @param dirPath {@link Path} to temporary directory.
     * @throws ImplerException if the compiler is null or if a compilation error occurs.
     */
    private void compile(Class<?> token, Path jarFile, Path dirPath) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Could not find java compiler");
        }
        final int exitCode = compiler.run(null, null, null, "-classpath",
                jarFile.getFileName() + File.pathSeparator + getClassPath(token),
                dirPath.resolve(getPathToOutputFile(token, dirPath).toString()).toString());
        if (exitCode != 0) {
            throw new ImplerException("Compiler exit code is not 0");
        }

    }

    /**
     * @param token {@link Class} which classpath is required
     * @return {@link String} of token classpath
     */
    private static String getClassPath(Class<?> token) {
        try {
            return Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (final URISyntaxException e) {
            throw new AssertionError(e);
        }
    }
}
