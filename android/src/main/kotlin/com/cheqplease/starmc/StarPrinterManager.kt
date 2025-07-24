package com.cheqplease.starmc

import android.content.Context
import java.lang.ref.WeakReference
import com.starmicronics.stario10.*
import android.graphics.Bitmap
import com.cheqplease.thermis.PrinterConfig
import com.cheqplease.thermis.PrinterManager
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

object StarPrinterManager : PrinterManager {
    private var context: WeakReference<Context>? = null
    private var macAddresses: List<String> = emptyList()
    private var discoveryManager: StarDeviceDiscoveryManager? = null
    private var eventSink: EventChannel.EventSink? = null

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
        return results.any { it }
    }

    private suspend fun printToDeviceMultipleTimes(
        bitmap: Bitmap, 
        macAddress: String, 
        times: Int, 
        shouldOpenCashDrawer: Boolean
    ): Boolean {
        var successCount = 0
        
        // Print to the same device multiple times sequentially
        repeat(times) { attempt ->
            try {
                val success = printToSingleDevice(bitmap, macAddress, shouldOpenCashDrawer)
                if (success) {
                    successCount++
                }
                
                // Add delay between prints to the same device to avoid busy conflicts
                if (attempt < times - 1) { // Don't delay after the last print
                    delay(1000) // 1 second delay between prints to same device
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return successCount > 0
    }

    private suspend fun printToSingleDevice(bitmap: Bitmap, macAddress: String, shouldOpenCashDrawer: Boolean): Boolean {
        val printer = createPrinter(macAddress)
        
        return try {
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
            true
        } catch (e: StarIO10Exception) {
            e.printStackTrace()
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                printer.closeAsync().await()
            } catch (e: Exception) {
                // Ignore close errors
            }
        }
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
            printer.openAsync().await()
            true
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