
import 'dart:typed_data';

import 'package:thermis/printer_config.dart';
import 'package:thermis/device.dart';

import 'thermis_platform_interface.dart';

class Thermis {
  static Future<bool?> init(PrinterConfig config) {
    return ThermisPlatform.instance.init(config);
  }

  static Future<void> printReceipt(String receiptDTOJSON) {
    return ThermisPlatform.instance.printCHEQReceipt(receiptDTOJSON);
  }

  static Future<bool?> openCashDrawer() {
    return ThermisPlatform.instance.openCashDrawer();
  }

  static Future<bool?> cutPaper() {
    return ThermisPlatform.instance.cutPaper();
  }

  static Future<bool?> checkPrinterConnection() {
    return ThermisPlatform.instance.checkPrinterConnection();
  }

  static Future<Uint8List?> getReceiptPreview(String receiptDTOJSON) {
    return ThermisPlatform.instance.previewReceipt(receiptDTOJSON);
  }

  static Stream<Device> discoverPrinters() {
    return ThermisPlatform.instance.discoverPrinters();
  }

  static Future<void> stopDiscovery() {
    return ThermisPlatform.instance.stopDiscovery();
  }
}
