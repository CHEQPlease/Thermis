package com.cheqplease.starmc

import android.content.Context
import java.lang.ref.WeakReference
import com.starmicronics.stario10.*
import android.graphics.Bitmap
import com.cheqplease.thermis.PrinterConfig
import com.cheqplease.thermis.PrinterManager
import com.cheqplease.thermis.PrintResult
import com.cheqplease.thermis.PrintFailureReason
import com.starmicronics.stario10.starxpandcommand.PrinterBuilder
import com.starmicronics.stario10.starxpandcommand.DocumentBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.starmicronics.stario10.starxpandcommand.*
import com.starmicronics.stario10.starxpandcommand.drawer.Channel
import com.starmicronics.stario10.starxpandcommand.drawer.OpenParameter
import com.starmicronics.stario10.starxpandcommand.printer.*
import io.flutter.plugin.common.EventChannel
import com.cheqplease.thermis.utils.MacUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

object StarPrinterManager : PrinterManager {
    private var context: WeakReference<Context>? = null
    private var macAddresses: List<String> = emptyList()
    private var discoveryManager: StarDeviceDiscoveryManager? = null
    private var eventSink: EventChannel.EventSink? = null
    
    // Retry configuration
    private const val MAX_RETRIES = 3
    private const val BASE_RETRY_DELAY_MS = 3000L
    private const val OPERATION_TIMEOUT_MS = 30000L // 30 seconds

    override fun init(config: PrinterConfig) {
        this.context = WeakReference(config.context)
        this.macAddresses = config.macAddresses ?: emptyList()
    }

    private fun createPrinter(macAddress: String): StarPrinter {
        val settings = StarConnectionSettings(
            interfaceType = InterfaceType.Lan,
            identifier = macAddress,
            autoSwitchInterface = false
        )
        return StarPrinter(settings, context?.get() ?: throw IllegalStateException("Context cannot be null"))
    }

    fun setEventSink(sink: EventChannel.EventSink?) {
        eventSink = sink
        if (sink != null && context?.get() != null) {
            startDiscovery()
        } else {
            stopDiscovery()
        }
    }

    private fun startDiscovery() {
        try {
            // Ensure context is initialized before starting discovery
            if (context?.get() == null) {
                eventSink?.error("DISCOVERY_ERROR", "StarPrinterManager not initialized", null)
                return
            }
            
            stopDiscovery()
            discoveryManager = StarDeviceDiscoveryManagerFactory.create(
                listOf(InterfaceType.Lan),
                context?.get() ?: throw IllegalStateException("Context cannot be null")
            )

            discoveryManager?.discoveryTime = 10000
            discoveryManager?.callback = object : StarDeviceDiscoveryManager.Callback {
                override fun onPrinterFound(printer: StarPrinter) {
                    val deviceMap = mapOf(
                        "deviceName" to printer.information?.model?.name,
                        "ip" to printer.information?.detail?.lan?.ipAddress,
                        "mac" to MacUtils.formatMacAddress(printer.information?.detail?.lan?.macAddress)
                    )
                    eventSink?.success(deviceMap)
                }

                override fun onDiscoveryFinished() {
                    eventSink?.endOfStream()
                }
            }
            discoveryManager?.startDiscovery()

        } catch (e: Exception) {
            eventSink?.error("DISCOVERY_ERROR", e.message, null)
        }
    }

    fun stopDiscovery() {
        try {
            discoveryManager?.stopDiscovery()
            discoveryManager = null
        } catch (e: Exception) {
            // Handle any errors that occur during stop discovery
        }
    }

    override suspend fun printBitmap(bitmap: Bitmap, shouldOpenCashDrawer: Boolean): Boolean {
        if (macAddresses.isEmpty()) {
            return false
        }

        // Group MAC addresses - same devices get sequential printing, different devices get parallel
        val deviceGroups = macAddresses.groupBy { it }
        
        // Print to each unique device in parallel
        val printJobs = deviceGroups.map { (macAddress, occurrences) ->
            CoroutineScope(Dispatchers.IO).async {
                // For same device, print sequentially (multiple times if needed)
                printToDeviceMultipleTimes(bitmap, macAddress, occurrences.size, shouldOpenCashDrawer)
            }
        }

        // Wait for all device groups to complete
        val results = printJobs.awaitAll()
        
        // Return true if at least one device group was successful
        return results.any { it.isSuccess() }
    }

