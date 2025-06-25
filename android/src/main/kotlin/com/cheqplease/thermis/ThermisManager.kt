package com.cheqplease.thermis

import android.content.Context
import android.graphics.Bitmap
import com.cheq.receiptify.Receiptify
import com.cheqplease.dantsu.DantsuPrintManager
import com.cheqplease.starmc.StarPrinterManager
import java.lang.ref.WeakReference

object ThermisManager {

    private lateinit var context: WeakReference<Context>

    fun init(context: Context) {
        this.context = WeakReference<Context>(context)
//        DantsuPrintManager.init(context)
        Receiptify.init(context)
        StarPrinterManager.init(context, "00:11:62:30:E5:B9") // Replace with actual MAC address

    }
    fun printCheqReceipt(receiptDTO: String,shouldOpenCashDrawer: Boolean = false) {
        val bitmap = Receiptify.buildReceipt(receiptDTO)
        if (bitmap != null) {
//            DantsuPrintManager.requestPrintBitmap(bitmap,shouldOpenCashDrawer)
            StarPrinterManager.printBitmap(bitmap)
        }
    }

    fun previewReceipt(receiptDTO: String) : Bitmap? {
        return Receiptify.buildReceipt(receiptDTO)
    }

    fun openCashDrawer(){
        DantsuPrintManager.requestOpenCashDrawer()
    }

    fun cutPaper(){
        DantsuPrintManager.requestCutPaper()
    }

    fun checkPrinterConnection() : Boolean{
        return DantsuPrintManager.checkConnection()
    }


}