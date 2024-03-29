package chaos.gabinator

import android.content.Context
import android.hardware.usb.UsbManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import chaos.gabinator.ui.theme.GabinatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GabinatorTheme {
                // Cambiar el modo de host a accesorio
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val DeviceMan = getSystemService(Context.USB_SERVICE) as UsbManager
                    val DeviceList = DeviceMan.deviceList
                    val DeviceIt = DeviceList.values.iterator()
                    Greeting(DeviceList.size.toString())
                    while (DeviceIt.hasNext()) {
                        val dev = DeviceIt.next()
                        val nom = dev.deviceName
                        Greeting(nom)
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GabinatorTheme {
        Greeting("Android")
    }
}