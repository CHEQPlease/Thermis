import 'dart:typed_data';

import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'package:thermis/printer_config.dart';

import 'thermis_method_channel.dart';

abstract class ThermisPlatform extends PlatformInterface {
  /// Constructs a ThermisPlatform.
  ThermisPlatform() : super(token: _token);

  static final Object _token = Object();

  static ThermisPlatform _instance = MethodChannelThermis();

  /// The default instance of [ThermisPlatform] to use.
  ///
  /// Defaults to [MethodChannelThermis].
  static ThermisPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [ThermisPlatform] when
  /// they register themselves.
  static set instance(ThermisPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<bool?> init(PrinterConfig printerConfig);
  Future<bool?> printCHEQReceipt(String receiptDTOJSON);
  Future<Uint8List?> getReceiptPreview(String receiptDTOJSON);
  Future<bool?> openCashDrawer();
  Future<bool?> cutPaper();
  Future<bool?> checkPrinterConnection();
}
