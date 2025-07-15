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

object StarPrinterManager : PrinterManager {
    private lateinit var context: WeakReference<Context>
    private var printerMac: String = ""

    private lateinit var printer: StarPrinter

    override fun init(config: PrinterConfig) {
        this.context = WeakReference(config.context)
        this.printerMac = config.macAddress ?: ""
        initPrinter()
    }

    private fun initPrinter() {
        val settings = StarConnectionSettings(
            interfaceType = InterfaceType.Lan,
            identifier = printerMac,
        )
        printer = StarPrinter(settings, context.get() ?: throw IllegalStateException("Context cannot be null"))
    }

    override fun printBitmap(bitmap: Bitmap, shouldOpenCashDrawer: Boolean) {
        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.IO + job)

        scope.launch {
            try {
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