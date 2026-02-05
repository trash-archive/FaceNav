package com.example.facenav.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.example.facenav.R
import com.example.facenav.data.PreferencesManager
import com.example.facenav.detection.GestureDetector
import com.example.facenav.model.AppSettings
import com.example.facenav.model.FacialGesture
import com.example.facenav.model.GestureMapping
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Foreground service that handles face detection and gesture recognition
 */
class FaceDetectionService : LifecycleService() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var gestureDetector: GestureDetector
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var killSwitch: EmergencyKillSwitch
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var gestureMappings: Map<FacialGesture, GestureMapping> = emptyMap()
    private var appSettings: AppSettings = AppSettings()
    private var isProcessing = false

    companion object {
        private const val TAG = "FaceDetectionService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "FaceNavChannel"
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }

    // ML Kit Face Detector
    private val faceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
        .setMinFaceSize(0.15f)
        .enableTracking()
        .build()

    private val faceDetector = FaceDetection.getClient(faceDetectorOptions)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        cameraExecutor = Executors.newSingleThreadExecutor()
        preferencesManager = PreferencesManager(this)
        gestureDetector = GestureDetector(appSettings)

        // Initialize kill switch
        killSwitch = EmergencyKillSwitch(this)
        killSwitch.showQuickAccessNotification()

        // Monitor kill switch state
        lifecycleScope.launch {
            killSwitch.isActive.collect { isActive ->
                if (!isActive) {
                    Log.d(TAG, "Kill switch activated - stopping gesture detection")
                    stopGestureDetection()
                }
            }
        }

        // Load settings and mappings
        serviceScope.launch {
            launch {
                preferencesManager.getSettings().collect { settings ->
                    appSettings = settings
                    gestureDetector.updateSettings(settings)
                }
            }

            launch {
                preferencesManager.getAllGestureMappings().collect { mappings ->
                    gestureMappings = mappings.associateBy { it.gesture }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification())
                startCamera()
            }

            ACTION_STOP -> {
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")

        killSwitch.resume() // Clean up kill switch
        cameraExecutor.shutdown()
        serviceScope.cancel()
        faceDetector.close()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    /**
     * Start the camera and face detection
     */
    @OptIn(ExperimentalGetImage::class)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Process camera frame for face detection
     */
    @androidx.camera.core.ExperimentalGetImage
    private fun processImageProxy(imageProxy: ImageProxy) {
        // Check kill switch first - don't process if deactivated
        if (!killSwitch.isActive.value) {
            imageProxy.close()
            return
        }

        if (!appSettings.detectionEnabled || isProcessing) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        isProcessing = true

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val face = faces[0] // Use the first detected face

                    val gesture = gestureDetector.detectGesture(face)
                    if (gesture != null) {
                        handleGestureDetected(gesture)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Face detection failed", e)
            }
            .addOnCompleteListener {
                isProcessing = false
                imageProxy.close()
            }
    }

    /**
     * Handle detected gesture by performing mapped action
     */
    private fun handleGestureDetected(gesture: FacialGesture) {
        // Broadcast gesture event for Test Mode
        GestureEventBus.emitGestureSync(gesture, confidence = 0.95f)

        // Check kill switch first - track blinks for triple blink detection
        killSwitch.onBlinkDetected(gesture)

        // Don't process gestures if kill switch is deactivated
        if (!killSwitch.isActive.value) {
            Log.d(TAG, "Kill switch is active - ignoring gesture: ${gesture.name}")
            return
        }

        val mapping = gestureMappings[gesture] ?: return

        if (!mapping.enabled) {
            Log.d(TAG, "Gesture ${gesture.name} is disabled")
            return
        }

        Log.d(TAG, "Gesture detected: ${gesture.name} -> Action: ${mapping.action.name}")

        val accessibilityService = FaceNavAccessibilityService.getInstance()
        if (accessibilityService == null) {
            Log.w(TAG, "Accessibility service not available")
            return
        }

        accessibilityService.performAction(mapping.action)
    }

    /**
     * Stop gesture detection (called when kill switch is activated)
     */
    private fun stopGestureDetection() {
        Log.d(TAG, "Stopping gesture detection")
        isProcessing = false
        // Optionally stop the service entirely
        // stopSelf()
    }

    /**
     * Create notification for foreground service
     */
    private fun createNotification(): Notification {
        createNotificationChannel()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FaceNav Active")
            .setContentText("Detecting facial gestures")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    /**
     * Create notification channel for Android O and above
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FaceNav Detection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when FaceNav is actively detecting gestures"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}