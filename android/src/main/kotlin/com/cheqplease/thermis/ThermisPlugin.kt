package com.cheqplease.thermis

import android.content.Context
import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/** ThermisPlugin */
class ThermisPlugin: FlutterPlugin, MethodCallHandler {

  private lateinit var channel : MethodChannel
  private lateinit var applicationContext: Context

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    onAttachedToEngine(flutterPluginBinding.applicationContext,flutterPluginBinding.binaryMessenger)
  }

  private fun onAttachedToEngine(applicationContext: Context,messenger: BinaryMessenger) {
    this.applicationContext = applicationContext
    channel = MethodChannel(messenger, "thermis")
    channel.setMethodCallHandler(this)
  }


  override fun onMethodCall(call: MethodCall, result: Result) {
    if (call.method == "print_cheq_receipt") {
      val receiptDTO = call.argument<String>("receipt_dto_json")
      ThermisManager.init(applicationContext)
      if (receiptDTO != null) {
        ThermisManager.printCheqReceipt(receiptDTO,false)
      };
    } else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }
}
