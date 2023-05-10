package com.cheqplease.thermis

import android.content.Context
import android.graphics.Bitmap
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream


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
    ThermisManager.init(applicationContext)
  }


  @OptIn(DelicateCoroutinesApi::class)
  override fun onMethodCall(call: MethodCall, result: Result) {
    if (call.method == "print_cheq_receipt") {
      val receiptDTO = call.argument<String>("receipt_dto_json")
      val openCashDrawer = call.argument<Boolean>("open_cash_drawer") ?: false
      if (receiptDTO != null) {
        ThermisManager.printCheqReceipt(receiptDTO,openCashDrawer)
        result.success(true)
      }
      else{
        result.success(false)
      }
    }

    else if (call.method == "open_cash_drawer"){
      ThermisManager.openCashDrawer()
      result.success(true)
    }

    else if (call.method == "cut_paper"){
      ThermisManager.cutPaper()
      result.success(true)
    }

    else if (call.method == "check_printer_connection"){
      result.success(ThermisManager.checkPrinterConnection())
    }

    else if(call.method == "preview_receipt")
    {
      val receiptDTO = call.argument<String>("receipt_dto_json")
      if (receiptDTO != null) {
        GlobalScope.launch(Dispatchers.IO) {
          val bitmap = ThermisManager.previewReceipt(receiptDTO)
          val stream = ByteArrayOutputStream()
          bitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream)
          val image = stream.toByteArray()
          result.success(image)
        }

      }
      else{
        result.success(null)
      }

    }

    else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }
}
