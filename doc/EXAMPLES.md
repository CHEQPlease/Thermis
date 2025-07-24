# Thermis Examples

This document provides comprehensive examples of using the Thermis Flutter plugin in real-world scenarios.

## Table of Contents

- [Basic Usage](#basic-usage)
- [Advanced Printing](#advanced-printing)
- [Device Discovery](#device-discovery)
- [Queue Management](#queue-management)
- [Error Handling](#error-handling)
- [Production Patterns](#production-patterns)
- [Integration Examples](#integration-examples)

## Basic Usage

### Simple Receipt Printing

```dart
import 'package:thermis/thermis.dart';

Future<void> printSimpleReceipt() async {
  const receiptJson = '''
  {
    "brandName": "CHEQ Diner",
    "orderNo": "001",
    "receiptType": "customer",
    "deviceType": "pos",
    "items": [
      {
        "itemName": "Coffee",
        "quantity": "1",
        "price": "\\$3.50"
      }
    ],
    "breakdown": [
      {
        "key": "TOTAL",
        "value": "\\$3.50",
        "important": true
      }
    ]
  }
  ''';

  final result = await Thermis.printReceipt(receiptJson);
  
  if (result?.success == true) {
    print('✅ Receipt printed successfully');
  } else {
    print('❌ Print failed: ${result?.reason?.displayName}');
  }
}
```

### USB Printer with Cash Drawer

```dart
import 'package:thermis/thermis.dart';

Future<void> printAndOpenDrawer() async {
  final config = PrinterConfig(printerType: PrinterType.usbGeneric);
  
  // Print receipt
  final printResult = await Thermis.printReceipt(receiptJson, config: config);
  
  if (printResult?.success == true) {
    print('Receipt printed');
    
    // Open cash drawer
    final drawerOpened = await Thermis.openCashDrawer(config: config);
    print('Cash drawer: ${drawerOpened ? 'opened' : 'failed'}');
    
    // Cut paper
    final paperCut = await Thermis.cutPaper(config: config);
    print('Paper cut: ${paperCut ? 'success' : 'failed'}');
  }
}
```

## Advanced Printing

### Multi-Printer Setup

```dart
import 'package:thermis/thermis.dart';

class PrinterManager {
  static final usbConfig = PrinterConfig(
    printerType: PrinterType.usbGeneric,
  );
  
  static final lanConfig = PrinterConfig(
    printerType: PrinterType.starMCLan,
    macAddresses: [
      'AA:BB:CC:DD:EE:FF', // Kitchen printer
      '11:22:33:44:55:66', // Counter printer
    ],
  );
  
  static Future<void> printToAllPrinters(String receiptJson) async {
    // Print to USB printer
    final usbResult = Thermis.printReceipt(receiptJson, config: usbConfig);
    
    // Print to LAN printers (parallel execution)
    final lanResult = Thermis.printReceipt(receiptJson, config: lanConfig);
    
    // Wait for all prints to complete
    final results = await Future.wait([usbResult, lanResult]);
    
    print('USB Print: ${results[0]?.success == true ? 'Success' : 'Failed'}');
    print('LAN Print: ${results[1]?.success == true ? 'Success' : 'Failed'}');
  }
  
  static Future<void> printKitchenOrder(String kitchenReceiptJson) async {
    // Print only to kitchen printer (first LAN printer)
    final kitchenConfig = PrinterConfig(
      printerType: PrinterType.starMCLan,
      macAddresses: ['AA:BB:CC:DD:EE:FF'],
    );
    
    final result = await Thermis.printReceipt(kitchenReceiptJson, config: kitchenConfig);
    print('Kitchen order: ${result?.success == true ? 'Sent' : 'Failed'}');
  }
}
```

### Receipt Preview and Print

```dart
import 'package:flutter/material.dart';
import 'package:thermis/thermis.dart';

class ReceiptPreviewWidget extends StatefulWidget {
  final String receiptJson;
  
  const ReceiptPreviewWidget({Key? key, required this.receiptJson}) : super(key: key);
  
  @override
  _ReceiptPreviewWidgetState createState() => _ReceiptPreviewWidgetState();
}

class _ReceiptPreviewWidgetState extends State<ReceiptPreviewWidget> {
  Uint8List? _previewBitmap;
  bool _isLoading = false;
  
  @override
  void initState() {
    super.initState();
    _generatePreview();
  }
  
  Future<void> _generatePreview() async {
    setState(() => _isLoading = true);
    
    try {
      final bitmap = await Thermis.getReceiptReview(widget.receiptJson);
      setState(() {
        _previewBitmap = bitmap;
        _isLoading = false;
      });
    } catch (e) {
      setState(() => _isLoading = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Failed to generate preview: $e')),
      );
    }
  }
  
  Future<void> _printReceipt() async {
    setState(() => _isLoading = true);
    
    final result = await Thermis.printReceipt(widget.receiptJson);
    
    setState(() => _isLoading = false);
    
    if (result?.success == true) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Receipt printed successfully!'),
          backgroundColor: Colors.green,
        ),
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Print failed: ${result?.reason?.displayName}'),
          backgroundColor: Colors.red,
        ),
      );
    }
  }
  
  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Expanded(
          child: _isLoading
              ? const Center(child: CircularProgressIndicator())
              : _previewBitmap != null
                  ? SingleChildScrollView(
                      child: Image.memory(_previewBitmap!),
                    )
                  : const Center(child: Text('No preview available')),
        ),
        Padding(
          padding: const EdgeInsets.all(16.0),
          child: Row(
            children: [
              Expanded(
                child: ElevatedButton(
                  onPressed: _isLoading ? null : _generatePreview,
                  child: const Text('Refresh Preview'),
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: ElevatedButton(
                  onPressed: _isLoading ? null : _printReceipt,
                  child: const Text('Print Receipt'),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
```

## Device Discovery

### Automatic Printer Discovery

```dart
import 'package:thermis/thermis.dart';

class PrinterDiscoveryService {
  static List<Device> _discoveredDevices = [];
  
  static Future<List<Device>> discoverPrinters({
    int timeoutMs = 10000,
  }) async {
    print('🔍 Starting printer discovery...');
    
    try {
      final devices = await Thermis.getAvailableDevices(durationMs: timeoutMs);
      _discoveredDevices = devices;
      
      print('✅ Discovery complete. Found ${devices.length} printer(s):');
      for (final device in devices) {
        print('  - ${device.deviceName} (${device.mac}) at ${device.ip}');
      }
      
      return devices;
    } catch (e) {
      print('❌ Discovery failed: $e');
      return [];
    }
  }
  
  static Stream<Device> discoverPrintersStream({
    int scanDurationMs = 15000,
  }) async* {
    print('🔍 Starting streaming discovery...');
    
    try {
      await for (final device in Thermis.discoverPrinters(scanDurationMs: scanDurationMs)) {
        print('📡 Found: ${device.deviceName}');
        _discoveredDevices.add(device);
        yield device;
      }
    } catch (e) {
      print('❌ Streaming discovery error: $e');
    }
    
    print('✅ Discovery stream ended. Total found: ${_discoveredDevices.length}');
  }
  
  static Future<Device?> findPrinterByName(String deviceName) async {
    if (_discoveredDevices.isEmpty) {
      await discoverPrinters();
    }
    
    try {
      return _discoveredDevices.firstWhere(
        (device) => device.deviceName.toLowerCase().contains(deviceName.toLowerCase()),
      );
    } catch (e) {
      print('Printer "$deviceName" not found');
      return null;
    }
  }
  
  static List<Device> getDiscoveredDevices() => List.from(_discoveredDevices);
  
  static Future<bool> testPrinterConnection(Device device) async {
    final config = PrinterConfig(
      printerType: PrinterType.starMCLan,
      macAddresses: [device.mac],
    );
    
    final connected = await Thermis.checkPrinterConnection(config: config);
    print('${device.deviceName} connection: ${connected ? 'OK' : 'Failed'}');
    
    return connected ?? false;
  }
}
```

### Discovery UI Component

```dart
import 'package:flutter/material.dart';
import 'package:thermis/thermis.dart';

class PrinterDiscoveryScreen extends StatefulWidget {
  @override
  _PrinterDiscoveryScreenState createState() => _PrinterDiscoveryScreenState();
}

class _PrinterDiscoveryScreenState extends State<PrinterDiscoveryScreen> {
  List<Device> _devices = [];
  bool _isDiscovering = false;
  Device? _selectedDevice;
  
  Future<void> _startDiscovery() async {
    setState(() {
      _isDiscovering = true;
      _devices.clear();
    });
    
    try {
      final devices = await Thermis.getAvailableDevices(durationMs: 15000);
      setState(() {
        _devices = devices;
        _isDiscovering = false;
      });
    } catch (e) {
      setState(() => _isDiscovering = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Discovery failed: $e')),
      );
    }
  }
  
  Future<void> _testConnection(Device device) async {
    final config = PrinterConfig(
      printerType: PrinterType.starMCLan,
      macAddresses: [device.mac],
    );
    
    final connected = await Thermis.checkPrinterConnection(config: config);
    
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('${device.deviceName}: ${connected ? 'Connected' : 'Not connected'}'),
        backgroundColor: connected ? Colors.green : Colors.red,
      ),
    );
  }
  
  Future<void> _printTestReceipt(Device device) async {
    const testReceipt = '''
    {
      "brandName": "Test Print",
      "orderNo": "TEST-001",
      "receiptType": "customer",
      "deviceType": "pos",
      "items": [
        {
          "itemName": "Test Item",
          "quantity": "1",
          "price": "\\$0.00"
        }
      ],
      "breakdown": [
        {
          "key": "TOTAL",
          "value": "\\$0.00",
          "important": true
        }
      ]
    }
    ''';
    
    final config = PrinterConfig(
      printerType: PrinterType.starMCLan,
      macAddresses: [device.mac],
    );
    
    final result = await Thermis.printReceipt(testReceipt, config: config);
    
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Test print: ${result?.success == true ? 'Success' : 'Failed'}'),
        backgroundColor: result?.success == true ? Colors.green : Colors.red,
      ),
    );
  }
  
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Printer Discovery'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _isDiscovering ? null : _startDiscovery,
          ),
        ],
      ),
      body: Column(
        children: [
          if (_isDiscovering)
            const LinearProgressIndicator(),
          Expanded(
            child: _devices.isEmpty
                ? Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Icon(Icons.print, size: 64, color: Colors.grey),
                        const SizedBox(height: 16),
                        const Text('No printers found'),
                        const SizedBox(height: 16),
                        ElevatedButton(
                          onPressed: _isDiscovering ? null : _startDiscovery,
                          child: const Text('Start Discovery'),
                        ),
                      ],
                    ),
                  )
                : ListView.builder(
                    itemCount: _devices.length,
                    itemBuilder: (context, index) {
                      final device = _devices[index];
                      final isSelected = _selectedDevice == device;
                      
                      return Card(
                        margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                        child: ListTile(
                          leading: Icon(
                            Icons.print,
                            color: isSelected ? Colors.blue : Colors.grey,
                          ),
                          title: Text(device.deviceName),
                          subtitle: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text('MAC: ${device.mac}'),
                              Text('IP: ${device.ip}'),
                            ],
                          ),
                          trailing: PopupMenuButton<String>(
                            onSelected: (action) {
                              switch (action) {
                                case 'test':
                                  _testConnection(device);
                                  break;
                                case 'print':
                                  _printTestReceipt(device);
                                  break;
                                case 'select':
                                  setState(() => _selectedDevice = device);
                                  break;
                              }
                            },
                            itemBuilder: (context) => [
                              const PopupMenuItem(
                                value: 'test',
                                child: Text('Test Connection'),
                              ),
                              const PopupMenuItem(
                                value: 'print',
                                child: Text('Print Test'),
                              ),
                              const PopupMenuItem(
                                value: 'select',
                                child: Text('Select Printer'),
                              ),
                            ],
                          ),
                          selected: isSelected,
                          onTap: () => setState(() => _selectedDevice = device),
                        ),
                      );
                    },
                  ),
          ),
        ],
      ),
    );
  }
}
```

## Queue Management

### Queue Monitor Dashboard

```dart
import 'package:flutter/material.dart';
import 'package:thermis/thermis.dart';
import 'dart:async';

class QueueMonitorWidget extends StatefulWidget {
  @override
  _QueueMonitorWidgetState createState() => _QueueMonitorWidgetState();
}

class _QueueMonitorWidgetState extends State<QueueMonitorWidget> {
  int _totalQueueSize = 0;
  Map<String, int> _deviceQueues = {};
  Timer? _refreshTimer;
  
  @override
  void initState() {
    super.initState();
    _startMonitoring();
  }
  
  @override
  void dispose() {
    _refreshTimer?.cancel();
    super.dispose();
  }
  
  void _startMonitoring() {
    _refreshQueueStatus();
    _refreshTimer = Timer.periodic(const Duration(seconds: 2), (_) {
      _refreshQueueStatus();
    });
  }
  
  Future<void> _refreshQueueStatus() async {
    try {
      final totalSize = await Thermis.getQueueSize();
      final deviceSizes = await Thermis.getDeviceQueueSizes();
      
      setState(() {
        _totalQueueSize = totalSize ?? 0;
        _deviceQueues = deviceSizes ?? {};
      });
    } catch (e) {
      print('Error refreshing queue status: $e');
    }
  }
  
  Future<void> _clearAllQueues() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Clear All Queues'),
        content: const Text('Are you sure you want to clear all print queues?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Clear All'),
          ),
        ],
      ),
    );
    
    if (confirmed == true) {
      final success = await Thermis.clearPrintQueue();
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(success == true ? 'All queues cleared' : 'Failed to clear queues'),
          backgroundColor: success == true ? Colors.green : Colors.red,
        ),
      );
    }
  }
  
  Future<void> _clearDeviceQueue(String deviceKey) async {
    final success = await Thermis.clearDeviceQueue(deviceKey);
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(success == true ? 'Queue cleared' : 'Failed to clear queue'),
        backgroundColor: success == true ? Colors.green : Colors.red,
      ),
    );
  }
  
  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'Print Queue Status',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
                IconButton(
                  icon: const Icon(Icons.refresh),
                  onPressed: _refreshQueueStatus,
                ),
              ],
            ),
            const SizedBox(height: 16),
            
            // Total queue size
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: _totalQueueSize > 10 ? Colors.red.shade100 : Colors.green.shade100,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Row(
                children: [
                  Icon(
                    Icons.queue,
                    color: _totalQueueSize > 10 ? Colors.red : Colors.green,
                  ),
                  const SizedBox(width: 8),
                  Text(
                    'Total Jobs: $_totalQueueSize',
                    style: TextStyle(
                      fontWeight: FontWeight.bold,
                      color: _totalQueueSize > 10 ? Colors.red.shade800 : Colors.green.shade800,
                    ),
                  ),
                  const Spacer(),
                  if (_totalQueueSize > 0)
                    TextButton(
                      onPressed: _clearAllQueues,
                      child: const Text('Clear All'),
                    ),
                ],
              ),
            ),
            
            const SizedBox(height: 16),
            
            // Device queues
            if (_deviceQueues.isNotEmpty) ...[
              Text(
                'Device Queues',
                style: Theme.of(context).textTheme.titleMedium,
              ),
              const SizedBox(height: 8),
              ...(_deviceQueues.entries.map((entry) {
                final deviceKey = entry.key;
                final queueSize = entry.value;
                final deviceName = deviceKey.startsWith('STARMC_LAN_') 
                    ? 'LAN ${deviceKey.substring(11, 28)}...'
                    : deviceKey;
                
                return Container(
                  margin: const EdgeInsets.only(bottom: 8),
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    border: Border.all(color: Colors.grey.shade300),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Row(
                    children: [
                      Icon(
                        deviceKey.startsWith('USB') ? Icons.usb : Icons.wifi,
                        size: 16,
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(deviceName),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                        decoration: BoxDecoration(
                          color: queueSize > 5 ? Colors.orange : Colors.blue,
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Text(
                          '$queueSize',
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                      if (queueSize > 0)
                        IconButton(
                          icon: const Icon(Icons.clear, size: 16),
                          onPressed: () => _clearDeviceQueue(deviceKey),
                          tooltip: 'Clear this queue',
                        ),
                    ],
                  ),
                );
              })),
            ],
          ],
        ),
      ),
    );
  }
}
```

### Batch Printing with Queue Management

```dart
import 'package:thermis/thermis.dart';

class BatchPrintManager {
  static Future<void> printMultipleReceipts(
    List<String> receiptJsonList, {
    PrinterConfig? config,
    int maxConcurrent = 3,
  }) async {
    print('📦 Starting batch print of ${receiptJsonList.length} receipts');
    
    // Check initial queue status
    final initialQueueSize = await Thermis.getQueueSize() ?? 0;
    print('Initial queue size: $initialQueueSize');
    
    // If queue is too full, wait or clear
    if (initialQueueSize > 20) {
      print('⚠️ Queue is full, clearing old jobs');
      await Thermis.clearPrintQueue();
    }
    
    final results = <PrintResult?>[];
    
    // Process in batches to avoid overwhelming the queue
    for (int i = 0; i < receiptJsonList.length; i += maxConcurrent) {
      final batch = receiptJsonList.skip(i).take(maxConcurrent).toList();
      print('Processing batch ${(i ~/ maxConcurrent) + 1}/${(receiptJsonList.length / maxConcurrent).ceil()}');
      
      // Submit batch jobs
      final batchFutures = batch.map((receiptJson) => 
        Thermis.printReceipt(receiptJson, config: config)
      ).toList();
      
      // Wait for batch to complete
      final batchResults = await Future.wait(batchFutures);
      results.addAll(batchResults);
      
      // Log batch results
      final successCount = batchResults.where((r) => r?.success == true).length;
      print('Batch completed: $successCount/${batchResults.length} successful');
      
      // Brief pause between batches
      if (i + maxConcurrent < receiptJsonList.length) {
        await Future.delayed(const Duration(milliseconds: 500));
      }
    }
    
    // Final summary
    final totalSuccess = results.where((r) => r?.success == true).length;
    final totalRetryable = results.where((r) => r?.retryable == true).length;
    
    print('📊 Batch print summary:');
    print('  Total: ${receiptJsonList.length}');
    print('  Successful: $totalSuccess');
    print('  Failed: ${receiptJsonList.length - totalSuccess}');
    print('  Retryable: $totalRetryable');
    
    // Check final queue status
    final finalQueueSize = await Thermis.getQueueSize() ?? 0;
    print('Final queue size: $finalQueueSize');
  }
  
  static Future<Map<String, dynamic>> getQueueHealthStatus() async {
    final totalSize = await Thermis.getQueueSize() ?? 0;
    final deviceQueues = await Thermis.getDeviceQueueSizes() ?? {};
    
    final maxDeviceQueue = deviceQueues.values.isEmpty ? 0 : deviceQueues.values.reduce((a, b) => a > b ? a : b);
    final avgDeviceQueue = deviceQueues.values.isEmpty ? 0 : deviceQueues.values.reduce((a, b) => a + b) / deviceQueues.length;
    
    String healthStatus;
    if (totalSize == 0) {
      healthStatus = 'idle';
    } else if (totalSize < 5) {
      healthStatus = 'normal';
    } else if (totalSize < 15) {
      healthStatus = 'busy';
    } else {
      healthStatus = 'overloaded';
    }
    
    return {
      'totalSize': totalSize,
      'deviceQueues': deviceQueues,
      'maxDeviceQueue': maxDeviceQueue,
      'avgDeviceQueue': avgDeviceQueue.round(),
      'healthStatus': healthStatus,
      'recommendClearQueue': totalSize > 20,
    };
  }
}
```

## Error Handling

### Comprehensive Error Handler

```dart
import 'package:thermis/thermis.dart';

class PrintErrorHandler {
  static Future<bool> handlePrintWithRetry(
    String receiptJson, {
    PrinterConfig? config,
    int maxManualRetries = 2,
    Duration retryDelay = const Duration(seconds: 5),
  }) async {
    int attempts = 0;
    
    while (attempts <= maxManualRetries) {
      attempts++;
      print('🖨️ Print attempt $attempts/${maxManualRetries + 1}');
      
      final result = await Thermis.printReceipt(receiptJson, config: config);
      
      if (result?.success == true) {
        print('✅ Print successful on attempt $attempts');
        return true;
      }
      
      if (result == null) {
        print('❌ Platform error - print operation failed completely');
        return false;
      }
      
      print('❌ Print failed: ${result.reason?.displayName}');
      
      // Handle specific error types
      switch (result.reason) {
        case PrintFailureReason.printerBusy:
        case PrintFailureReason.deviceInUse:
          if (result.retryable && attempts <= maxManualRetries) {
            print('⏳ Printer busy, waiting before retry...');
            await Future.delayed(retryDelay);
            continue;
          }
          break;
          
        case PrintFailureReason.networkError:
        case PrintFailureReason.communicationError:
        case PrintFailureReason.timeoutError:
          if (result.retryable && attempts <= maxManualRetries) {
            print('🌐 Network issue, retrying...');
            await Future.delayed(retryDelay);
            continue;
          }
          break;
          
        case PrintFailureReason.printerOffline:
          print('📴 Printer offline - manual intervention required');
          await _showPrinterOfflineDialog();
          return false;
          
        case PrintFailureReason.outOfPaper:
          print('📄 Out of paper - manual intervention required');
          await _showOutOfPaperDialog();
          return false;
          
        case PrintFailureReason.coverOpen:
          print('🔓 Printer cover open - manual intervention required');
          await _showCoverOpenDialog();
          return false;
          
        case PrintFailureReason.printerNotFound:
          print('🔍 Printer not found - check configuration');
          return false;
          
        case PrintFailureReason.unknownError:
        default:
          print('❓ Unknown error: ${result.message}');
          if (attempts <= maxManualRetries) {
            await Future.delayed(retryDelay);
            continue;
          }
          break;
      }
      
      // If we get here, error is not retryable or max attempts reached
      break;
    }
    
    print('❌ Print failed after $attempts attempts');
    return false;
  }
  
  static Future<void> _showPrinterOfflineDialog() async {
    // In a real app, show UI dialog
    print('💡 Please check printer power and connection');
  }
  
  static Future<void> _showOutOfPaperDialog() async {
    // In a real app, show UI dialog
    print('💡 Please refill paper and try again');
  }
  
  static Future<void> _showCoverOpenDialog() async {
    // In a real app, show UI dialog
    print('💡 Please close printer cover and try again');
  }
  
  static String getErrorActionMessage(PrintFailureReason reason) {
    switch (reason) {
      case PrintFailureReason.printerBusy:
        return 'Printer is busy. Please wait and try again.';
      case PrintFailureReason.printerOffline:
        return 'Printer is offline. Check power and connection.';
      case PrintFailureReason.outOfPaper:
        return 'Printer is out of paper. Please refill and try again.';
      case PrintFailureReason.coverOpen:
        return 'Printer cover is open. Please close it and try again.';
      case PrintFailureReason.networkError:
        return 'Network error. Check your connection and try again.';
      case PrintFailureReason.printerNotFound:
        return 'Printer not found. Check configuration and connection.';
      case PrintFailureReason.deviceInUse:
        return 'Printer is being used by another application.';
      case PrintFailureReason.communicationError:
        return 'Communication error. Please try again.';
      case PrintFailureReason.timeoutError:
        return 'Operation timed out. Please try again.';
      case PrintFailureReason.unknownError:
      default:
        return 'An unknown error occurred. Please try again.';
    }
  }
  
  static bool shouldShowRetryButton(PrintResult result) {
    if (result.retryable) return true;
    
    // Some non-retryable errors might be resolved by user action
    switch (result.reason) {
      case PrintFailureReason.printerOffline:
      case PrintFailureReason.outOfPaper:
      case PrintFailureReason.coverOpen:
        return true;
      default:
        return false;
    }
  }
}
```

## Production Patterns

### Restaurant POS Integration

```dart
import 'package:thermis/thermis.dart';

class RestaurantPrintManager {
  // Printer configurations
  static final _counterPrinter = PrinterConfig(
    printerType: PrinterType.usbGeneric,
  );
  
  static final _kitchenPrinter = PrinterConfig(
    printerType: PrinterType.starMCLan,
    macAddresses: ['AA:BB:CC:DD:EE:FF'], // Kitchen printer MAC
  );
  
  static final _receiptPrinter = PrinterConfig(
    printerType: PrinterType.starMCLan,
    macAddresses: ['11:22:33:44:55:66'], // Receipt printer MAC
  );
  
  /// Process a complete order with all required prints
  static Future<OrderPrintResult> processOrder(Order order) async {
    final results = <String, PrintResult?>{};
    
    try {
      // 1. Print kitchen order (highest priority)
      if (order.hasKitchenItems) {
        final kitchenReceipt = _generateKitchenReceipt(order);
        results['kitchen'] = await Thermis.printReceipt(
          kitchenReceipt, 
          config: _kitchenPrinter,
        );
        print('Kitchen order sent: ${results['kitchen']?.success == true}');
      }
      
      // 2. Print customer receipt
      final customerReceipt = _generateCustomerReceipt(order);
      results['customer'] = await Thermis.printReceipt(
        customerReceipt,
        config: _receiptPrinter,
      );
      
      // 3. Open cash drawer if cash payment
      if (order.paymentMethod == PaymentMethod.cash) {
        final drawerOpened = await Thermis.openCashDrawer(config: _counterPrinter);
        print('Cash drawer: ${drawerOpened ? 'opened' : 'failed'}');
      }
      
      // 4. Print merchant copy if required
      if (order.requiresMerchantCopy) {
        final merchantReceipt = _generateMerchantReceipt(order);
        results['merchant'] = await Thermis.printReceipt(
          merchantReceipt,
          config: _counterPrinter,
        );
      }
      
      return OrderPrintResult(
        orderId: order.id,
        results: results,
        success: results.values.every((r) => r?.success == true),
      );
      
    } catch (e) {
      print('Error processing order prints: $e');
      return OrderPrintResult(
        orderId: order.id,
        results: results,
        success: false,
        error: e.toString(),
      );
    }
  }
  
  /// Handle end-of-day reports
  static Future<void> printEndOfDayReports({
    required SalesReport salesReport,
    required List<TipReport> tipReports,
  }) async {
    print('📊 Printing end-of-day reports...');
    
    // Sales summary
    final salesReceipt = _generateSalesReport(salesReport);
    final salesResult = await Thermis.printReceipt(salesReceipt, config: _counterPrinter);
    print('Sales report: ${salesResult?.success == true ? 'printed' : 'failed'}');
    
    // Tip reports for each server
    for (final tipReport in tipReports) {
      final tipReceipt = _generateTipReport(tipReport);
      final tipResult = await Thermis.printReceipt(tipReceipt, config: _counterPrinter);
      print('Tip report for ${tipReport.serverName}: ${tipResult?.success == true ? 'printed' : 'failed'}');
      
      // Small delay between reports
      await Future.delayed(const Duration(milliseconds: 500));
    }
    
    print('✅ End-of-day reports completed');
  }
  
  static String _generateKitchenReceipt(Order order) {
    return '''
    {
      "brandName": "${order.restaurantName}",
      "orderType": "Kitchen Order",
      "orderNo": "${order.number}",
      "tableNo": "${order.tableNumber}",
      "receiptType": "kitchen",
      "deviceType": "pos",
      "timeOfOrder": "Order Time: ${order.timestamp}",
      "items": ${_formatKitchenItems(order.items)},
      "specialInstructions": "${order.specialInstructions}"
    }
    ''';
  }
  
  static String _generateCustomerReceipt(Order order) {
    return '''
    {
      "brandName": "${order.restaurantName}",
      "orderType": "${order.orderType}",
      "orderNo": "${order.number}",
      "tableNo": "${order.tableNumber}",
      "receiptType": "customer",
      "deviceType": "pos",
      "timeOfOrder": "Order Time: ${order.timestamp}",
      "items": ${_formatReceiptItems(order.items)},
      "breakdown": ${_formatPriceBreakdown(order)},
      "paymentMethod": "${order.paymentMethod.name}"
    }
    ''';
  }
  
  // ... other helper methods
}

class OrderPrintResult {
  final String orderId;
  final Map<String, PrintResult?> results;
  final bool success;
  final String? error;
  
  OrderPrintResult({
    required this.orderId,
    required this.results,
    required this.success,
    this.error,
  });
}
```

### Error Recovery Service

```dart
import 'package:thermis/thermis.dart';
import 'dart:async';

class PrintErrorRecoveryService {
  static final _failedJobs = <FailedPrintJob>[];
  static Timer? _recoveryTimer;
  
  static void startRecoveryService() {
    _recoveryTimer = Timer.periodic(const Duration(minutes: 2), (_) {
      _attemptRecovery();
    });
  }
  
  static void stopRecoveryService() {
    _recoveryTimer?.cancel();
  }
  
  static void addFailedJob(String receiptJson, PrinterConfig? config, PrintResult result) {
    if (result.retryable) {
      _failedJobs.add(FailedPrintJob(
        receiptJson: receiptJson,
        config: config,
        failureReason: result.reason!,
        timestamp: DateTime.now(),
        retryCount: 0,
      ));
      print('📝 Added failed job to recovery queue (${_failedJobs.length} total)');
    }
  }
  
  static Future<void> _attemptRecovery() async {
    if (_failedJobs.isEmpty) return;
    
    print('🔄 Attempting recovery for ${_failedJobs.length} failed jobs');
    
    final jobsToRetry = _failedJobs.where((job) => 
      job.retryCount < 3 && 
      DateTime.now().difference(job.timestamp).inMinutes >= (job.retryCount + 1) * 2
    ).toList();
    
    for (final job in jobsToRetry) {
      print('🔄 Retrying job: ${job.failureReason.name}');
      
      final result = await Thermis.printReceipt(job.receiptJson, config: job.config);
      
      if (result?.success == true) {
        print('✅ Recovery successful');
        _failedJobs.remove(job);
      } else {
        job.retryCount++;
        job.timestamp = DateTime.now();
        
        if (job.retryCount >= 3) {
          print('❌ Job failed permanently after 3 retries');
          _failedJobs.remove(job);
        }
      }
    }
    
    // Clean up old jobs
    _failedJobs.removeWhere((job) => 
      DateTime.now().difference(job.timestamp).inHours > 24
    );
  }
  
  static List<FailedPrintJob> getFailedJobs() => List.from(_failedJobs);
  
  static void clearFailedJobs() {
    _failedJobs.clear();
    print('🧹 Cleared all failed jobs');
  }
}

class FailedPrintJob {
  final String receiptJson;
  final PrinterConfig? config;
  final PrintFailureReason failureReason;
  DateTime timestamp;
  int retryCount;
  
  FailedPrintJob({
    required this.receiptJson,
    required this.config,
    required this.failureReason,
    required this.timestamp,
    required this.retryCount,
  });
}
```

This comprehensive examples document provides real-world usage patterns for the Thermis plugin, covering everything from basic printing to advanced production scenarios with error recovery and queue management. 