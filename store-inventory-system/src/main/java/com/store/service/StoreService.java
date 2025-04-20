package com.store.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.store.model.Product;
import com.store.model.PerishableProduct;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service class that implements ProductManager interface to handle 
 * inventory management operations and data persistence.
 * This class follows the Singleton pattern to ensure only one instance exists
 * throughout the application lifecycle.
 */
public class StoreService implements ProductManager {
    // Singleton instance
    private static StoreService instance;

    private List<Product> inventory;
    private final ObjectMapper objectMapper;
    
    // File configuration constants
    private static final String USER_INVENTORY_DIR = ".store-inventory";
    private static final String USER_INVENTORY_FILENAME = "inventory.json";
    private File inventoryFile;

    /**
     * Get the singleton instance of StoreService
     * @return The singleton instance
     */
    public static synchronized StoreService getInstance() {
        if (instance == null) {
            instance = new StoreService();
        }
        return instance;
    }

    /**
     * Constructor for StoreService.
     * Note: For proper singleton implementation, use getInstance() instead.
     */
    public StoreService() {
        this.inventory = new ArrayList<>();
        
        // Create a simple ObjectMapper without type information
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        // Initialize inventory file in dedicated directory
        setupUserInventoryFile();
        
        // Load inventory data
        loadInventory();
    }

