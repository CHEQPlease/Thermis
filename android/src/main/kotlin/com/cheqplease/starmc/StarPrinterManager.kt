package com.cheqplease.starmc

import android.content.Context
import java.lang.ref.WeakReference
import com.starmicronics.stario10.*
import android.graphics.Bitmap
import com.starmicronics.stario10.starxpandcommand.PrinterBuilder
import com.starmicronics.stario10.starxpandcommand.DocumentBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.starmicronics.stario10.starxpandcommand.*
import com.starmicronics.stario10.starxpandcommand.printer.*

object StarPrinterManager {
    private lateinit var context: WeakReference<Context>
    private var printerMac: String = ""

    private lateinit var printer: StarPrinter

    fun init(context: Context, mac: String) {
        this.context = WeakReference(context)
        this.printerMac = mac

        initPrinter()
    }

    private fun initPrinter() {
        val settings = StarConnectionSettings(
            interfaceType = InterfaceType.Lan,
            identifier = printerMac,
        )
        printer = StarPrinter(settings, context.get() ?: throw IllegalStateException("Context cannot be null"))
    }

    fun printBitmap(bitmap: Bitmap) {
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

    suspend fun checkConnection(): Boolean {
        return try {
            printer.openAsync().await()
            true
        } catch (e: Exception) {
            false
        } finally {
            printer.closeAsync().await()
        }
    }
}