
# Ayaan Islamic Studio

Ayaan Islamic Studio provides prayer times, Quran reading and audio, Qibla, Islamic tools, AI assistance, notifications, and account services.

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` and `GROQ_API_KEY` there as needed (see `.env.example` for the variable names). These values are packaged in the client APK and are not suitable for protecting production credentials; use a server-side proxy for production provider keys.
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device
7. Configure Ayaan Islamic Studio signing and Firebase settings before publishing a release APK.