    private suspend fun printToDeviceMultipleTimes(
        bitmap: Bitmap, 
        macAddress: String, 
        times: Int, 
        shouldOpenCashDrawer: Boolean
    ): PrintResult {
        var successCount = 0
        var lastResult: PrintResult = PrintResult.Failed(PrintFailureReason.UNKNOWN_ERROR, false)
        
        // Print to the same device multiple times sequentially
        repeat(times) { attempt ->
            val result = printToSingleDeviceWithRetry(bitmap, macAddress, shouldOpenCashDrawer)
            lastResult = if (result.isSuccess()) {
                successCount++
                result
            } else {
                result
            }
            
            // Add delay between prints to the same device to avoid busy conflicts
            if (attempt < times - 1) { // Don't delay after the last print
                delay(1000) // 1 second delay between prints to same device
            }
        }
        
        return if (successCount > 0) PrintResult.Success else lastResult
    }

    private suspend fun printToSingleDeviceWithRetry(
        bitmap: Bitmap, 
        macAddress: String, 
        shouldOpenCashDrawer: Boolean
    ): PrintResult {
        repeat(MAX_RETRIES) { attempt ->
            val result = printToSingleDevice(bitmap, macAddress, shouldOpenCashDrawer)
            
            when {
                result.isSuccess() -> return result
                result is PrintResult.Failed && result.retryable && attempt < MAX_RETRIES - 1 -> {
//                    val delayMs = calculateBackoffDelay(attempt)
                    val delayMs = BASE_RETRY_DELAY_MS
                    println("Print failed (${result.reason}), retrying in ${delayMs}ms (attempt ${attempt + 1})")
                    delay(delayMs)
                }
                else -> return result
            }
        }
        
        return PrintResult.Failed(
            PrintFailureReason.UNKNOWN_ERROR, 
            false, 
            "Max retries exceeded"
        )
    }

    private suspend fun printToSingleDevice(
        bitmap: Bitmap, 
        macAddress: String, 
        shouldOpenCashDrawer: Boolean
    ): PrintResult {
        val printer = createPrinter(macAddress)
        
        return try {
            withTimeout(OPERATION_TIMEOUT_MS) {
                val builder = StarXpandCommandBuilder()
                builder.addDocument(
                    DocumentBuilder()
                        .addPrinter(
                            PrinterBuilder()
                                .styleAlignment(Alignment.Left)
                                .actionPrintImage(ImageParameter(bitmap, 560))
                                .styleInternationalCharacter(InternationalCharacterType.Usa)
                                .actionCut(CutType.Partial)
                        )
                )

                if (shouldOpenCashDrawer) {
                    builder.addDocument(
                        DocumentBuilder()
                            .addDrawer(
                                DrawerBuilder()
                                    .actionOpen(
                                        OpenParameter()
                                        .setChannel(Channel.No1)
                                    )
                            )
                    )
                }

                val commands = builder.getCommands()

                printer.openAsync().await()
                printer.printAsync(commands).await()
                PrintResult.Success
            }
        } catch (e: TimeoutCancellationException) {
            PrintResult.Failed(PrintFailureReason.TIMEOUT_ERROR, true, "Operation timed out")
        } catch (e: StarIO10NotFoundException) {
            PrintResult.Failed(PrintFailureReason.PRINTER_NOT_FOUND, false, e.message)
        } catch (e: StarIO10CommunicationException) {
            PrintResult.Failed(PrintFailureReason.COMMUNICATION_ERROR, true, e.message)
        } catch (e: StarIO10BadResponseException) {
            val failureReason = classifyBadResponse(e.message)
            PrintResult.Failed(failureReason, failureReason.isRetryable(), e.message)
        } catch (e: StarIO10Exception) {
            val failureReason = classifyStarIOException(e)
            PrintResult.Failed(failureReason, failureReason.isRetryable(), e.message)
        } catch (e: Exception) {
            PrintResult.Failed(PrintFailureReason.UNKNOWN_ERROR, false, e.message)
        } finally {
            try {
                printer.closeAsync().await()
            } catch (e: Exception) {
                // Ignore close errors
            }
        }
    }

    private fun classifyBadResponse(message: String?): PrintFailureReason {
        return when {
            message?.contains("paper", ignoreCase = true) == true -> PrintFailureReason.OUT_OF_PAPER
            message?.contains("cover", ignoreCase = true) == true -> PrintFailureReason.COVER_OPEN
            message?.contains("busy", ignoreCase = true) == true -> PrintFailureReason.PRINTER_BUSY
            message?.contains("offline", ignoreCase = true) == true -> PrintFailureReason.PRINTER_OFFLINE
            else -> PrintFailureReason.UNKNOWN_ERROR
        }
    }

