package com.cheqplease.thermis

import android.content.Context
import android.graphics.Bitmap
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import com.cheqplease.thermis.PrinterType

/** ThermisPlugin */
class ThermisPlugin : FlutterPlugin, MethodCallHandler {

    private lateinit var channel: MethodChannel
    private lateinit var applicationContext: Context
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        onAttachedToEngine(flutterPluginBinding.applicationContext, flutterPluginBinding.binaryMessenger)
    }

    private fun onAttachedToEngine(applicationContext: Context, messenger: BinaryMessenger) {
        this.applicationContext = applicationContext
        channel = MethodChannel(messenger, "thermis")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {

            "init" -> {
                val printerTypeString = call.argument<String>("printer_type") ?: "GENERIC"
                val printerType = PrinterType.valueOf(printerTypeString.uppercase())
                val macAddress = call.argument<String>("printer_mac")
                val config = PrinterConfig(
                    context = applicationContext,
                    printerType = printerType,
                    macAddress = macAddress
                )
                ThermisManager.init(config)
                result.success(true)
            }

            "print_cheq_receipt" -> {
                val receiptDTO = call.argument<String>("receipt_dto_json")
                val openCashDrawer = call.argument<Boolean>("open_cash_drawer") ?: false
                if (receiptDTO != null) {
                    ThermisManager.printCheqReceipt(receiptDTO, openCashDrawer)
                    result.success(true)
                } else {
                    result.success(false)
                }
            }
            "open_cash_drawer" -> {
                ThermisManager.openCashDrawer()
                result.success(true)
            }
            "cut_paper" -> {
                ThermisManager.cutPaper()
                result.success(true)
            }
            "check_printer_connection" -> {
                coroutineScope.launch {
                    val isConnected = ThermisManager.checkPrinterConnection()
                    result.success(isConnected)
                }
            }
            "get_receipt_preview" -> {
                val receiptDTO = call.argument<String>("receipt_dto_json")
                if (receiptDTO != null) {
                    coroutineScope.launch {
                        try {
                            val bitmap = ThermisManager.previewReceipt(receiptDTO)
                            val stream = ByteArrayOutputStream()
                            bitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            val image = stream.toByteArray()
                            result.success(image)
                        } catch (e: Exception) {
                            result.error("Error", e.message, null)
                        }
                    }
                } else {
                    result.error("Error", "Receipt DTO is null", null)
                }
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}
