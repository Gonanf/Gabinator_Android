package chaos.gabinator

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.commons.io.IOUtils
import java.io.ByteArrayOutputStream
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import kotlin.concurrent.thread
import kotlin.math.log

var bt: Bitmap? = null
var Mensaje: String = ""
var usbManager : UsbManager? = null
var HasPermisions = false
var FD : ParcelFileDescriptor? = null
var IS: FileInputStream? = null
var OS: FileOutputStream? = null
var device: UsbAccessory? = null
var runned = false
var UIDelay = 1
var UIHandle = Handler()
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        Connect()
        var Inter: Intent = Intent("com.android.example.USB_PERMISSION")
        val PI : PendingIntent = PendingIntent.getBroadcast(this,0,Inter,PendingIntent.FLAG_IMMUTABLE)
    }

    override fun onResume() {
        super.onResume()
        val tex: TextView = findViewById(R.id.Consola)
        val AccesoryList : Array<out UsbAccessory>? = usbManager?.accessoryList
        if (AccesoryList != null){
            if(!HasPermisions){
                Mensaje += "No tengo permisos\n"
                device = AccesoryList[0]
                val Pint : PendingIntent = PendingIntent.getBroadcast(applicationContext,0,Intent("com.android.example.USB_PERMISSION"),
                    PendingIntent.FLAG_IMMUTABLE)
                runned = false
                usbManager?.requestPermission(device,Pint)
            }
            else{
                Mensaje += "Tengo permisos\n"
                Mensaje += device?.model + "\n"

            }
            tex.text = Mensaje

        }
    }
    var m = false

    fun ByteArrayToInt(buffer: ByteArray) : Int{
        var offset: Int = 0
        return (buffer[offset++].toInt() and 0xff shl 24) or
                (buffer[offset++].toInt() and 0xff shl 16) or
                (buffer[offset++].toInt() and 0xff shl 8) or
                (buffer[offset].toInt() and 0xff)
    }
    private fun Connect() {
        val tex: TextView = findViewById(R.id.Consola)
        Mensaje += "Escuchando\n"
        tex.text = Mensaje
        var usbReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {

                Mensaje += "Onreciver\n"
                val Accion = intent?.action
                if (intent != null){
                    Mensaje += Accion.toString() + "\n"
                }
                tex.text = Mensaje

                when (intent?.action) {
                    UsbManager.ACTION_USB_ACCESSORY_DETACHED -> {
                        // USB device detached
                        Mensaje += "Detached\n"
                        var tempdevice: UsbAccessory? = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY)
                        if (tempdevice != null && device != null) {
                            if (tempdevice!!.model == device!!.model){
                                Log.d(TAG, "USB RECEIVER Detached: ${tempdevice!!.manufacturer}")
                                Mensaje += device!!.model + " detached\n"
                            }
                        }
                        tex.text = Mensaje
                    }

                    "com.android.example.USB_PERMISSION" ->{
                        if (!HasPermisions){
                            Mensaje += "Asking permissions\n"
                            FD = usbManager?.openAccessory(device)
                            if (FD != null && !runned){
                                HasPermisions = true
                                runned = true
                                Mensaje += "Permissions granted\n"
                                val tempFD : FileDescriptor = FD!!.fileDescriptor
                                IS = FileInputStream(tempFD)
                                OS = FileOutputStream(tempFD)
                                Mensaje += device?.model + "\n"
                                Mensaje += "Starting Thread\n"
                                UIHandle.postDelayed(object: Runnable{
                                    override fun run(){
                                        val image = findViewById<ImageView>(R.id.IMG)
                                        if (bt != null){
                                            image.setImageBitmap(bt)
                                        }
                                        tex.text = Mensaje
                                        UIHandle.postDelayed(this, UIDelay.toLong())
                                    }
                                } ,UIDelay.toLong())
                                /*thread {
                                    val image = findViewById<ImageView>(R.id.IMG)
                                    while (true){
                                    if (IS != null && HasPermisions) {
                                        println("InputStream not null")
                                        bt = BitmapFactory.decodeStream(IS!!)
                                        println("Created bitmap")
                                        if (bt != null) {
                                            Mensaje += "Ajustando con bitmap\n"
                                            image.setImageBitmap(bt)
                                        } else {
                                            Mensaje += "Not getting data\n"
                                            image.setImageResource(R.drawable.ic_launcher_foreground)
                                        }
                                    } }}.start()*/

                                val meg = Thread {
                                    Mensaje += "Trhead started\n"
                                    while (true){
                                        if (IS != null){
                                            try {
                                                val by = ByteArray(8)
                                                IS!!.read(by,0,8)
                                                try {
                                                    var size = ByteBuffer.wrap(by).getLong()
                                                }
                                                catch (io: BufferUnderflowException){
                                                    Mensaje += "Error underflos\n"
                                                    continue
                                                }
                                                var size = 700000
                                                Mensaje += size.toString() + "\n"
                                                    var byt = ByteArray(size.toInt())
                                                    IS!!.read(byt,0,size.toInt())
                                                    bt = BitmapFactory.decodeByteArray(byt,0,size.toInt())
                                                    if (bt != null){
                                                        Mensaje += "Logrado\n"
                                                    }
                                            }
                                            catch (io: IOException){
                                                Mensaje += io.message + "\n"
                                            }

                                        }
                                        else{
                                            Mensaje += "IS == NULL\n"
                                        }
                                    }
                                }

                                meg.start()



                            }
                            if (FD == null){
                                Mensaje += "failed opening File Descriptor\nRunner: " + runned.toString() + "\n"
                            }
                            tex.text = Mensaje
                        }
                    }
                }

            }
        }

        // Register the BroadcastReceiver with the intent filters
        val filter = IntentFilter()
        filter.addAction("com.android.example.USB_PERMISSION")
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
        registerReceiver(usbReceiver, filter)
    }
}

