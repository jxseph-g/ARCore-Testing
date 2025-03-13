package ch.hslu.moblab

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import ch.hslu.moblab.SceneView.SceneViewScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ch.hslu.moblab.AugmentedImages.AugmentedImagesView
import ch.hslu.moblab.ObjectDetection.ObjectDetectionView
import com.google.ar.core.ArCoreApk
import com.google.ar.core.exceptions.UnavailableException

/**
 * Main Activity of this Demo App. "Only" thing it does is to display the
 * home screen and load up the different ARCore / SceneView Scenarios.
 * It also checks if ARCore is installed and supported on this device.
 */

class
MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Check if ARCore is installed and supported on this device. Copied form Google Samples.
        try {
            when (ArCoreApk.getInstance().requestInstall(this, /*userRequestedInstall=*/ true)) {
                ArCoreApk.InstallStatus.INSTALLED -> {
                    // Success, create the AR session.
                    setContent {
                        AppNavigation()
                    }
                }
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    // Ensures next invocation of requestInstall() will either return
                    // INSTALLED or throw an exception. 1
                    return
                }
            }
        } catch (e: UnavailableException) {
            Log.e("MainActivity", "ARCore unavailable", e)
        }
    }
}


@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination
    = "home") {
        composable("home") { HomeScreen(navController) }
        composable("sceneView") { SceneViewScreen() }
        composable("augmentedImages") { AugmentedImagesView() }
        composable("ObjectDetection") { ObjectDetectionView() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("CoreShow - SA 36") })
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("chose your AR experience", fontSize = 30.sp)

            Spacer(modifier = Modifier.height(32.dp))
            //SceneView
            Button(onClick = { navController.navigate("sceneView") }) {
                Text("1. Virtuelles Platzieren")
            }
            Spacer(modifier = Modifier.height(32.dp))
            //Augmented Images
            Button(onClick = { navController.navigate("augmentedImages") }) {
                Text("2. Augmented Images")
            }
            Spacer(modifier = Modifier.height(32.dp))
            //MLKit Object Detection
            Button(onClick = { navController.navigate("ObjectDetection") }) {
                Text("3. MLKit Object Detection")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AppNavigation()
}