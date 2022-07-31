package it.hixos.cameracontroller

import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.Nullable
import androidx.lifecycle.*
import com.google.gson.Gson
import com.google.gson.JsonIOException
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean


class SocketViewModel() : ViewModel() {
    private val PORT = 60099;

    override fun onCleared() {
        Log.d("SocketViewModel", "onCleared")
        disconnect()
        super.onCleared()
    }

    fun getConnected() : LiveData<Boolean>
    {
        return connected
    }

    fun getCaptureStartedTime() : LiveData<Instant>
    {
        return capture_started_time
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

    fun connect(ip: String, port: Int = PORT)
    {
        viewModelScope.launch(Dispatchers.Main)
        {
            val connection_result = viewModelScope.async { client.connect(ip, port)}
            connected.value = connection_result.await()
        }
    }

    fun connectLoop(ip: String, port: Int = PORT) : Job
    {
        return viewModelScope.async(Dispatchers.Main)
        {
            val connection_result = viewModelScope.async {
                while(!client.connect(ip, port))
                {
                    delay(1000)
                }
            }

            connection_result.join()

            connected.value = true
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
//        Log.i("SocketViewModel", "Received packet: $packet")
    }

    private fun handleEvent(e: Event)
    {
//        Log.d("SocketViewModel", "Handle Event")
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
            is EventHeartBeat -> {
                last_hb = System.currentTimeMillis()
            }
            is EventCameraCaptureStarted -> {
                Log.d("dada", "ADdsad")
                capture_started_time.value = Instant.now()
            }
        }
    }

    private var shutterSpeed = OnlyChangeMutableLiveData<Int>(0)
    private var shutterSpeedChoices = OnlyChangeMutableLiveData<ArrayList<Int>>(arrayListOf<Int>())

    private var aperture = OnlyChangeMutableLiveData<Int>(0)
    private var apertureChoices = OnlyChangeMutableLiveData<ArrayList<Int>>(arrayListOf<Int>())

    private var iso = OnlyChangeMutableLiveData<Int>(0)
    private var isoChoices = OnlyChangeMutableLiveData<ArrayList<Int>>(arrayListOf<Int>())

    private var lightmeter = OnlyChangeMutableLiveData<Float>(0f)
    private var exp_program = OnlyChangeMutableLiveData<String>("M")
    private var focal_length = OnlyChangeMutableLiveData<Int>(0)
    private var battery = OnlyChangeMutableLiveData<Int>(100)
    private var focus_mode = OnlyChangeMutableLiveData<String>("MF")

    private var autoIso = OnlyChangeMutableLiveData<Boolean>(false)
    private var longExpNr = OnlyChangeMutableLiveData<Boolean>(false)


    private var bulb = OnlyChangeMutableLiveData<Boolean>(false)
    private var connected = SingleLiveEvent<Boolean>()
    private var camera_connected = OnlyChangeMutableLiveData<Boolean>(false)
    private var cc_state = OnlyChangeMutableLiveData<String>("Disconnected")
    private var download_enabled = OnlyChangeMutableLiveData<Boolean>(false)

    private var capture_started_time = OnlyChangeMutableLiveData<Instant>()
    private var capture_file = OnlyChangeMutableLiveData<String>("")
    private var intervalometer_state = OnlyChangeMutableLiveData<IntervalometerState>()
    private var mode = OnlyChangeMutableLiveData<String>()

    private var last_hb : Long = 0

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

/**
 * A lifecycle-aware observable that sends only new updates after subscription, used for events like
 * navigation and Snackbar messages.
 *
 *
 * This avoids a common problem with events: on configuration change (like rotation) an update
 * can be emitted if the observer is active. This LiveData only calls the observable if there's an
 * explicit call to setValue() or call().
 *
 *
 * Note that only one observer is going to be notified of changes.
 */
class SingleLiveEvent<T>() : MutableLiveData<T>() {
    private val mPending: AtomicBoolean = AtomicBoolean(false)

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T?>) {
        if (hasActiveObservers()) {
            Log.w(TAG, "Multiple observers registered but only one will be notified of changes.")
        }

        // Observe the internal MutableLiveData
        super.observe(owner, Observer { t ->
            if (mPending.compareAndSet(true, false)) {
                observer.onChanged(t)
            }
        })
    }

    @MainThread
    override fun setValue(@Nullable t: T?) {
        mPending.set(true)
        super.setValue(t)
    }

    /**
     * Used for cases where T is Void, to make calls cleaner.
     */
    @MainThread
    fun call() {
        value = null
    }

    companion object {
        private const val TAG = "SingleLiveEvent"
    }
}

class OnlyChangeMutableLiveData<T>(v: T? = null) : MutableLiveData<T>(v) {

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T?>) {
        // Observe the internal MutableLiveData
        super.observe(owner, Observer { t ->
            observer.onChanged(t)
        })
    }

    @MainThread
    override fun setValue(@Nullable t: T?) {
        if(t != super.getValue())
            super.setValue(t)
    }

    /**
     * Used for cases where T is Void, to make calls cleaner.
     */
    @MainThread
    fun call() {
        value = null
    }

    companion object {
        private const val TAG = "SingleLiveEvent"
    }
}