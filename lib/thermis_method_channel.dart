import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'thermis_platform_interface.dart';

/// An implementation of [ThermisPlatform] that uses method channels.
class MethodChannelThermis extends ThermisPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('thermis');

  @override
  Future<String?> printCHEQReceipt(String receiptDTOJSON) async {
   return await methodChannel.invokeMethod<String>('print_cheq_receipt',{"receipt_dto_json" : receiptDTOJSON});
  }
}
