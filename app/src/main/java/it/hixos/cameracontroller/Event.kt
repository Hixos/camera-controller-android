package it.hixos.cameracontroller

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

open class Event(id: Int)
{
    @SerializedName("event_id" ) private var _id : Int = 0

    init {
        this._id = id
    }
    fun getId() : Int = _id

}

class EventCameraCmdConnect : Event(10)
{
}

class EventCameraCmdDisconnect : Event(11)
{
}

class EventCameraCmdRecoverError : Event(12)
{
}

class EventCameraCmdCapture : Event(13)
{
}

class EventCameraCmdCapture_Internal : Event(14)
{
}

class EventCameraCmdDownload : Event(15)
{
    @SerializedName("download" ) var download : Boolean? = null
}

class EventCameraCmdDownload_Internal : Event(16)
{
}

class EventCameraConnected : Event(17)
{
}

class EventCameraReady : Event(18)
{
}

class EventCameraDisconnected : Event(19)
{
}

class EventCameraConnectionError : Event(20)
{
}

class EventCameraError : Event(21)
{
}

class EventCameraIgnoreError : Event(22)
{
}

class EventCameraCmdLowLatency : Event(23)
{
    @SerializedName("low_latency" ) var lowLatency : Boolean? = null
}

class EventCameraCaptureDone : Event(24)
{
    @SerializedName("downloaded" ) var downloaded : Boolean? = null
    @SerializedName("file" ) var file : String? = null
}

class EventCameraControllerState : Event(25)
{
    @SerializedName("state" ) var state : UByte? = null
}

class EventConfigGetShutterSpeed : Event(26)
{
}

class EventConfigSetShutterSpeed : Event(27)
{
    @SerializedName("shutter_speed" ) var shutterSpeed : Int? = null
}

class EventConfigValueShutterSpeed : Event(28)
{
    @SerializedName("shutter_speed" ) var shutterSpeed : Int? = null
    @SerializedName("bulb" ) var bulb : Boolean? = null
}

class EventConfigGetAperture : Event(29)
{
}

class EventConfigSetAperture : Event(30)
{
    @SerializedName("aperture" ) var aperture : Int? = null
}

class EventConfigValueAperture : Event(31)
{
    @SerializedName("aperture" ) var aperture : Int? = null
}

class EventConfigGetISO : Event(32)
{
}

class EventConfigSetISO : Event(33)
{
    @SerializedName("iso" ) var iso : Int? = null
}

class EventConfigValueISO : Event(34)
{
    @SerializedName("iso" ) var iso : Int? = null
}

class EventConfigGetBattery : Event(35)
{
}

class EventConfigValueBattery : Event(36)
{
    @SerializedName("battery" ) var battery : Int? = null
}

class EventConfigGetFocalLength : Event(37)
{
}

class EventConfigValueFocalLength : Event(38)
{
    @SerializedName("focal_length" ) var focalLength : Int? = null
}

class EventConfigGetFocusMode : Event(39)
{
}

class EventConfigValueFocusMode : Event(40)
{
    @SerializedName("focus_mode" ) var focusMode : String? = null
}

class EventConfigGetLongExpNR : Event(41)
{
}

class EventConfigValueLongExpNR : Event(42)
{
    @SerializedName("long_exp_nr" ) var longExpNr : Boolean? = null
}

class EventConfigGetVibRed : Event(43)
{
}

class EventConfigValueVibRed : Event(44)
{
    @SerializedName("vr" ) var vr : Boolean? = null
}

class EventConfigGetCaptureTarget : Event(45)
{
}

class EventConfigSetCaptureTarget : Event(46)
{
    @SerializedName("target" ) var target : String? = null
}

class EventConfigValueCaptureTarget : Event(47)
{
    @SerializedName("target" ) var target : String? = null
}

class EventConfigGetCameraMode : Event(48)
{
}

class EventConfigValueCameraMode : Event(49)
{
    @SerializedName("target" ) var target : String? = null
}

class EventConfigGetCommon : Event(50)
{
}



fun jsonToEvent(json: String) : Event?
{
    val gson = Gson()
    val e: Event = gson.fromJson(json, Event::class.java)
    when(e.getId())
    {
        10 -> return gson.fromJson(json, EventCameraCmdConnect::class.java)
        11 -> return gson.fromJson(json, EventCameraCmdDisconnect::class.java)
        12 -> return gson.fromJson(json, EventCameraCmdRecoverError::class.java)
        13 -> return gson.fromJson(json, EventCameraCmdCapture::class.java)
        14 -> return gson.fromJson(json, EventCameraCmdCapture_Internal::class.java)
        15 -> return gson.fromJson(json, EventCameraCmdDownload::class.java)
        16 -> return gson.fromJson(json, EventCameraCmdDownload_Internal::class.java)
        17 -> return gson.fromJson(json, EventCameraConnected::class.java)
        18 -> return gson.fromJson(json, EventCameraReady::class.java)
        19 -> return gson.fromJson(json, EventCameraDisconnected::class.java)
        20 -> return gson.fromJson(json, EventCameraConnectionError::class.java)
        21 -> return gson.fromJson(json, EventCameraError::class.java)
        22 -> return gson.fromJson(json, EventCameraIgnoreError::class.java)
        23 -> return gson.fromJson(json, EventCameraCmdLowLatency::class.java)
        24 -> return gson.fromJson(json, EventCameraCaptureDone::class.java)
        25 -> return gson.fromJson(json, EventCameraControllerState::class.java)
        26 -> return gson.fromJson(json, EventConfigGetShutterSpeed::class.java)
        27 -> return gson.fromJson(json, EventConfigSetShutterSpeed::class.java)
        28 -> return gson.fromJson(json, EventConfigValueShutterSpeed::class.java)
        29 -> return gson.fromJson(json, EventConfigGetAperture::class.java)
        30 -> return gson.fromJson(json, EventConfigSetAperture::class.java)
        31 -> return gson.fromJson(json, EventConfigValueAperture::class.java)
        32 -> return gson.fromJson(json, EventConfigGetISO::class.java)
        33 -> return gson.fromJson(json, EventConfigSetISO::class.java)
        34 -> return gson.fromJson(json, EventConfigValueISO::class.java)
        35 -> return gson.fromJson(json, EventConfigGetBattery::class.java)
        36 -> return gson.fromJson(json, EventConfigValueBattery::class.java)
        37 -> return gson.fromJson(json, EventConfigGetFocalLength::class.java)
        38 -> return gson.fromJson(json, EventConfigValueFocalLength::class.java)
        39 -> return gson.fromJson(json, EventConfigGetFocusMode::class.java)
        40 -> return gson.fromJson(json, EventConfigValueFocusMode::class.java)
        41 -> return gson.fromJson(json, EventConfigGetLongExpNR::class.java)
        42 -> return gson.fromJson(json, EventConfigValueLongExpNR::class.java)
        43 -> return gson.fromJson(json, EventConfigGetVibRed::class.java)
        44 -> return gson.fromJson(json, EventConfigValueVibRed::class.java)
        45 -> return gson.fromJson(json, EventConfigGetCaptureTarget::class.java)
        46 -> return gson.fromJson(json, EventConfigSetCaptureTarget::class.java)
        47 -> return gson.fromJson(json, EventConfigValueCaptureTarget::class.java)
        48 -> return gson.fromJson(json, EventConfigGetCameraMode::class.java)
        49 -> return gson.fromJson(json, EventConfigValueCameraMode::class.java)
        50 -> return gson.fromJson(json, EventConfigGetCommon::class.java)


        else -> return null
    }
}