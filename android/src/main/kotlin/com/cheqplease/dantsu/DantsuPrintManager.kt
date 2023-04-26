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
import android.util.Log
import com.cheqplease.thermis.utils.PrintingQueue
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.usb.UsbConnection
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import java.lang.ref.WeakReference


object DantsuPrintManager {

    private lateinit var context: WeakReference<Context>
    private var isReceiverRegistered = false

    fun init(context: Context) {
        DantsuPrintManager.context = WeakReference<Context>(context)
    }

    private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"

    fun printUsb(bitmap: Bitmap) {
        val usbConnection: UsbConnection? = UsbPrintersConnections.selectFirstConnected(context.get())
        val usbManager = context.get()?.getSystemService(Context.USB_SERVICE) as UsbManager?
        if (usbConnection != null && usbManager != null) {
            val permissionIntent = PendingIntent.getBroadcast(
                context.get(),
                0,
                Intent(ACTION_USB_PERMISSION),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            )
            val filter = IntentFilter(ACTION_USB_PERMISSION)

            if(!isReceiverRegistered){
                Log.d("PRINT----->>>>>", "Registering Receiver")
                isReceiverRegistered = true
                context.get()?.registerReceiver(object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent) {
                        val action = intent.action
                        if (ACTION_USB_PERMISSION == action) {
                            synchronized(this) {
                                val usbServiceManager = context?.getSystemService(Context.USB_SERVICE) as UsbManager?
                                val usbDevice = intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                                    if (usbServiceManager != null && usbDevice != null) {
                                        PrintingQueue.addPrintingTask(Runnable {
                                        print(usbServiceManager, usbDevice, bitmap)
                                            Log.d("PRINT----->>>>>", "Executing Printing Task")
                                        })
                                    }
                                }
                            }
                        }
                    }
                }, filter)
            }


            usbManager.requestPermission(usbConnection.device, permissionIntent)
        }
    }

    fun print(usbManager: UsbManager, usbDevice: UsbDevice,bitmap: Bitmap) {
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
            textToPrint += ("[C]<img>${
                PrinterTextParserImg.bitmapToHexadecimalString(
                    printer,
                    newBitmap
                )
            }</img>\n")
            y += 256
        }

        printer.printFormattedText(textToPrint)
        printer.printFormattedText(textToPrint)
        printer.disconnectPrinter()
    }

}