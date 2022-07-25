package it.hixos.cameracontroller.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import it.hixos.cameracontroller.R
import it.hixos.cameracontroller.SocketViewModel
import it.hixos.cameracontroller.databinding.FragmentIntervalometerBinding
import it.hixos.cameracontroller.databinding.FragmentLogBinding

class LogFragment : Fragment() {
    private var _binding: FragmentLogBinding? = null
    private val binding get() = _binding!!
    val socketViewModel: SocketViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLogBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }
}