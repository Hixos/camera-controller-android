package it.hixos.cameracontroller

import android.util.JsonReader
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException


class SocketViewModel() : ViewModel() {
    override fun onCleared() {
        Log.d("SocketViewModel", "onCleared")
        disconnect()
        super.onCleared()
    }

    fun getConnected() : LiveData<Boolean>
    {
        return connected
    }

    fun getShutterSpeed() : LiveData<Int>
    {
        return shutterSpeed
    }

    fun getBulb() : LiveData<Boolean>
    {
        return bulb
    }

    fun connect(ip: String, port: Int)
    {
        viewModelScope.launch(Dispatchers.Main)
        {
            val connection_result = viewModelScope.async { client.connect(ip, port)}
            connected.value = connection_result.await()
        }
    }

    fun disconnect()
    {
        client.disconnect()
        connected.value = false
    }

    fun startReceiver()
    {
        viewModelScope.launch(Dispatchers.Main)
        {
            try {
                client.receiver().collect { pkt -> handlePacket(pkt) }
            }catch(ioe : IOException)
            {
                disconnect()
                Log.e("SocketViewModel", "Error receiving from socket: ${ioe.message}")
            }
        }
    }

    fun send(pkt: String)
    {
        viewModelScope.launch(Dispatchers.Main)
        {
            try{
                client.send(pkt)
            }catch(ioe : IOException)
            {
                disconnect()
                Log.e("SocketViewModel", "Error sending from socket: ${ioe.message}")
            }
        }
    }

    private fun handlePacket(packet: String)
    {
        var json : JSONObject?
        try {
            json = JSONObject(packet)
        }catch (je : JSONException)
        {
            Log.e("SocketViewModel", "Error decoding JSON: $packet")
            return
        }

        if (json.has("event_id")) {
            val e = jsonToEvent(packet)
            if (e != null)
                handleEvent(e)
        }
        Log.i("SocketViewModel", "Received packet: $packet")
    }

    private fun handleEvent(e: Event)
    {
        Log.d("SocketViewModel", "Handle Event")
        when(e)
        {
            is EventConfigValueShutterSpeed -> {
                shutterSpeed.value = e.shutterSpeed
                bulb.value = e.bulb
            }
        }
    }

    private var shutterSpeed = MutableLiveData<Int>(0)
    private var bulb = MutableLiveData<Boolean>(false)
    private var connected = MutableLiveData<Boolean>(false)

    private var client = JsonTcpClient()
}