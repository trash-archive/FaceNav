# FaceNav

An Android accessibility app that lets users navigate their device hands-free using facial gestures detected via the front camera.

## How It Works

FaceNav runs a foreground camera service that continuously processes the front camera feed using ML Kit Face Detection. Detected gestures are translated into accessibility actions (taps, swipes, scrolls, etc.) via an Accessibility Service, allowing full device control without touching the screen.

## Features

- Facial gesture recognition (e.g. smile, blink, head tilt/turn)
- Customizable gesture-to-action mappings
- Voice command support
- Emergency kill switch (notification action or dedicated screen)
- Onboarding flow and test mode
- Settings persisted with DataStore

## Requirements

- Android 8.0 (API 26) or higher
- Front-facing camera
- Accessibility Service must be enabled in device settings

## Permissions

| Permission | Purpose |
|---|---|
| `CAMERA` | Front camera access for face detection |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_CAMERA` | Keep detection running in background |
| `POST_NOTIFICATIONS` | Show persistent service notification & kill switch action |
| `RECORD_AUDIO` | Voice command support |
| `VIBRATE` | Haptic feedback on gesture recognition |
| `WAKE_LOCK` | Prevent CPU sleep during active session |

## Setup

1. Clone the repo and open in Android Studio.
2. Build and install the app (`./gradlew installDebug`).
3. Open FaceNav and follow the onboarding flow.
4. Enable the **FaceNav Accessibility Service** in *Settings → Accessibility*.
5. Grant camera and notification permissions when prompted.

## Project Structure

```
app/src/main/java/com/example/facenav/
├── data/           # DataStore & preferences
├── detection/      # Gesture detection logic
├── model/          # Data models
├── service/        # Foreground service, accessibility service, kill switch
├── ui/
│   ├── screens/    # Compose screens (Home, Settings, Gesture Mapping, …)
│   └── theme/      # Material3 theme
└── MainActivity.kt
```

## Tech Stack

- Kotlin + Jetpack Compose
- CameraX 1.4
- ML Kit Face Detection
- Accessibility Service API
- DataStore Preferences
- Kotlin Coroutines
