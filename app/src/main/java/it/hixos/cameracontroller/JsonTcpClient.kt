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
    private val TAG = "JsonTCPClient"

    suspend fun connect(ip: String, port: Int, timeout: Int = 2000) : Boolean = withContext(Dispatchers.IO)
    {
        if(connected) {
            Log.e(TAG, "Already connected!")
            return@withContext false
        }
        socket.close()
        socket = Socket()
        socket.soTimeout = 5000
        try{
            Log.d(TAG, "Connecting... $ip:$port")
            socket.connect(InetSocketAddress(ip, port), timeout)
            connected = true
            Log.d(TAG, "Connected!")
            return@withContext true
        }catch (sto : SocketTimeoutException)
        {
            Log.d(TAG, "Connection timeout")
            return@withContext false
        }
        catch (e : Exception)
        {
            Log.e(TAG, "Unmanaged exception: ${e.message}")
            return@withContext false
        }
    }

    fun disconnect()
    {
        connected = false
        socket.close()

        socket = Socket()
        Log.i(TAG, "Disconnected")
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
                Log.e(TAG, "Error reading packet header")
                return@flow
            }
            val packetLen = java.nio.ByteBuffer.wrap(buf).getInt()

            val pkt = ByteArray(packetLen)
            if(!sockReadBytes(pkt, istream, packetLen))
            {
                disconnect()
                Log.e(TAG, "Error reading packet")
                return@flow
            }
            emit(String(pkt))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun send(data: String, delayms: Long = 0) = withContext(Dispatchers.IO)
    {
        delay(delayms)

        val ostream = socket.getOutputStream()
        var buf_len = java.nio.ByteBuffer.allocate(4).putInt(data.length).array()
        var buf_data = data.toByteArray()

//        Log.d(TAG, "Sending $buf_len bytes")
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
//            Log.d(TAG, "Received $res bytes")
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