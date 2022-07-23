package it.hixos.cameracontroller.ui

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.gson.Gson
import it.hixos.cameracontroller.EventConfigGetShutterSpeed
import it.hixos.cameracontroller.R
import it.hixos.cameracontroller.SocketViewModel
import it.hixos.cameracontroller.databinding.FragmentCameraConfigBinding


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

//        val button = binding.button

//        button.setOnClickListener(View.OnClickListener { view ->
//            var ess = EventConfigGetShutterSpeed()
//            val gson = Gson()
//
//            socketViewModel.send(gson.toJson(ess))
//        })
//
//        socketViewModel.getShutterSpeed().observe(viewLifecycleOwner) { shutter_speed ->
//            Log.d("CameraConfigFragment", "Setting shutter speed")
//            val ss = shutter_speed.toFloat()
//            button.text = "SS: ${ss/1000000}"
//        }

        return root
    }
}