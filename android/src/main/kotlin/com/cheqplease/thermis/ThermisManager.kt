package com.cheqplease.thermis

import android.content.Context
import android.graphics.Bitmap
import com.cheq.receiptify.Receiptify
import com.cheqplease.dantsu.DantsuPrintManager
import com.cheqplease.starmc.StarPrinterManager
import java.lang.ref.WeakReference
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentHashMap

object ThermisManager {

    private lateinit var context: WeakReference<Context>
    
    // Print job data class
    private data class PrintJob(
        val receiptDTO: String,
        val shouldOpenCashDrawer: Boolean,
        val config: PrinterConfig,
        val result: CompletableDeferred<Boolean>
    )
    
    // Per-device queues and processors
    private val deviceQueues = ConcurrentHashMap<String, Channel<PrintJob>>()
    private val deviceProcessors = ConcurrentHashMap<String, Job>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val totalQueueSize = AtomicInteger(0)

    fun init(context: Context) {
        this.context = WeakReference(context)
        Receiptify.init(context)
    }
    
    private fun getDeviceKey(config: PrinterConfig): String {
        return when (config.printerType) {
            PrinterType.USB_GENERIC -> "USB_GENERIC"
            PrinterType.STARMC_LAN -> {
                // For LAN printers, group by first MAC address (primary device)
                val primaryMac = config.macAddresses?.firstOrNull() ?: "UNKNOWN"
                "STARMC_LAN_$primaryMac"
            }
        }
    }
    
    private fun getOrCreateDeviceQueue(deviceKey: String): Channel<PrintJob> {
        return deviceQueues.getOrPut(deviceKey) {
            val queue = Channel<PrintJob>(Channel.UNLIMITED)
            startDeviceProcessor(deviceKey, queue)
            queue
        }
    }
    
    private fun startDeviceProcessor(deviceKey: String, queue: Channel<PrintJob>) {
        val processor = coroutineScope.launch {
            for (job in queue) {
                try {
                    // Get the appropriate printer manager for this job
                    val printerManager = getPrinterManager(job.config)
                    
                    // Process each print job sequentially for this device
                    val bitmap = Receiptify.buildReceipt(job.receiptDTO)
                    if (bitmap != null) {
                        val printResult = printerManager.printBitmap(bitmap, job.shouldOpenCashDrawer)
                        job.result.complete(printResult)
                    } else {
                        job.result.complete(false)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    job.result.complete(false)
                }
                
                // Decrement total queue size after processing
                totalQueueSize.decrementAndGet()
                
                // Small delay between prints for same device to ensure printer is ready
                delay(500)
            }
        }
        
        deviceProcessors[deviceKey] = processor
    }
    
    private fun getPrinterManager(config: PrinterConfig): PrinterManager {
        return when (config.printerType) {
            PrinterType.USB_GENERIC -> DantsuPrintManager.apply { init(config) }
            PrinterType.STARMC_LAN -> StarPrinterManager.apply { init(config) }
        }
    }

    suspend fun printCheqReceipt(receiptDTO: String, config: PrinterConfig, shouldOpenCashDrawer: Boolean = false): Boolean {
        val result = CompletableDeferred<Boolean>()
        val job = PrintJob(receiptDTO, shouldOpenCashDrawer, config, result)
        
        // Get device-specific queue
        val deviceKey = getDeviceKey(config)
        val deviceQueue = getOrCreateDeviceQueue(deviceKey)
        
        // Increment total queue size
        totalQueueSize.incrementAndGet()
        
        // Add to device-specific queue (enables parallel execution across devices)
        deviceQueue.send(job)
        
        // Wait for result
        return result.await()
    }

    fun previewReceipt(receiptDTO: String): Bitmap? {
        return Receiptify.buildReceipt(receiptDTO)
    }

    suspend fun openCashDrawer(config: PrinterConfig): Boolean {
        return try {
            val printerManager = getPrinterManager(config)
            printerManager.openCashDrawer()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun cutPaper(config: PrinterConfig): Boolean {
        return try {
            val printerManager = getPrinterManager(config)
            printerManager.cutPaper()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun checkPrinterConnection(config: PrinterConfig): Boolean {
        return try {
            val printerManager = getPrinterManager(config)
            printerManager.checkConnection()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun getQueueSize(): Int {
        return totalQueueSize.get()
    }
    
    fun getDeviceQueueSizes(): Map<String, Int> {
        // Return queue sizes per device for debugging
        return deviceQueues.mapValues { (_, queue) ->
            // Note: Channel doesn't expose size directly, so we track it via totalQueueSize
            // This is an approximation for debugging purposes
            0 // Could be enhanced with per-device counters if needed
        }
    }
    
    suspend fun clearQueue() {
        // Clear all device queues
        deviceQueues.values.forEach { queue ->
            var job = queue.tryReceive().getOrNull()
            while (job != null) {
                job.result.complete(false)
                totalQueueSize.decrementAndGet()
                job = queue.tryReceive().getOrNull()
            }
        }
    }
    
    suspend fun clearDeviceQueue(deviceKey: String) {
        // Clear specific device queue
        deviceQueues[deviceKey]?.let { queue ->
            var job = queue.tryReceive().getOrNull()
            while (job != null) {
                job.result.complete(false)
                totalQueueSize.decrementAndGet()
                job = queue.tryReceive().getOrNull()
            }
        }
    }
    
    fun destroy() {
        // Cancel all device processors
        deviceProcessors.values.forEach { it.cancel() }
        deviceProcessors.clear()
        
        // Close all device queues
        deviceQueues.values.forEach { it.close() }
        deviceQueues.clear()
    }
}