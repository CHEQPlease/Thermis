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

object ThermisManager {

    private lateinit var context: WeakReference<Context>
    
    // Print job data class
    private data class PrintJob(
        val receiptDTO: String,
        val shouldOpenCashDrawer: Boolean,
        val config: PrinterConfig,
        val result: CompletableDeferred<Boolean>
    )
    
    // Queue for print jobs
    private val printQueue = Channel<PrintJob>(Channel.UNLIMITED)
    private var queueProcessor: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val printQueueSize = AtomicInteger(0)

    fun init(context: Context) {
        this.context = WeakReference(context)
        Receiptify.init(context)
        startQueueProcessor()
    }
    
    private fun startQueueProcessor() {
        queueProcessor?.cancel()
        queueProcessor = coroutineScope.launch {
            for (job in printQueue) {
                try {
                    // Get the appropriate printer manager for this job
                    val printerManager = getPrinterManager(job.config)
                    
                    // Process each print job sequentially
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
                
                // Decrement queue size after processing
                printQueueSize.decrementAndGet()
                
                // Small delay between prints to ensure printer is ready
                delay(500)
            }
        }
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
        
        // Increment queue size
        printQueueSize.incrementAndGet()
        
        // Add to queue
        printQueue.send(job)
        
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
        return printQueueSize.get()
    }
    
    suspend fun clearQueue() {
        // Cancel all pending jobs
        var job = printQueue.tryReceive().getOrNull()
        while (job != null) {
            job.result.complete(false)
            printQueueSize.decrementAndGet()
            job = printQueue.tryReceive().getOrNull()
        }
    }
    
    fun destroy() {
        queueProcessor?.cancel()
        printQueue.close()
    }
}