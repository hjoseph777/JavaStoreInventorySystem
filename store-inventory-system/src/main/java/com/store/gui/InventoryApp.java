package com.store.gui;

import com.store.model.PerishableProduct;
import com.store.model.Product;
import com.store.service.StoreService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import javafx.beans.property.SimpleObjectProperty;

public class InventoryApp extends Application {

    // Use the singleton instance instead of creating a new one
    private final StoreService storeService = StoreService.getInstance();
    private TableView<Product> productTable;
    private final ObservableList<Product> productData = FXCollections.observableArrayList();

    // Form fields
    private TextField nameField;
    private TextField priceField;
    private TextField quantityField;
    private TextField discountField;
    private CheckBox perishableCheckBox;
    private DatePicker expirationDatePicker;
    
    // Summary labels
    private Label totalQuantityValue;
    private Label totalGrossPriceValue;
    private Label totalPerishablePriceValue;
    private Label totalNetPriceValue;
    
    // Summary section visibility control
    private VBox summaryBox;
    private boolean summaryVisible = false;

    @Override
    public void start(Stage primaryStage) {
        try {
            BorderPane root = new BorderPane();
            root.setPadding(new Insets(10));
            
            // Create main sections
            VBox leftPanel = createLeftPanel();
            VBox centerPanel = createCenterPanel();
            HBox topPanel = createTopPanel();
            
            root.setLeft(leftPanel);
            root.setCenter(centerPanel);
            root.setTop(topPanel);
            
            // Load data
            refreshTableData();
            updateSummary();
            
            Scene scene = new Scene(root, 1000, 700);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            primaryStage.setTitle("Store Inventory Management System");
            primaryStage.setScene(scene);
            
            // Simplified window close handler
            primaryStage.setOnCloseRequest(event -> saveInventory());
            
            primaryStage.show();
            System.out.println("GUI window displayed successfully");
        } catch (Exception e) {
            handleError("Error initializing GUI", e, true);
        }
    }
    
    /**
     * Helper method to save inventory with proper error handling
     */
    private void saveInventory() {
        try {
            // Only save if we have products to save
            if (!productData.isEmpty()) {
                storeService.saveAndCloseInventory();
                System.out.println("Inventory saved successfully: " + productData.size() + " products");
            } else {
                System.out.println("WARNING: Product data is empty! Not overwriting inventory file.");
            }
        } catch (Exception e) {
            handleError("CRITICAL ERROR: Failed to save inventory", e, true);
        }
    }
    
    @Override
    public void stop() throws Exception {
        // This method is called when the application is stopping
        System.out.println("JavaFX application stop() method called, ensuring inventory is saved...");
        saveInventory();
        super.stop();
    }
    
    private HBox createTopPanel() {
        Label titleLabel = new Label("Inventory Management System");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.getStyleClass().add("header-label");
        titleLabel.setTextFill(Color.BLACK); // Changed to black for better contrast against light grey
        
        // Create a background for the title with light grey color
        Color lightGrey = Color.web("#e0e0e0");
        
        // Create a container for the title with enhanced styling
        HBox titleContainer = new HBox(titleLabel);
        
        // Apply styling with light grey color
        Background titleBackground = new Background(
            new BackgroundFill(lightGrey, new CornerRadii(5), new Insets(0)));
        
        titleContainer.setBackground(titleBackground);
        titleContainer.setPadding(new Insets(10));
        titleContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        // Add a subtle border to help it blend with the UI
        titleContainer.setBorder(new Border(new BorderStroke(
            Color.web("#cccccc"), 
            BorderStrokeStyle.SOLID, 
            new CornerRadii(5), 
            new BorderWidths(1))));
        
        // Apply a subtle effect to make it stand out
        titleContainer.setEffect(new javafx.scene.effect.DropShadow(
            4, 0, 2, Color.rgb(0, 0, 0, 0.2)));
        
        HBox topPanel = new HBox(titleContainer);
        topPanel.setPadding(new Insets(10, 0, 20, 0));
        return topPanel;
    }
    
