package n.startapp.wordwaveriseapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import n.startapp.wordwaveriseapp.presentation.main.MainScreen
import n.startapp.wordwaveriseapp.presentation.main.MainViewModel
import n.startapp.wordwaveriseapp.ui.theme.WordWaveriseAppTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WordWaveriseAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        state = viewModel.state.value,
                        onCheckConnection = { viewModel.checkConnection() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}