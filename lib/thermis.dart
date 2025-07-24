
import 'dart:typed_data';

import 'package:thermis/printer_config.dart';
import 'package:thermis/device.dart';

import 'thermis_platform_interface.dart';

class Thermis {
  static Future<PrintResult?> printReceipt(String receiptDTOJson, {PrinterConfig? config}) {
    return ThermisPlatform.instance.printCHEQReceipt(receiptDTOJson, config: config);
  }

  static Future<bool?> openCashDrawer({PrinterConfig? config}) {
    config ??= PrinterConfig(printerType: PrinterType.usbGeneric);
    return ThermisPlatform.instance.openCashDrawer(config);
  }

  static Future<bool?> cutPaper({PrinterConfig? config}) {
    config ??= PrinterConfig(printerType: PrinterType.usbGeneric);
    return ThermisPlatform.instance.cutPaper(config);
  }

  static Future<bool?> checkPrinterConnection({PrinterConfig? config}) {
    config ??= PrinterConfig(printerType: PrinterType.usbGeneric);
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
  
  static Future<Map<String, int>?> getDeviceQueueSizes() {
    return ThermisPlatform.instance.getDeviceQueueSizes();
  }
  
  static Future<bool?> clearPrintQueue() {
    return ThermisPlatform.instance.clearPrintQueue();
  }
  
  static Future<bool?> clearDeviceQueue(String deviceKey) {
    return ThermisPlatform.instance.clearDeviceQueue(deviceKey);
  }
}
