package it.hixos.cameracontroller.ui

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.activityViewModels
import com.github.razir.progressbutton.attachTextChangeAnimator
import com.github.razir.progressbutton.bindProgressButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import it.hixos.cameracontroller.*
import it.hixos.cameracontroller.databinding.FragmentIntervalometerBinding
import it.hixos.cameracontroller.databinding.FragmentManualControlBinding

class IntervalometerFragment : Fragment() {
    private var _binding: FragmentIntervalometerBinding? = null
    private val binding get() = _binding!!
    val socketViewModel: SocketViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    var started: Boolean = false
    var stopping = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val sharedPrefs = activity?.getPreferences(Context.MODE_PRIVATE)!!

        _binding = FragmentIntervalometerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val buttonStart = binding.buttonStartStop
        bindProgressButton(buttonStart)
        buttonStart.attachTextChangeAnimator()

        val max_captures = resources.getInteger(R.integer.intervalometer_max_captures)

        val crollerInterval = binding.crollerInterval
        val crollerNumCaptures = binding.crollerNumCaptures
        val progressBar = binding.progressBar
        progressBar.min = 0

        crollerInterval.progress = sharedPrefs.getInt(getString(R.string.key_intervalometer_interval), crollerInterval.min)
        crollerNumCaptures.progress = sharedPrefs.getInt(getString(R.string.key_intervalometer_total_captures), crollerNumCaptures.min)

        buttonStart.setOnClickListener { btn ->
            if(!started)
            {
                val e = EventModeIntervalometer()
                e.intervalms = binding.crollerInterval.progress * 1000
                e.totalCaptures = binding.crollerNumCaptures.progress

                if(e.intervalms == 0)
                    e.intervalms = -1
                if(e.totalCaptures == max_captures)
                    e.totalCaptures = -1
                socketViewModel.send(e)
                buttonStart.setText(R.string.stop)

                if(e.totalCaptures == max_captures){
                    progressBar.isIndeterminate = true
                }else{
                    progressBar.isIndeterminate = false
                    progressBar.max = binding.crollerNumCaptures.progress
                    progressBar.progress = 0
                }

                started = true
            }else if(!stopping){
                val e = EventModeStop()
                socketViewModel.send(e)
                buttonStart.showProgress {
                    buttonTextRes = R.string.stopping
                }
                stopping = true
            }
        }

        socketViewModel.getCurrentMode().observe(viewLifecycleOwner) { mode ->
            if(socketViewModel.getIntervalometerState().value!!.state != "CamNotReady")
                buttonStart.isEnabled = (mode == "Manual" || mode == "Intervalometer")
        }

        socketViewModel.getIntervalometerState().observe(viewLifecycleOwner) { state ->

            val (rMin, rSec) = toMinuteSeconds(state.interval * (state.total_captures - state.progress))

            if(state.interval < 0)
                binding.textViewRemainingTime.setText("-")
            else
                binding.textViewRemainingTime.setText("%d min %d sec".format(rMin, rSec))
            if(state.total_captures < 0) {
                binding.textViewRemainingTime.setText("∞")
                progressBar.isIndeterminate = true
                binding.textViewProgress.setText("%d/∞".format(state.progress))
            }else{
                progressBar.isIndeterminate = false
                progressBar.max = state.total_captures
                progressBar.progress = state.progress
                binding.textViewProgress.setText("%d/%d".format(state.progress, state.total_captures))
            }

            binding.textViewState.setText(state.state)

            when(state.state)
            {
                "CamNotReady" -> {
                    buttonStart.isEnabled = false
                    progressBar.isIndeterminate = false
                    if(stopping){
                        buttonStart.hideProgress(R.string.start)
                        stopping = false
                        started = false
                    }else{
                        buttonStart.setText(R.string.start)
                        started = false
                    }
                }
                "Ready" -> {
                    buttonStart.isEnabled = true
                    progressBar.isIndeterminate = false
                    if(stopping){
                        buttonStart.hideProgress(R.string.start)
                        stopping = false
                        started = false
                    }else{
                        buttonStart.setText(R.string.start)
                        started = false
                    }
                }
                else -> {
                    buttonStart.isEnabled = true
                    buttonStart.setText(R.string.stop)
                    started = true
                }
            }
        }

        crollerInterval.setOnProgressChangedListener { progress ->
            if(progress == 0) {
                crollerInterval.label = "Instant"
            }else{
                val (min, sec) = toMinuteSeconds(progress * 1000)
                crollerInterval.label = "%dm:%ds".format(min, sec)
            }
        }

        crollerInterval.setOnProgressSetListener { progress ->
            with (sharedPrefs.edit()) {
                putInt(getString(R.string.key_intervalometer_interval), progress)
                apply()
            }
        }

        crollerNumCaptures.setOnProgressSetListener { progress ->
            with (sharedPrefs.edit()) {
                putInt(getString(R.string.key_intervalometer_total_captures), progress)
                apply()
            }
        }

        crollerNumCaptures.setOnProgressChangedListener { progress ->
            if(progress == resources.getInteger(R.integer.intervalometer_max_captures))
                crollerNumCaptures.label = "∞"
            else
                crollerNumCaptures.label = progress.toString()
        }

        socketViewModel.send(EventGetCurrentMode())
        return root
    }

    fun toMinuteSeconds(ms: Int) : Pair<Int, Int>
    {
        var sec = (ms / 1000f).toInt()
        var min = (sec / 60f).toInt()
        sec = sec - min*60

        return Pair(min, sec)
    }

}