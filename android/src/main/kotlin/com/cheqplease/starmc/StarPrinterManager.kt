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

object StarPrinterManager : PrinterManager {
    private lateinit var context: WeakReference<Context>
    private var printerMac: String = ""
    private lateinit var printer: StarPrinter
    private var discoveryManager: StarDeviceDiscoveryManager? = null
    private var eventSink: EventChannel.EventSink? = null

    override fun init(config: PrinterConfig) {
        this.context = WeakReference(config.context)
        this.printerMac = config.macAddress ?: ""
        initPrinter()
    }

    private fun initPrinter() {
        val settings = StarConnectionSettings(
            interfaceType = InterfaceType.Lan,
            identifier = printerMac,
            autoSwitchInterface = false
        )
        printer = StarPrinter(settings, context.get() ?: throw IllegalStateException("Context cannot be null"))
    }

    fun setEventSink(sink: EventChannel.EventSink?) {
        eventSink = sink
        if (sink != null) {
            startDiscovery()
        } else {
            stopDiscovery()
        }
    }

    private fun startDiscovery() {
        try {
            stopDiscovery()
            discoveryManager = StarDeviceDiscoveryManagerFactory.create(
                listOf(InterfaceType.Lan),
                context.get() ?: throw IllegalStateException("Context cannot be null")
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
        return try {
            printer.openAsync().await()
            true
        } catch (e: Exception) {
            false
        } finally {
            printer.closeAsync().await()
        }
    }

    override fun openCashDrawer() {
        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.Default + job)

        scope.launch {
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
                printer.closeAsync().await()
            }
        }
    }

    override fun cutPaper() {
        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.Default + job)

        scope.launch {
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
                printer.closeAsync().await()
            }
        }
    }
}