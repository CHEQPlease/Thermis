import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:thermis/device.dart';
import 'package:thermis/printer_config.dart';

import 'thermis_platform_interface.dart';

/// An implementation of [ThermisPlatform] that uses method channels.
class MethodChannelThermis extends ThermisPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('thermis');
  final eventChannel = const EventChannel('thermis/starmc_discovery');

  @override
  Future<bool?> init(PrinterConfig config) async {
    final result = await methodChannel.invokeMethod<bool>('init', config.toMap());
    return result;
  }

  @override
  Future<void> printCHEQReceipt(String receiptDTOJSON) async {
    await methodChannel.invokeMethod<void>('print_cheq_receipt', {
      'receipt_dto_json': receiptDTOJSON,
    });
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

  @override
  Future<Uint8List?> previewReceipt(String receiptDTOJSON) async {
    final result = await methodChannel.invokeMethod<Uint8List>('get_receipt_preview', {
      'receipt_dto_json': receiptDTOJSON,
    });
    return result;
  }

  @override
  Stream<Device> discoverPrinters() {
    return eventChannel.receiveBroadcastStream().map((dynamic event) {
      final Map<String, dynamic> deviceMap = Map<String, dynamic>.from(event);
      return Device.fromMap(deviceMap);
    });
  }

  @override
  Future<void> stopDiscovery() async {
    await methodChannel.invokeMethod<void>('stop_discovery');
  }
}
