package com.store;

import com.store.gui.InventoryApp;
import com.store.service.StoreService;
import com.store.util.JavaFxUtil;
import com.store.util.InventoryFileManager;
import javafx.application.Application;
import javafx.application.Platform;

/**
 * Application launcher that determines whether to run in GUI or console mode.
 */
public class AppLauncher {
    
    // Static flag to track if console mode is requested after GUI exits
    private static volatile boolean switchToConsoleMode = false;
    
    // Static method for InventoryApp to request console mode
    public static void requestConsoleMode() {
        switchToConsoleMode = true;
    }
    
    public static void main(String[] args) {
        logSystemInfo();
        
        // Special flag handling for "reset" option
        if (shouldResetInventory(args)) {
            System.out.println("RESET requested - replacing inventory with complete template...");
            InventoryFileManager.refreshUserInventoryFromTemplate(true); // Skip confirmation
            System.exit(0); // Exit after reset
        }
        
        // Directly fix user inventory file first - this is critical
        InventoryFileManager.fixUserInventoryFile();
        
        // Register shutdown hook to ensure clean exit in all scenarios
        registerShutdownHook();
        
        // Configure JavaFX module path if needed
        JavaFxUtil.configureJavaFxEnvironment();
        
        // Enhanced mode selection logic - automatic detection
        boolean useConsoleMode = determineAppropriateMode(args);
        
        if (useConsoleMode) {
            System.out.println("Starting in console mode...");
            launchConsoleMode(args);
        } else {
            System.out.println("Starting in GUI mode...");
            
            // Launch GUI mode
            launchGuiMode(args);
            
            // After GUI exits, check if we should switch to console mode
            if (switchToConsoleMode) {
                System.out.println("Switching to console mode after GUI exit...");
                launchConsoleMode(new String[]{"console"});
            }
        }
    }
    
    /**
     * Intelligently determine the most appropriate application mode
     * based on environment and explicit user preferences.
     */
    private static boolean determineAppropriateMode(String[] args) {
        // 1. Explicit user preference takes highest priority
        if (args != null && args.length > 0) {
            String mode = args[0].toLowerCase();
            if (mode.equals("console") || mode.equals("--console") || mode.equals("-c")) {
                System.out.println("Console mode explicitly requested via command line argument");
                return true;
            }
            if (mode.equals("gui") || mode.equals("--gui") || mode.equals("-g")) {
                System.out.println("GUI mode explicitly requested via command line argument");
                return false;
            }
        }
        
        // 2. Check environment capabilities
        boolean headless = isHeadlessEnvironment();
        boolean javaFxAvailable = JavaFxUtil.isJavaFxAvailable();
        
        if (headless) {
            System.out.println("Detected headless environment, using console mode");
            return true;
        }
        
        if (!javaFxAvailable) {
            System.out.println("JavaFX not available in the current environment, falling back to console mode");
            return true;
        }
        
        // 3. Default to GUI on modern systems with graphical capabilities
        return false;
    }
    
    /**
     * Check if we're running in a headless environment
     */
    private static boolean isHeadlessEnvironment() {
        // Check Java headless property
        if (Boolean.getBoolean("java.awt.headless")) {
            return true;
        }
        
        // Check for display environment variable (Unix/Linux)
        String display = System.getenv("DISPLAY");
        if (System.getProperty("os.name").toLowerCase().contains("linux") && 
            (display == null || display.trim().isEmpty())) {
            return true;
        }
        
        return false;
    }
    
    private static void logSystemInfo() {
        System.out.println("Java version: " + System.getProperty("java.version"));
        JavaFxUtil.detectJavaFxRuntime();
    }
    
    // Legacy method kept for compatibility
    private static boolean shouldRunInConsoleMode(String[] args) {
        // Explicitly check for console mode argument
        if (args != null && args.length > 0) {
            String mode = args[0].toLowerCase();
            return mode.equals("console") || mode.equals("--console") || mode.equals("-c");
        }
        return false; // Default to GUI mode when no args provided
    }
    
    /**
     * Check if user requested a complete inventory reset
     */
    private static boolean shouldResetInventory(String[] args) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--reset") || 
                arg.equalsIgnoreCase("-reset") || 
                arg.equalsIgnoreCase("reset")) {
                return true;
            }
        }
        return false;
    }
    
    private static boolean shouldForceRefresh(String[] args) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--refresh") || arg.equalsIgnoreCase("-r")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Register a JVM shutdown hook to ensure resources are released
     * even if the application is terminated unexpectedly
     */
    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Application shutting down, cleaning up resources...");
            cleanupResources();
        }));
    }
    
    /**
     * Clean up any application-wide resources that need to be released
     */
    private static void cleanupResources() {
        try {
            // Initialize StoreService if not already done to ensure proper template loading
            StoreService service = StoreService.getInstance();
            if (service != null) {
                // Re-enable saving in AppLauncher
                service.saveAndCloseInventory();
                System.out.println("Inventory saved and file resources released.");
            }
        } catch (Exception e) {
            System.err.println("Error during final inventory cleanup: " + e.getMessage());
        }
    }
    
    private static void launchConsoleMode(String[] args) {
        try {
            // Check if a refresh is requested
            if (shouldForceRefresh(args)) {
                InventoryFileManager.refreshUserInventoryFromTemplate(false);  // Keep confirmation for manual refresh
            }
            
            // Make sure StoreService is initialized with correct template path before Main runs
            StoreService.getInstance();
            System.out.println("StoreService initialized with template from resources directory");
            
            // Console mode cleanup is handled by Main.java
            Main.main(args);
        } catch (Exception e) {
            System.err.println("Error in console mode: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void launchGuiMode(String[] args) {
        try {
            // IMPORTANT: Initialize StoreService FIRST before checking for refresh
            StoreService service = StoreService.getInstance();
            System.out.println("StoreService initialized with template from resources directory");
            
            // Only after loading, check if a refresh is requested
            if (shouldForceRefresh(args)) {
                // Create a backup of current inventory before refreshing
                InventoryFileManager.createInventoryBackup();
                InventoryFileManager.refreshUserInventoryFromTemplate(false);  // Keep confirmation for manual refresh
                // Re-initialize StoreService to reload the refreshed data
                service.loadInventory(); // Reload inventory data after refresh
            }
            
            System.out.println("Starting GUI application...");
            
            // Set up JavaFX exit handler for clean shutdown
            Platform.setImplicitExit(true);
            
            // Verify JavaFX configuration
            if (!JavaFxUtil.verifyJavaFxConfiguration(args)) {
                System.err.println("Failed to verify JavaFX configuration.");
                System.err.println("Falling back to console mode...");
                launchConsoleMode(args);
                return;
            }
            
            // Launch the GUI application
            System.out.println("Launching JavaFX application...");
            
            // Critical fix for preloader error - set empty preloader
            System.setProperty("javafx.preloader", "");
            
            // Basic JavaFX configuration without module system interference
            System.setProperty("javafx.verbose", "false");
            System.setProperty("javafx.debug", "false");
            
            // Launch the application - keep it simple to avoid module issues
            Application.launch(InventoryApp.class, args);
        } catch (Exception e) {
            System.err.println("Failed to launch JavaFX application: " + e.getMessage());
            System.err.println("Falling back to console mode...");
            e.printStackTrace();
            launchConsoleMode(args);
        }
    }
}
