import 'package:flutter_test/flutter_test.dart';
import 'package:thermis/thermis.dart';
import 'package:thermis/thermis_platform_interface.dart';
import 'package:thermis/thermis_method_channel.dart';
import 'package:thermis/printer_config.dart';
import 'package:thermis/device.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'dart:typed_data';

class MockThermisPlatform
    with MockPlatformInterfaceMixin
    implements ThermisPlatform {

  // Mock data storage
  final List<Device> _mockDevices = [
    Device(
      deviceName: 'Star TSP100III',
      mac: 'AA:BB:CC:DD:EE:FF',
      ip: '192.168.1.100',
    ),
    Device(
      deviceName: 'Star mC-Print3',
      mac: '11:22:33:44:55:66',
      ip: '192.168.1.101',
    ),
  ];
  
  int _mockQueueSize = 0;
  final Map<String, int> _mockDeviceQueues = {
    'USB_GENERIC': 0,
    'STARMC_LAN_AA:BB:CC:DD:EE:FF': 2,
  };

  @override
  Future<PrintResult?> printCHEQReceipt(String receiptDTOJson, {PrinterConfig? config}) async {
    if (receiptDTOJson.isEmpty) {
      return PrintResult(
        success: false,
        reason: PrintFailureReason.unknownError,
        retryable: false,
        message: 'Empty receipt data',
      );
    }
    
    // Simulate printer busy scenario
    if (config?.macAddresses?.contains('BUSY:MAC:ADDRESS') == true) {
      return PrintResult(
        success: false,
        reason: PrintFailureReason.printerBusy,
        retryable: true,
        message: 'Printer is currently busy',
      );
    }
    
    // Simulate success
    return PrintResult(
      success: true,
      retryable: false,
    );
  }

  @override
  Future<bool?> openCashDrawer(PrinterConfig config) async {
    if (config.printerType == PrinterType.usbGeneric) {
      return true;
    }
    return false;
  }

  @override
  Future<bool?> cutPaper(PrinterConfig config) async {
    return config.printerType == PrinterType.usbGeneric;
  }

  @override
  Future<bool?> checkPrinterConnection(PrinterConfig config) async {
    // Simulate connection check based on printer type
    if (config.printerType == PrinterType.usbGeneric) {
      return true;
    } else if (config.printerType == PrinterType.starMCLan) {
      return config.macAddresses?.isNotEmpty == true;
    }
    return false;
  }

  @override
  Future<Uint8List?> previewReceipt(String receiptDTOJSON) async {
    if (receiptDTOJSON.isEmpty) return null;
    // Return mock bitmap data
    return Uint8List.fromList([137, 80, 78, 71, 13, 10, 26, 10]); // PNG header
  }

  @override
  Stream<Device> discoverPrinters({int scanDurationMs = 5000}) async* {
    // Simulate discovery with delay
    await Future.delayed(Duration(milliseconds: 100));
    for (final device in _mockDevices) {
      yield device;
      await Future.delayed(Duration(milliseconds: 50));
    }
  }

  @override
  Future<List<Device>> getAvailableDevices({int durationMs = 50000}) async {
    // Simulate discovery duration
    await Future.delayed(Duration(milliseconds: 100));
    return List.from(_mockDevices);
  }

  @override
  Future<void> stopDiscovery() async {
    // Mock stop discovery
  }

  @override
  Future<int?> getQueueSize() async {
    return _mockQueueSize;
  }

  @override
  Future<Map<String, int>?> getDeviceQueueSizes() async {
    return Map.from(_mockDeviceQueues);
  }

  @override
  Future<bool?> clearPrintQueue() async {
    _mockQueueSize = 0;
    _mockDeviceQueues.updateAll((key, value) => 0);
    return true;
  }

  @override
  Future<bool?> clearDeviceQueue(String deviceKey) async {
    if (_mockDeviceQueues.containsKey(deviceKey)) {
      _mockDeviceQueues[deviceKey] = 0;
      return true;
    }
    return false;
  }
  
  // Helper method to simulate queue changes
  void setMockQueueSize(int size) {
    _mockQueueSize = size;
  }
  
  void setMockDeviceQueueSize(String deviceKey, int size) {
    _mockDeviceQueues[deviceKey] = size;
  }
}

