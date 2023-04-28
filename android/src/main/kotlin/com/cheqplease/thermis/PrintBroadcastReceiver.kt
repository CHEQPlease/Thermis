package com.cheqplease.thermis

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Parcelable
import android.util.Log
import com.cheqplease.dantsu.DantsuPrintManager
import com.cheqplease.thermis.utils.PrintingQueue

class PrintBroadcastReceiver (var receiptBitmap: Bitmap) : BroadcastReceiver() {
    private val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        if (ACTION_USB_PERMISSION == action) {
            synchronized(this) {
                val usbServiceManager = context?.getSystemService(Context.USB_SERVICE) as UsbManager?
                val usbDevice = intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if (usbServiceManager != null && usbDevice != null) {
                        PrintingQueue.addPrintingTask(Runnable {
                            DantsuPrintManager.printImage(
                                usbServiceManager,
                                usbDevice,
                                receiptBitmap,
                                false
                            )
                            Log.d("PRINT----->>>>>", "Executing Printing Task")
                        })
                        context?.unregisterReceiver(this)
                    }
                }
            }
        }
    }
}

class CashDrawerBroadCastReceiver : BroadcastReceiver() {
    private val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        if (ACTION_USB_PERMISSION == action) {
            synchronized(this) {
                val usbServiceManager = context?.getSystemService(Context.USB_SERVICE) as UsbManager?
                val usbDevice = intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if (usbServiceManager != null && usbDevice != null) {
                        PrintingQueue.addPrintingTask(Runnable {
                            DantsuPrintManager.openCashDrawer()
                        })
                        context?.unregisterReceiver(this)
                    }
                }
            }
        }
    }
}