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