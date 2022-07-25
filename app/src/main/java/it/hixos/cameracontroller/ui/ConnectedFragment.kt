package it.hixos.cameracontroller.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import it.hixos.cameracontroller.EventGetCurrentMode
import it.hixos.cameracontroller.R
import it.hixos.cameracontroller.SocketViewModel
import it.hixos.cameracontroller.databinding.FragmentConnectedBinding


class ConnectedFragment : Fragment() {
    private var _binding: FragmentConnectedBinding? = null
    private val binding get() = _binding!!
    val socketViewModel: SocketViewModel by activityViewModels()

    var auto_selected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentConnectedBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val bottomNav : BottomNavigationView = binding.bottomNavigationView

        val navHost = childFragmentManager.findFragmentById(R.id.nav_host_fragment_mode) as NavHostFragment

        val navController = navHost.navController

//        binding.root.findViewById<BottomNavigationView>(R.id.bottomNavigationView).setupWithNavController(navController!!)

        socketViewModel.getConnected().observe(viewLifecycleOwner) {connected ->
            if(!connected)
            {
                val action = ConnectedFragmentDirections.actionConnectedFragmentToConnectionFragment()
                view?.findNavController()?.navigate(action)
            }
        }

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_manual_ctrl, R.id.navigation_intervalometer, R.id.navigation_log
            )
        )
//        setupActionBarWithNavController(activity, navController, appBarConfiguration)

        bottomNav.setupWithNavController(navController)

        socketViewModel.getCurrentMode().observe(viewLifecycleOwner) { mode ->
            val sel = auto_selected
            auto_selected = true
            when(mode)
            {
                "Intervalometer" -> {
                    if(!sel)
                        bottomNav.selectedItemId = R.id.navigation_intervalometer
                    binding.textViewMode.setText(R.string.mode_intervalometer)
                }
                "Manual" -> {
                    binding.textViewMode.setText(R.string.mode_manual_mode)
                }
                else -> {
                    auto_selected = false
                }
            }

        }

        socketViewModel.send(EventGetCurrentMode())
        return root
    }
}