package it.hixos.cameracontroller.ui

import android.content.Context
import android.net.ConnectivityManager
import android.opengl.Visibility
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import it.hixos.cameracontroller.SocketViewModel
import it.hixos.cameracontroller.databinding.FragmentConnectionBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.NumberFormatException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.SocketException


public class ConnectionFragment : Fragment() {

    private var _binding: FragmentConnectionBinding? = null

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
        val root: View = binding.root

        val buttonConnect = _binding?.buttonManualConnect
        val editTextAddress = _binding?.editTextServerAddress
        val progressBar = _binding?.progressBarConnection

        buttonConnect?.setOnClickListener (View.OnClickListener { view ->
            val action = ConnectionFragmentDirections.actionConnectionFragmentToConnectedFragment()
            view?.findNavController()?.navigate(action)
            return@OnClickListener
            if (editTextAddress != null) {
                val s = editTextAddress.text.split(":")

                if(s.size != 2)
                {
                    Log.e("ConnectionFragment", "Cannot parse address ${editTextAddress.text}")
                    return@OnClickListener
                }

                val ip = s[0]
                var port : Int = -1
                try {
                    port = s[1].toInt()
                }catch (ne: NumberFormatException)
                {
                    Log.e("ConnectionFragment", "Cannot parse address ${editTextAddress.text}")
                    return@OnClickListener
                }

                socketViewModel.connect(ip, port)
                progressBar?.visibility = View.VISIBLE
            }
        })

        socketViewModel.getConnected().observe(viewLifecycleOwner) {connected ->
            if(connected)
            {
                socketViewModel.startReceiver()
                val action = ConnectionFragmentDirections.actionConnectionFragmentToConnectedFragment()
                view?.findNavController()?.navigate(action)
            }
        }


        return root
    }

    fun navigateToConnected()
    {
        var nav_ctrl = findNavController()
        val action = ConnectionFragmentDirections.actionConnectionFragmentToConnectedFragment()
        nav_ctrl.navigate(action)
    }

    suspend fun udpecho() = withContext(Dispatchers.IO)
    {
        var b = ByteArray(1)
        b[0] = 0x66
        var p = DatagramPacket(b, 1)
        p.address = Inet4Address.getByName("239.154.117.1")
        p.port = 60050
        var sock = DatagramSocket()

        sock.send(p)
        Log.i("UDP", "Packet sent! ${b[0].toString(16)}")

        launch {
            var b2 = ByteArray(1)
            var prec = DatagramPacket(b2,1)
            var ok = false
            try{
                sock.receive(prec)
                ok = true
            }catch (se : SocketException)
            {
            }
            if(ok)
                Log.i("UDP", "Received response from ${prec.address.toString()}! ${b2[0].toString(16)}")
        }
        launch {
            delay(2000)
            Log.i("UDP", "Closing socket")
            sock.close()
        }
    }

    fun getNetMask()
    {
        val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var lp = cm.getLinkProperties(cm.activeNetwork)
//        if(lp != null && lp.nat64Prefix != null)
        var addr0 = lp?.nat64Prefix?.address
        Log.i("NET", "Addr0: ${addr0.toString()}")
        Log.i("NET", "Prefix ${lp?.nat64Prefix?.prefixLength}")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}