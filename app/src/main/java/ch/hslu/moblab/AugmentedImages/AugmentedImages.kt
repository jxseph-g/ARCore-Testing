package ch.hslu.moblab.AugmentedImages

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.viewinterop.AndroidView
import com.google.ar.core.Config
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.addAugmentedImage
import io.github.sceneview.ar.arcore.getUpdatedAugmentedImages
import io.github.sceneview.ar.node.AugmentedImageNode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode

/**
 * Augmented Images Class. It implements two augmented images scenarios:
 * - Pom picture, with a simple 3D-pomeranian as soon as it recognises the image
 * - 3D Mario, it should look like it goes inside of the screen - try it out :)
 * all the reference images are in assets/augmentedImages
 */

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AugmentedImagesView()
        }
    }
}

@Composable
fun AugmentedImagesView() {
    val augmentedImageNodes by remember { mutableStateOf(mutableListOf<AugmentedImageNode>()) }

    //start ARCore session inside of a AndroidView(), then start a SceneView session with ARSceneView(context)
    AndroidView(
        factory = { context ->
            ARSceneView(context).apply {
                configureSession { session, config ->
                    // with config.addAugmentedImage() it's possible to add all the reference-pictures and give them a "session-name"
                    config.addAugmentedImage(
                        session, "mario",
                        context.assets.open("augmentedImages/mario.jpg")
                            .use(BitmapFactory::decodeStream)
                    )
                    config.addAugmentedImage(
                        session, "pom",
                        context.assets.open("augmentedImages/pom.jpg")
                            .use(BitmapFactory::decodeStream)
                    )
                    config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                    config.focusMode = Config.FocusMode.AUTO
                    config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                    planeRenderer.isVisible = false
                }

                onSessionUpdated = { session, frame ->
                    // Get updated augmented images from the frame
                    frame.getUpdatedAugmentedImages().forEach { augmentedImage ->
                        // Only add node if it hasn't been added already
                        if (augmentedImageNodes.none { it.imageName == augmentedImage.name }) {
                            val augmentedImageNode = AugmentedImageNode(engine, augmentedImage).apply {
                                when (augmentedImage.name) {
                                    //start with "session-name" and add the 3D models here
                                    "mario" -> addChildNode(
                                        ModelNode(
                                            modelInstance = modelLoader.createModelInstance(
                                                assetFileLocation = "augmentedImages/mario3D.glb"
                                            ),
                                            //change this scale, to fit the 3D object over your picture
                                            scaleToUnits = 0.215f,
                                            centerOrigin = Position(0.0f)
                                        ).apply {
                                            // i slightly placed the 3D model "inside" of the screen. That's why -> y = -0.05f
                                            position = Position(x = 0.0f, y = -0.05f, z = 0.0f)
                                            rotation = Rotation(x = -90.0f, y = 0.0f, z = 90.0f)
                                        }
                                    )
                                    "pom" -> addChildNode(
                                        ModelNode(
                                            modelInstance = modelLoader.createModelInstance(
                                                assetFileLocation = "augmentedImages/pom3D.glb"
                                            ),
                                            scaleToUnits = 0.1f,
                                            centerOrigin = Position(0.0f)
                                        ).apply {
                                        })
                                }
                            }
                            addChildNode(augmentedImageNode)
                            augmentedImageNodes.add(augmentedImageNode)
                        }
                    }
                }
            }
        },
    )
}
