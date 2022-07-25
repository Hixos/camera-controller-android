package it.hixos.cameracontroller

import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream
import java.lang.Exception
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

class JsonTcpClient {
    suspend fun connect(ip: String, port: Int, timeout: Int = 2000) : Boolean = withContext(Dispatchers.IO)
    {
        if(connected) {
            Log.e("JsonTCPClient", "Already connected!")
            return@withContext false
        }
        socket.close()
        socket = Socket()
        try{
            Log.d("JsonTCPClient", "Connecting... $ip:$port")
            socket.connect(InetSocketAddress(ip, port), timeout)
            connected = true
            Log.d("JsonTCPClient", "Connected!")
            return@withContext true
        }catch (sto : SocketTimeoutException)
        {
            Log.d("JsonTCPClient", "Connection timeout")
            return@withContext false
        }
        catch (e : Exception)
        {
            Log.e("JsonTCPClient", "Unmanaged exception: ${e.message}")
            return@withContext false
        }
    }

    fun disconnect()
    {
        connected = false
        socket.close()

        socket = Socket()
        Log.i("JsonTCPClient", "Disconnected")
    }

    fun receiver() : Flow<String> = flow {
        var i = 0
        val istream = socket.getInputStream()

        while(true)
        {
            val buf = ByteArray(4)
            if(!sockReadBytes(buf, istream, 4))
            {
                disconnect()
                Log.e("JsonTCPClient", "Error reading packet header")
                return@flow
            }
            val packetLen = java.nio.ByteBuffer.wrap(buf).getInt()
            Log.d("JsonTCPClient", "Header ok. len = ${packetLen}")

            val pkt = ByteArray(packetLen)
            if(!sockReadBytes(pkt, istream, packetLen))
            {
                disconnect()
                Log.e("JsonTCPClient", "Error reading packet")
                return@flow
            }
            Log.d("JsonTCPClient", "Packet ok: val = ${String(pkt)}")
            emit(String(pkt))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun send(data: String, delayms: Long = 0) = withContext(Dispatchers.IO)
    {
        delay(delayms)
        val ostream = socket.getOutputStream()
        var buf_len = java.nio.ByteBuffer.allocate(4).putInt(data.length).array()
        var buf_data = data.toByteArray()
        mutex.withLock {
            ostream.write(buf_len)
            ostream.write(buf_data)
            ostream.flush()
        }
    }

    private fun sockReadBytes(buf: ByteArray, istream: InputStream, len: Int) : Boolean
    {
        var r : Int = 0
        while(r < len)
        {
            val res = istream.read(buf, r, len - r)
            if(res < 0) {
                return false
            }else {
                r += res
            }
        }
        return  true
    }

    private var socket: Socket = Socket()
    private var connected = false

    private val mutex = Mutex()
}