    private VBox createLeftPanel() {
        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setMinWidth(250);
        leftPanel.getStyleClass().add("inventory-section");
        
        // Create a more prominent menu title
        Label menuTitle = new Label("Main Menu");
        menuTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        menuTitle.setTextFill(Color.WHITE);
        
        // Add a container for the menu title with light background
        HBox menuTitleContainer = new HBox(menuTitle);
        menuTitleContainer.setAlignment(javafx.geometry.Pos.CENTER);
        menuTitleContainer.setPadding(new Insets(5, 0, 5, 0));
        menuTitleContainer.setBackground(new Background(
            new BackgroundFill(Color.web("#4d4d4d"), new CornerRadii(5), new Insets(0))));
        menuTitleContainer.setBorder(new Border(new BorderStroke(
            Color.web("#5d5d5d"), BorderStrokeStyle.SOLID, new CornerRadii(5), new BorderWidths(1))));
        menuTitleContainer.setPrefHeight(50);
        
        // Create menu buttons using utility method
        Button addProductBtn = createMenuButton("Add Product", e -> showAddProductForm());
        Button viewInventoryBtn = createMenuButton("View Inventory", e -> refreshTableData());
        Button searchProductBtn = createMenuButton("Search Product", e -> showSearchDialog());
        Button displaySummaryBtn = createMenuButton("Display Summary", e -> toggleSummaryVisibility());
        Button removeProductBtn = createMenuButton("Remove Product", e -> removeSelectedProduct());
        Button consoleModeBtn = createMenuButton("Switch to Console", e -> switchToConsoleMode());
        Button exitBtn = createMenuButton("Exit", e -> System.exit(0));
        
        // Create a VBox for the buttons with some styling
        VBox buttonContainer = new VBox(5);
        buttonContainer.setPadding(new Insets(10, 5, 10, 5));
        buttonContainer.getChildren().addAll(
            addProductBtn, 
            viewInventoryBtn, 
            searchProductBtn, 
            displaySummaryBtn, 
            removeProductBtn, 
            new Separator(), 
            consoleModeBtn, 
            exitBtn
        );
        
        // Summary section
        summaryBox = createSummarySection();
        
        // Initially hide the summary section
        summaryBox.setVisible(false);
        summaryBox.setManaged(false);
        
        leftPanel.getChildren().addAll(menuTitleContainer, buttonContainer, summaryBox);
        
        return leftPanel;
    }
    
    private VBox createSummarySection() {
        VBox summaryBox = new VBox(5);
        summaryBox.setPadding(new Insets(10, 0, 0, 0));
        
        Label summaryTitle = new Label("Inventory Summary");
        summaryTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        summaryTitle.setTextFill(Color.WHITE);
        
        // Create summary items with styled labels
        totalQuantityValue = createStyledLabel("0");
        totalGrossPriceValue = createStyledLabel("$0.00");
        totalPerishablePriceValue = createStyledLabel("$0.00");
        totalNetPriceValue = createStyledLabel("$0.00");
        
        // Create grid for summary
        GridPane summaryGrid = new GridPane();
        summaryGrid.setHgap(5);
        summaryGrid.setVgap(5);
        
        // Add labels to grid
        addSummaryRow(summaryGrid, 0, "Total Quantity:", totalQuantityValue);
        addSummaryRow(summaryGrid, 1, "Total Gross Price:", totalGrossPriceValue);
        addSummaryRow(summaryGrid, 2, "With Perishable Discount:", totalPerishablePriceValue);
        addSummaryRow(summaryGrid, 3, "With 15% Discount:", totalNetPriceValue);
        
        summaryBox.getChildren().addAll(summaryTitle, summaryGrid);
        return summaryBox;
    }
    
