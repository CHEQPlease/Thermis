package com.cheqplease.dantsu

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Parcelable
import com.cheqplease.thermis.PrinterConfig
import com.cheqplease.thermis.PrinterManager
import com.cheqplease.thermis.utils.PrintingQueue
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.EscPosPrinterCommands
import com.dantsu.escposprinter.connection.usb.UsbConnection
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import java.lang.ref.WeakReference


object DantsuPrintManager : PrinterManager {

    private lateinit var context: WeakReference<Context>


    private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"

    override fun init(config: PrinterConfig) {
        context = WeakReference<Context>(config.context)
    }

    private fun getPrintBroadcastReceiver(bitmap: Bitmap, shouldOpenCashDrawer: Boolean): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                val action = intent.action
                val permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                if (ACTION_USB_PERMISSION == action && permissionGranted) {
                    synchronized(this) {
                        val usbServiceManager = context?.getSystemService(Context.USB_SERVICE) as UsbManager?
                        val usbDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as UsbDevice?
                        }
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (usbServiceManager != null && usbDevice != null) {
                                PrintingQueue.addPrintingTask {
                                    printImage(
                                        usbServiceManager,
                                        usbDevice,
                                        bitmap,
                                        shouldOpenCashDrawer
                                    )
                                }
                                context?.unregisterReceiver(this)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getPaperCutBroadcastReceiver(): BroadcastReceiver {

        return object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                val action = intent.action
                val permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                if (ACTION_USB_PERMISSION == action && permissionGranted) {
                    synchronized(this) {
                        val usbServiceManager = context?.getSystemService(Context.USB_SERVICE) as UsbManager?
                        val usbDevice = intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (usbServiceManager != null && usbDevice != null) {
                                PrintingQueue.addPrintingTask(
                                    Runnable {
                                        cutPaper()
                                    }
                                )
                                PrintingQueue.addPrintingTask {
                                    cutPaper()
                                }
                                context?.unregisterReceiver(this)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getCashDrawerOpenBroadcastReceiver(): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                val action = intent.action
                val permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                if (ACTION_USB_PERMISSION == action && permissionGranted) {
                    synchronized(this) {
                        val usbServiceManager = context?.getSystemService(Context.USB_SERVICE) as UsbManager?
                        val usbDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE) as UsbDevice?
                        }
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (usbServiceManager != null && usbDevice != null) {
                                PrintingQueue.addPrintingTask {
                                    openCashDrawer()
                                }
                                PrintingQueue.addPrintingTask {
                                    openCashDrawer()
                                }
                                context?.unregisterReceiver(this)
                            }
                        }
                    }
                }
            }
        }
    }


