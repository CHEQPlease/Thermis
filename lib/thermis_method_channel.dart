import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'thermis_platform_interface.dart';

/// An implementation of [ThermisPlatform] that uses method channels.
class MethodChannelThermis extends ThermisPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('thermis');

  @override
  Future<bool?> printCHEQReceipt(String receiptDTOJSON) async {
   return await methodChannel.invokeMethod<bool>('print_cheq_receipt',{"receipt_dto_json" : receiptDTOJSON});
  }

  @override
  Future<bool?> openCashDrawer() async {
    return await methodChannel.invokeMethod<bool>('open_cash_drawer');
  }

  @override
  Future<bool?> cutPaper() async {
    return await methodChannel.invokeMethod<bool>('cut_paper');
  }

  @override
  Future<bool?> checkPrinterConnection() async {
    return await methodChannel.invokeMethod<bool>('check_printer_connection');
  }

}
