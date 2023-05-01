
import 'thermis_platform_interface.dart';

class Thermis {
  Future<String?> printCHEQReceipt(String receiptDTOJSON){
    return ThermisPlatform.instance.printCHEQReceipt(receiptDTOJSON);
  }

  Future<bool?> openCashDrawer(){
    return ThermisPlatform.instance.openCashDrawer();
  }

  Future<bool?> cutPaper(){
    return ThermisPlatform.instance.cutPaper();
  }

  Future <bool?> checkPrinterConnection(){
    return ThermisPlatform.instance.checkPrinterConnection();
  }
}
