
import 'dart:typed_data';

import 'package:thermis/printer_config.dart';
import 'package:thermis/device.dart';

import 'thermis_platform_interface.dart';

class Thermis {
  static Future<bool?> printReceipt(String receiptDTOJSON, {PrinterConfig? config}) {
    config ??= const PrinterConfig(printerType: PrinterType.usbGeneric);
    return ThermisPlatform.instance.printCHEQReceipt(receiptDTOJSON, config);
  }

  static Future<bool?> openCashDrawer({PrinterConfig? config}) {
    config ??= const PrinterConfig(printerType: PrinterType.usbGeneric);
    return ThermisPlatform.instance.openCashDrawer(config);
  }

  static Future<bool?> cutPaper({PrinterConfig? config}) {
    config ??= const PrinterConfig(printerType: PrinterType.usbGeneric);
    return ThermisPlatform.instance.cutPaper(config);
  }

  static Future<bool?> checkPrinterConnection({PrinterConfig? config}) {
    config ??= const PrinterConfig(printerType: PrinterType.usbGeneric);
    return ThermisPlatform.instance.checkPrinterConnection(config);
  }

  static Future<Uint8List?> getReceiptReview(String receiptDTOJSON) {
    return ThermisPlatform.instance.previewReceipt(receiptDTOJSON);
  }

  static Stream<Device> discoverPrinters() {
    return ThermisPlatform.instance.discoverPrinters();
  }

  static Future<void> stopDiscovery() {
    return ThermisPlatform.instance.stopDiscovery();
  }
  
  static Future<int?> getQueueSize() {
    return ThermisPlatform.instance.getQueueSize();
  }
  
  static Future<bool?> clearPrintQueue() {
    return ThermisPlatform.instance.clearPrintQueue();
  }
}
