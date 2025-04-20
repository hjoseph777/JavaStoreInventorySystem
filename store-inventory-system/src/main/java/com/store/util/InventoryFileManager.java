package com.store.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;

/**
 * Utility class to handle all inventory file management operations.
 * This class centralizes operations like file creation, backup, and validation
 * that were previously scattered throughout the application.
 */
public class InventoryFileManager {
    
    private static final String USER_INVENTORY_DIR = ".store-inventory";
    private static final String USER_INVENTORY_FILENAME = "inventory.json";
    
    /**
     * Fixes issues with the user inventory file format.
     * This directly addresses the "Could not resolve type id 'product'" error
     * WITHOUT replacing user data.
     */
    public static void fixUserInventoryFile() {
        try {
            String userHome = System.getProperty("user.home");
            Path userInventoryDir = Paths.get(userHome, USER_INVENTORY_DIR);
            Path userInventoryFile = userInventoryDir.resolve(USER_INVENTORY_FILENAME);
            
            if (Files.exists(userInventoryFile)) {
                System.out.println("Using inventory file at: " + userInventoryFile);
                System.out.println("Existing inventory file found. Using current data.");
                String content = new String(Files.readAllBytes(userInventoryFile), StandardCharsets.UTF_8);
                
                // Check for missing or invalid 'type' property
                boolean hasMissingType = content.contains("\"name\":") && 
                                        !content.contains("\"type\":");
                                        
                boolean hasInvalidType = content.contains("\"type\":\"product\"") || 
                                        content.contains("\"type\": \"product\"");
                
                // Create a backup before making any changes
                if (hasMissingType || hasInvalidType) {
                    String backupSuffix = hasMissingType ? ".missing-type.bak" : ".invalid-type.bak";
                    Path backupFile = userInventoryDir.resolve(USER_INVENTORY_FILENAME + backupSuffix);
                    Files.copy(userInventoryFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Created backup at " + backupFile);
                }
                
                String fixedContent = content;
                
                // Fix missing 'type' property
                if (hasMissingType) {
                    System.out.println("Detected inventory JSON missing 'type' property. Fixing format...");
                    // Add 'type' property before 'name' for each product
                    fixedContent = fixedContent.replaceAll("\\{\\s*\"name\":", "{\"type\":\"non-perishable\",\"name\":");
                    System.out.println("Fixed inventory file format by adding 'type' property to all products");
                }
                
                // Fix invalid 'product' type
                if (hasInvalidType) {
                    System.out.println("Detected invalid 'product' type in inventory. Fixing format...");
                    // Replace "type":"product" with "type":"non-perishable"
                    fixedContent = fixedContent.replaceAll("\"type\"\\s*:\\s*\"product\"", "\"type\":\"non-perishable\"");
                    System.out.println("Fixed inventory file by replacing 'product' type with 'non-perishable'");
                }
                
                // Only write the file if changes were made
                if (!content.equals(fixedContent)) {
                    Files.write(userInventoryFile, fixedContent.getBytes(StandardCharsets.UTF_8));
                    System.out.println("Updated inventory file with corrected format");
                }
            }
        } catch (IOException e) {
            System.err.println("Error during emergency inventory fix: " + e.getMessage());
            e.printStackTrace();
            // If all else fails, create a minimal valid inventory
            createMinimalValidInventory();
        }
    }
    
    /**
     * Ensures the template file exists with all required products.
     */
    public static void ensureFullTemplateExists() {
        try {
            String projectDir = System.getProperty("user.dir");
            Path templateDir = Paths.get(projectDir, "src", "main", "resources");
            Path templatePath = templateDir.resolve("inventory.json");
            
            System.out.println("Checking for full template file at: " + templatePath);
            
            // Create directories if they don't exist
            if (!Files.exists(templateDir)) {
                Files.createDirectories(templateDir);
                System.out.println("Created template directory: " + templateDir);
            }
            
            // Check if file exists and has all products
            if (Files.exists(templatePath)) {
                String content = new String(Files.readAllBytes(templatePath), StandardCharsets.UTF_8);
                int productCount = countProducts(content);
                
                // If template has less than 10 products, recreate it with full data
                if (productCount < 10) {
                    System.out.println("Template has only " + productCount + " products. Creating full template...");
                    createFullTemplate(templatePath);
                } else {
                    System.out.println("Template file exists with " + productCount + " products.");
                }
            } else {
                System.out.println("Template file not found, creating complete template");
                createFullTemplate(templatePath);
            }
        } catch (IOException e) {
            System.err.println("Error ensuring full template: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Creates the full template inventory file with all products
     */
    public static void createFullTemplate(Path templatePath) throws IOException {
        // Full inventory with all products
        String content = "[\n" +
            "  {\n" +
            "    \"type\": \"perishable\",\n" +
            "    \"name\": \"Apples\",\n" +
            "    \"price\": 1.99,\n" +
            "    \"quantity\": 50,\n" +
            "    \"discount\": 0.05,\n" +
            "    \"expirationDate\": \"2025-04-25\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"type\": \"perishable\",\n" +
            "    \"name\": \"Bananas\",\n" +
            "    \"price\": 0.89,\n" +
            "    \"quantity\": 40,\n" +
            "    \"discount\": 0.0,\n" +
            "    \"expirationDate\": \"2025-04-20\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"type\": \"perishable\",\n" +
            "    \"name\": \"Strawberries\",\n" +
            "    \"price\": 3.49,\n" +
            "    \"quantity\": 20,\n" +
            "    \"discount\": 0.1,\n" +
            "    \"expirationDate\": \"2025-04-19\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"type\": \"perishable\",\n" +
            "    \"name\": \"Tomatoes\",\n" +
            "    \"price\": 2.29,\n" +
            "    \"quantity\": 30,\n" +
            "    \"discount\": 0.0,\n" +
            "    \"expirationDate\": \"2025-04-24\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"type\": \"perishable\",\n" +
            "    \"name\": \"Lettuce\",\n" +
            "    \"price\": 1.79,\n" +
            "    \"quantity\": 15,\n" +
            "    \"discount\": 0.05,\n" +
            "    \"expirationDate\": \"2025-04-18\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"type\": \"perishable\",\n" +
            "    \"name\": \"Cucumbers\",\n" +
            "    \"price\": 0.99,\n" +
            "    \"quantity\": 25,\n" +
            "    \"discount\": 0.0,\n" +
            "    \"expirationDate\": \"2025-04-23\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"type\": \"perishable\",\n" +
            "    \"name\": \"Broccoli\",\n" +
            "    \"price\": 2.49,\n" +
            "    \"quantity\": 18,\n" +
            "    \"discount\": 0.05,\n" +
            "    \"expirationDate\": \"2025-04-21\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"type\": \"perishable\",\n" +
            "    \"name\": \"Grapes\",\n" +
            "    \"price\": 4.99,\n" +
            "    \"quantity\": 12,\n" +
            "    \"discount\": 0.1,\n" +
            "    \"expirationDate\": \"2025-04-19\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"type\": \"non-perishable\",\n" +
            "    \"name\": \"Potatoes\",\n" +
            "    \"price\": 0.79,\n" +
            "    \"quantity\": 60,\n" +
            "    \"discount\": 0.0\n" +
            "  },\n" +
            "  {\n" +
            "    \"type\": \"non-perishable\",\n" +
            "    \"name\": \"Onions\",\n" +
            "    \"price\": 0.89,\n" +
            "    \"quantity\": 45,\n" +
            "    \"discount\": 0.05\n" +
            "  }\n" +
            "]";
        Files.write(templatePath, content.getBytes(StandardCharsets.UTF_8));
        System.out.println("Created COMPLETE template inventory file with all 10 products at: " + templatePath);
    }
    
    /**
     * Creates a minimal valid inventory as a last resort
     */
    public static void createMinimalValidInventory() {
        try {
            String userHome = System.getProperty("user.home");
            Path userInventoryDir = Paths.get(userHome, USER_INVENTORY_DIR);
            Path userInventoryFile = userInventoryDir.resolve(USER_INVENTORY_FILENAME);
            
            // Create directory if needed
            if (!Files.exists(userInventoryDir)) {
                Files.createDirectories(userInventoryDir);
            }
            
            // Create minimal valid inventory JSON
            String json = "[\n" +
                "  {\n" +
                "    \"type\": \"non-perishable\",\n" +
                "    \"name\": \"Example Product\",\n" +
                "    \"price\": 9.99,\n" +
                "    \"quantity\": 10,\n" +
                "    \"discount\": 0.0\n" +
                "  }\n" +
                "]";
            
            Files.write(userInventoryFile, json.getBytes(StandardCharsets.UTF_8));
            System.out.println("Created minimal valid inventory file as emergency recovery");
        } catch (IOException e) {
            System.err.println("Failed to create minimal inventory: " + e.getMessage());
        }
    }
    
    /**
     * Forces a refresh by replacing the user inventory with the template inventory.
     * This version will always use the complete template from resources.
     * 
     * @param skipConfirmation if true, will overwrite without user confirmation
     */
    public static void refreshUserInventoryFromTemplate(boolean skipConfirmation) {
        System.out.println("Refreshing inventory from template file...");
        try {
            // First, ensure the template file exists and has ALL products
            ensureFullTemplateExists();
            
            // Now copy the complete template to the user directory
            String projectDir = System.getProperty("user.dir");
            Path templatePath = Paths.get(projectDir, "src", "main", "resources", "inventory.json");
            File templateFile = templatePath.toFile();
            String userHome = System.getProperty("user.home");
            Path userInventoryDir = Paths.get(userHome, USER_INVENTORY_DIR);
            Path userInventoryFile = userInventoryDir.resolve(USER_INVENTORY_FILENAME);
            
            // Ensure directory exists
            if (!Files.exists(userInventoryDir)) {
                Files.createDirectories(userInventoryDir);
                System.out.println("Created user inventory directory: " + userInventoryDir);
            }
            
            // Check if file exists and ask for confirmation before overwriting
            if (Files.exists(userInventoryFile) && !skipConfirmation) {
                System.out.println("WARNING: An existing inventory file was found at: " + userInventoryFile);
                System.out.print("Do you want to overwrite it with the COMPLETE template? (y/n): ");
                
                try (Scanner scanner = new Scanner(System.in)) {
                    String response = scanner.nextLine().trim().toLowerCase();
                    if (!response.startsWith("y")) {
                        System.out.println("Refresh cancelled. Using existing inventory file.");
                        return;
                    }
                    System.out.println("Proceeding with refresh...");
                }
            }
            
            // Create backup of existing file if it exists
            if (Files.exists(userInventoryFile)) {
                Path backupFile = userInventoryDir.resolve("inventory.json.before-refresh-" + 
                                                         System.currentTimeMillis() + ".bak");
                Files.copy(userInventoryFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Created backup of current inventory at: " + backupFile);
            }
            
            if (templateFile.exists()) {
                System.out.println("Template file exists, size: " + Files.size(templatePath) + " bytes");
                
                // Perform the copy with explicit replace option
                Files.copy(templatePath, userInventoryFile, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Successfully refreshed user inventory with COMPLETE template.");
                
                // Show the number of products in the template
                String content = new String(Files.readAllBytes(templatePath), StandardCharsets.UTF_8);
                int productCount = countProducts(content);
                System.out.println("Template contains " + productCount + " products.");
            } else {
                System.err.println("ERROR: Template file not found at: " + templateFile.getAbsolutePath());
                createFullTemplate(templatePath);
                if (Files.exists(templatePath)) {
                    Files.copy(templatePath, userInventoryFile, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Created and copied full template inventory to user directory.");
                } else {
                    System.err.println("Failed to create template. Using minimal inventory as fallback.");
                    createMinimalValidInventory();
                }
            }
        } catch (IOException e) {
            System.err.println("Error refreshing inventory: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Overloaded method for backward compatibility
     */
    public static void refreshUserInventoryFromTemplate() {
        refreshUserInventoryFromTemplate(false);
    }
    
    /**
     * Count the number of products in a JSON string
     */
    public static int countProducts(String json) {
        int count = 0;
        int index = 0;
        while ((index = json.indexOf("\"name\":", index + 1)) != -1) {
            count++;
        }
        return count;
    }
    
    /**
     * Creates a backup of the user's current inventory file
     */
    public static void createInventoryBackup() {
        try {
            String userHome = System.getProperty("user.home");
            Path userInventoryDir = Paths.get(userHome, USER_INVENTORY_DIR);
            Path userInventoryFile = userInventoryDir.resolve(USER_INVENTORY_FILENAME);
            
            if (Files.exists(userInventoryFile)) {
                Path backupFile = userInventoryDir.resolve("inventory.json.bak");
                Files.copy(userInventoryFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Created backup of current inventory at " + backupFile);
            }
        } catch (IOException e) {
            System.err.println("Failed to create inventory backup: " + e.getMessage());
        }
    }
    
    /**
     * Gets the path to the user's inventory file
     */
    public static Path getUserInventoryFilePath() {
        String userHome = System.getProperty("user.home");
        Path userInventoryDir = Paths.get(userHome, USER_INVENTORY_DIR);
        return userInventoryDir.resolve(USER_INVENTORY_FILENAME);
    }
    
    /**
     * Gets the path to the template inventory file
     */
    public static Path getTemplateInventoryFilePath() {
        String projectDir = System.getProperty("user.dir");
        Path templateDir = Paths.get(projectDir, "src", "main", "resources");
        return templateDir.resolve("inventory.json");
    }
}