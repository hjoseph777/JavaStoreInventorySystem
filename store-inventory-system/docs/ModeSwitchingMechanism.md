# Store Inventory System: Mode Switching Mechanism

## Overview

The Store Inventory System application supports two operational modes:
1. **GUI Mode** - A graphical user interface built with JavaFX
2. **Console Mode** - A text-based command-line interface

The `AppLauncher` class implements a sophisticated mode switching mechanism that allows:
- Starting the application in either mode
- Switching from GUI to console mode during runtime
- Automatic fallback to console mode if GUI initialization fails

## Mode Switching Flow

```
┌─────────────────┐
│                 │
│  Application    │
│     Start       │
│                 │
└────────┬────────┘
         │
         ▼
┌─────────────────┐     Yes    ┌─────────────────┐
│  Console Mode   │◄───────────┤ Command Line    │
│   Requested?    │            │ Arguments Check │
└────────┬────────┘            └─────────────────┘
         │ No
         ▼
┌─────────────────┐
│  Initialize     │
│  JavaFX and     │
│  GUI Components │
└────────┬────────┘
         │
         ▼
┌─────────────────┐     Yes    ┌─────────────────┐
│  JavaFX         │◄───────────┤  Fall Back to   │
│  Load Failed?   ├───────────►│  Console Mode   │
└────────┬────────┘            └─────────────────┘
         │ No
         ▼
┌─────────────────┐
│                 │
│  Run GUI Mode   │
│                 │
└────────┬────────┘
         │
         ▼
┌─────────────────┐     Yes    ┌─────────────────┐
│  Switch to      │◄───────────┤ User Requested  │
│  Console Mode?  ├───────────►│ Mode Switch     │
└────────┬────────┘            └─────────────────┘
         │ No
         ▼
┌─────────────────┐
│                 │
│   Application   │
│      Exit       │
│                 │
└─────────────────┘
```

## Mode Selection Logic

### Initial Mode Determination

The application decides which mode to start in through the `shouldRunInConsoleMode()` method:

```java
private static boolean shouldRunInConsoleMode(String[] args) {
    // Explicitly check for console mode argument
    if (args != null && args.length > 0) {
        String mode = args[0].toLowerCase();
        return mode.equals("console") || mode.equals("--console") || mode.equals("-c");
    }
    return false; // Default to GUI mode when no args provided
}
```

By default, the application starts in GUI mode unless specifically requested to run in console mode.

### GUI-to-Console Switching

The switching mechanism uses a static volatile boolean flag:

```java
// Static flag to track if console mode is requested after GUI exits
private static volatile boolean switchToConsoleMode = false;
    
// Static method for InventoryApp to request console mode
public static void requestConsoleMode() {
    switchToConsoleMode = true;
}
```

When a user chooses to switch to console mode from the GUI (typically through a menu option), the JavaFX application calls `AppLauncher.requestConsoleMode()`, setting the flag to true. After the GUI exits, the `main()` method checks this flag and launches console mode if requested:

```java
// After GUI exits, check if we should switch to console mode
if (switchToConsoleMode) {
    System.out.println("Switching to console mode after GUI exit...");
    launchConsoleMode(new String[]{"console"});
}
```

## Data Consistency

A critical aspect of the mode switching mechanism is ensuring data consistency across modes. This is achieved through:

1. **Shared Singleton Service** - Both modes use the same `StoreService` singleton instance:
   ```java
   StoreService service = StoreService.getInstance();
   ```

2. **Explicit Saving Before Mode Switching** - The inventory data is saved before switching modes:
   ```java
   service.saveAndCloseInventory();
   ```

3. **Shutdown Hooks** - A JVM shutdown hook ensures resources are properly released:
   ```java
   Runtime.getRuntime().addShutdownHook(new Thread(() -> {
       System.out.println("Application shutting down, cleaning up resources...");
       cleanupResources();
   }));
   ```

## Fallback Mechanism

If the GUI fails to launch (due to JavaFX configuration issues or missing dependencies), the application automatically falls back to console mode:

```java
try {
    // GUI initialization code...
    Application.launch(InventoryApp.class, args);
} catch (Exception e) {
    System.err.println("Failed to launch JavaFX application: " + e.getMessage());
    System.err.println("Falling back to console mode...");
    e.printStackTrace();
    launchConsoleMode(args);
}
```

This ensures the application remains usable even on systems with limited graphical capabilities or misconfigured JavaFX.

## Mode Initialization Sequence

### GUI Mode Initialization

1. Initialize `StoreService` singleton
2. Check for inventory refresh requests
3. Configure JavaFX environment via `JavaFxUtil`
4. Launch the JavaFX application with `InventoryApp` as the root class

### Console Mode Initialization

1. Check for inventory refresh requests
2. Initialize `StoreService` singleton 
3. Call `Main.main()` to start the console interface

## Benefits of the Design

1. **Flexibility** - Users can choose their preferred interface
2. **Robustness** - Fallback mechanism ensures operation even with GUI issues
3. **Data Integrity** - Consistent data handling across mode switches
4. **Modularity** - Clear separation between UI logic and business logic

## Technical Implementation Details

The mode switching mechanism relies on several Java features:

1. **Static volatile flag** for thread-safe communication
2. **Singleton pattern** for maintaining state across modes
3. **Runtime shutdown hooks** for resource cleanup
4. **Exception handling** for graceful fallback

This design allows for seamless transitions between interfaces while maintaining a coherent user experience and data consistency.