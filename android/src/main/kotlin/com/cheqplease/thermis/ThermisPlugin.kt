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
                // Initialize StarPrinterManager with context for discovery
                val discoveryConfig = PrinterConfig(
                    context = applicationContext,
                    printerType = PrinterType.STARMC_LAN,
                    macAddresses = null
                )
                StarPrinterManager.init(discoveryConfig)
                StarPrinterManager.setEventSink(events)
            }

            override fun onCancel(arguments: Any?) {
                StarPrinterManager.setEventSink(null)
            }
        })
        
        // Initialize ThermisManager with context
        ThermisManager.init(applicationContext)
    }

    private fun createPrinterConfig(call: MethodCall): PrinterConfig? {
        val printerTypeString = call.argument<String>("printer_type") ?: return null
        val macAddresses = call.argument<List<String>>("mac_addresses")
        
        val printerType = when (printerTypeString.lowercase()) {
            "usbgeneric" -> PrinterType.USB_GENERIC
            "starmclan" -> PrinterType.STARMC_LAN
            else -> return null
        }
        
        return PrinterConfig(
            context = applicationContext,
            printerType = printerType,
            macAddresses = macAddresses
        )
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "print_cheq_receipt" -> {
                val receiptDTO = call.argument<String>("receiptDTO")
                val shouldOpenCashDrawer = call.argument<Boolean>("shouldOpenCashDrawer") ?: false
                val config = createPrinterConfig(call)
                
                if (receiptDTO != null && config != null) {
                    coroutineScope.launch {
                        try {
                            val printResult = ThermisManager.printCheqReceipt(receiptDTO, config, shouldOpenCashDrawer)
                            result.success(printResult.toMap())
                        } catch (e: Exception) {
                            val errorResult = PrintResult.Failed(
                                PrintFailureReason.UNKNOWN_ERROR,
                                false,
                                e.message ?: "Unknown error occurred"
                            )
                            result.success(errorResult.toMap())
                        }
                    }
                } else {
                    val errorResult = PrintResult.Failed(
                        PrintFailureReason.UNKNOWN_ERROR,
                        false,
                        "Missing required parameters: receiptDTO or printer configuration"
                    )
                    result.success(errorResult.toMap())
                }
            }
            "open_cash_drawer" -> {
                val config = createPrinterConfig(call)
                
                if (config != null) {
                    coroutineScope.launch {
                        val success = ThermisManager.openCashDrawer(config)
                        result.success(success)
                    }
                } else {
                    result.success(false)
                }
            }
            "cut_paper" -> {
                val config = createPrinterConfig(call)
                
                if (config != null) {
                    coroutineScope.launch {
                        val success = ThermisManager.cutPaper(config)
                        result.success(success)
                    }
                } else {
                    result.success(false)
                }
            }
            "check_printer_connection" -> {
                val config = createPrinterConfig(call)
                
                if (config != null) {
                    coroutineScope.launch {
                        val isConnected = ThermisManager.checkPrinterConnection(config)
                        result.success(isConnected)
                    }
                } else {
                    result.success(false)
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
            "get_queue_size" -> {
                val queueSize = ThermisManager.getQueueSize()
                result.success(queueSize)
            }
            "get_device_queue_sizes" -> {
                val deviceQueueSizes = ThermisManager.getDeviceQueueSizes()
                result.success(deviceQueueSizes)
            }
            "clear_print_queue" -> {
                coroutineScope.launch {
                    ThermisManager.clearQueue()
                    result.success(true)
                }
            }
            "clear_device_queue" -> {
                val deviceKey = call.argument<String>("device_key")
                if (deviceKey != null) {
                    coroutineScope.launch {
                        ThermisManager.clearDeviceQueue(deviceKey)
                        result.success(true)
                    }
                } else {
                    result.success(false)
                }
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
        ThermisManager.destroy()
    }
}
