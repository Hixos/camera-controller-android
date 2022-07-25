package it.hixos.cameracontroller

import android.util.JsonReader
import android.util.Log
import androidx.lifecycle.*
import com.google.gson.Gson
import com.google.gson.JsonIOException
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

    fun getShutterSpeedChoices() : LiveData<ArrayList<Int>>
    {
        return shutterSpeedChoices
    }

    fun getAperture() : LiveData<Int>
    {
        return aperture
    }

    fun getApertureChoices() : LiveData<ArrayList<Int>>
    {
        return apertureChoices
    }

    fun getISO() : LiveData<Int>
    {
        return iso
    }

    fun getISOChoices() : LiveData<ArrayList<Int>>
    {
        return isoChoices
    }

    fun getLightMeter() : LiveData<Float>
    {
        return lightmeter
    }

    fun getExpProgram() : LiveData<String>
    {
        return exp_program
    }

    fun getFocalLength() : LiveData<Int>
    {
        return focal_length
    }

    fun getBattery() : LiveData<Int>
    {
        return battery
    }

    fun getFocusMode() : LiveData<String>
    {
        return focus_mode
    }

    fun getAutoIso() : LiveData<Boolean>
    {
        return autoIso
    }

    fun getLongExpNR() : LiveData<Boolean>
    {
        return longExpNr
    }

    fun getCameraConnected() : LiveData<Boolean>
    {
        return camera_connected
    }

    fun getCCState() : LiveData<String>
    {
        return cc_state
    }

    fun getDownloadEnabled() : LiveData<Boolean>
    {
        return download_enabled
    }

    fun getCapturedFile() : LiveData<String>
    {
        return capture_file
    }

    fun getIntervalometerState() : LiveData<IntervalometerState>
    {
        return intervalometer_state
    }

    fun getCurrentMode() : LiveData<String>
    {
        return mode
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
                Log.e("SocketViewModel", "Error receiving from socket: ${ioe.message}")
            }finally {
                disconnect()
                Log.e("SocketViewModel", "End receive")
            }
        }
    }

    fun send(pkt: String, delayms: Long = 0)
    {
        viewModelScope.launch(Dispatchers.Main)
        {
            try{
                client.send(pkt, delayms)
            }catch(ioe : IOException)
            {
                disconnect()
                Log.e("SocketViewModel", "Error sending from socket: ${ioe.message}")
            }
        }
    }

    fun send(pkt: Any, delayms: Long = 0)
    {
        val gson = Gson()
        try {
            val s = gson.toJson(pkt)
            send(s, delayms)
        }catch (je : JsonIOException)
        {
            Log.e("SocketViewModel", "Send error: cannot convert class to gson")
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
            is EventConfigChoicesShutterSpeed -> {
                shutterSpeedChoices.value = e.shutterSpeedChoices
            }
            is EventConfigValueAperture -> {
                aperture.value = e.aperture
            }
            is EventConfigChoicesAperture -> {
                apertureChoices.value = e.apertureChoices
            }
            is EventConfigValueISO -> {
                iso.value = e.iso
            }
            is EventConfigChoicesISO -> {
                isoChoices.value = e.isoChoices
            }
            is EventConfigValueLightMeter -> {
                lightmeter.value = e.lightMeter!!/(e.max!! - e.min!!)*20;
            }
            is EventConfigValueExposureProgram -> {
                exp_program.value = e.exposureProgram
            }
            is EventConfigValueFocalLength -> {
                focal_length.value = e.focalLength
            }
            is EventConfigValueBattery -> {
                battery.value = e.battery
            }
            is EventConfigValueFocusMode -> {
                focus_mode.value = e.focusMode
            }
            is EventConfigValueAutoISO -> {
                autoIso.value = e.autoIso
            }
            is EventConfigValueLongExpNR -> {
                longExpNr.value = e.longExpNr
            }
            is EventCameraControllerState -> {
                camera_connected.value = e.cameraConnected
                cc_state.value = e.state
                download_enabled.value = e.downloadEnabled
            }
            is EventCameraCaptureDone -> {
                capture_file.value = e.file
            }
            is EventIntervalometerState -> {
                intervalometer_state.value = IntervalometerState(e)
            }
            is EventValueCurrentMode -> {
                mode.value = e.mode
            }
        }
    }

    private var shutterSpeed = MutableLiveData<Int>(0)
    private var shutterSpeedChoices = MutableLiveData<ArrayList<Int>>(arrayListOf<Int>())

    private var aperture = MutableLiveData<Int>(0)
    private var apertureChoices = MutableLiveData<ArrayList<Int>>(arrayListOf<Int>())

    private var iso = MutableLiveData<Int>(0)
    private var isoChoices = MutableLiveData<ArrayList<Int>>(arrayListOf<Int>())

    private var lightmeter = MutableLiveData<Float>(0f)
    private var exp_program = MutableLiveData<String>("M")
    private var focal_length = MutableLiveData<Int>(0)
    private var battery = MutableLiveData<Int>(100)
    private var focus_mode = MutableLiveData<String>("MF")

    private var autoIso = MutableLiveData<Boolean>(false)
    private var longExpNr = MutableLiveData<Boolean>(false)


    private var bulb = MutableLiveData<Boolean>(false)
    private var connected = MutableLiveData<Boolean>(false)
    private var camera_connected = MutableLiveData<Boolean>(false)
    private var cc_state = MutableLiveData<String>("Disconnected")
    private var download_enabled = MutableLiveData<Boolean>(false)
    private var capture_file = MutableLiveData<String>("")
    private var intervalometer_state = MutableLiveData<IntervalometerState>()
    private var mode = MutableLiveData<String>()

    private var client = JsonTcpClient()
}

class IntervalometerState(e : EventIntervalometerState? = null)
{
    var state: String = ""
    var interval : Int = 0
    var progress : Int = 0
    var total_captures : Int = 0

    init
    {
        if(e != null) {
            state = e.state!!
            interval = e.intervalms!!
            progress = e.numCaptures!!
            total_captures = e.totalCaptures!!
        }
    }
}
