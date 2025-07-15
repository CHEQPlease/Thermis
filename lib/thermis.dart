
import 'dart:typed_data';

import 'package:thermis/printer_config.dart';

import 'thermis_platform_interface.dart';

class Thermis {

  static Future<bool?> init(PrinterConfig printerConfig){
    return ThermisPlatform.instance.init(printerConfig);
  }

  static Future<bool?> printCHEQReceipt(String receiptDTOJSON){
    return ThermisPlatform.instance.printCHEQReceipt(receiptDTOJSON);
  }

  static Future<Uint8List?> previewReceipt(String receiptDTOJSON){
    return ThermisPlatform.instance.getReceiptPreview(receiptDTOJSON);
  }

  static Future<bool?> openCashDrawer(){
    return ThermisPlatform.instance.openCashDrawer();
  }

  static Future<bool?> cutPaper(){
    return ThermisPlatform.instance.cutPaper();
  }

  static Future <bool?> isPrinterConnected(){
    return ThermisPlatform.instance.checkPrinterConnection();
  }
}
