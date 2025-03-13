package ch.hslu.moblab.SceneView

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.hslu.moblab.R
import com.google.android.filament.Engine
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
import com.google.ar.core.Frame
import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.getUpdatedPlanes
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.getDescription
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.ar.rememberARCameraStream
import io.github.sceneview.ar.rememberAREnvironment
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberRenderer
import io.github.sceneview.rememberScene
import io.github.sceneview.rememberView

/**
 * This Class implements a "simple" form of AR with SceneView. You can place
 * a Bugdroid or an Anime Character.
 * Its also possible to activate and deactivate planes, bounding boxes
 * or to simply empty the scene.
 */

    //define path for 3D model
    //you can put any .glb file you want and try it out yourself!
    //just be careful with the object size, you might want to
    //adjust editableScaleRange inside createAnchorNode()

    const val itachiModel = "models/itachi.glb"
    const val androidModel = "models/android-mascot.glb"

    @Composable
    fun SceneViewScreen() {

        //set up ARScene as desired, see: https://sceneview.github.io/api/sceneview-android/arsceneview/io.github.sceneview.ar/-a-r-scene.html
        val engine = rememberEngine()
        val modelLoader = rememberModelLoader(engine)
        val materialLoader = rememberMaterialLoader(engine)
        val environmentLoader = rememberEnvironmentLoader(engine)
        val childNodes = rememberNodes()
        val view = rememberView(engine)
        var frame by remember { mutableStateOf<Frame?>(null) }
        var planeRenderer by remember { mutableStateOf(true) }

        var trackingFailureReason by remember {
            mutableStateOf<TrackingFailureReason?>(null)
        }

        val mainLightNode = rememberMainLightNode(engine)

        // in order to not automatically replace a model when clearing the scene
        var modelPlaced by remember { mutableStateOf(false) }

        // State to hold the current model path
        var currentModel by remember { mutableStateOf(androidModel) }

        //0.1f since the first model, without touching the Switch button, is the Bugdroid
        var modelSize by remember { mutableFloatStateOf(0.1f) }

        //depth warning if not supported
        var showDepthWarning by remember { mutableStateOf(false) }

        var showPlanes by remember { mutableStateOf(true) }

        var boundingBoxVisible by remember { mutableStateOf(false) }

        ARScene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            childNodes = childNodes,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            environmentLoader = environmentLoader,

            mainLightNode = mainLightNode,
            // Fundamental session features that can be requested.
            sessionFeatures = setOf(),
            // The camera config to use.
            // The config must be one returned by [Session.getSupportedCameraConfigs].
            // Provides details of a camera configuration such as size of the CPU image and GPU texture.
            sessionCameraConfig = null,
            // Configures the session and verifies that the enabled features in the specified session config
            // are supported with the currently set camera config.
            sessionConfiguration = { session, config ->
                config.depthMode =
                        //DepthMode requires ARCore installed (see MainActivity)!
                    when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                        true -> Config.DepthMode.AUTOMATIC
                        else -> Config.DepthMode.DISABLED
                    }
                val depthSupported = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
                if (!depthSupported) {
                    showDepthWarning = true
                }

                //try changing these settings if you want, especially LightEstimationMode will bring
                //different results
                config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
                config.lightEstimationMode =
                    Config.LightEstimationMode.ENVIRONMENTAL_HDR
                config.planeFindingMode = HORIZONTAL_AND_VERTICAL
            },

            planeRenderer = showPlanes,
            cameraStream = rememberARCameraStream(materialLoader),
            view = view,
            renderer = rememberRenderer(engine),
            scene = rememberScene(engine),
            environment = rememberAREnvironment(engine),
            cameraNode = rememberARCameraNode(engine),
            collisionSystem = rememberCollisionSystem(view),

            onSessionUpdated = { session, updatedFrame ->
                frame = updatedFrame

                if (childNodes.isEmpty() && !modelPlaced) {
                    modelPlaced = true
                    updatedFrame.getUpdatedPlanes()
                        .firstOrNull()
                        //.firstOrNull { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING } // original code from SceneView API, but seems unnecessary in this App Case
                        ?.let { it.createAnchorOrNull(it.centerPose) }?.let { anchor ->
                            childNodes += createAnchorNode(
                                //pass the variables needed for function createAnchorNode()
                                engine = engine,
                                modelLoader = modelLoader,
                                materialLoader = materialLoader,
                                anchor = anchor,
                                modelPath = currentModel,
                                modelSize = modelSize,
                                boundingBoxVisible = boundingBoxVisible
                            )
                        }
                }
            },
            onGestureListener = rememberOnGestureListener(
                onSingleTapConfirmed = { motionEvent, node ->
                    if (node == null) {
                        val hitResults = frame?.hitTest(motionEvent.x, motionEvent.y)
                        hitResults?.firstOrNull {
                            it.isValid(
                                depthPoint = true,  // allow depth points for interaction
                                point = false        // dont' allow general point cloud points
                            )
                        }?.createAnchorOrNull()
                            ?.let { anchor ->
                                planeRenderer = true
                                childNodes += createAnchorNode(
                                    engine = engine,
                                    modelLoader = modelLoader,
                                    materialLoader = materialLoader,
                                    anchor = anchor,
                                    modelPath = currentModel,
                                    modelSize = modelSize,
                                    boundingBoxVisible = boundingBoxVisible
                                )
                            }
                    }
                }),
            onTrackingFailureChanged = {
                trackingFailureReason = it
            }
        )

        // UI Elements from here on - Could be refactored into separate composables for better organization. Sorry >.<
        // Skip to -> fun createAnchorNode(), if you only want to see the AR functionality!

        //Notification Text for User
        Text(
            modifier = Modifier
                .systemBarsPadding()
                .fillMaxWidth()
                .padding(top = 16.dp, start = 32.dp, end = 32.dp),
            textAlign = TextAlign.Center,
            fontSize = 28.sp,
            color = Color.White,
            text = trackingFailureReason?.getDescription(LocalContext.current) ?: if (childNodes.isEmpty()) {
                stringResource(R.string.point_your_phone_down)
            } else {
                stringResource(R.string.tap_anywhere_to_add_model)
            }
        )

        // Box holding the buttons
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                //Reset Scene Button
                Button(onClick = {
                    childNodes.clear()
                    modelPlaced = false
                }) {
                    Text("reset scene")
                }
                // enable / disable Planes button
                Button(onClick = {
                    showPlanes = !showPlanes
                }) {
                    Text(if (showPlanes) "disable planes" else "enable planes")
                }
                // enable / disable bounding boxes button
                Button(onClick = {
                    boundingBoxVisible = !boundingBoxVisible
                }) {
                    Text(if (boundingBoxVisible) "bounding box ON" else "bounding box OFF")
                }
            }
        }

        // Display the warning message for depth, if not supported
        if (showDepthWarning) {
            Text(
                text = "Depth not supported",
                color = Color.Red,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }

        //Model switch Button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(top = 264.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            IconButton(
                onClick = {
                    if (currentModel == androidModel){
                        currentModel = itachiModel
                        modelSize = 3.5f
                        childNodes.clear()
                        modelPlaced = false
                    } else {
                        currentModel = androidModel
                        modelSize = 0.1f
                        childNodes.clear()
                        modelPlaced = false
                    }
                },
                modifier = Modifier
                    .size(48.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.LightGray
                        )
            ) {
                Icon(
                    imageVector = Icons.Filled.Create,
                    contentDescription = "Switch Models",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }

// Creates an AnchorNode, which serves as a container for rendering and positioning a 3D model
// in the scene. This function handles loading the model, scaling it, and attaching it to an anchor.
    fun createAnchorNode(
        engine: Engine,
        modelLoader: ModelLoader,
        materialLoader: MaterialLoader,
        anchor: Anchor,
        modelPath: String,
        modelSize: Float,
        boundingBoxVisible: Boolean
    ): AnchorNode {
        val anchorNode = AnchorNode(engine = engine, anchor = anchor)
        val modelNode = ModelNode(
            modelInstance = modelLoader.createModelInstance(modelPath),
            // Scale to fit
            scaleToUnits = modelSize
        ).apply {
            // Model Node needs to be editable for independent rotation from the anchor rotation
            isEditable = true
            editableScaleRange = 0.1f..3.5f
            isShadowCaster = true
        }
        val boundingBoxNode = CubeNode(
            engine,
            size = modelNode.extents,
            center = modelNode.center,
            materialInstance = materialLoader.createColorInstance(Color.White.copy(alpha = 0.5f))
        ).apply {
            //bounding box not visible all the time
            isVisible = boundingBoxVisible
        }

        modelNode.addChildNode(boundingBoxNode)
        anchorNode.addChildNode(modelNode)

        listOf(modelNode, anchorNode).forEach {
            it.onEditingChanged = { editingTransforms ->
                //bounding box only visible while moving the model around
                boundingBoxNode.isVisible = editingTransforms.isNotEmpty()
            }

        }
        return anchorNode
    }
