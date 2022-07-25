package it.hixos.cameracontroller.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.sdsmdg.harjot.crollerTest.Croller
import it.hixos.cameracontroller.*
import it.hixos.cameracontroller.databinding.FragmentCameraConfigBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.NumberFormat


class CameraConfigFragment : Fragment() {
    private var _binding: FragmentCameraConfigBinding? = null
    private val binding get() = _binding!!
    private val socketViewModel: SocketViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCameraConfigBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val crollerShutterSpeed = binding.crollerShutterSpeed
        val crollerAperture = binding.crollerAperture
        val crollerISO = binding.crollerISO
        val switchDownload = binding.switchDownload

        val disabling_views = listOf<View>(binding.crollerShutterSpeed, binding.crollerAperture,
            binding.crollerISO, binding.switchAutoISO, binding.switchLongExpNR, binding.textViewLightMeter,
            binding.textViewCameraMode, binding.textViewFocalLength, binding.textViewFocusMode, binding.textViewBattery)

        socketViewModel.getDownloadEnabled().observe(viewLifecycleOwner) {download ->
            switchDownload.isChecked = download
        }

        switchDownload.setOnCheckedChangeListener { button, checked ->
            if(button.isPressed())
            {
                var e = EventCameraCmdDownload()
                e.download = checked
                socketViewModel.send(e)
            }
        }
        socketViewModel.getCapturedFile().observe(viewLifecycleOwner) { file ->
            binding.textViewLastCapture.setText(file)
        }

        socketViewModel.getShutterSpeed().observe(viewLifecycleOwner) { ss ->
             val choices = socketViewModel.getShutterSpeedChoices().value!!

            updateCroller(crollerShutterSpeed, ss, augmentShutterSpeedChoices(choices), ::shutterSpeedToString)
        }

        socketViewModel.getShutterSpeedChoices().observe(viewLifecycleOwner) { choices ->
            val ss = socketViewModel.getShutterSpeed().value!!
            updateCroller(crollerShutterSpeed, ss, augmentShutterSpeedChoices(choices), ::shutterSpeedToString)
        }

        socketViewModel.getAperture().observe(viewLifecycleOwner) { ap ->
            val choices = socketViewModel.getApertureChoices().value!!

            updateCroller(crollerAperture, ap, choices, ::apertureToString)
        }

        socketViewModel.getApertureChoices().observe(viewLifecycleOwner) { choices ->
            val ap = socketViewModel.getAperture().value!!
            updateCroller(crollerAperture, ap, choices, ::apertureToString)
        }

        socketViewModel.getISO().observe(viewLifecycleOwner) { iso ->
            val choices = socketViewModel.getISOChoices().value!!

            updateCroller(crollerISO, iso, choices, ::isoToString)
        }

        socketViewModel.getISOChoices().observe(viewLifecycleOwner) { choices ->
            val iso = socketViewModel.getISO().value!!
            updateCroller(crollerISO, iso, choices, ::isoToString)
        }

        socketViewModel.getLightMeter().observe(viewLifecycleOwner) { lightmeter ->
            binding.textViewLightMeter.setText("%+.1f".format(lightmeter))
        }

        socketViewModel.getExpProgram().observe(viewLifecycleOwner) { exp_program ->
            binding.textViewCameraMode.setText(exp_program)
        }

        socketViewModel.getFocalLength().observe(viewLifecycleOwner) { focal_length ->
            binding.textViewFocalLength.setText(focal_length.toString().plus(" mm"))
        }

        socketViewModel.getBattery().observe(viewLifecycleOwner) { battery ->
            binding.textViewBattery.setText(battery.toString().plus("%"))
        }

        socketViewModel.getFocusMode().observe(viewLifecycleOwner) { focus_mode ->
            binding.textViewFocusMode.setText(focus_mode)
        }

        socketViewModel.getAutoIso().observe(viewLifecycleOwner) { auto_iso ->
            binding.switchAutoISO.isChecked = auto_iso
        }

        socketViewModel.getLongExpNR().observe(viewLifecycleOwner) { long_exp_nr ->
            binding.switchLongExpNR.isChecked = long_exp_nr
        }

        socketViewModel.getCameraConnected().observe(viewLifecycleOwner) { connected ->
            if(connected)
            {
                disabling_views.forEach { view -> view.isEnabled = true }
            }else{
                disabling_views.forEach { view -> view.isEnabled = false }
            }
        }

        socketViewModel.getCCState().observe(viewLifecycleOwner) { state ->
            binding.textViewState.setText(state)
        }

//        socketViewModel.setN

        crollerShutterSpeed.setOnProgressChangedListener { progress ->
            val choices = augmentShutterSpeedChoices(socketViewModel.getShutterSpeedChoices().value!!)
            val ss = choices.getOrElse(progress) {_ -> 0}
            crollerShutterSpeed.label = shutterSpeedToString(ss)
        }

