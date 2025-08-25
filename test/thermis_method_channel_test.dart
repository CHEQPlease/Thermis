import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:thermis/thermis_method_channel.dart';
import 'package:thermis/printer_config.dart';
import 'dart:typed_data';

void main() {
  MethodChannelThermis platform = MethodChannelThermis();
  const MethodChannel methodChannel = MethodChannel('thermis');
  const EventChannel eventChannel = EventChannel('thermis/starmc_discovery');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    // Mock method channel responses
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(methodChannel, (MethodCall methodCall) async {
      switch (methodCall.method) {
        case 'print_cheq_receipt':
          return {
            'success': true,
            'reason': null,
            'retryable': false,
            'message': null,
          };
        case 'open_cash_drawer':
          return true;
        case 'cut_paper':
          return true;
        case 'check_printer_connection':
          return true;
        case 'get_receipt_preview':
          return Uint8List.fromList([137, 80, 78, 71, 13, 10, 26, 10]);
        case 'get_available_devices':
          return [
            {
              'deviceName': 'Star TSP100III',
              'mac': 'AA:BB:CC:DD:EE:FF',
              'ip': '192.168.1.100',
            },
            {
              'deviceName': 'Star mC-Print3',
              'mac': '11:22:33:44:55:66',
              'ip': '192.168.1.101',
            },
          ];
        case 'stop_discovery':
          return null;
        case 'get_queue_size':
          return 5;
        case 'get_device_queue_sizes':
          return {
            'USB_GENERIC': 0,
            'STARMC_LAN_AA:BB:CC:DD:EE:FF': 2,
          };
        case 'clear_print_queue':
          return true;
        case 'clear_device_queue':
          return true;
        default:
          throw PlatformException(code: 'UNIMPLEMENTED');
      }
    });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(methodChannel, null);
  });

  group('Method Channel Print Tests', () {
    test('printCHEQReceipt with default config', () async {
      final result = await platform.printCHEQReceipt('{"test": "receipt"}');
      
      expect(result, isNotNull);
      expect(result!.success, isTrue);
      expect(result.reason, isNull);
    });

    test('printCHEQReceipt with USB config', () async {
      final config = PrinterConfig(printerType: PrinterType.usbGeneric);
      final result = await platform.printCHEQReceipt('{"test": "receipt"}', config: config);
      
      expect(result, isNotNull);
      expect(result!.success, isTrue);
    });

    test('printCHEQReceipt with LAN config', () async {
      final config = PrinterConfig(
        printerType: PrinterType.starMCLan,
        macAddresses: ['AA:BB:CC:DD:EE:FF'],
      );
      final result = await platform.printCHEQReceipt('{"test": "receipt"}', config: config);
      
      expect(result, isNotNull);
      expect(result!.success, isTrue);
    });
  });

  group('Method Channel Printer Operations Tests', () {
    test('openCashDrawer', () async {
      final config = PrinterConfig(printerType: PrinterType.usbGeneric);
      final result = await platform.openCashDrawer(config);
      
      expect(result, isTrue);
    });

    test('cutPaper', () async {
      final config = PrinterConfig(printerType: PrinterType.usbGeneric);
      final result = await platform.cutPaper(config);
      
      expect(result, isTrue);
    });

    test('checkPrinterConnection', () async {
      final config = PrinterConfig(printerType: PrinterType.usbGeneric);
      final result = await platform.checkPrinterConnection(config);
      
      expect(result, isTrue);
    });

    test('previewReceipt', () async {
      final result = await platform.previewReceipt('{"test": "receipt"}');
      
      expect(result, isNotNull);
      expect(result!.length, greaterThan(0));
      expect(result.first, equals(137)); // PNG header
    });
  });

  group('Method Channel Discovery Tests', () {
    test('getAvailableDevices with default duration', () async {
      final devices = await platform.getAvailableDevices();
      
      expect(devices, hasLength(2));
      expect(devices[0].deviceName, equals('Star TSP100III'));
      expect(devices[0].mac, equals('AA:BB:CC:DD:EE:FF'));
      expect(devices[1].deviceName, equals('Star mC-Print3'));
    });

    test('getAvailableDevices with custom duration', () async {
      final devices = await platform.getAvailableDevices(durationMs: 8000);
      
      expect(devices, hasLength(2));
    });

    test('stopDiscovery', () async {
      await expectLater(platform.stopDiscovery(), completes);
    });
  });

  group('Method Channel Queue Management Tests', () {
    test('getQueueSize', () async {
      final size = await platform.getQueueSize();
      
      expect(size, equals(5));
    });

    test('getDeviceQueueSizes', () async {
      final queues = await platform.getDeviceQueueSizes();
      
      expect(queues, isNotNull);
      expect(queues!.containsKey('USB_GENERIC'), isTrue);
      expect(queues['STARMC_LAN_AA:BB:CC:DD:EE:FF'], equals(2));
    });

    test('clearPrintQueue', () async {
      final result = await platform.clearPrintQueue();
      
      expect(result, isTrue);
    });

    test('clearDeviceQueue', () async {
      final result = await platform.clearDeviceQueue('USB_GENERIC');
      
      expect(result, isTrue);
    });
  });

  group('Method Channel Error Handling Tests', () {
    test('handles method call exceptions', () async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(methodChannel, (MethodCall methodCall) async {
        throw PlatformException(code: 'TEST_ERROR', message: 'Test error message');
      });

      expect(
        () => platform.getQueueSize(),
        throwsA(isA<PlatformException>()),
      );
    });

    test('handles null responses gracefully', () async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(methodChannel, (MethodCall methodCall) async {
        return null;
      });

      final result = await platform.printCHEQReceipt('{"test": "receipt"}');
      expect(result, isNull);
    });
  });

  group('Method Channel Data Type Tests', () {
    test('handles various data types correctly', () async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(methodChannel, (MethodCall methodCall) async {
        switch (methodCall.method) {
          case 'get_queue_size':
            return 42;
          case 'get_device_queue_sizes':
            return <String, int>{'device1': 1, 'device2': 2};
          case 'clear_print_queue':
            return true;
          case 'get_available_devices':
            return <Map<String, dynamic>>[
              {'deviceName': 'Test', 'mac': 'AA:BB:CC', 'ip': '192.168.1.1'}
            ];
          default:
            return null;
        }
      });

      final queueSize = await platform.getQueueSize();
      expect(queueSize, isA<int>());
      expect(queueSize, equals(42));

      final deviceQueues = await platform.getDeviceQueueSizes();
      expect(deviceQueues, isA<Map<String, int>>());
      expect(deviceQueues!['device1'], equals(1));

      final clearResult = await platform.clearPrintQueue();
      expect(clearResult, isA<bool>());
      expect(clearResult, isTrue);

      final devices = await platform.getAvailableDevices();
      expect(devices, isA<List>());
      expect(devices.first.deviceName, equals('Test'));
    });
  });
}
