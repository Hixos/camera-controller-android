package it.hixos.cameracontroller.ui

import android.os.Bundle
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

        socketViewModel.getCCState().observe(viewLifecycleOwner) { state ->
            when(state)
            {
                "Capturing" -> {
                    camera_busy = true
                    buttonCapture.showProgress { buttonTextRes = R.string.capturing }
                }
                "Downloading" -> {
                    camera_busy = true
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

            buttonCapture.isClickable = !camera_busy
        }


        socketViewModel.send(EventGetCurrentMode())
        return root
    }
}