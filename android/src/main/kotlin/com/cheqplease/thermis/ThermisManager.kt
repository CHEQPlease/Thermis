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
    
    // Enhanced print job data class with retry tracking
    private data class PrintJob(
        val receiptDTO: String,
        val shouldOpenCashDrawer: Boolean,
        val config: PrinterConfig,
        val result: CompletableDeferred<PrintResult>,
        var retryCount: Int = 0,
        val maxRetries: Int = 3
    ) {
        fun canRetry(): Boolean = retryCount < maxRetries
        fun incrementRetry() { retryCount++ }
    }
    
    // Per-device queues and processors
    private val deviceQueues = ConcurrentHashMap<String, Channel<PrintJob>>()
    private val deviceProcessors = ConcurrentHashMap<String, Job>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val totalQueueSize = AtomicInteger(0)
    
    // Retry configuration
    private const val BASE_RETRY_DELAY_MS = 2000L

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
                        val result = if (printResult) PrintResult.Success else PrintResult.Failed(
                            PrintFailureReason.UNKNOWN_ERROR, 
                            false, 
                            "Print operation returned false"
                        )
                        
                        handlePrintResult(job, result, deviceKey, queue)
                    } else {
                        val result = PrintResult.Failed(
                            PrintFailureReason.UNKNOWN_ERROR, 
                            false, 
                            "Failed to build receipt bitmap"
                        )
                        handlePrintResult(job, result, deviceKey, queue)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    val result = PrintResult.Failed(
                        PrintFailureReason.UNKNOWN_ERROR, 
                        false, 
                        e.message ?: "Unknown exception occurred"
                    )
                    handlePrintResult(job, result, deviceKey, queue)
                }
                
                // Decrement total queue size after processing
                totalQueueSize.decrementAndGet()
                
                // Small delay between prints for same device to ensure printer is ready
                delay(500)
            }
        }
        
        deviceProcessors[deviceKey] = processor
    }
    
    private suspend fun handlePrintResult(
        job: PrintJob, 
        result: PrintResult, 
        deviceKey: String, 
        queue: Channel<PrintJob>
    ) {
        when {
            result.isSuccess() -> {
                // Print succeeded
                job.result.complete(result)
            }
            result is PrintResult.Failed && result.retryable && job.canRetry() -> {
                // Print failed but can be retried
                job.incrementRetry()
                val delayMs = calculateBackoffDelay(job.retryCount - 1)
                
                println("Print failed for device $deviceKey (${result.reason}), retrying in ${delayMs}ms (attempt ${job.retryCount})")
                
                // Re-queue the job after delay
                coroutineScope.launch {
                    delay(delayMs)
                    totalQueueSize.incrementAndGet() // Re-increment since we're re-queuing
                    queue.send(job)
                }
            }
            else -> {
                // Print failed and cannot be retried, or max retries exceeded
                if (job.retryCount > 0) {
                    val finalResult = PrintResult.Failed(
                        (result as? PrintResult.Failed)?.reason ?: PrintFailureReason.UNKNOWN_ERROR,
                        false,
                        "Max retries (${job.maxRetries}) exceeded. Last error: ${result.let { (it as? PrintResult.Failed)?.message }}"
                    )
                    job.result.complete(finalResult)
                } else {
                    job.result.complete(result)
                }
            }
        }
    }
    
    private fun calculateBackoffDelay(attempt: Int): Long {
        // Exponential backoff with jitter: 2s, 4s, 8s, etc. + random jitter
        val baseDelay = BASE_RETRY_DELAY_MS * (1L shl attempt)
        val jitter = (Math.random() * 1000).toLong() // 0-1000ms jitter
        return baseDelay + jitter
    }
    
    private fun getPrinterManager(config: PrinterConfig): PrinterManager {
        return when (config.printerType) {
            PrinterType.USB_GENERIC -> DantsuPrintManager.apply { init(config) }
            PrinterType.STARMC_LAN -> StarPrinterManager.apply { init(config) }
        }
    }

    suspend fun printCheqReceipt(receiptDTO: String, config: PrinterConfig, shouldOpenCashDrawer: Boolean = false): PrintResult {
        val result = CompletableDeferred<PrintResult>()
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
                val cancelledResult = PrintResult.Failed(
                    PrintFailureReason.UNKNOWN_ERROR, 
                    false, 
                    "Queue cleared by user"
                )
                job.result.complete(cancelledResult)
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
                val cancelledResult = PrintResult.Failed(
                    PrintFailureReason.UNKNOWN_ERROR, 
                    false, 
                    "Device queue cleared by user"
                )
                job.result.complete(cancelledResult)
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