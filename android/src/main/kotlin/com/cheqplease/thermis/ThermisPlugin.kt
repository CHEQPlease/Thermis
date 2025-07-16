package com.cheqplease.thermis

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import com.cheqplease.starmc.StarPrinterManager

/** ThermisPlugin */
class ThermisPlugin : FlutterPlugin, MethodCallHandler {

    private lateinit var channel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private lateinit var applicationContext: Context
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "thermis")
        channel.setMethodCallHandler(this)

        eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "thermis/starmc_discovery")
        eventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                StarPrinterManager.setEventSink(events)
            }

            override fun onCancel(arguments: Any?) {
                StarPrinterManager.setEventSink(null)
            }
        })
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
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
            "stop_discovery" -> {
                StarPrinterManager.stopDiscovery()
                result.success(null)
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
        StarPrinterManager.stopDiscovery()
    }
}
