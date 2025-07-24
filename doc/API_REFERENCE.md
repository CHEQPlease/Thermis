# Thermis API Reference

This document provides a comprehensive reference for all public APIs in the Thermis Flutter plugin.

## Table of Contents

- [Core Printing Methods](#core-printing-methods)
- [Printer Operations](#printer-operations)
- [Device Discovery](#device-discovery)
- [Queue Management](#queue-management)
- [Data Models](#data-models)
- [Enums](#enums)
- [Error Handling](#error-handling)

## Core Printing Methods

### `printReceipt`

```dart
static Future<PrintResult?> printReceipt(
  String receiptDTOJson, {
  PrinterConfig? config,
})
```

**Description:** Prints a receipt using the specified configuration.

**Parameters:**
- `receiptDTOJson` (String, required): JSON string containing receipt data
- `config` (PrinterConfig?, optional): Printer configuration. Defaults to USB generic printer if not provided.

**Returns:** `Future<PrintResult?>` - Contains success status, error details, and retry information.

**Example:**
```dart
// Basic usage with default USB printer
final result = await Thermis.printReceipt(receiptJson);

// With specific configuration
final result = await Thermis.printReceipt(
  receiptJson,
  config: PrinterConfig(
    printerType: PrinterType.starMCLan,
    macAddresses: ['AA:BB:CC:DD:EE:FF'],
  ),
);

// Handle result
if (result?.success == true) {
  print('Print successful');
} else {
  print('Print failed: ${result?.reason?.displayName}');
  if (result?.retryable == true) {
    print('Will retry automatically');
  }
}
```

**Error Handling:**
- Returns `null` if operation fails at platform level
- Returns `PrintResult` with detailed error information for application-level failures
- Automatic retry for retryable errors with exponential backoff

### `getReceiptReview`

```dart
static Future<Uint8List?> getReceiptReview(String receiptDTOJSON)
```

**Description:** Generates a bitmap preview of the receipt without printing.

**Parameters:**
- `receiptDTOJSON` (String, required): JSON string containing receipt data

**Returns:** `Future<Uint8List?>` - Receipt bitmap data in PNG format, or `null` if generation fails.

**Example:**
```dart
final bitmap = await Thermis.getReceiptReview(receiptJson);
if (bitmap != null) {
  // Display in UI
  Image.memory(bitmap);
} else {
  print('Failed to generate preview');
}
```

## Printer Operations

### `openCashDrawer`

```dart
static Future<bool?> openCashDrawer({PrinterConfig? config})
```

**Description:** Opens the cash drawer connected to the printer.

**Parameters:**
- `config` (PrinterConfig?, optional): Printer configuration. Defaults to USB generic printer.

**Returns:** `Future<bool?>` - `true` if successful, `false` if failed, `null` if operation couldn't be performed.

**Example:**
```dart
final success = await Thermis.openCashDrawer(
  config: PrinterConfig(printerType: PrinterType.usbGeneric),
);
print(success ? 'Cash drawer opened' : 'Failed to open');
```

**Notes:**
- Only supported on USB printers
- Requires compatible cash drawer connected to printer
- LAN printers may not support this operation

### `cutPaper`

```dart
static Future<bool?> cutPaper({PrinterConfig? config})
```

**Description:** Cuts the receipt paper using the printer's built-in cutter.

**Parameters:**
- `config` (PrinterConfig?, optional): Printer configuration. Defaults to USB generic printer.

**Returns:** `Future<bool?>` - `true` if successful, `false` if failed, `null` if operation couldn't be performed.

**Example:**
```dart
final success = await Thermis.cutPaper(
  config: PrinterConfig(printerType: PrinterType.usbGeneric),
);
print(success ? 'Paper cut' : 'Failed to cut paper');
```

**Notes:**
- Requires printer with built-in paper cutter
- Some printers may not support programmatic paper cutting

### `checkPrinterConnection`

```dart
static Future<bool?> checkPrinterConnection({PrinterConfig? config})
```

**Description:** Checks if the specified printer is connected and ready for printing.

**Parameters:**
- `config` (PrinterConfig?, optional): Printer configuration. Defaults to USB generic printer.

**Returns:** `Future<bool?>` - `true` if connected, `false` if not connected, `null` if check couldn't be performed.

**Example:**
```dart
// Check USB printer
final usbConnected = await Thermis.checkPrinterConnection();

// Check LAN printer
final lanConnected = await Thermis.checkPrinterConnection(
  config: PrinterConfig(
    printerType: PrinterType.starMCLan,
    macAddresses: ['AA:BB:CC:DD:EE:FF'],
  ),
);

print('USB: ${usbConnected ? 'Connected' : 'Disconnected'}');
print('LAN: ${lanConnected ? 'Connected' : 'Disconnected'}');
```

## Device Discovery

### `discoverPrinters`

```dart
static Stream<Device> discoverPrinters({int scanDurationMs = 5000})
```

**Description:** Discovers available printers and returns them as a stream for real-time updates.

**Parameters:**
- `scanDurationMs` (int, optional): Discovery duration in milliseconds. Default: 5000ms (5 seconds).

**Returns:** `Stream<Device>` - Stream of discovered devices.

**Example:**
```dart
// Basic discovery
await for (final device in Thermis.discoverPrinters()) {
  print('Found: ${device.deviceName} at ${device.ip}');
}

// Extended discovery time
await for (final device in Thermis.discoverPrinters(scanDurationMs: 15000)) {
  print('Device: ${device.deviceName}');
  print('MAC: ${device.mac}');
  print('IP: ${device.ip}');
  
  // Use device immediately
  final config = PrinterConfig(
    printerType: PrinterType.starMCLan,
    macAddresses: [device.mac],
  );
  // ... print operations
}
```

**Notes:**
- Only discovers Star Micronics LAN printers
- Discovery continues for the specified duration
- Devices may be discovered multiple times during the scan period

### `getAvailableDevices`

```dart
static Future<List<Device>> getAvailableDevices({int durationMs = 50000})
```

**Description:** Discovers all available printers and returns them as a list after the discovery period completes.

**Parameters:**
- `durationMs` (int, optional): Discovery duration in milliseconds. Default: 50000ms (50 seconds).

**Returns:** `Future<List<Device>>` - List of all discovered devices.

**Example:**
```dart
// Quick discovery
final devices = await Thermis.getAvailableDevices(durationMs: 10000);

if (devices.isEmpty) {
  print('No printers found');
} else {
  print('Found ${devices.length} printer(s):');
  for (final device in devices) {
    print('- ${device.deviceName} (${device.mac})');
  }
  
  // Use first available printer
  final config = PrinterConfig(
    printerType: PrinterType.starMCLan,
    macAddresses: [devices.first.mac],
  );
}
```

**Notes:**
- Waits for the full discovery duration before returning results
- Deduplicates devices found multiple times during discovery
- Better for scenarios where you need the complete list before proceeding

### `stopDiscovery`

```dart
static Future<void> stopDiscovery()
```

**Description:** Stops the current printer discovery process.

**Returns:** `Future<void>` - Completes when discovery is stopped.

**Example:**
```dart
// Start discovery in background
final discoveryStream = Thermis.discoverPrinters(scanDurationMs: 30000);

// Listen for a few devices
int deviceCount = 0;
await for (final device in discoveryStream) {
  print('Found: ${device.deviceName}');
  deviceCount++;
  
  // Stop after finding 3 devices
  if (deviceCount >= 3) {
    await Thermis.stopDiscovery();
    break;
  }
}
```

## Queue Management

### `getQueueSize`

```dart
static Future<int?> getQueueSize()
```

**Description:** Gets the total number of print jobs across all device queues.

**Returns:** `Future<int?>` - Total number of queued jobs, or `null` if unavailable.

**Example:**
```dart
final totalJobs = await Thermis.getQueueSize();
if (totalJobs != null) {
  print('Total jobs in queue: $totalJobs');
  if (totalJobs > 10) {
    print('Warning: High queue load');
  }
}
```

### `getDeviceQueueSizes`

```dart
static Future<Map<String, int>?> getDeviceQueueSizes()
```

**Description:** Gets the number of print jobs in each device-specific queue.

**Returns:** `Future<Map<String, int>?>` - Map of device keys to queue sizes, or `null` if unavailable.

**Example:**
```dart
final deviceQueues = await Thermis.getDeviceQueueSizes();
if (deviceQueues != null) {
  print('Device queue status:');
  deviceQueues.forEach((deviceKey, queueSize) {
    print('  $deviceKey: $queueSize jobs');
  });
  
  // Check for problematic queues
  final busyQueues = deviceQueues.entries
    .where((entry) => entry.value > 5)
    .toList();
    
  if (busyQueues.isNotEmpty) {
    print('Busy queues detected: ${busyQueues.length}');
  }
}
```

**Queue Key Format:**
- USB Generic: `"USB_GENERIC"`
- Star LAN: `"STARMC_LAN_{MAC_ADDRESS}"`

### `clearPrintQueue`

```dart
static Future<bool?> clearPrintQueue()
```

**Description:** Clears all print jobs from all device queues.

**Returns:** `Future<bool?>` - `true` if successful, `false` if failed, `null` if operation couldn't be performed.

**Example:**
```dart
final cleared = await Thermis.clearPrintQueue();
if (cleared == true) {
  print('All queues cleared successfully');
} else {
  print('Failed to clear queues');
}

// Verify clearing
final remainingJobs = await Thermis.getQueueSize();
print('Remaining jobs: $remainingJobs');
```

**Notes:**
- Cancels all pending print jobs
- Jobs currently being processed may complete before cancellation
- Use with caution as this affects all devices

### `clearDeviceQueue`

```dart
static Future<bool?> clearDeviceQueue(String deviceKey)
```

**Description:** Clears all print jobs from a specific device queue.

**Parameters:**
- `deviceKey` (String, required): Device identifier (e.g., "USB_GENERIC" or "STARMC_LAN_AA:BB:CC:DD:EE:FF")

**Returns:** `Future<bool?>` - `true` if successful, `false` if failed, `null` if operation couldn't be performed.

**Example:**
```dart
// Clear USB printer queue
final usbCleared = await Thermis.clearDeviceQueue('USB_GENERIC');

// Clear specific LAN printer queue
final lanCleared = await Thermis.clearDeviceQueue('STARMC_LAN_AA:BB:CC:DD:EE:FF');

print('USB queue cleared: $usbCleared');
print('LAN queue cleared: $lanCleared');
```

## Data Models

### `Device`

Represents a discovered printer device.

```dart
class Device {
  final String deviceName;
  final String mac;
  final String ip;
  
  Device({
    required this.deviceName,
    required this.mac,
    required this.ip,
  });
  
  factory Device.fromMap(Map<String, dynamic> map);
  Map<String, dynamic> toMap();
  String toString();
}
```

**Properties:**
- `deviceName` (String): Human-readable device name (e.g., "Star TSP100III")
- `mac` (String): MAC address (e.g., "AA:BB:CC:DD:EE:FF")
- `ip` (String): IP address (e.g., "192.168.1.100")

**Example:**
```dart
final device = Device(
  deviceName: 'Star mC-Print3',
  mac: 'AA:BB:CC:DD:EE:FF',
  ip: '192.168.1.100',
);

print(device.toString()); // Device(deviceName: Star mC-Print3, mac: AA:BB:CC:DD:EE:FF, ip: 192.168.1.100)
```

### `PrinterConfig`

Configuration for printer operations.

```dart
class PrinterConfig {
  final PrinterType printerType;
  final List<String>? macAddresses;
  
  PrinterConfig({
    required this.printerType,
    this.macAddresses,
  });
  
  Map<String, dynamic> toMap();
}
```

**Properties:**
- `printerType` (PrinterType): Type of printer (USB or LAN)
- `macAddresses` (List<String>?): MAC addresses for LAN printers (required for StarMC LAN)

**Example:**
```dart
// USB configuration
final usbConfig = PrinterConfig(
  printerType: PrinterType.usbGeneric,
);

// LAN configuration
final lanConfig = PrinterConfig(
  printerType: PrinterType.starMCLan,
  macAddresses: ['AA:BB:CC:DD:EE:FF', '11:22:33:44:55:66'],
);
```

### `PrintResult`

Result of a print operation with detailed status information.

```dart
class PrintResult {
  final bool success;
  final PrintFailureReason? reason;
  final bool retryable;
  final String? message;
  
  PrintResult({
    required this.success,
    this.reason,
    required this.retryable,
    this.message,
  });
  
  factory PrintResult.fromMap(Map<String, dynamic> map);
  String toString();
}
```

**Properties:**
- `success` (bool): Whether the operation succeeded
- `reason` (PrintFailureReason?): Specific failure reason if unsuccessful
- `retryable` (bool): Whether the operation can be retried automatically
- `message` (String?): Additional error message or details

**Example:**
```dart
final result = await Thermis.printReceipt(receiptJson);

if (result?.success == true) {
  print('Success!');
} else {
  print('Failed: ${result?.reason?.displayName}');
  print('Retryable: ${result?.retryable}');
  print('Message: ${result?.message}');
}
```

## Enums

### `PrinterType`

Defines the type of printer to use.

```dart
enum PrinterType {
  usbGeneric,  // Generic USB thermal printer
  starMCLan,   // Star Micronics LAN printer
}
```

**Values:**
- `usbGeneric`: Generic USB thermal printers using ESC/POS commands
- `starMCLan`: Star Micronics network printers using StarIO10 SDK

### `PrintFailureReason`

Specific reasons for print operation failures.

```dart
enum PrintFailureReason {
  // Retryable errors
  printerBusy,
  deviceInUse,
  networkError,
  communicationError,
  timeoutError,
  
  // Non-retryable errors
  printerOffline,
  printerNotFound,
  outOfPaper,
  coverOpen,
  unknownError,
}
```

**Properties:**
- `isRetryable` (bool): Whether this error type is automatically retryable
- `displayName` (String): User-friendly error message

**Retryable Errors:**
- `printerBusy`: Printer is processing another job
- `deviceInUse`: Device is being used by another process
- `networkError`: Network connectivity issues
- `communicationError`: Protocol or communication errors
- `timeoutError`: Operation timed out

**Non-Retryable Errors:**
- `printerOffline`: Printer is not responding
- `printerNotFound`: Printer device not found
- `outOfPaper`: Printer is out of paper
- `coverOpen`: Printer cover is open
- `unknownError`: Unclassified error

**Example:**
```dart
final result = await Thermis.printReceipt(receiptJson);
if (result?.success != true && result?.reason != null) {
  final reason = result!.reason!;
  print('Error: ${reason.displayName}');
  print('Can retry: ${reason.isRetryable}');
  
  switch (reason) {
    case PrintFailureReason.printerBusy:
      // Handle busy printer
      break;
    case PrintFailureReason.outOfPaper:
      // Notify user to refill paper
      break;
    // ... handle other cases
  }
}
```

## Error Handling

### Automatic Retry Logic

Thermis automatically retries failed operations for retryable errors:

- **Base Delay**: 3000ms (3 seconds)
- **Max Retries**: 3 attempts
- **Backoff Strategy**: Exponential with jitter
- **Retry Delays**: ~3s, ~6s, ~12s

### Error Classification

Errors are classified into two categories:

1. **Retryable Errors**: Temporary issues that may resolve automatically
   - Printer busy
   - Network timeouts
   - Communication errors
   - Device in use

2. **Non-Retryable Errors**: Permanent issues requiring user intervention
   - Printer offline
   - Out of paper
   - Cover open
   - Printer not found

### Best Practices

1. **Always check PrintResult**: Don't assume operations succeed
2. **Handle retryable vs non-retryable**: Different UX for different error types
3. **Provide user feedback**: Use `displayName` for user-friendly messages
4. **Monitor queues**: Check queue sizes to prevent overload
5. **Graceful degradation**: Have fallback options for critical operations

**Example Error Handling Pattern:**
```dart
Future<void> printWithErrorHandling(String receiptJson) async {
  final result = await Thermis.printReceipt(receiptJson);
  
  if (result?.success == true) {
    // Success - show confirmation
    showSuccessMessage('Receipt printed successfully');
    return;
  }
  
  if (result?.retryable == true) {
    // Retryable error - show progress indicator
    showRetryMessage('Printing in progress, please wait...');
  } else {
    // Non-retryable error - show error and suggest action
    final message = result?.reason?.displayName ?? 'Unknown error';
    showErrorMessage('Print failed: $message');
    
    // Suggest specific actions based on error type
    switch (result?.reason) {
      case PrintFailureReason.outOfPaper:
        showActionButton('Refill Paper', () => checkPrinterStatus());
        break;
      case PrintFailureReason.printerOffline:
        showActionButton('Check Connection', () => checkPrinterConnection());
        break;
      default:
        showActionButton('Retry', () => printWithErrorHandling(receiptJson));
    }
  }
}
``` 