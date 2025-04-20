package com.store.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for JavaFX environment configuration and detection
 */
public class JavaFxUtil {

    private static final String JAVAFX_SDK_PATH = "./javafx-sdk";
    private static boolean javaFxAvailable = false;

    /**
     * Configures JavaFX environment variables and module path if needed
     */
    public static void configureJavaFxEnvironment() {
        try {
            // Try setting the path to the embedded JavaFX SDK
            Path javafxPath = Paths.get(JAVAFX_SDK_PATH);
            if (Files.exists(javafxPath)) {
                String binPath = javafxPath.resolve("bin").toAbsolutePath().toString();
                String libPath = javafxPath.resolve("lib").toAbsolutePath().toString();
                
                // Set JavaFX system properties if they haven't been set already
                if (System.getProperty("javafx.home") == null) {
                    System.setProperty("javafx.home", javafxPath.toAbsolutePath().toString());
                }
                
                // Only set module path if we're on Java 9+
                if (getMajorJavaVersion() >= 9) {
                    String modulePath = System.getProperty("jdk.module.path");
                    if (modulePath == null || modulePath.isEmpty()) {
                        System.setProperty("jdk.module.path", libPath);
                    } else if (!modulePath.contains(libPath)) {
                        System.setProperty("jdk.module.path", modulePath + File.pathSeparator + libPath);
                    }
                }
                
                // Add the directory to the PATH on Windows
                if (isWindows()) {
                    String path = System.getenv("PATH");
                    if (path != null && !path.contains(binPath)) {
                        System.setProperty("java.library.path", 
                            System.getProperty("java.library.path") + File.pathSeparator + binPath);
                    }
                }
                
                System.out.println("JavaFX SDK configured at: " + javafxPath.toAbsolutePath());
            } else {
                System.out.println("Embedded JavaFX SDK not found at: " + javafxPath.toAbsolutePath() + 
                                   ". Using system JavaFX if available.");
            }
        } catch (Exception e) {
            System.err.println("Error configuring JavaFX environment: " + e.getMessage());
        }
    }
    
    /**
     * Detects the presence of JavaFX runtime in the environment
     * @return true if JavaFX classes are available
     */
    public static boolean detectJavaFxRuntime() {
        try {
            // Try loading a core JavaFX class
            Class.forName("javafx.application.Application");
            System.out.println("JavaFX runtime detected on classpath");
            javaFxAvailable = true;
            return true;
        } catch (ClassNotFoundException e) {
            try {
                // Check for JavaFX JARs in the embedded SDK
                Path javafxLib = Paths.get(JAVAFX_SDK_PATH, "lib");
                if (Files.exists(javafxLib)) {
                    long javaFxJars = Files.list(javafxLib)
                            .filter(p -> p.toString().endsWith(".jar"))
                            .filter(p -> p.getFileName().toString().startsWith("javafx"))
                            .count();
                    if (javaFxJars > 0) {
                        System.out.println("JavaFX SDK JARs found: " + javaFxJars);
                        javaFxAvailable = true;
                        return true;
                    }
                }
            } catch (IOException ex) {
                // Ignore, will return false below
            }
            
            System.out.println("JavaFX runtime not detected in classpath or SDK folder");
            javaFxAvailable = false;
            return false;
        }
    }
    
    /**
     * Checks if JavaFX is available in the current environment
     * @return true if JavaFX is available
     */
    public static boolean isJavaFxAvailable() {
        // If we haven't checked yet, check now
        if (!javaFxAvailable) {
            detectJavaFxRuntime();
        }
        return javaFxAvailable;
    }
    
    /**
     * Verifies JavaFX configuration by checking critical environment settings
     * @return true if configuration looks correct
     */
    public static boolean verifyJavaFxConfiguration(String[] args) {
        // Skip full verification if it's running in test mode
        for (String arg : args) {
            if (arg.toLowerCase().contains("test")) {
                return true;
            }
        }
        
        // Check if JavaFX runtime is detected
        if (!isJavaFxAvailable()) {
            System.err.println("JavaFX runtime not detected. Please check your installation.");
            return false;
        }
        
        // Java version check
        int javaVersion = getMajorJavaVersion();
        System.out.println("Detected Java version: " + javaVersion);
        
        // Add specific configuration for different Java versions
        if (javaVersion >= 11) {
            // JavaFX is not included in standard JDK since Java 11
            if (!isJavaFxModuleAvailable()) {
                System.err.println("JavaFX modules not available. Check your module path configuration.");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Checks if we're running on Windows
     */
    private static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }
    
    /**
     * Get the major Java version (8, 11, 17, etc.)
     */
    public static int getMajorJavaVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            // For Java 8 and earlier: 1.8.x
            return Integer.parseInt(version.substring(2, 3));
        } else {
            // For Java 9 and later: 9.x, 11.x, etc.
            int dot = version.indexOf(".");
            if (dot != -1) {
                return Integer.parseInt(version.substring(0, dot));
            } else {
                return Integer.parseInt(version);
            }
        }
    }
    
    /**
     * Check if JavaFX module is available (for Java 9+)
     */
    private static boolean isJavaFxModuleAvailable() {
        try {
            // Try to access a class from the javafx.graphics module
            Class.forName("javafx.scene.Scene");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}