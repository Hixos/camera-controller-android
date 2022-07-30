package it.hixos.cameracontroller.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.opengl.Visibility
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.github.razir.progressbutton.*
import it.hixos.cameracontroller.R
import it.hixos.cameracontroller.SocketViewModel
import it.hixos.cameracontroller.databinding.FragmentConnectionBinding
import kotlinx.coroutines.*
import java.lang.NumberFormatException
import java.net.*


public class ConnectionFragment : Fragment() {
    private val TAG = "ConnectionFragment"
    private val NSD_SERVICE_TYPE = "_workstation._tcp."
    private val NSD_SERVICE_NAME = "cameracontroller"
    private var _binding: FragmentConnectionBinding? = null

    private var nsdManager: NsdManager? = null
    private var connectJob : Job? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    val socketViewModel: SocketViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnectionBinding.inflate(inflater, container, false)
        val sharedPrefs = activity?.getPreferences(Context.MODE_PRIVATE)!!
        val root: View = binding.root

        val buttonConnect = binding.buttonManualConnect
        val editTextAddress = binding.editTextServerAddress
        val progressBar = binding.progressBarConnection

        bindProgressButton(buttonConnect)
        buttonConnect.attachTextChangeAnimator()

        editTextAddress.setText(sharedPrefs.getString(getString(R.string.key_server_address), ""))

        progressBar.setOnClickListener(View.OnClickListener {
            stopDiscovery()
            buttonConnect.visibility = View.VISIBLE
            editTextAddress.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
        })

        buttonConnect.setOnClickListener (View.OnClickListener { view ->
            with (sharedPrefs.edit()) {
                putString(getString(R.string.key_server_address), editTextAddress.text.toString())
                apply()
            }

            val ip = editTextAddress.text.toString()

            socketViewModel.connect(ip)

            buttonConnect.showProgress { buttonTextRes = R.string.connecting }
            buttonConnect.isClickable = false
        })


        val buttonErrorDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_button_error_24)
        buttonErrorDrawable?.setBounds(0, 0, 50, 50)
        val buttonOkDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_button_ok_24)
        buttonOkDrawable?.setBounds(0, 0, 50, 50)


        socketViewModel.getConnected().observe(viewLifecycleOwner) {connected ->
            if(connected)
            {
                socketViewModel.startReceiver()
                val action = ConnectionFragmentDirections.actionConnectionFragmentToConnectedFragment()
                view?.findNavController()?.navigate(action)
                buttonConnect.showDrawable(buttonOkDrawable!!) { buttonTextRes = R.string.connected }
            }
            else
            {
                buttonConnect.showDrawable(buttonErrorDrawable!!) { buttonTextRes = R.string.error }
            }

            lifecycleScope.launch(Dispatchers.Main)
            {
                delay(
                    resources.getInteger(R.integer.task_done_display_duration).toLong()
                )
                buttonConnect.hideProgress(R.string.connect)
                buttonConnect.isClickable = true
            }
        }
        nsdManager = requireActivity().application.getSystemService(Context.NSD_SERVICE) as NsdManager
        nsdManager?.discoverServices(NSD_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }
    override fun onDestroyView() {
        super.onDestroyView()
        stopDiscovery()
        _binding = null
    }

    fun stopDiscovery()
    {
        try {
            connectJob?.cancel()
            connectJob = null
            nsdManager?.stopServiceDiscovery(discoveryListener)
        }catch (e: IllegalArgumentException)
        {

        }
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {

        // Called as soon as service discovery begins.
        override fun onDiscoveryStarted(regType: String) {
            Log.d(TAG, "Service discovery started ($regType)")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            // A service was found! Do something with it.
            Log.d(TAG, "Service discovery success: ${service.serviceName}")
            if(service.serviceName.contains(NSD_SERVICE_NAME))
            {
                nsdManager?.resolveService(service, resolveListener)
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            Log.e(TAG, "service lost: $service")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.i(TAG, "Discovery stopped: $serviceType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed: Error code:$errorCode")
            nsdManager?.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed: Error code:$errorCode")
            nsdManager?.stopServiceDiscovery(this)
        }
    }

    private val resolveListener = object : NsdManager.ResolveListener {

        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Called when the resolve fails. Use the error code to debug.
            Log.e(TAG, "Resolve failed: $errorCode")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Log.e(TAG, "Resolve Succeeded. $serviceInfo")
            if(connectJob != null) {
                connectJob?.cancel()
                connectJob = null
            }
            if (serviceInfo.serviceName.startsWith(NSD_SERVICE_NAME)) {
                connectJob = socketViewModel.connectLoop(serviceInfo.host.hostAddress!!)
                return
            }
        }
    }
}