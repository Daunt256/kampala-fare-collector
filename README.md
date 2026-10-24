# Kampala Fare Collector

Start with [START-HERE.md](START-HERE.md). The current pilot is **Kampala Fare Collector 2**, Android application ID `com.abaho.farecollector.pilot2`, version 0.2.0. It installs alongside the first pilot so that version can retain its local records.

The collector includes SafeBoda, Bolt, Faras, Lolo Cab, Konnecta, Little, Ride Now, Union and Tinka. Uber is excluded. The provider/package allowlist and the 31 configurable product categories are recorded in [config/catalog.json](config/catalog.json). The app runs only the supported passenger apps that are installed; each app and mode needs training and fare-screen calibration on the test phone.

Connection entries survive switching apps, activity recreation and reopening. Draft tokens and saved tokens use Android Keystore encryption. Saved tokens appear as password dots. **Save connection only** stores the settings without making a network request; **Save and load routes** saves first and then checks the receiver.

Download the APK from the latest successful **Build test APK** workflow. The workflow builds and lints the app and runs Android emulator regression tests for connection persistence before publishing its APK artifact. Minimum Android 8.0; target API 35; JDK 17; Gradle 8.9; Android Gradle Plugin 8.7.3. Test-only AndroidX dependencies are not included in the collector APK.

The phone uses user-started accessibility workflows and readable screen text. It stops before booking/payment controls and requires review before upload. It does not generate prices or call undocumented ride-provider APIs. A configured provider is not a claim that its current phone screens have already been calibrated.

Run the local checks with `python3 tools/check_project.py`. Android connection tests are in `app/src/androidTest` and run with `gradle :app:connectedDebugAndroidTest` on a device or emulator.

Attach the receiver using the target Google Sheet’s **Extensions → Apps Script** menu. It writes only to the four FC tabs. Routes, pairing tokens and deployed URLs stay private and are not committed to this repository.
