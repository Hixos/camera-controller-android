package it.hixos.cameracontroller.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import it.hixos.cameracontroller.SocketViewModel
import it.hixos.cameracontroller.databinding.FragmentConnectedBinding


class ConnectedFragment : Fragment() {
    private var _binding: FragmentConnectedBinding? = null
    private val binding get() = _binding!!
    val socketViewModel: SocketViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentConnectedBinding.inflate(inflater, container, false)
        val root: View = binding.root

        socketViewModel.getConnected().observe(viewLifecycleOwner) {connected ->
            if(!connected)
            {
                val action = ConnectedFragmentDirections.actionConnectedFragmentToConnectionFragment()
                view?.findNavController()?.navigate(action)
            }
        }

        return root
    }
}