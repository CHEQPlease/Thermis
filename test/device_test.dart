import 'package:flutter_test/flutter_test.dart';
import 'package:thermis/device.dart';

void main() {
  group('Device Tests', () {
    test('Device constructor creates device with all properties', () {
      const deviceName = 'Star TSP100III';
      const mac = 'AA:BB:CC:DD:EE:FF';
      const ip = '192.168.1.100';
      
      final device = Device(
        deviceName: deviceName,
        mac: mac,
        ip: ip,
      );
      
      expect(device.deviceName, equals(deviceName));
      expect(device.mac, equals(mac));
      expect(device.ip, equals(ip));
    });

    test('Device.fromMap creates device from valid map', () {
      final map = {
        'deviceName': 'Star mC-Print3',
        'mac': '11:22:33:44:55:66',
        'ip': '192.168.1.101',
      };
      
      final device = Device.fromMap(map);
      
      expect(device.deviceName, equals('Star mC-Print3'));
      expect(device.mac, equals('11:22:33:44:55:66'));
      expect(device.ip, equals('192.168.1.101'));
    });

    test('Device.fromMap handles null values gracefully', () {
      final map = {
        'deviceName': null,
        'mac': 'AA:BB:CC:DD:EE:FF',
        'ip': null,
      };
      
      final device = Device.fromMap(map);
      
      expect(device.deviceName, equals('Unknown'));
      expect(device.mac, equals('AA:BB:CC:DD:EE:FF'));
      expect(device.ip, equals('Unknown'));
    });

    test('Device.fromMap handles missing keys', () {
      final map = <String, dynamic>{
        'mac': 'AA:BB:CC:DD:EE:FF',
        // deviceName and ip are missing
      };
      
      final device = Device.fromMap(map);
      
      expect(device.deviceName, equals('Unknown'));
      expect(device.mac, equals('AA:BB:CC:DD:EE:FF'));
      expect(device.ip, equals('Unknown'));
    });

    test('Device.fromMap handles empty map', () {
      final map = <String, dynamic>{};
      
      final device = Device.fromMap(map);
      
      expect(device.deviceName, equals('Unknown'));
      expect(device.mac, equals('Unknown'));
      expect(device.ip, equals('Unknown'));
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

    test('Device toString with unknown values', () {
      final device = Device(
        deviceName: 'Unknown',
        mac: 'Unknown',
        ip: 'Unknown',
      );
      
      final deviceString = device.toString();
      
      expect(deviceString, contains('Unknown'));
    });

    test('Device equality comparison', () {
      final device1 = Device(
        deviceName: 'Star TSP100III',
        mac: 'AA:BB:CC:DD:EE:FF',
        ip: '192.168.1.100',
      );
      
      final device2 = Device(
        deviceName: 'Star TSP100III',
        mac: 'AA:BB:CC:DD:EE:FF',
        ip: '192.168.1.100',
      );
      
      final device3 = Device(
        deviceName: 'Star mC-Print3',
        mac: '11:22:33:44:55:66',
        ip: '192.168.1.101',
      );
      
      expect(device1.toString(), equals(device2.toString()));
      expect(device1.toString(), isNot(equals(device3.toString())));
    });

    test('Device with special characters in names', () {
      final device = Device(
        deviceName: 'Star™ TSP100III (USB)',
        mac: 'AA:BB:CC:DD:EE:FF',
        ip: '192.168.1.100',
      );
      
      expect(device.deviceName, equals('Star™ TSP100III (USB)'));
      expect(device.toString(), contains('Star™ TSP100III (USB)'));
    });

    test('Device with IPv6 address', () {
      final device = Device(
        deviceName: 'Star TSP100III',
        mac: 'AA:BB:CC:DD:EE:FF',
        ip: '2001:0db8:85a3:0000:0000:8a2e:0370:7334',
      );
      
      expect(device.ip, equals('2001:0db8:85a3:0000:0000:8a2e:0370:7334'));
      expect(device.toString(), contains('2001:0db8:85a3:0000:0000:8a2e:0370:7334'));
    });

    test('Device with long MAC address formats', () {
      final device = Device(
        deviceName: 'Star TSP100III',
        mac: 'AA-BB-CC-DD-EE-FF',
        ip: '192.168.1.100',
      );
      
      expect(device.mac, equals('AA-BB-CC-DD-EE-FF'));
    });
  });
} 