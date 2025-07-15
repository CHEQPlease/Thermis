package com.cheqplease.thermis

import android.content.Context

data class PrinterConfig(val context: Context, val printerType: PrinterType, val macAddress: String? = null) {
    // Additional configuration parameters can be added here
} 

