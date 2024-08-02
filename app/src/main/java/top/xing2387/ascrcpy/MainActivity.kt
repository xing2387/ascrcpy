package top.xing2387.ascrcpy

import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cgutman.adblib.AdbBase64
import com.cgutman.adblib.AdbConnection
import com.cgutman.adblib.AdbCrypto
import com.cgutman.adblib.AdbStream
import com.cgutman.adblib.TcpChannel
import java.io.File
import java.net.Inet4Address
import java.net.Socket

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val base64 = AdbBase64 { data -> Base64.encodeToString(data, Base64.NO_WRAP) }
        var adbCrypto: AdbCrypto? = null
        try {
            adbCrypto = AdbCrypto.loadAdbKeyPair(base64, File(filesDir, "private_key"), File(filesDir, "public_key"))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (adbCrypto == null) {
            try {
                adbCrypto = AdbCrypto.generateAdbKeyPair(base64)
                adbCrypto.saveAdbKeyPair(File(filesDir, "private_key"), File(filesDir, "public_key"))
            } catch (e: Exception) {
                Log.w("~~~~", "fail to generate and save key-pair", e)
            }
        }

        Thread {
            try {
                val socket = Socket(Inet4Address.getByName("10.42.67.16"), 5555)
                val tcpChannel = TcpChannel(socket)
                val connection: AdbConnection = AdbConnection.create(tcpChannel, adbCrypto)
                connection.connect()
                connection.open("shell:exec date")
                val ss: AdbStream = connection.open("shell:exec ls /")
                while (!ss.isClosed) {
                    try {
                        Log.d("~~~~", String(ss.read()))
                    } catch (e: Exception) {
                        // there must be a ss Close Exception
//                        Log.e("~~~~", "ss.read(): ", e)
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e("~~~~", "onCreate: ", e)
            }
        }.start()

    }
}