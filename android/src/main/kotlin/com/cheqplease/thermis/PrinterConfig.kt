package com.cheqplease.thermis

import android.content.Context

data class PrinterConfig(
    val context: Context,
    val printerType: PrinterType,
    val macAddresses: List<String>? = null
)

enum class PrinterType {
    USB_GENERIC,
    STARMC_LAN
} 

