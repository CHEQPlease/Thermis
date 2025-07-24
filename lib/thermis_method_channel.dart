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
  Future<PrintResult?> printCHEQReceipt(String receiptDTOJson, {PrinterConfig? config}) async {
    final arguments = <String, dynamic>{
      'receiptDTO': receiptDTOJson,
      'shouldOpenCashDrawer': false,
    };
    
    // Add printer config if provided
    if (config != null) {
      arguments.addAll(config.toMap());
    } else {
      // Default to USB generic if no config provided
      final defaultConfig = PrinterConfig(printerType: PrinterType.usbGeneric);
      arguments.addAll(defaultConfig.toMap());
    }

    final result = await methodChannel.invokeMethod<Map>('print_cheq_receipt', arguments);
    if (result != null) {
      return PrintResult.fromMap(result.cast<String, dynamic>());
    }
    return null;
  }

  @override
  Future<bool?> openCashDrawer(PrinterConfig config) async {
    final Map<String, dynamic> arguments = config.toMap();
    
    return await methodChannel.invokeMethod<bool>('open_cash_drawer', arguments);
  }

  @override
  Future<bool?> cutPaper(PrinterConfig config) async {
    final Map<String, dynamic> arguments = config.toMap();
    
    return await methodChannel.invokeMethod<bool>('cut_paper', arguments);
  }

  @override
  Future<bool?> checkPrinterConnection(PrinterConfig config) async {
    final Map<String, dynamic> arguments = config.toMap();
    
    return await methodChannel.invokeMethod<bool>('check_printer_connection', arguments);
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
  
  @override
  Future<int?> getQueueSize() async {
    return await methodChannel.invokeMethod<int>('get_queue_size');
  }
  
  @override
  Future<Map<String, int>?> getDeviceQueueSizes() async {
    final result = await methodChannel.invokeMethod<Map>('get_device_queue_sizes');
    return result?.cast<String, int>();
  }
  
  @override
  Future<bool?> clearPrintQueue() async {
    return await methodChannel.invokeMethod<bool>('clear_print_queue');
  }
  
  @override
  Future<bool?> clearDeviceQueue(String deviceKey) async {
    return await methodChannel.invokeMethod<bool>('clear_device_queue', {
      'device_key': deviceKey,
    });
  }
}
