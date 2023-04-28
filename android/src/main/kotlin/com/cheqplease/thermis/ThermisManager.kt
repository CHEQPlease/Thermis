package com.cheqplease.thermis

import android.content.Context
import com.cheq.receiptify.Receiptify
import com.cheqplease.dantsu.DantsuPrintManager
import java.lang.ref.WeakReference

object ThermisManager {

    private lateinit var context: WeakReference<Context>

    fun init(context: Context) {
        this.context = WeakReference<Context>(context)
        DantsuPrintManager.init(context)
        Receiptify.init(context)

    }
    fun printCheqReceipt(receiptDTO: String,shouldOpenCashDrawer: Boolean = false) {
        val bitmap = Receiptify.buildReceipt(receiptDTO)
        if (bitmap != null) {
            DantsuPrintManager.requestPrintBitmap(bitmap,shouldOpenCashDrawer)
        }
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