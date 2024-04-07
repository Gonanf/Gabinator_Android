package chaos.gabinator

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import kotlin.concurrent.thread


var Mensaje: String = ""
var usbManager : UsbManager? = null
var HasPermisions = false
var FD : ParcelFileDescriptor? = null
var IS: FileInputStream? = null
var OS: FileOutputStream? = null
var device: UsbAccessory? = null
var runned = false
var th: Thread = Thread(MainActivity.GetData())
class MainActivity : ComponentActivity() {

    class GetData: Runnable{
        override fun run() {
            var lastIOExeption : IOException? = null
            while (true){
                try {
                    if (IS != null){

                        val disp = IS!!.available()
                        Mensaje += disp.toString() + "\n"
                        if (disp > 0){
                            var buf = ByteArray(disp)
                            val r = IS!!.read(buf,0,disp)
                            Mensaje += buf.toString() + "\n"
                            break
                        }

                    }
                    else{
                        Mensaje += "IS == NULL\n"
                    }

                }
                catch (error: IOException){
                        if (lastIOExeption != error){
                            Mensaje += error.message + "\n"
                        }
                }
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        Connect()
        var Inter: Intent = Intent("com.android.example.USB_PERMISSION")
        val PI : PendingIntent = PendingIntent.getBroadcast(this,0,Inter,
            PendingIntent.FLAG_IMMUTABLE)
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
                    UsbManager.ACTION_USB_ACCESSORY_ATTACHED -> {
                        // USB device attached
                        Mensaje += "Attached\n"
                        device = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY)
                        if (device != null) {
                            Log.d(TAG, "USB RECEIVER Attached: ${device!!.manufacturer}")
                            Mensaje += device!!.model + " attached\n"
                        }
                        tex.text = Mensaje

                    }

                    UsbManager.ACTION_USB_ACCESSORY_DETACHED -> {
                        // USB device detached
                        Mensaje += "Detached\n"
                        var tempdevice: UsbAccessory? = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY)
                        if (tempdevice != null && device != null) {
                            if (tempdevice!!.model == device!!.model){
                                Log.d(TAG, "USB RECEIVER Detached: ${tempdevice!!.manufacturer}")
                                Mensaje += device!!.model + " detached\n"
                                device = null
                                HasPermisions = false
                                runned = false
                                IS = null
                                OS = null
                                th.join()
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
                                th.start()
                                th.join()
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

