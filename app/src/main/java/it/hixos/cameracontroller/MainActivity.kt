package it.hixos.cameracontroller

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.google.gson.Gson
import com.jaredrummler.ktsh.Shell
import it.hixos.cameracontroller.databinding.ActivityMainBinding
import it.hixos.cameracontroller.ui.ConnectedFragmentDirections
import it.hixos.cameracontroller.ui.ConnectionFragmentDirections
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.

        val model: SocketViewModel by viewModels()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onCleared")
    }
}