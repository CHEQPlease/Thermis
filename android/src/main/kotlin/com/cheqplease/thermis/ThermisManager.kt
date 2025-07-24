package com.cheqplease.thermis

import android.content.Context
import android.graphics.Bitmap
import com.cheq.receiptify.Receiptify
import com.cheqplease.dantsu.DantsuPrintManager
import com.cheqplease.starmc.StarPrinterManager
import java.lang.ref.WeakReference

object ThermisManager {

    private lateinit var context: WeakReference<Context>
    private lateinit var printerManager: PrinterManager

    fun init(config: PrinterConfig) {
        this.context = WeakReference(config.context)
        printerManager = when (config.printerType) {
            PrinterType.GENERIC -> DantsuPrintManager
            PrinterType.STARMC -> StarPrinterManager
        }
        printerManager.init(config)
        Receiptify.init(config.context)
    }

    suspend fun printCheqReceipt(receiptDTO: String, shouldOpenCashDrawer: Boolean = false): Boolean {
        return try {
            val bitmap = Receiptify.buildReceipt(receiptDTO)
            if (bitmap != null) {
                val printResult = printerManager.printBitmap(bitmap, shouldOpenCashDrawer)
                printResult
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun previewReceipt(receiptDTO: String): Bitmap? {
        return Receiptify.buildReceipt(receiptDTO)
    }

    fun openCashDrawer() {
        printerManager.openCashDrawer()
    }

    fun cutPaper() {
        printerManager.cutPaper()
    }

    suspend fun checkPrinterConnection(): Boolean {
        return printerManager.checkConnection()
    }
}