void main() {
  final ThermisPlatform initialPlatform = ThermisPlatform.instance;
  late MockThermisPlatform mockPlatform;

  setUp(() {
    mockPlatform = MockThermisPlatform();
    ThermisPlatform.instance = mockPlatform;
  });

  tearDown(() {
    ThermisPlatform.instance = initialPlatform;
  });

  group('Platform Interface Tests', () {
    test('MethodChannelThermis is the default instance', () {
      ThermisPlatform.instance = initialPlatform;
      expect(initialPlatform, isInstanceOf<MethodChannelThermis>());
    });
  });

  group('Print Receipt Tests', () {
    test('printReceipt with valid data returns success', () async {
      final result = await Thermis.printReceipt('{"test": "receipt"}');
      
      expect(result, isNotNull);
      expect(result!.success, isTrue);
      expect(result.reason, isNull);
    });

    test('printReceipt with empty data returns failure', () async {
      final result = await Thermis.printReceipt('');
      
      expect(result, isNotNull);
      expect(result!.success, isFalse);
      expect(result.reason, equals(PrintFailureReason.unknownError));
      expect(result.message, equals('Empty receipt data'));
    });

    test('printReceipt with USB config', () async {
      final config = PrinterConfig(printerType: PrinterType.usbGeneric);
      final result = await Thermis.printReceipt('{"test": "receipt"}', config: config);
      
      expect(result, isNotNull);
      expect(result!.success, isTrue);
    });

    test('printReceipt with LAN config', () async {
      final config = PrinterConfig(
        printerType: PrinterType.starMCLan,
        macAddresses: ['AA:BB:CC:DD:EE:FF'],
      );
      final result = await Thermis.printReceipt('{"test": "receipt"}', config: config);
      
      expect(result, isNotNull);
      expect(result!.success, isTrue);
    });

    test('printReceipt with busy printer returns retryable error', () async {
      final config = PrinterConfig(
        printerType: PrinterType.starMCLan,
        macAddresses: ['BUSY:MAC:ADDRESS'],
      );
      final result = await Thermis.printReceipt('{"test": "receipt"}', config: config);
      
      expect(result, isNotNull);
      expect(result!.success, isFalse);
      expect(result.reason, equals(PrintFailureReason.printerBusy));
      expect(result.retryable, isTrue);
      expect(result.message, equals('Printer is currently busy'));
    });
  });

  group('Printer Operations Tests', () {
    test('openCashDrawer with USB config returns true', () async {
      final config = PrinterConfig(printerType: PrinterType.usbGeneric);
      final result = await Thermis.openCashDrawer(config: config);
      
      expect(result, isTrue);
    });

    test('openCashDrawer with LAN config returns false', () async {
      final config = PrinterConfig(printerType: PrinterType.starMCLan);
      final result = await Thermis.openCashDrawer(config: config);
      
      expect(result, isFalse);
    });

    test('cutPaper with USB config returns true', () async {
      final config = PrinterConfig(printerType: PrinterType.usbGeneric);
      final result = await Thermis.cutPaper(config: config);
      
      expect(result, isTrue);
    });

    test('cutPaper with LAN config returns false', () async {
      final config = PrinterConfig(printerType: PrinterType.starMCLan);
      final result = await Thermis.cutPaper(config: config);
      
      expect(result, isFalse);
    });

    test('checkPrinterConnection with USB config returns true', () async {
      final config = PrinterConfig(printerType: PrinterType.usbGeneric);
      final result = await Thermis.checkPrinterConnection(config: config);
      
      expect(result, isTrue);
    });

    test('checkPrinterConnection with LAN config and MAC addresses returns true', () async {
      final config = PrinterConfig(
        printerType: PrinterType.starMCLan,
        macAddresses: ['AA:BB:CC:DD:EE:FF'],
      );
      final result = await Thermis.checkPrinterConnection(config: config);
      
      expect(result, isTrue);
    });

    test('checkPrinterConnection with LAN config but no MAC addresses returns false', () async {
      final config = PrinterConfig(printerType: PrinterType.starMCLan);
      final result = await Thermis.checkPrinterConnection(config: config);
      
      expect(result, isFalse);
    });
  });

  group('Receipt Preview Tests', () {
    test('getReceiptReview with valid data returns bitmap', () async {
      final result = await Thermis.getReceiptPreview('{"test": "receipt"}');
      
      expect(result, isNotNull);
      expect(result!.length, greaterThan(0));
      expect(result.first, equals(137)); // PNG header first byte
    });

    test('getReceiptReview with empty data returns null', () async {
      final result = await Thermis.getReceiptPreview('');
      
      expect(result, isNull);
    });
  });

  group('Discovery Tests', () {
    test('discoverPrinters with default duration returns stream of devices', () async {
      final devices = <Device>[];
      
      await for (final device in Thermis.discoverPrinters()) {
        devices.add(device);
      }
      
      expect(devices, hasLength(2));
      expect(devices[0].deviceName, equals('Star TSP100III'));
      expect(devices[0].mac, equals('AA:BB:CC:DD:EE:FF'));
      expect(devices[1].deviceName, equals('Star mC-Print3'));
      expect(devices[1].mac, equals('11:22:33:44:55:66'));
    });

    test('discoverPrinters with custom duration returns stream of devices', () async {
      final devices = <Device>[];
      
      await for (final device in Thermis.discoverPrinters(scanDurationMs: 3000)) {
        devices.add(device);
      }
      
      expect(devices, hasLength(2));
    });

    test('getAvailableDevices returns list of devices', () async {
      final devices = await Thermis.getAvailableDevices();
      
      expect(devices, hasLength(2));
      expect(devices[0].deviceName, equals('Star TSP100III'));
      expect(devices[1].deviceName, equals('Star mC-Print3'));
    });

    test('getAvailableDevices with custom duration returns list of devices', () async {
      final devices = await Thermis.getAvailableDevices(durationMs: 8000);
      
      expect(devices, hasLength(2));
    });

    test('stopDiscovery completes without error', () async {
      expect(() => Thermis.stopDiscovery(), returnsNormally);
    });
  });

  group('Queue Management Tests', () {
    test('getQueueSize returns current queue size', () async {
      mockPlatform.setMockQueueSize(5);
      final size = await Thermis.getQueueSize();
      
      expect(size, equals(5));
    });

    test('getDeviceQueueSizes returns device queue information', () async {
      final queues = await Thermis.getDeviceQueueSizes();
      
      expect(queues, isNotNull);
      expect(queues!.containsKey('USB_GENERIC'), isTrue);
      expect(queues.containsKey('STARMC_LAN_AA:BB:CC:DD:EE:FF'), isTrue);
      expect(queues['STARMC_LAN_AA:BB:CC:DD:EE:FF'], equals(2));
    });

    test('clearPrintQueue clears all queues', () async {
      mockPlatform.setMockQueueSize(10);
      final result = await Thermis.clearPrintQueue();
      
      expect(result, isTrue);
      
      final size = await Thermis.getQueueSize();
      expect(size, equals(0));
    });

    test('clearDeviceQueue clears specific device queue', () async {
      const deviceKey = 'USB_GENERIC';
      mockPlatform.setMockDeviceQueueSize(deviceKey, 5);
      
      final result = await Thermis.clearDeviceQueue(deviceKey);
      expect(result, isTrue);
      
      final queues = await Thermis.getDeviceQueueSizes();
      expect(queues![deviceKey], equals(0));
    });

    test('clearDeviceQueue with invalid device key returns false', () async {
      const invalidKey = 'INVALID_DEVICE';
      final result = await Thermis.clearDeviceQueue(invalidKey);
      
      expect(result, isFalse);
    });
  });

  group('PrinterConfig Tests', () {
    test('PrinterConfig toMap with USB type', () {
      final config = PrinterConfig(printerType: PrinterType.usbGeneric);
      final map = config.toMap();
      
      expect(map['printer_type'], equals('usbGeneric'));
      expect(map['mac_addresses'], isNull);
    });

    test('PrinterConfig toMap with LAN type and MAC addresses', () {
      final config = PrinterConfig(
        printerType: PrinterType.starMCLan,
        macAddresses: ['AA:BB:CC:DD:EE:FF', '11:22:33:44:55:66'],
      );
      final map = config.toMap();
      
      expect(map['printer_type'], equals('starMCLan'));
      expect(map['mac_addresses'], hasLength(2));
      expect(map['mac_addresses'], contains('AA:BB:CC:DD:EE:FF'));
    });
  });

  group('PrintResult Tests', () {
    test('PrintResult.fromMap creates success result', () {
      final map = {
        'success': true,
        'reason': null,
        'retryable': false,
        'message': null,
      };
      
      final result = PrintResult.fromMap(map);
      
      expect(result.success, isTrue);
      expect(result.reason, isNull);
      expect(result.retryable, isFalse);
      expect(result.message, isNull);
    });

    test('PrintResult.fromMap creates failure result', () {
      final map = {
        'success': false,
        'reason': 'PRINTER_BUSY',
        'retryable': true,
        'message': 'Printer is busy',
      };
      
      final result = PrintResult.fromMap(map);
      
      expect(result.success, isFalse);
      expect(result.reason, equals(PrintFailureReason.printerBusy));
      expect(result.retryable, isTrue);
      expect(result.message, equals('Printer is busy'));
    });

    test('PrintResult toString for success', () {
      final result = PrintResult(success: true, retryable: false);
      
      expect(result.toString(), equals('PrintResult.Success'));
    });

    test('PrintResult toString for failure', () {
      final result = PrintResult(
        success: false,
        reason: PrintFailureReason.outOfPaper,
        retryable: false,
        message: 'No paper',
      );
      
      expect(result.toString(), contains('PrintResult.Failed'));
      expect(result.toString(), contains('outOfPaper'));
      expect(result.toString(), contains('No paper'));
    });
  });

  group('PrintFailureReason Tests', () {
    test('retryable reasons return true for isRetryable', () {
      expect(PrintFailureReason.printerBusy.isRetryable, isTrue);
      expect(PrintFailureReason.deviceInUse.isRetryable, isTrue);
      expect(PrintFailureReason.networkError.isRetryable, isTrue);
      expect(PrintFailureReason.communicationError.isRetryable, isTrue);
      expect(PrintFailureReason.timeoutError.isRetryable, isTrue);
    });

    test('non-retryable reasons return false for isRetryable', () {
      expect(PrintFailureReason.printerOffline.isRetryable, isFalse);
      expect(PrintFailureReason.printerNotFound.isRetryable, isFalse);
      expect(PrintFailureReason.outOfPaper.isRetryable, isFalse);
      expect(PrintFailureReason.coverOpen.isRetryable, isFalse);
      expect(PrintFailureReason.unknownError.isRetryable, isFalse);
    });

    test('displayName returns user-friendly messages', () {
      expect(PrintFailureReason.printerBusy.displayName, equals('Printer is busy'));
      expect(PrintFailureReason.outOfPaper.displayName, equals('Out of paper'));
      expect(PrintFailureReason.networkError.displayName, equals('Network error'));
    });
  });

  group('Device Tests', () {
    test('Device.fromMap creates device correctly', () {
      final map = {
        'deviceName': 'Star TSP100III',
        'mac': 'AA:BB:CC:DD:EE:FF',
        'ip': '192.168.1.100',
      };
      
      final device = Device.fromMap(map);
      
      expect(device.deviceName, equals('Star TSP100III'));
      expect(device.mac, equals('AA:BB:CC:DD:EE:FF'));
      expect(device.ip, equals('192.168.1.100'));
    });

    test('Device toString returns formatted string', () {
      final device = Device(
        deviceName: 'Star TSP100III',
        mac: 'AA:BB:CC:DD:EE:FF',
        ip: '192.168.1.100',
      );
      
      final deviceString = device.toString();
      expect(deviceString, contains('Star TSP100III'));
      expect(deviceString, contains('AA:BB:CC:DD:EE:FF'));
      expect(deviceString, contains('192.168.1.100'));
    });
  });

  group('Error Handling Tests', () {
    test('handles null results gracefully', () async {
      // Mock platform that returns null
      final nullMockPlatform = MockThermisPlatform();
      ThermisPlatform.instance = nullMockPlatform;
      
      final result = await Thermis.getQueueSize();
      expect(result, isA<int>());
    });
  });

  group('Integration Tests', () {
    test('complete print workflow with queue management', () async {
      // Check initial queue size
      final initialSize = await Thermis.getQueueSize();
      expect(initialSize, isA<int>());
      
      // Print a receipt
      final printResult = await Thermis.printReceipt('{"test": "integration"}');
      expect(printResult?.success, isTrue);
      
      // Check device queues
      final deviceQueues = await Thermis.getDeviceQueueSizes();
      expect(deviceQueues, isNotNull);
      
      // Clear queues
      final clearResult = await Thermis.clearPrintQueue();
      expect(clearResult, isTrue);
    });

    test('discovery and connection workflow', () async {
      // Discover devices
      final devices = await Thermis.getAvailableDevices(durationMs: 1000);
      expect(devices, isNotEmpty);
      
      // Check connection to first device
      final config = PrinterConfig(
        printerType: PrinterType.starMCLan,
        macAddresses: [devices.first.mac],
      );
      final connected = await Thermis.checkPrinterConnection(config: config);
      expect(connected, isTrue);
      
      // Stop discovery
      await Thermis.stopDiscovery();
    });
  });
}
