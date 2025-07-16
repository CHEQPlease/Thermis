
import 'dart:typed_data';

import 'package:thermis/printer_config.dart';
import 'package:thermis/device.dart';

import 'thermis_platform_interface.dart';

class Thermis {
  static bool _isInitialized = false;

  static Future<bool?> init(PrinterConfig config) async {
    final result = await ThermisPlatform.instance.init(config);
    _isInitialized = result ?? false;
    return result;
  }

  static void _checkInitialization() {
    if (!_isInitialized) {
      throw StateError('Thermis must be initialized before using any printer operations. Call Thermis.init() first.');
    }
  }

  static Future<void> printReceipt(String receiptDTOJSON) {
    _checkInitialization();
    return ThermisPlatform.instance.printReceipt(receiptDTOJSON);
  }

  static Future<bool?> openCashDrawer() {
    _checkInitialization();
    return ThermisPlatform.instance.openCashDrawer();
  }

  static Future<bool?> cutPaper() {
    _checkInitialization();
    return ThermisPlatform.instance.cutPaper();
  }

  static Future<bool?> checkPrinterConnection() {
    _checkInitialization();
    return ThermisPlatform.instance.checkPrinterConnection();
  }

  static Future<Uint8List?> getReceiptPreview(String receiptDTOJSON) {
    return ThermisPlatform.instance.getReceiptPreview(receiptDTOJSON);
  }

  static Stream<Device> discoverPrinters() {
    _checkInitialization();
    return ThermisPlatform.instance.discoverPrinters();
  }

  static Future<void> stopDiscovery() {
    _checkInitialization();
    return ThermisPlatform.instance.stopDiscovery();
  }
}
