package it.hixos.cameracontroller.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.gson.Gson
import com.sdsmdg.harjot.crollerTest.Croller
import it.hixos.cameracontroller.*
import it.hixos.cameracontroller.databinding.FragmentCameraConfigBinding
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

        val button = binding.button
        val crollerShutterSpeed = binding.crollerShutterSpeed

        button.setOnClickListener(View.OnClickListener { view ->
            var ess = EventConfigGetChoicesShutterSpeed()
            val gson = Gson()

            socketViewModel.send(gson.toJson(ess))
        })

        socketViewModel.getShutterSpeed().observe(viewLifecycleOwner) { ss ->
             val choices = socketViewModel.getShutterSpeedChoices().value!!

            updateCroller(crollerShutterSpeed, ss, augmentShutterSpeedChoices(choices), ::shutterSpeedToString)
        }

        socketViewModel.getShutterSpeedChoices().observe(viewLifecycleOwner) { choices ->
            val ss = socketViewModel.getShutterSpeed().value!!
            updateCroller(crollerShutterSpeed, ss, augmentShutterSpeedChoices(choices), ::shutterSpeedToString)
        }

        crollerShutterSpeed.setOnProgressChangedListener { progress ->
            val choices = augmentShutterSpeedChoices(socketViewModel.getShutterSpeedChoices().value!!)
            val ss = choices.getOrElse(progress) {_ -> 0}
            crollerShutterSpeed.label = shutterSpeedToString(ss)

            var ess = EventConfigSetShutterSpeed()
            ess.shutterSpeed = ss
        }

        crollerShutterSpeed.setOnProgressSetListener { progress ->
            val choices = augmentShutterSpeedChoices(socketViewModel.getShutterSpeedChoices().value!!)
            val ss = choices.getOrElse(progress) {_ -> 0}
            Log.d("CameraConfigFragment", "On Progress Set $ss")
            var ess = EventConfigSetShutterSpeed()
            if(ss != 0) {
                ess.shutterSpeed = ss
                socketViewModel.send(ess)
            }
        }

        // Request data
        val ecomm = EventConfigGetCommon()
        socketViewModel.send(ecomm)
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
}