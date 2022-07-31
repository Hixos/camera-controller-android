package it.hixos.cameracontroller.ui

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.github.razir.progressbutton.*
import it.hixos.cameracontroller.*
import it.hixos.cameracontroller.R
import it.hixos.cameracontroller.databinding.FragmentConnectedBinding
import it.hixos.cameracontroller.databinding.FragmentManualControlBinding
import kotlinx.coroutines.*
import java.lang.Float.min
import java.time.Duration
import java.time.Instant

class ManualControlFragment : Fragment() {
    private var _binding: FragmentManualControlBinding? = null
    private val binding get() = _binding!!
    val socketViewModel: SocketViewModel by activityViewModels()

    var camera_busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentManualControlBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val buttonCapture = binding.buttonProgressCapture
        bindProgressButton(buttonCapture)
        buttonCapture.attachTextChangeAnimator()

        buttonCapture.setOnClickListener {
            buttonCapture.showProgress {
                buttonTextRes = R.string.capturing
            }
            val e = EventCameraCmdCapture()
            socketViewModel.send(e)
            camera_busy = true
            buttonCapture.isClickable = false
        }

        val buttonErrorDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_button_error_24)
        buttonErrorDrawable?.setBounds(0, 0, 50, 50)
        val buttonOkDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_button_ok_24)
        buttonOkDrawable?.setBounds(0, 0, 50, 50)

        socketViewModel.getCurrentMode().observe(viewLifecycleOwner) { mode ->
            buttonCapture.isEnabled = mode == "Manual"
        }

        var captureProgressJob : Job? = null

        socketViewModel.getCCState().observe(viewLifecycleOwner) { state ->
            when(state)
            {
                "Capturing" -> {
                    camera_busy = true
                    buttonCapture.showProgress { buttonTextRes = R.string.capturing }
                }
                "Downloading" -> {
                    camera_busy = true
                    binding.progressBarCapture.isIndeterminate = false
                    buttonCapture.showProgress { buttonTextRes = R.string.downloading }
                }
                "Error" -> {
                    camera_busy = true
                    buttonCapture.showDrawable(buttonErrorDrawable!!) { buttonTextRes = R.string.error }
                }
                "Connection Error" -> {
                    camera_busy = true
                    buttonCapture.showDrawable(buttonErrorDrawable!!) { buttonTextRes = R.string.camera_missing }
                }
                "Ready" -> {
                    if(camera_busy) {
                        buttonCapture.showDrawable(buttonOkDrawable!!) {
                            buttonTextRes = R.string.done
                        }
                        lifecycleScope.launch(Dispatchers.Main)
                        {
                            delay(
                                resources.getInteger(R.integer.task_done_display_duration).toLong()
                            )
                            camera_busy = false
                            buttonCapture.hideProgress(R.string.capture)
                            buttonCapture.isClickable = true
                        }
                    }
                }
            }

            if(state == "Capturing")
            {
                binding.progressBarCapture.isIndeterminate = false
                val ss = socketViewModel.getShutterSpeed().value!!.toFloat() / 1000f
                binding.progressBarCapture.max = (ss).toInt()
                binding.progressBarCapture.visibility = View.VISIBLE
                captureProgressJob = lifecycleScope.async(Dispatchers.Main)
                {

                    do {
                        val dur = Duration.between(socketViewModel.getCaptureStartedTime().value, Instant.now()).toMillis()
                        binding.textViewCaptureTime.setText("%.1f/%.1f s".format(min(dur.toFloat()/1000f,ss/1000f), ss/1000f))
                        binding.progressBarCapture.progress = kotlin.math.min(dur.toInt(), binding.progressBarCapture.max)
                        Log.d("Prog", "${binding.progressBarCapture.progress}/${binding.progressBarCapture.max}")
                        delay(1000)
                    }while (dur < ss)

                    binding.progressBarCapture.isIndeterminate = true
                }
            } else {
                captureProgressJob?.cancel()
                captureProgressJob = null
                binding.textViewCaptureTime.setText("")

                if(state == "Downloading")
                {
                    binding.progressBarCapture.visibility = View.VISIBLE
                    binding.progressBarCapture.isIndeterminate = true
                }else{
                    binding.progressBarCapture.visibility = View.INVISIBLE
                    binding.progressBarCapture.isIndeterminate = false
                }
            }



            buttonCapture.isClickable = !camera_busy
        }


        socketViewModel.send(EventGetCurrentMode())
        return root
    }
}