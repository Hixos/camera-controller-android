package it.hixos.cameracontroller.ui

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import it.hixos.cameracontroller.*
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // The usage of an interface lets you inject your own implementation
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.action_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId)
                {
                    R.id.menu_restart_cc -> {
                        socketViewModel.send(EventCmdRestart())
                        Toast.makeText(requireContext(), "Restarting...", Toast.LENGTH_LONG)
                    }

                    R.id.menu_raspi_reboot -> {
                        socketViewModel.send(EventCmdReboot())
                        Toast.makeText(requireContext(), "Rebooting...", Toast.LENGTH_LONG)
                    }

                    R.id.menu_raspi_shutdown -> {
                        socketViewModel.send(EventCmdShutdown())
                        Toast.makeText(requireContext(), "Shutting down...", Toast.LENGTH_LONG)
                    }
                }
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
}