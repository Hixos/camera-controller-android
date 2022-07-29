package it.hixos.cameracontroller.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.razir.progressbutton.attachTextChangeAnimator
import com.github.razir.progressbutton.bindProgressButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import it.hixos.cameracontroller.*
import it.hixos.cameracontroller.databinding.FragmentIntervalometerBinding
import java.lang.Integer.max
import kotlin.math.pow
import kotlin.math.round


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

        val max_captures = resources.getInteger(R.integer.intervalometer_max_captures_count)

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
                if(binding.crollerInterval.progress == 0)
                    e.intervalms = -1
                else
                    e.intervalms = crollerToSec(binding.crollerInterval.progress) * 1000

                if(binding.crollerNumCaptures.progress == max_captures)
                    e.totalCaptures = -1
                else
                    e.totalCaptures = crollerToNum(binding.crollerNumCaptures.progress)

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
            {
                buttonStart.isEnabled = (mode == "Manual" || mode == "Intervalometer")
            }
        }

        socketViewModel.getIntervalometerState().observe(viewLifecycleOwner) { state ->

            val (rMin, rSec) = toMinuteSeconds(round(state.interval * (state.total_captures - state.progress) / 1000f).toInt())

            if(state.interval < 0) {
                binding.textViewRemainingTime.setText("-")
            }else {
                binding.textViewRemainingTime.setText("%d min %d sec".format(rMin, rSec))
            }
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
                    crollerInterval.isEnabled = true
                    crollerNumCaptures.isEnabled = true
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
                    crollerInterval.isEnabled = true
                    crollerNumCaptures.isEnabled = true
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
                    crollerInterval.isEnabled = false
                    crollerNumCaptures.isEnabled = false
                    buttonStart.isEnabled = true
                    buttonStart.setText(R.string.stop)
                    started = true
                    crollerInterval.progress = msToCroller(state.interval)
                    crollerNumCaptures.progress = numToCroller(state.total_captures)
                }
            }
        }

        crollerInterval.setOnProgressChangedListener { progress ->
            updateIntervalCrollerLabel(progress)
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
            updateNumCaptureCrollerLabel(progress)
        }

        socketViewModel.send(EventGetCurrentMode())
        return root
    }

    fun toMinuteSeconds(sec: Int) : Pair<Int, Int>
    {
        val min = (sec / 60f).toInt()
        val s = sec - min * 60

        return Pair(min, s)
    }

    fun updateIntervalCrollerLabel(progress: Int)
    {
        if(progress == 0) {
            binding.crollerInterval.label = "Instant"
        }else{
            val (min, sec) = toMinuteSeconds(crollerToSec(progress))
            binding.crollerInterval.label = "%02dm:%02ds".format(min, sec)
        }
    }

    fun updateNumCaptureCrollerLabel(progress: Int)
    {
        if(progress == resources.getInteger(R.integer.intervalometer_max_captures_count))
            binding.crollerNumCaptures.label = "∞"
        else
            binding.crollerNumCaptures.label = crollerToNum(progress).toString()
    }

    private val p_tosec = 2.5f

    fun crollerToSec(cv: Int,
                    crollMax: Int = resources.getInteger(R.integer.intervalometer_max_interval_count),
                     maxSec: Int = resources.getInteger(R.integer.intervalometer_max_interval)) : Int
    {
        val N = maxSec.toFloat() / crollMax.toFloat().pow(p_tosec)

        val sec = max(round(N*cv.toFloat().pow(p_tosec)).toInt(), 1)
        return when {
            sec > 600 -> (round(sec/60f)*60).toInt()
            sec > 300 -> (round(sec/30f)*30).toInt()
            sec > 120 -> (round(sec/15f)*15).toInt()
            sec > 60 -> (round(sec/10f)*10).toInt()
            sec > 15 -> (round(sec/5f)*5).toInt()
            else -> sec
        }
    }

    private val p_tonum = 2.75f

    fun crollerToNum(cv: Int,
                     crollMax: Int = resources.getInteger(R.integer.intervalometer_max_captures_count) - 1,
                     maxNum: Int = resources.getInteger(R.integer.intervalometer_max_captures)) : Int
    {
        val N = maxNum.toFloat() / crollMax.toFloat().pow(p_tonum)

        val num = max(round(N*(cv.toFloat()).pow(p_tonum)).toInt(), 1)
        return when {
            num > 100 -> (round(num/10f)*10).toInt()
            num > 50 -> (round(num/5f)*5).toInt()
            else -> num
        }
    }

    fun msToCroller(ms: Int,
                    crollMax: Int = resources.getInteger(R.integer.intervalometer_max_interval_count),
                    maxSec: Int = resources.getInteger(R.integer.intervalometer_max_interval)) : Int
    {
        if(ms > 0)
        {
            val N = maxSec.toFloat() * 1000 / crollMax.toFloat().pow(p_tosec)
            return round((ms / N).pow(1 / p_tosec)).toInt()
        }else{
            return 0
        }
    }

    fun numToCroller(num: Int,
                    crollMax: Int = resources.getInteger(R.integer.intervalometer_max_captures_count) - 1,
                    maxNum: Int = resources.getInteger(R.integer.intervalometer_max_captures)) : Int
    {
        if(num > 0) {
            val N = maxNum.toFloat()  / crollMax.toFloat().pow(p_tonum)
            return round((num.toFloat() / N).pow(1 / p_tonum)).toInt()
        }else{
            return crollMax + 1
        }
    }

}