import 'package:flutter_test/flutter_test.dart';
import 'package:thermis/printer_config.dart';

void main() {
  group('PrinterConfig Tests', () {
    test('PrinterConfig constructor with USB type', () {
      final config = PrinterConfig(printerType: PrinterType.usbGeneric);
      
      expect(config.printerType, equals(PrinterType.usbGeneric));
      expect(config.macAddresses, isNull);
    });

    test('PrinterConfig constructor with LAN type and MAC addresses', () {
      final macAddresses = ['AA:BB:CC:DD:EE:FF', '11:22:33:44:55:66'];
      final config = PrinterConfig(
        printerType: PrinterType.starMCLan,
        macAddresses: macAddresses,
      );
      
      expect(config.printerType, equals(PrinterType.starMCLan));
      expect(config.macAddresses, equals(macAddresses));
      expect(config.macAddresses!.length, equals(2));
    });

    test('PrinterConfig toMap with USB type', () {
      final config = PrinterConfig(printerType: PrinterType.usbGeneric);
      final map = config.toMap();
      
      expect(map['printer_type'], equals('usbGeneric'));
      expect(map['mac_addresses'], isNull);
    });

    test('PrinterConfig toMap with LAN type and MAC addresses', () {
      final macAddresses = ['AA:BB:CC:DD:EE:FF', '11:22:33:44:55:66'];
      final config = PrinterConfig(
        printerType: PrinterType.starMCLan,
        macAddresses: macAddresses,
      );
      final map = config.toMap();
      
      expect(map['printer_type'], equals('starMCLan'));
      expect(map['mac_addresses'], equals(macAddresses));
      expect(map['mac_addresses'], hasLength(2));
      expect(map['mac_addresses'], contains('AA:BB:CC:DD:EE:FF'));
      expect(map['mac_addresses'], contains('11:22:33:44:55:66'));
    });

    test('PrinterConfig toMap with empty MAC addresses list', () {
      final config = PrinterConfig(
        printerType: PrinterType.starMCLan,
        macAddresses: [],
      );
      final map = config.toMap();
      
      expect(map['printer_type'], equals('starMCLan'));
      expect(map['mac_addresses'], isEmpty);
    });

    test('PrinterConfig with single MAC address', () {
      final config = PrinterConfig(
        printerType: PrinterType.starMCLan,
        macAddresses: ['AA:BB:CC:DD:EE:FF'],
      );
      
      expect(config.macAddresses!.length, equals(1));
      expect(config.macAddresses!.first, equals('AA:BB:CC:DD:EE:FF'));
    });

    test('PrinterConfig with multiple duplicate MAC addresses', () {
      final macAddresses = [
        'AA:BB:CC:DD:EE:FF',
        'AA:BB:CC:DD:EE:FF',
        'AA:BB:CC:DD:EE:FF'
      ];
      final config = PrinterConfig(
        printerType: PrinterType.starMCLan,
        macAddresses: macAddresses,
      );
      
      expect(config.macAddresses, equals(macAddresses));
      expect(config.macAddresses!.length, equals(3));
    });
  });

  group('PrinterType Tests', () {
    test('PrinterType enum values', () {
      expect(PrinterType.values, hasLength(2));
      expect(PrinterType.values, contains(PrinterType.usbGeneric));
      expect(PrinterType.values, contains(PrinterType.starMCLan));
    });

    test('PrinterType name property', () {
      expect(PrinterType.usbGeneric.name, equals('usbGeneric'));
      expect(PrinterType.starMCLan.name, equals('starMCLan'));
    });

    test('PrinterType toString', () {
      expect(PrinterType.usbGeneric.toString(), contains('usbGeneric'));
      expect(PrinterType.starMCLan.toString(), contains('starMCLan'));
    });
  });

  group('PrintResult Tests', () {
    test('PrintResult success constructor', () {
      final result = PrintResult(
        success: true,
        retryable: false,
      );
      
      expect(result.success, isTrue);
      expect(result.reason, isNull);
      expect(result.retryable, isFalse);
      expect(result.message, isNull);
    });

    test('PrintResult failure constructor', () {
      final result = PrintResult(
        success: false,
        reason: PrintFailureReason.printerBusy,
        retryable: true,
        message: 'Printer is currently busy',
      );
      
      expect(result.success, isFalse);
      expect(result.reason, equals(PrintFailureReason.printerBusy));
      expect(result.retryable, isTrue);
      expect(result.message, equals('Printer is currently busy'));
    });

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

    test('PrintResult.fromMap handles unknown reason', () {
      final map = {
        'success': false,
        'reason': 'UNKNOWN_REASON_TYPE',
        'retryable': false,
        'message': 'Unknown error',
      };
      
      final result = PrintResult.fromMap(map);
      
      expect(result.success, isFalse);
      expect(result.reason, equals(PrintFailureReason.unknownError));
      expect(result.retryable, isFalse);
      expect(result.message, equals('Unknown error'));
    });

    test('PrintResult.fromMap handles missing fields', () {
      final map = <String, dynamic>{
        'success': false,
      };
      
      final result = PrintResult.fromMap(map);
      
      expect(result.success, isFalse);
      expect(result.reason, isNull);
      expect(result.retryable, isFalse);
      expect(result.message, isNull);
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
        message: 'No paper available',
      );
      
      final resultString = result.toString();
      expect(resultString, contains('PrintResult.Failed'));
      expect(resultString, contains('outOfPaper'));
      expect(resultString, contains('retryable: false'));
      expect(resultString, contains('No paper available'));
    });
  });

  group('PrintFailureReason Tests', () {
    test('PrintFailureReason enum values', () {
      expect(PrintFailureReason.values, hasLength(10));
      expect(PrintFailureReason.values, contains(PrintFailureReason.printerBusy));
      expect(PrintFailureReason.values, contains(PrintFailureReason.unknownError));
    });

    test('retryable reasons return true for isRetryable', () {
      final retryableReasons = [
        PrintFailureReason.printerBusy,
        PrintFailureReason.deviceInUse,
        PrintFailureReason.networkError,
        PrintFailureReason.communicationError,
        PrintFailureReason.timeoutError,
      ];
      
      for (final reason in retryableReasons) {
        expect(reason.isRetryable, isTrue, reason: 'Expected ${reason.name} to be retryable');
      }
    });

    test('non-retryable reasons return false for isRetryable', () {
      final nonRetryableReasons = [
        PrintFailureReason.printerOffline,
        PrintFailureReason.printerNotFound,
        PrintFailureReason.outOfPaper,
        PrintFailureReason.coverOpen,
        PrintFailureReason.unknownError,
      ];
      
      for (final reason in nonRetryableReasons) {
        expect(reason.isRetryable, isFalse, reason: 'Expected ${reason.name} to be non-retryable');
      }
    });

    test('displayName returns user-friendly messages', () {
      final expectedDisplayNames = {
        PrintFailureReason.printerBusy: 'Printer is busy',
        PrintFailureReason.printerOffline: 'Printer is offline',
        PrintFailureReason.printerNotFound: 'Printer not found',
        PrintFailureReason.outOfPaper: 'Out of paper',
        PrintFailureReason.coverOpen: 'Printer cover is open',
        PrintFailureReason.networkError: 'Network error',
        PrintFailureReason.communicationError: 'Communication error',
        PrintFailureReason.deviceInUse: 'Device is in use',
        PrintFailureReason.timeoutError: 'Operation timed out',
        PrintFailureReason.unknownError: 'Unknown error',
      };
      
      for (final entry in expectedDisplayNames.entries) {
        expect(entry.key.displayName, equals(entry.value));
      }
    });

    test('PrintFailureReason name property', () {
      expect(PrintFailureReason.printerBusy.name, equals('printerBusy'));
      expect(PrintFailureReason.outOfPaper.name, equals('outOfPaper'));
      expect(PrintFailureReason.unknownError.name, equals('unknownError'));
    });

    test('PrintFailureReason toString', () {
      expect(PrintFailureReason.printerBusy.toString(), contains('printerBusy'));
      expect(PrintFailureReason.networkError.toString(), contains('networkError'));
    });
  });

  group('Edge Cases and Error Handling', () {
    test('PrinterConfig with null MAC addresses', () {
      final config = PrinterConfig(
        printerType: PrinterType.starMCLan,
        macAddresses: null,
      );
      
      expect(config.macAddresses, isNull);
      
      final map = config.toMap();
      expect(map['mac_addresses'], isNull);
    });

    test('PrintResult.fromMap with invalid data types', () async {
      final map = {
        'success': 'true', // String instead of bool
        'reason': 123, // Number instead of string
        'retryable': 'false', // String instead of bool
        'message': ['error'], // Array instead of string
      };
      
      // Should handle gracefully and convert types appropriately
      final result = PrintResult.fromMap(map);
      expect(result.success, isTrue); // Should convert 'true' string to bool
      expect(result.retryable, isFalse); // Should convert 'false' string to bool
      expect(result.reason, equals(PrintFailureReason.unknownError)); // Should handle non-string reason
      expect(result.message, isA<String>()); // Should convert array to string
    });

    test('PrinterConfig toMap consistency', () {
      final originalConfig = PrinterConfig(
        printerType: PrinterType.starMCLan,
        macAddresses: ['AA:BB:CC:DD:EE:FF', '11:22:33:44:55:66'],
      );
      
      final map = originalConfig.toMap();
      
      // Verify map contains expected keys and values
      expect(map, hasLength(2));
      expect(map.containsKey('printer_type'), isTrue);
      expect(map.containsKey('mac_addresses'), isTrue);
      expect(map['printer_type'], isA<String>());
      expect(map['mac_addresses'], isA<List<String>>());
    });
  });
} 