    override fun printBitmap(bitmap: Bitmap, shouldOpenCashDrawer: Boolean) {
        val usbConnection: UsbConnection? = UsbPrintersConnections.selectFirstConnected(context.get())
        val usbManager = context.get()?.getSystemService(Context.USB_SERVICE) as UsbManager?

        val usbPermissionIntent = Intent(ACTION_USB_PERMISSION).apply {
            setPackage(context.get()?.packageName)
        }
        val usbPermissionIntentFilter = IntentFilter(ACTION_USB_PERMISSION)

        if (usbConnection != null && usbManager != null) {
            val permissionIntent = PendingIntent.getBroadcast(
                context.get(),
                0,
                usbPermissionIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.get()?.registerReceiver(
                    getPrintBroadcastReceiver(bitmap, shouldOpenCashDrawer),
                    usbPermissionIntentFilter,
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.get()?.registerReceiver(
                    getPrintBroadcastReceiver(bitmap, shouldOpenCashDrawer),
                    usbPermissionIntentFilter
                )
            }
            usbManager.requestPermission(usbConnection.device, permissionIntent)
        }
    }

    private fun rescale(printer: EscPosPrinter, bitmap: Bitmap): Bitmap {
        val maxWidth: Int = printer.printerWidthPx
        var newBitmap = bitmap
        bitmap.apply {
            if (width > maxWidth) {
                val ratio = height.toDouble() / width.toDouble()
                val newHeight = kotlin.math.ceil(maxWidth * ratio).toInt()
                newBitmap = Bitmap.createScaledBitmap(this, maxWidth, newHeight, true)
            }
        }

        return newBitmap
    }

    private fun getFormattedPrintText(printer: EscPosPrinter, newBitmap: Bitmap): String? {
        val imageBytes = EscPosPrinterCommands.bitmapToBytes(rescale(printer, newBitmap), false)

        return PrinterTextParserImg.bytesToHexadecimalString(imageBytes)
    }

    fun printImage(
        usbManager: UsbManager,
        usbDevice: UsbDevice,
        bitmap: Bitmap,
        shouldOpenCashDrawer: Boolean
    ) {
        val width: Int = bitmap.width
        val height: Int = bitmap.height
        var textToPrint = ""
        val printer = EscPosPrinter(UsbConnection(usbManager, usbDevice), 200, 72f, 47)

        var y = 0
        while (y < height) {
            val newBitmap = Bitmap.createBitmap(
                bitmap, 0, y, width,
                if (y + 256 >= height) height - y else 256
            )
            textToPrint += ("[C]<img>${ getFormattedPrintText(printer, newBitmap) }</img>\n")
            y += 256
        }

        textToPrint += "[C]\n\n"

        if (shouldOpenCashDrawer) {
            printer.printFormattedTextAndOpenCashBox(textToPrint,20f)
        }else{
            printer.printFormattedText(textToPrint)
            Thread.sleep(300)
            cutPaper()
        }
        printer.disconnectPrinter()
    }


    fun requestOpenCashDrawer(){
        val usbConnection: UsbConnection? = UsbPrintersConnections.selectFirstConnected(context.get())
        val usbManager = context.get()?.getSystemService(Context.USB_SERVICE) as UsbManager?

        val usbPermissionIntent = Intent(ACTION_USB_PERMISSION)
        val usbPermissionIntentFilter = IntentFilter(ACTION_USB_PERMISSION);

        if (usbConnection != null && usbManager != null) {
            val permissionIntent = PendingIntent.getBroadcast(
                context.get(),
                0,
                usbPermissionIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            )
            context.get()?.registerReceiver(getCashDrawerOpenBroadcastReceiver(), usbPermissionIntentFilter)
            usbManager.requestPermission(usbConnection.device, permissionIntent)
        }
    }

    override fun openCashDrawer() {
        val usbConnection: UsbConnection? = UsbPrintersConnections.selectFirstConnected(context.get())
        val printerRaw = EscPosPrinterCommands(usbConnection)
        printerRaw.connect()
        printerRaw.openCashBox()
        printerRaw.disconnect()
    }


    fun requestCutPaper(){
        val usbConnection: UsbConnection? = UsbPrintersConnections.selectFirstConnected(context.get())
        val usbManager = context.get()?.getSystemService(Context.USB_SERVICE) as UsbManager?

        val usbPermissionIntent = Intent(ACTION_USB_PERMISSION)
        val usbPermissionIntentFilter = IntentFilter(ACTION_USB_PERMISSION);

        if (usbConnection != null && usbManager != null) {
            val permissionIntent = PendingIntent.getBroadcast(
                context.get(),
                0,
                usbPermissionIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            )
            context.get()?.registerReceiver(getPaperCutBroadcastReceiver(), usbPermissionIntentFilter)
            usbManager.requestPermission(usbConnection.device, permissionIntent)
        }
    }

    override fun cutPaper() {
        val usbConnection: UsbConnection? = UsbPrintersConnections.selectFirstConnected(context.get())
        val printerRaw = EscPosPrinterCommands(usbConnection)
        printerRaw.connect()
        printerRaw.cutPaper()
        printerRaw.disconnect()
    }

    override suspend fun checkConnection() : Boolean {
        return UsbPrintersConnections.selectFirstConnected(context.get()) != null
    }


}