    /**
     * Sets up the user-specific inventory file in the .store-inventory directory
     * within the user's home directory. Creates the directory if it doesn't exist.
     */
    private void setupUserInventoryFile() {
        try {
            // Get user home directory path
            String userHome = System.getProperty("user.home");
            
            // Create path to .store-inventory directory
            Path userInventoryDirPath = Paths.get(userHome, USER_INVENTORY_DIR);
            
            // Create the directory if it doesn't exist
            if (!Files.exists(userInventoryDirPath)) {
                Files.createDirectories(userInventoryDirPath);
                System.out.println("Created user inventory directory: " + userInventoryDirPath);
            }
            
            // Set up the full path to inventory.json in the .store-inventory directory
            Path userInventoryPath = userInventoryDirPath.resolve(USER_INVENTORY_FILENAME);
            inventoryFile = userInventoryPath.toFile();
            
            // Check if user's inventory file exists
            if (Files.exists(userInventoryPath)) {
                System.out.println("✓ Log: Existing inventory file found. Using current data.");
                return; // Exit here - we'll use the existing file as is
            } else {
                // User's inventory file not found - create one with hard-coded products
                System.out.println("User inventory file not found. Creating new file with 10 products...");
                createDefaultInventoryWithTenProducts(userInventoryPath);
            }
        } catch (IOException e) {
            System.err.println("Error setting up user inventory file: " + e.getMessage());
            e.printStackTrace();
            
            try {
                // Emergency fallback - create minimal inventory with just 2 products
                System.err.println("Using emergency minimal inventory fallback");
                createMinimalInventory();
            } catch (IOException ex) {
                System.err.println("Fatal error: Cannot initialize inventory file: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
    
    /**
     * Creates a default inventory file with 10 products when template doesn't exist
     */
    private void createDefaultInventoryWithTenProducts(Path userInventoryPath) throws IOException {
        // This mirrors the createFullTemplate method in AppLauncher but is self-contained
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
            
        Files.writeString(userInventoryPath, content);
        System.out.println("Created inventory file with 10 default products at: " + userInventoryPath);
    }
    
    /**
     * Creates a minimal inventory with just 2 products as an emergency fallback
     */
    private void createMinimalInventory() throws IOException {
        // Create a correctly formatted minimal inventory with the required "type" field
        String defaultInventory = 
            "[\n" +
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
        
        Path userInventoryPath = inventoryFile.toPath();
        Files.writeString(userInventoryPath, defaultInventory);
        System.out.println("Created minimal inventory file with 2 products as emergency fallback.");
    }

    /**
     * Creates an empty inventory with sample products in the correct format
     */
    private void createEmptyInventory() {
        try {
            // Create the default inventory file with 10 products
            createDefaultInventoryWithTenProducts(inventoryFile.toPath());
            
            // Reload the inventory after creating the default file
            JsonNode rootNode = objectMapper.readTree(inventoryFile);
                
            if (rootNode.isArray()) {
                inventory.clear();
                
                for (JsonNode productNode : rootNode) {
                    Product product;
                    
                    // Determine which product type to instantiate
                    if (productNode.has("type") && "perishable".equals(productNode.get("type").asText())) {
                        // Create a perishable product
                        String name = productNode.get("name").asText();
                        double price = productNode.get("price").asDouble();
                        int quantity = productNode.get("quantity").asInt();
                        double discount = productNode.get("discount").asDouble();
                        String expirationDate = productNode.get("expirationDate").asText();
                        
                        product = new PerishableProduct(name, price, quantity, expirationDate, discount);
                    } else {
                        // Create a regular product
                        String name = productNode.get("name").asText();
                        double price = productNode.get("price").asDouble();
                        int quantity = productNode.get("quantity").asInt();
                        double discount = productNode.get("discount").asDouble();
                        
                        product = new Product(name, price, quantity, discount);
                    }
                    
                    inventory.add(product);
                }
            }
        } catch (IOException e) {
            System.err.println("Error creating empty inventory: " + e.getMessage());
            // Fallback to in-memory inventory
            inventory = new ArrayList<>();
            inventory.add(new Product("Sample Product", 9.99, 10, 0.0));
        }
    }

    /**
     * Creates a default inventory with standard products
     * Used to ensure we always have inventory data available
     */
    public void createDefaultInventory() {
        try {
            // Clear existing inventory
            inventory.clear();
            
            // Add perishable products
            inventory.add(new PerishableProduct("Apples", 1.99, 50, "2025-04-25", 0.05));
            inventory.add(new PerishableProduct("Bananas", 0.89, 40, "2025-04-20", 0.0));
            inventory.add(new PerishableProduct("Strawberries", 3.49, 20, "2025-04-19", 0.1));
            inventory.add(new PerishableProduct("Tomatoes", 2.29, 30, "2025-04-24", 0.0));
            inventory.add(new PerishableProduct("Lettuce", 1.79, 15, "2025-04-18", 0.05));
            inventory.add(new PerishableProduct("Cucumbers", 0.99, 25, "2025-04-23", 0.0));
            inventory.add(new PerishableProduct("Broccoli", 2.49, 18, "2025-04-21", 0.05));
            
            // Add non-perishable products
            inventory.add(new Product("Potatoes", 0.79, 60, 0.0));
            inventory.add(new Product("Onions", 0.89, 45, 0.05));
            inventory.add(new Product("Rice", 3.99, 30, 0.0));
            
            // Save the newly created inventory
            saveInventory();
            System.out.println("Default inventory created with " + inventory.size() + " products");
        } catch (Exception e) {
            System.err.println("Error creating default inventory: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addProduct(Product product) {
        inventory.add(product);
        saveInventory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeProduct(int index) {
        if (index >= 0 && index < inventory.size()) {
            inventory.remove(index);
            saveInventory();
            return true;
        } else {
            System.err.println("Invalid product index: " + index);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Product> getInventory() {
        return new ArrayList<>(inventory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Product> findProductByName(String name) {
        return inventory.stream()
                .filter(product -> product.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTotalQuantity() {
        return inventory.stream()
                .mapToInt(Product::getQuantity)
                .sum();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigDecimal getTotalGrossPrice() {
        return inventory.stream()
                .map(product -> product.getPrice().multiply(BigDecimal.valueOf(product.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigDecimal getTotalPriceWithPerishableDiscount() {
        return inventory.stream()
                .map(Product::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigDecimal getTotalNetPriceWithDiscount() {
        return getTotalPriceWithPerishableDiscount()
                .multiply(BigDecimal.valueOf(0.85))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Reloads inventory data from disk after template refreshes
     */
    public void loadInventory() {
        if (inventoryFile != null && inventoryFile.exists()) {
            try {
                // First try to fix any format issues in the inventory file
                fixInventoryFileFormat();
                
                // Parse the JSON directly to handle proper class instantiation
                JsonNode rootNode = objectMapper.readTree(inventoryFile);
                
                if (rootNode.isArray()) {
                    inventory.clear();
                    
                    for (JsonNode productNode : rootNode) {
                        Product product;
                        
                        try {
                            // Safely get required values with null checks
                            String type = productNode.has("type") ? productNode.get("type").asText("non-perishable") : "non-perishable";
                            String name = productNode.has("name") ? productNode.get("name").asText("Unnamed Product") : "Unnamed Product";
                            double price = productNode.has("price") ? productNode.get("price").asDouble(0.0) : 0.0;
                            int quantity = productNode.has("quantity") ? productNode.get("quantity").asInt(0) : 0;
                            double discount = productNode.has("discount") ? productNode.get("discount").asDouble(0.0) : 0.0;
                            
                            // Determine which product type to instantiate
                            if ("perishable".equals(type) && productNode.has("expirationDate")) {
                                String expirationDate = productNode.get("expirationDate").asText("2025-12-31");
                                product = new PerishableProduct(name, price, quantity, expirationDate, discount);
                            } else {
                                product = new Product(name, price, quantity, discount);
                            }
                            
                            inventory.add(product);
                        } catch (Exception e) {
                            System.err.println("Error processing product: " + e.getMessage());
                            // Create a default product if there's an error with one item
                            product = new Product("Error Product", 0.0, 0, 0.0);
                            inventory.add(product);
                        }
                    }
                    
                    System.out.println("Inventory loaded successfully with " + inventory.size() + 
                        " products from " + inventoryFile.getPath());
                } else {
                    System.err.println("Invalid inventory format: root element is not an array");
                    createEmptyInventory();
                }
            } catch (IOException e) {
                System.err.println("Error loading inventory: " + e.getMessage());
                // Attempt to recover from backup instead of creating one
                tryRestoreFromBackup();
            }
        } else {
            System.out.println("No valid inventory file. Starting with empty inventory.");
            inventory = new ArrayList<>();
            saveInventory();
        }
    }
    
    /**
     * Fixes the inventory file format permanently if needed
     * @return true if the file is now in a valid format for loading
     */
    private boolean fixInventoryFileFormat() {
        try {
            // Read the file content
            String content = Files.readString(inventoryFile.toPath());
            
            // Parse as generic JSON first
            JsonNode rootNode = objectMapper.readTree(content);
            
            if (!rootNode.isArray()) {
                System.err.println("Invalid inventory format: root element is not an array");
                return false;
            }
            
            // Check if this is a file missing type properties
            boolean needsTypeProperty = false;
            for (JsonNode item : rootNode) {
                if (!item.has("type")) {
                    needsTypeProperty = true;
                    break;
                }
            }
            
            if (needsTypeProperty) {
                System.out.println("Detected inventory JSON missing 'type' property. Fixing format...");
                
                // Create a backup first
                Path backupFile = inventoryFile.toPath().resolveSibling(inventoryFile.getName() + ".missing-type.bak");
                Files.copy(inventoryFile.toPath(), backupFile, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Created backup at " + backupFile);
                
                // Process the array manually and add type information
                ObjectNode[] updatedItems = new ObjectNode[rootNode.size()];
                for (int i = 0; i < rootNode.size(); i++) {
                    JsonNode item = rootNode.get(i);
                    ObjectNode updatedItem = objectMapper.createObjectNode();
                    
                    // Add type property first (default to non-perishable)
                    updatedItem.put("type", "non-perishable");
                    
                    // Copy all other properties
                    item.fields().forEachRemaining(field -> 
                        updatedItem.set(field.getKey(), field.getValue()));
                    
                    updatedItems[i] = updatedItem;
                }
                
                // Write back the updated JSON
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(inventoryFile, updatedItems);
                System.out.println("Fixed inventory file format by adding 'type' property to all products");
            }
            
            return true;
        } catch (IOException e) {
            System.err.println("Error fixing inventory file format: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Attempts to restore inventory from a backup file if it exists
     */
    private void tryRestoreFromBackup() {
        // Define backup file path
        Path backupPath = inventoryFile.toPath().resolveSibling(inventoryFile.getName() + ".bak");
        
        if (Files.exists(backupPath)) {
            System.out.println("Attempting to restore inventory from backup file: " + backupPath);
            
            try {
                // Read the backup file manually
                JsonNode rootNode = objectMapper.readTree(backupPath.toFile());
                
                if (rootNode.isArray()) {
                    inventory.clear();
                    
                    for (JsonNode productNode : rootNode) {
                        try {
                            Product product;
                            
                            // Safely get required values with null checks
                            String type = productNode.has("type") ? productNode.get("type").asText("non-perishable") : "non-perishable";
                            String name = productNode.has("name") ? productNode.get("name").asText("Unnamed Product") : "Unnamed Product";
                            double price = productNode.has("price") ? productNode.get("price").asDouble(0.0) : 0.0;
                            int quantity = productNode.has("quantity") ? productNode.get("quantity").asInt(0) : 0;
                            double discount = productNode.has("discount") ? productNode.get("discount").asDouble(0.0) : 0.0;
                            
                            if ("perishable".equals(type) && productNode.has("expirationDate")) {
                                String expirationDate = productNode.get("expirationDate").asText("2025-12-31");
                                product = new PerishableProduct(name, price, quantity, expirationDate, discount);
                            } else {
                                product = new Product(name, price, quantity, discount);
                            }
                            
                            inventory.add(product);
                        } catch (Exception e) {
                            System.err.println("Error processing backup product: " + e.getMessage());
                            // Create a default product if there's an error with one item
                            Product product = new Product("Backup Error Product", 0.0, 0, 0.0);
                            inventory.add(product);
                        }
                    }
                    
                    System.out.println("Successfully restored " + inventory.size() + " products from backup file");
                    
                    // Save the restored data back to the main file
                    saveInventory();
                } else {
                    System.err.println("Invalid backup format, creating empty inventory");
                    createEmptyInventory();
                }
            } catch (IOException backupError) {
                System.err.println("Failed to restore from backup: " + backupError.getMessage());
                createEmptyInventory();
            }
        } else {
            System.out.println("No backup file found. Creating new empty inventory");
            createEmptyInventory();
        }
    }

    /**
     * Saves the current inventory to the user-specific inventory file.
     */
    private void saveInventory() {
        if (inventoryFile != null) {
            try {
                // First check if inventory is empty but file exists with content
                if (inventory.isEmpty() && inventoryFile.exists() && inventoryFile.length() > 10) {
                    System.out.println("WARNING: Attempting to save empty inventory over existing data!");
                    System.out.println("Creating backup before proceeding...");
                    
                    // Create emergency backup
                    Path backupPath = inventoryFile.toPath().resolveSibling(inventoryFile.getName() + ".emergency.bak");
                    Files.copy(inventoryFile.toPath(), backupPath, StandardCopyOption.REPLACE_EXISTING);
                }
                
                // Ensure parent directory exists
                Path parent = inventoryFile.toPath().getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                
                // Only save if we actually have data
                if (!inventory.isEmpty()) {
                    // Create JSON array node manually
                    ObjectNode[] productNodes = new ObjectNode[inventory.size()];
                    for (int i = 0; i < inventory.size(); i++) {
                        Product product = inventory.get(i);
                        ObjectNode node = objectMapper.createObjectNode();
                        
                        // Common properties
                        if (product instanceof PerishableProduct) {
                            node.put("type", "perishable");
                            node.put("expirationDate", ((PerishableProduct) product).getExpirationDate().toString());
                        } else {
                            node.put("type", "non-perishable");
                        }
                        
                        node.put("name", product.getName());
                        node.put("price", product.getPrice().doubleValue());
                        node.put("quantity", product.getQuantity());
                        node.put("discount", product.getDiscount().doubleValue());
                        
                        productNodes[i] = node;
                    }
                    
                    // Write directly to file
                    objectMapper.writerWithDefaultPrettyPrinter().writeValue(inventoryFile, productNodes);
                    System.out.println("Inventory saved with " + inventory.size() + " products to " + inventoryFile.getPath());
                } else {
                    System.out.println("Skipping save since inventory is empty!");
                }
            } catch (IOException e) {
                System.err.println("Error saving inventory: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("Cannot save inventory: No valid inventory file path.");
        }
    }

    /**
     * Ensures inventory is saved and any file resources are released
     * before application exit
     */
    public void saveAndCloseInventory() {
        try {
            // Check if inventory is empty before saving
            if (inventory.isEmpty()) {
                System.out.println("WARNING: Inventory is empty! Checking if this is correct...");
                
                // Double-check by trying to load from file directly
                if (inventoryFile != null && inventoryFile.exists() && inventoryFile.length() > 10) {
                    try {
                        // Try to load the file to see if it has data we should keep
                        JsonNode rootNode = objectMapper.readTree(inventoryFile);
                        if (rootNode.isArray() && rootNode.size() > 0) {
                            System.out.println("Found " + rootNode.size() + 
                                " products in file but memory is empty! Preserving file data.");
                            return; // Don't save and overwrite the file data
                        }
                    } catch (Exception e) {
                        System.err.println("Error checking file content: " + e.getMessage());
                    }
                }
            }
            
            // Final save to ensure latest changes are persisted
            saveInventory();
            
            // Log successful cleanup
            System.out.println("Inventory data saved successfully.");
        } catch (Exception e) {
            System.err.println("Error during final inventory save: " + e.getMessage());
        }
    }
}