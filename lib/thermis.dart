
import 'thermis_platform_interface.dart';

class Thermis {
  static Future<String?> printCHEQReceipt(String receiptDTOJSON){
    return ThermisPlatform.instance.printCHEQReceipt(receiptDTOJSON);
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
