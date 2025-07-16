package com.cheqplease.thermis.utils

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object PrintingQueue {

    private var printingQueueService: ExecutorService = Executors.newSingleThreadExecutor()

    fun addPrintingTask(task: Runnable) {
        printingQueueService.submit{
            task.run()
        }
    }
}

object MacUtils {
    fun formatMacAddress(plainMac: String?): String? {
        if (plainMac == null) return null
        
        // Remove any non-alphanumeric characters and convert to uppercase
        val cleanMac = plainMac.replace(Regex("[^A-Fa-f0-9]"), "").uppercase()
        
        // Check if we have a valid MAC address length (12 characters)
        if (cleanMac.length != 12) return plainMac
        
        // Insert colons every 2 characters
        return cleanMac.chunked(2).joinToString(":")
    }
}