    private fun classifyStarIOException(exception: StarIO10Exception): PrintFailureReason {
        return when {
            exception.message?.contains("busy", ignoreCase = true) == true -> PrintFailureReason.PRINTER_BUSY
            exception.message?.contains("in use", ignoreCase = true) == true -> PrintFailureReason.DEVICE_IN_USE
            exception.message?.contains("network", ignoreCase = true) == true -> PrintFailureReason.NETWORK_ERROR
            exception.message?.contains("timeout", ignoreCase = true) == true -> PrintFailureReason.TIMEOUT_ERROR
            exception.message?.contains("offline", ignoreCase = true) == true -> PrintFailureReason.PRINTER_OFFLINE
            else -> PrintFailureReason.COMMUNICATION_ERROR
        }
    }

    private fun calculateBackoffDelay(attempt: Int): Long {
        // Exponential backoff: 1s, 2s, 4s, etc.
        return BASE_RETRY_DELAY_MS * (1L shl attempt)
    }

    override suspend fun checkConnection(): Boolean {
        if (macAddresses.isEmpty()) {
            return false
        }

        // Check connection to all devices in parallel
        val connectionJobs = macAddresses.map { macAddress ->
            CoroutineScope(Dispatchers.IO).async {
                checkSingleDeviceConnection(macAddress)
            }
        }

        // Wait for all connection checks to complete
        val results = connectionJobs.awaitAll()
        
        // Return true if at least one device is connected
        return results.any { it }
    }

    private suspend fun checkSingleDeviceConnection(macAddress: String): Boolean {
        val printer = createPrinter(macAddress)
        
        return try {
            withTimeout(5000) { // 5 second timeout for connection check
                printer.openAsync().await()
                true
            }
        } catch (e: Exception) {
            false
        } finally {
            try {
                printer.closeAsync().await()
            } catch (e: Exception) {
                // Ignore close errors
            }
        }
    }

    override fun openCashDrawer() {
        if (macAddresses.isEmpty()) {
            return
        }

        // Group MAC addresses to handle duplicates
        val deviceGroups = macAddresses.groupBy { it }

        // Open cash drawer on each unique device (multiple times if needed)
        deviceGroups.forEach { (macAddress, occurrences) ->
            val job = SupervisorJob()
            val scope = CoroutineScope(Dispatchers.Default + job)

            scope.launch {
                // For same device, open cash drawer multiple times sequentially
                repeat(occurrences.size) { attempt ->
                    try {
                        openCashDrawerOnSingleDevice(macAddress)
                        
                        // Add delay between operations to the same device
                        if (attempt < occurrences.size - 1) {
                            delay(500) // 0.5 second delay between operations to same device
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private suspend fun openCashDrawerOnSingleDevice(macAddress: String) {
        val printer = createPrinter(macAddress)
        
        try {
            withTimeout(10000) { // 10 second timeout
                val builder = StarXpandCommandBuilder()
                builder.addDocument(
                    DocumentBuilder()
                        .addDrawer(
                            DrawerBuilder()
                                .actionOpen(
                                    OpenParameter()
                                    .setChannel(Channel.No1)
                                )
                        )
                )

                val commands = builder.getCommands()

                printer.openAsync().await()
                printer.printAsync(commands).await()
            }
        } catch (e: StarIO10NotFoundException) {
            // Handle printer not found error
            e.printStackTrace()
        } catch (e: Exception) {
            // Handle other exceptions
            e.printStackTrace()
        } finally {
            try {
                printer.closeAsync().await()
            } catch (e: Exception) {
                // Ignore close errors
            }
        }
    }

    override fun cutPaper() {
        if (macAddresses.isEmpty()) {
            return
        }

        // Group MAC addresses to handle duplicates
        val deviceGroups = macAddresses.groupBy { it }

        // Cut paper on each unique device (multiple times if needed)
        deviceGroups.forEach { (macAddress, occurrences) ->
            val job = SupervisorJob()
            val scope = CoroutineScope(Dispatchers.Default + job)

            scope.launch {
                // For same device, cut paper multiple times sequentially
                repeat(occurrences.size) { attempt ->
                    try {
                        cutPaperOnSingleDevice(macAddress)
                        
                        // Add delay between operations to the same device
                        if (attempt < occurrences.size - 1) {
                            delay(500) // 0.5 second delay between operations to same device
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private suspend fun cutPaperOnSingleDevice(macAddress: String) {
        val printer = createPrinter(macAddress)
        
        try {
            withTimeout(10000) { // 10 second timeout
                val builder = StarXpandCommandBuilder()
                builder.addDocument(
                    DocumentBuilder()
                        .addPrinter(
                            PrinterBuilder()
                                .actionCut(CutType.Partial)
                        )
                )
                val commands = builder.getCommands()

                printer.openAsync().await()
                printer.printAsync(commands).await()
            }
        } catch (e: StarIO10Exception) {
            e.printStackTrace()
        } finally {
            try {
                printer.closeAsync().await()
            } catch (e: Exception) {
                // Ignore close errors
            }
        }
    }
}