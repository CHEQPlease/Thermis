import 'dart:typed_data';

import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'package:flutter/foundation.dart';
import 'package:thermis/device.dart';
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

  Future<PrintResult?> printCHEQReceipt(String receiptDTOJson, {PrinterConfig? config}) {
    throw UnimplementedError('printCHEQReceipt() has not been implemented.');
  }

  Future<bool?> openCashDrawer(PrinterConfig config) {
    throw UnimplementedError('openCashDrawer() has not been implemented.');
  }

  Future<bool?> cutPaper(PrinterConfig config) {
    throw UnimplementedError('cutPaper() has not been implemented.');
  }

  Future<bool?> checkPrinterConnection(PrinterConfig config) {
    throw UnimplementedError('checkPrinterConnection() has not been implemented.');
  }

  Future<Uint8List?> previewReceipt(String receiptDTOJSON) {
    throw UnimplementedError('previewReceipt() has not been implemented.');
  }

  Stream<Device> discoverPrinters() {
    throw UnimplementedError('discoverPrinters() has not been implemented.');
  }

  Future<void> stopDiscovery() {
    throw UnimplementedError('stopDiscovery() has not been implemented.');
  }
  
  Future<int?> getQueueSize() {
    throw UnimplementedError('getQueueSize() has not been implemented.');
  }
  
  Future<Map<String, int>?> getDeviceQueueSizes() {
    throw UnimplementedError('getDeviceQueueSizes() has not been implemented.');
  }
  
  Future<bool?> clearPrintQueue() {
    throw UnimplementedError('clearPrintQueue() has not been implemented.');
  }
  
  Future<bool?> clearDeviceQueue(String deviceKey) {
    throw UnimplementedError('clearDeviceQueue() has not been implemented.');
  }
}
