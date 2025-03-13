@file:Suppress("NAME_SHADOWING")

package ch.hslu.moblab.ObjectDetection

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import com.google.ar.core.*
import com.google.ar.core.CameraConfig.FacingDirection
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import io.github.sceneview.ar.ARSceneView
import android.graphics.ImageFormat
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.nativeCanvas

/**
 *
 * Wow, this was tough! (or maybe it's just me). Implementing MLKit with an
 * AndroidView() and Jetpack Compose certainly did not help. I tried to follow
 * Google's doc, that's why you will see "Step 1.", "Step 2." etc.
 * You will find these steps in the links below.
 * After a lot of trial and error it kind of works as wished, but certainly
 * could need some improvement
 * https://developers.google.com/ml-kit/vision/object-detection/android
 * https://developers.google.com/ml-kit/vision/object-detection/custom-models/android
 *
 */

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ObjectDetectionView()
        }
    }
}

@Composable
fun ObjectDetectionView() {

    Log.d("ObjectDetection", "session started.")

    val options = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .build()
    val objectDetector = ObjectDetection.getClient(options)


    var detectedBoundingBoxes by remember { mutableStateOf<List<Rect>>(emptyList()) }
    var detectedAnchors by remember { mutableStateOf<List<Anchor>>(emptyList()) }

    AndroidView(
        factory = { context ->
            ARSceneView(context).apply {
                configureSession { session, config ->
                            session.getSupportedCameraConfigs(
                        CameraConfigFilter(session)
                            .setFacingDirection(FacingDirection.BACK))
                    config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                    config.focusMode = Config.FocusMode.AUTO
                    config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                    config.depthMode = Config.DepthMode.AUTOMATIC
                    planeRenderer.isVisible = false
                }

                onSessionUpdated = { _, frame ->
                    Log.d("ObjectDetection", "onSessionUpdated triggered.")
                    try {
                        val image = frame.acquireCameraImage()
                        Log.d("ObjectDetection", "Image format: ${image.format}")
                        image.use { image ->
                            val bitmap = image.toBitmap()
                            val inputImage = InputImage.fromBitmap(bitmap, 0)

                            objectDetector.process(inputImage)
                                .addOnSuccessListener { detectedObjects ->
                                    val boundingBoxes = mutableListOf<Rect>()
                                    val anchors = mutableListOf<Anchor>()

                                    for (detectedObject in detectedObjects) {
                                        val boundingBox = detectedObject.boundingBox
                                        val trackingId = detectedObject.trackingId

                                        Log.d("ObjectDetection",
                                        "Detected object: $boundingBox, $trackingId")

                                        val cpuCoordinates = floatArrayOf(
                                            boundingBox.exactCenterX(),
                                            boundingBox.exactCenterY()
                                        )
                                        val viewCoordinates = FloatArray(2)
                                        frame.transformCoordinates2d(
                                            Coordinates2d.IMAGE_PIXELS, cpuCoordinates,
                                            Coordinates2d.VIEW, viewCoordinates
                                        )

                                        val rectInViewCoordinates = Rect(
                                            left = (viewCoordinates[0] - boundingBox.height()),
                                            top = (viewCoordinates[1] - boundingBox.width()),
                                            right = (viewCoordinates[0] + boundingBox.height()),
                                            bottom = (viewCoordinates[1] + boundingBox.width())
                                        )
                                        boundingBoxes.add(rectInViewCoordinates)

                                        val hits = frame.hitTest(viewCoordinates[0], viewCoordinates[1])
                                        val depthPointResult = hits.filter { it.trackable is DepthPoint }.firstOrNull()
                                        if (depthPointResult != null) {
                                            val anchor = depthPointResult.trackable.createAnchor(depthPointResult.hitPose)
                                            anchors.add(anchor)
                                        }
                                    }
                                    detectedBoundingBoxes = boundingBoxes
                                    detectedAnchors = detectedAnchors + anchors
                                }
                        }
                    } catch (e: NotYetAvailableException) {
                        Log.e("ObjectDetection", "Camera image not yet available: $e")
                    } catch (e: Exception) {
                        Log.e("ObjectDetection", "Error acquiring camera image: $e")
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        detectedBoundingBoxes.forEach { boundingBox ->
            drawRect(
                color = Color.Red,
                topLeft = Offset(boundingBox.left, boundingBox.top),
                size = Size(boundingBox.width, boundingBox.height),
                style = Stroke(width = 4.dp.toPx())
            )
            val coordinatesText = "$boundingBox"
            drawContext.canvas.nativeCanvas.drawText(
                coordinatesText,
                boundingBox.left - 150f,
                boundingBox.top - 20f,
                Paint().apply {
                    color = android.graphics.Color.RED
                    textSize = 40f
                }
            )
            Log.d("ObjectDetection", "Detected bounding box: $boundingBox")
        }
    }

    detectedAnchors.forEach { anchor ->
        // Create and position your virtual object using the anchor
    }
}
/*
Helper function to convert android.media.Image to android.graphics.Bitmap
this has been created with the help of ARCore GitHub, Gemini and StackOverflow infos
thanks to the file format I Logged (and 1 million other Logging, Trial & Error), Gemini put
together all pieces to create this effective function.

Reason:
    try {
        val image = frame.acquireCameraImage()
        image.use { image ->
            val inputImage = InputImage.fromMediaImage(image, image.imageInfo.rotationDegrees)
            // ... (rest of detection code)
        }
    Jetpack Compose and SceneView access the image data differently.
    That's why the code below was needed.
    https://github.com/google-ar/arcore-android-sdk/issues/510
 */
private fun Image.toBitmap(): Bitmap {
    if (format != ImageFormat.YUV_420_888) {
        throw IllegalArgumentException("Unsupported image format: $format")
    }

    val yBuffer = planes[0].buffer // Y plane
    val uBuffer = planes[1].buffer // U plane
    val vBuffer = planes[2].buffer // V plane

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    // Copy Y, U, and V buffers into a single array
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    // Convert NV21 to Bitmap
    val yuvImage = android.graphics.YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = java.io.ByteArrayOutputStream()
    yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 100, out)
    val jpegBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
}