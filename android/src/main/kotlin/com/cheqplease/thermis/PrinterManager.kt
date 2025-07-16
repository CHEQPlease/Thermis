package com.cheqplease.thermis

import android.graphics.Bitmap

interface PrinterManager {
    fun init(config: PrinterConfig)
    fun printBitmap(bitmap: Bitmap, shouldOpenCashDrawer: Boolean = false)
    suspend fun checkConnection(): Boolean
    fun openCashDrawer()
    fun cutPaper()
} 