    /**
     * Creates a label with standard white text styling
     */
    private Label createStyledLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.WHITE);
        return label;
    }
    
    private void addSummaryRow(GridPane grid, int row, String labelText, Label valueLabel) {
        Label label = createStyledLabel(labelText);
        grid.add(label, 0, row);
        grid.add(valueLabel, 1, row);
    }
    
    /**
     * Creates a menu button with standard styling and event handler
     */
    private Button createMenuButton(String text, EventHandler<ActionEvent> handler) {
        Button button = new Button(text);
        button.setPrefWidth(230);
        button.setPrefHeight(40);
        button.setOnAction(handler);
        return button;
    }
    
    private VBox createCenterPanel() {
        VBox centerPanel = new VBox(10);
        centerPanel.setPadding(new Insets(10));
        
        // Create table view for products
        productTable = new TableView<>();
        productTable.setPlaceholder(new Label("No products in inventory"));
        
        // Define columns using our helper methods
        TableColumn<Product, String> nameCol = createStyledColumn("Name", "name", 150);
        TableColumn<Product, BigDecimal> priceCol = createStyledColumn("Price", "price", 100);
        TableColumn<Product, Integer> quantityCol = createStyledColumn("Quantity", "quantity", 100);
        
        TableColumn<Product, BigDecimal> discountCol = createStyledColumn("Discount", "discount", 100);
        discountCol.setCellFactory(column -> createPercentageCell());
        
        TableColumn<Product, LocalDate> expirationCol = createStyledColumn("Expiration Date", null, 150);
        expirationCol.setCellValueFactory(cellData -> {
            Product product = cellData.getValue();
            if (product instanceof PerishableProduct) {
                return new SimpleObjectProperty<>(((PerishableProduct) product).getExpirationDate());
            }
            return new SimpleObjectProperty<>(null);
        });
        expirationCol.setCellFactory(column -> createDateCell());
        
        TableColumn<Product, String> totalValueCol = createStyledColumn("Total Value", null, 120);
        totalValueCol.setCellValueFactory(cellData -> {
            Product product = cellData.getValue();
            return new SimpleObjectProperty<>(product.getTotalValue().toString());
        });
        totalValueCol.setCellFactory(column -> createCurrencyCell());
        
        // Add columns individually to avoid type safety warning with varargs
        productTable.getColumns().add(nameCol);
        productTable.getColumns().add(priceCol);
        productTable.getColumns().add(quantityCol);
        productTable.getColumns().add(discountCol);
        productTable.getColumns().add(expirationCol);
        productTable.getColumns().add(totalValueCol);
        
        productTable.setItems(productData);
        
        centerPanel.getChildren().add(productTable);
        VBox.setVgrow(productTable, Priority.ALWAYS);
        
        return centerPanel;
    }
    
    /**
     * Creates a styled TableColumn with a standardized cell factory
     */
    private <S, T> TableColumn<S, T> createStyledColumn(String title, String propertyName, int prefWidth) {
        TableColumn<S, T> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(propertyName));
        column.setPrefWidth(prefWidth);
        return column;
    }
    
    /**
     * Creates a standard TableCell for currency display
     */
    private <S> TableCell<S, String> createCurrencyCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText("$" + value);
                }
            }
        };
    }
    
    /**
     * Creates a standard TableCell for percentage display
     */
    private <S> TableCell<S, BigDecimal> createPercentageCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal discount, boolean empty) {
                super.updateItem(discount, empty);
                if (empty || discount == null) {
                    setText(null);
                } else {
                    // Format discount as percentage
                    setText(discount.multiply(BigDecimal.valueOf(100)) + "%");
                }
            }
        };
    }
    
    /**
     * Creates a standard TableCell for date display
     */
    private <S> TableCell<S, LocalDate> createDateCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date.format(DateTimeFormatter.ISO_LOCAL_DATE));
                }
            }
        };
    }
    
    public void showAddProductForm() {
        // Create a dialog
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Add Product");
        dialog.setHeaderText("Enter Product Details");
        
        // Set the button types
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        // Create the form grid pane
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        nameField = new TextField();
        nameField.setPromptText("Enter product name (e.g., Organic Apples)");
        priceField = new TextField();
        priceField.setPromptText("Enter price (e.g., 2.99)");
        quantityField = new TextField();
        quantityField.setPromptText("Enter quantity (e.g., 50)");
        discountField = new TextField();
        discountField.setPromptText("Enter discount percentage (e.g., 10)");
        perishableCheckBox = new CheckBox("Perishable Product");
        expirationDatePicker = new DatePicker();
        expirationDatePicker.setPromptText("Select expiration date (YYYY-MM-DD)");
        
        // Show/hide expiration date picker based on checkbox
        expirationDatePicker.setVisible(false);
        expirationDatePicker.setManaged(false);
        
        // Set date converter for expiration date
        expirationDatePicker.setConverter(new StringConverter<>() {
            private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            
            @Override
            public String toString(LocalDate date) {
                return (date != null) ? dateFormatter.format(date) : "";
            }
            
            @Override
            public LocalDate fromString(String string) {
                return (string != null && !string.isEmpty()) ? LocalDate.parse(string, dateFormatter) : null;
            }
        });
        
        // Show/hide expiration date picker based on checkbox
        perishableCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            expirationDatePicker.setVisible(newVal);
            expirationDatePicker.setManaged(newVal);
        });
        
        // Add fields to grid
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Price:"), 0, 1);
        grid.add(priceField, 1, 1);
        grid.add(new Label("Quantity:"), 0, 2);
        grid.add(quantityField, 1, 2);
        grid.add(new Label("Discount (%):"), 0, 3);
        grid.add(discountField, 1, 3);
        grid.add(perishableCheckBox, 0, 4, 2, 1);
        grid.add(new Label("Expiration Date:"), 0, 5);
        grid.add(expirationDatePicker, 1, 5);
        
        dialog.getDialogPane().setContent(grid);
        
        // Request focus on the name field
        nameField.requestFocus();
        
        // Convert the result to a Product when the add button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    String name = nameField.getText().trim();
                    double price = Double.parseDouble(priceField.getText().trim());
                    int quantity = Integer.parseInt(quantityField.getText().trim());
                    double discount = Double.parseDouble(discountField.getText().trim()) / 100.0;
                    
                    if (name.isEmpty()) {
                        showAlert("Error", "Product name cannot be empty.");
                        return null;
                    }
                    
                    if (perishableCheckBox.isSelected()) {
                        LocalDate expirationDate = expirationDatePicker.getValue();
                        if (expirationDate == null) {
                            showAlert("Error", "Please select an expiration date.");
                            return null;
                        }
                        return new PerishableProduct(name, price, quantity, 
                                expirationDate.toString(), discount);
                    } else {
                        return new Product(name, price, quantity, discount);
                    }
                } catch (NumberFormatException e) {
                    handleError("Invalid number format in product form", e, true);
                    return null;
                }
            }
            return null;
        });
        
        // Show the dialog and process the result
        Optional<Product> result = dialog.showAndWait();
        
        result.ifPresent(product -> {
            try {
                storeService.addProduct(product);
                refreshTableData();
                updateSummary();
            } catch (Exception e) {
                handleError("Failed to add product", e, true);
            }
        });
    }
    
    public void refreshTableData() {
        productData.clear();
        productData.addAll(storeService.getInventory());
    }
    
    public void updateSummary() {
        totalQuantityValue.setText(String.valueOf(storeService.getTotalQuantity()));
        totalGrossPriceValue.setText("$" + storeService.getTotalGrossPrice());
        totalPerishablePriceValue.setText("$" + storeService.getTotalPriceWithPerishableDiscount());
        totalNetPriceValue.setText("$" + storeService.getTotalNetPriceWithDiscount());
    }
    
    private void showSearchDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Search Product");
        dialog.setHeaderText("Enter product name to search");
        dialog.setContentText("Product name:");
        
        Optional<String> result = dialog.showAndWait();
        
        result.ifPresent(name -> {
            Optional<Product> product = storeService.findProductByName(name);
            if (product.isPresent()) {
                // Filter table to show only this product
                productData.clear();
                productData.add(product.get());
            } else {
                showAlert("Product Not Found", "No product found with name: " + name);
                refreshTableData();
            }
        });
    }
    
    private void removeSelectedProduct() {
        Product selectedProduct = productTable.getSelectionModel().getSelectedItem();
        if (selectedProduct != null) {
            // Create confirmation dialog to give the user a chance to change their mind
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Removal");
            confirmAlert.setHeaderText("Remove Product Confirmation");
            confirmAlert.setContentText("Are you sure you want to remove \"" + selectedProduct.getName() + "\"?");
            
            // Add Yes/No buttons
            ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
            ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
            confirmAlert.getButtonTypes().setAll(yesButton, noButton);
            
            Optional<ButtonType> result = confirmAlert.showAndWait();
            
            // Only proceed if user confirms with "Yes"
            if (result.isPresent() && result.get() == yesButton) {
                int index = storeService.getInventory().indexOf(selectedProduct);
                if (index >= 0) {
                    storeService.removeProduct(index);
                    refreshTableData();
                    updateSummary();
                }
            }
        } else {
            showAlert("No Selection", "Please select a product to remove.");
        }
    }
    
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    /**
     * Centralized error handling with logging
     * @param operation Description of the operation that failed
     * @param e The exception that was thrown
     * @param showToUser Whether to show an alert to the user
     */
    private void handleError(String operation, Exception e, boolean showToUser) {
        // Log the error
        System.err.println("ERROR: " + operation + ": " + e.getMessage());
        e.printStackTrace();
        
        // Show to user if requested
        if (showToUser) {
            showAlert("Error", operation + ": " + e.getMessage());
        }
    }
    
    /**
     * Switches from GUI mode to console mode
     */
    private void switchToConsoleMode() {
        try {
            // Save inventory before switching
            saveInventory();
            
            // Show confirmation dialog
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Switch to Console Mode");
            alert.setHeaderText("Mode Change Confirmation");
            alert.setContentText("Do you want to switch to console mode? This will close the current window.");
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Use our static method to notify AppLauncher to switch modes
                com.store.AppLauncher.requestConsoleMode();
                
                // Properly close the JavaFX application
                Stage stage = (Stage) productTable.getScene().getWindow();
                stage.close();
                Platform.exit();
            }
        } catch (Exception e) {
            handleError("Failed to switch to console mode", e, true);
        }
    }
    
    /**
     * Toggles the visibility of the summary section.
     */
    private void toggleSummaryVisibility() {
        summaryVisible = !summaryVisible;
        summaryBox.setVisible(summaryVisible);
        summaryBox.setManaged(summaryVisible);
    }
}