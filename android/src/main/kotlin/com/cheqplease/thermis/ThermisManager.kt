package com.cheqplease.thermis

import android.R
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import com.cheq.receiptify.Receiptify
import com.cheqplease.dantsu.DantsuPrintManager
import com.common.CommonConstants
import com.common.apiutil.ErrorCode
import com.common.apiutil.moneybox.MoneyBox
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

    fun previewReceipt(receiptDTO: String) : Bitmap? {
        return Receiptify.buildReceipt(receiptDTO)
    }

    fun openCashDrawer(){
        MoneyBox.init(context.get())
        val moneyBoxType = CommonConstants.MoneyBoxType.MoneyBox_1;
        when (MoneyBox.open()) {
            ErrorCode.OK -> {
                Toast.makeText(
                    this.context.get(),
                    "Cash Drawer Opened",
                    Toast.LENGTH_SHORT
                ).show()
            }
            ErrorCode.ERR_SYS_UNEXPECT -> {
                Toast.makeText(
                    this.context.get(),
                    "unknown error",
                    Toast.LENGTH_SHORT
                ).show()
                DantsuPrintManager.openCashDrawer()
            }
            ErrorCode.ERR_SYS_NOT_SUPPORT -> {
                Toast.makeText(
                    this.context.get(),
                    "not support",
                    Toast.LENGTH_SHORT
                ).show()
                DantsuPrintManager.openCashDrawer()
            }
            ErrorCode.FAIL -> {
                Toast.makeText(
                    this.context.get(),
                    "Failed",
                    Toast.LENGTH_SHORT
                ).show()
                DantsuPrintManager.openCashDrawer()
            }
            else -> {
                DantsuPrintManager.openCashDrawer()
            }
        }
    }

    fun cutPaper(){
        DantsuPrintManager.requestCutPaper()
    }

    fun checkPrinterConnection() : Boolean{
        return DantsuPrintManager.checkConnection()
    }


}