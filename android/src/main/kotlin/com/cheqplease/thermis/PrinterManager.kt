package com.cheqplease.thermis

import android.graphics.Bitmap

interface PrinterManager {
    fun init(config: PrinterConfig)
    suspend fun printBitmap(bitmap: Bitmap, shouldOpenCashDrawer: Boolean = false): Boolean
    suspend fun checkConnection(): Boolean
    fun openCashDrawer()
    fun cutPaper()
} 