        crollerShutterSpeed.setOnProgressSetListener { progress ->
            val choices = augmentShutterSpeedChoices(socketViewModel.getShutterSpeedChoices().value!!)
            val ss = choices.getOrElse(progress) {_ -> 0}
            if(ss != 0) {
                var ess = EventConfigSetShutterSpeed()
                ess.shutterSpeed = ss
                socketViewModel.send(ess)
                var e3 = EventConfigGetLightMeter()
                socketViewModel.send(e3, resources.getInteger(R.integer.lightmeter_cmd_delay).toLong())
            }
        }

        crollerAperture.setOnProgressChangedListener { progress ->
            val choices = socketViewModel.getApertureChoices().value!!
            val ap = choices.getOrElse(progress) {_ -> 0}
            crollerAperture.label = apertureToString(ap)
        }

        crollerAperture.setOnProgressSetListener { progress ->
            val choices = socketViewModel.getApertureChoices().value!!
            val ap = choices.getOrElse(progress) {_ -> 0}
            if(ap != 0) {
                var e = EventConfigSetAperture()
                e.aperture = ap
                socketViewModel.send(e)
                var e2 = EventConfigGetChoicesAperture()
                socketViewModel.send(e2)
                var e3 = EventConfigGetLightMeter()
                socketViewModel.send(e3, resources.getInteger(R.integer.lightmeter_cmd_delay).toLong())
            }
        }

        crollerISO.setOnProgressChangedListener { progress ->
            val choices = socketViewModel.getISOChoices().value!!
            val iso = choices.getOrElse(progress) {_ -> 0}
            crollerISO.label = isoToString(iso)
        }

        crollerISO.setOnProgressSetListener { progress ->
            val choices = socketViewModel.getISOChoices().value!!
            val iso = choices.getOrElse(progress) {_ -> 0}
            if(iso != 0) {
                var e = EventConfigSetISO()
                e.iso = iso
                socketViewModel.send(e)
                var e3 = EventConfigGetLightMeter()
                socketViewModel.send(e3, resources.getInteger(R.integer.lightmeter_cmd_delay).toLong())
            }
        }

        binding.textViewFocalLength.setOnClickListener{
            socketViewModel.send(EventConfigGetFocalLength())
            socketViewModel.send(EventConfigGetAperture())
            socketViewModel.send(EventConfigGetChoicesAperture())
            socketViewModel.send(EventConfigGetLightMeter(), resources.getInteger(R.integer.lightmeter_cmd_delay).toLong())
        }

        binding.textViewState.setOnClickListener{
            socketViewModel.send(EventConfigGetAll())
        }


        binding.switchAutoISO.setOnCheckedChangeListener { view, checked ->
            if(view.isPressed) {
                var e = EventConfigSetAutoISO()
                e.autoIso = checked
                socketViewModel.send(e)
            }
        }

        binding.switchLongExpNR.setOnCheckedChangeListener { view, checked ->
            if(view.isPressed) {
                var e = EventConfigSetLongExpNR()
                e.longExpNr = checked
                socketViewModel.send(e)
            }
        }
// Request data
        socketViewModel.send(EventConfigGetAll())
        return root
    }

    fun augmentShutterSpeedChoices(choices: ArrayList<Int>) : ArrayList<Int>
    {
        var out = ArrayList<Int>()
        out.addAll(choices)
        out.addAll(listOf(45000000, 60000000, 75000000, 90000000, 105000000, 120000000, 150000000, 180000000))
        return out
    }


    fun updateCroller(croller: Croller, value: Int, list: ArrayList<Int>, valToString : (Int) -> String)
    {
        croller.min = 0
        croller.max = list.size - 1
        val i = list.indexOfFirst { it == value }
        if (i != -1)
        {
            croller.progress = i
            croller.label = valToString(value)
        }else{
            Log.e("CameraConfigFragment", "Could not find element $value in croller list")
        }
    }

    fun shutterSpeedToString(ss: Int) : String
    {
        if(ss == 0)
            return "0 s"
        val sec = ss.toFloat()/1000000
        val nf: NumberFormat = DecimalFormat("####.#")
        if (sec < 1)
        {
            val frac = 1/sec
            return "1/".plus(nf.format(frac)).plus(" s")
        }else{
            return nf.format(sec).plus(" s")
        }
    }

    fun apertureToString(ap: Int) : String
    {
        val v = ap.toFloat() / 100
        val nf: NumberFormat = DecimalFormat("##.#")

        return "f ".plus(nf.format(v))
    }

    fun isoToString(iso: Int) : String
    {
        return "ISO ".plus(iso.toString())
    }
}