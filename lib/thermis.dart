
import 'thermis_platform_interface.dart';

class Thermis {
  Future<String?> printCHEQReceipt(String receiptDTOJSON){
    return ThermisPlatform.instance.printCHEQReceipt(receiptDTOJSON);
  }
}
