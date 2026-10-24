# Kampala Fare Collector — phone pilot

This first version is for your Android phone with **SafeBoda, Bolt and Faras**. Uber is excluded.

**A signed test APK has been generated.** The Android SDK build and lint passed in [build run 2](https://github.com/Daunt256/kampala-fare-collector/actions/runs/40008272989). Download **Kampala-Fare-Collector-test-APK** from that run’s Artifacts and extract its APK. Phone installation, provider calibration and the live sheet connection remain to be tested.

## 1. Generate the APK once

Choose one route. After generating the APK, ordinary collection runs on the phone without a connected computer.

### Windows build

1. Extract this whole ZIP on your Windows computer.
2. Open PowerShell in the extracted project folder.
3. Run:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Build-APK.ps1
```

The script downloads a portable JDK, Gradle and Google's Android SDK into a folder under your Windows user account. It verifies the downloaded tool packages against published checksums. The first build needs internet access and downloads the Android tools. Review the Android SDK licence prompts yourself.

On success, it creates **Kampala-Fare-Collector-test.apk** beside this guide. Transfer that APK to your phone and install it. The downloaded tools remain available for later builds.

If a build fails, retain the exact error output. There is no APK to install until the build reports success.

### GitHub build alternative

The included `.github/workflows/build-apk.yml` builds and lints the app, then provides an APK download under the workflow run's Artifacts. The source is in this repository, including the workflow file. A push to main starts a build. You can also use **Actions → Build test APK → Run workflow**. Download the artifact named **Kampala-Fare-Collector-test-APK** after a successful run and extract the APK.

The workflow uses GitHub-hosted build machines. No credentials or pairing tokens belong in the repository. This project has no production signing key. Test builds from different build machines may use different debug signing keys; upload pending quotes before uninstalling an old build if Android refuses an update.

## 2. Prepare the spreadsheet script

The script belongs inside the Google Sheet under **Extensions → Apps Script**, not in worksheet cells or on the phone. An account with **Editor** access is required; **Comment only** access cannot install it. Setup captures the bound spreadsheet ID privately in Script properties and creates these four new tabs:

| Tab | Purpose |
| --- | --- |
| FC Routes | Pickup and destination searches with an Active switch |
| FC Products | Category names and the visible labels the phone should recognize |
| FC Quotes | Reviewed quotes with exact capture times and retained fare ranges |
| FC Comparison | Latest run for each route and mode, with timing and completeness notes |

Existing monthly tabs are not edited by this pilot.

1. Open the original spreadsheet in Google Sheets using an account that can edit it.
2. Choose **Extensions → Apps Script**.
3. If the project is empty, paste `apps-script/Code.gs` into its code file.
4. In **Project Settings**, enable **Show appsscript.json manifest file in editor** and use the supplied `apps-script/appsscript.json`.
5. Run **installCollector** and authorize the requested spreadsheet access. Setup creates the FC tabs and a private pairing token. Add your test routes in **FC Routes**: a unique ID such as R01, pickup, destination and TRUE in Active. Route data stays in the sheet. Verify the initial product names and aliases against the installed ride apps.
6. Choose **Deploy → New deployment → Web app**. Set **Execute as: Me**, **Who has access: Anyone**. The receiver checks a private pairing token before returning configuration or accepting quotes.
7. Copy the deployed public URL ending in `/exec`. In **Project Settings → Script properties**, add **FC_WEB_APP_URL** with that URL. The editor can report a `/dev` test URL; the collector never uses that for phone pairing.
8. Reload the spreadsheet and choose **Fare Collector → Show connection settings**. Copy the pairing token privately.

If the existing Apps Script project contains other code, preserve it. Use a separate standalone Apps Script project if its onOpen, doGet, doPost or function names conflict. Add the supplied files, then set **Project Settings → Script properties → FC_SPREADSHEET_ID** to the original spreadsheet ID privately. Run **installCollector** and deploy as above. Copy the pairing token from **FC_PAIRING_TOKEN** in Script properties. Standalone setup still needs Editor access to the target sheet, and it does not add a menu to that sheet. Run `refreshCollectorComparisons` from its script editor if needed.

If your Google Workspace administrator prevents an Anyone deployment, the token-based phone connection cannot be used until an allowed deployment or a Google sign-in design is arranged. Do not make the spreadsheet itself public.

## 3. Connect the phone

1. Install the generated APK and open **Kampala Fare Collector**.
2. Read the disclosure and continue.
3. Open **Connection**. Paste the deployment URL and pairing token, then choose **Save and load routes**.
4. Open **Collect → Enable collection accessibility**, and enable Kampala Fare Collector.
5. Sign in normally to SafeBoda, Bolt and Faras. The collector does not handle account passwords or verification codes.

If Android displays Restricted setting for the installed test app, its App info screen may offer **Allow restricted settings** in the menu. Review Android's warning and enable the service only after you have reviewed this test app's purpose. If the option is absent or a company policy blocks it, stop and report the message.

Keep the phone unlocked, in portrait orientation and available for the test. Use automatic date/time. The floating control panel can be moved vertically by dragging its status text.

## 4. First test: read one fare screen

Start with one short test route from **FC Routes**, in **Boda** mode. Connect and load routes first; the APK contains no private route list.

1. Select SafeBoda and choose **Open app for inspection or price capture**.
2. Set the route yourself in SafeBoda and navigate to its fare screen. Verify the exact pickup and destination pins.
3. Tap **Read prices** on the collector's floating panel.
4. If prices appear, verify them, identify the payment and promotion state, tick the route/fare confirmation, and accept.
5. Check **FC Quotes** in the spreadsheet. An offline quote stays queued on the phone; use **Records → Upload queued quotes** to retry.

If it reports **No unambiguous price rows found**, stay on that fare screen and tap **Inspect**. Tap **Stop**, reopen the collector and choose **Share screen details**. Review the exported JSON before sharing it back in this conversation. It includes the phone model, Android version, screen text and control identifiers; email addresses and phone numbers are masked. It never includes the pairing token. Inspect only the fare screen.

Repeat for Bolt and Faras. This first check establishes which visible category labels and screen structures your installed versions expose.

## 5. Teach automatic route entry

Teach each app separately, and teach Boda and Car mode separately.

1. Return the selected ride app to its home screen.
2. In the collector, select R01, the provider and the mode. Choose **Teach this app's route workflow**.
3. Navigate normally toward route entry. Navigation taps inside that provider are recorded.
4. Before tapping the editable pickup search field, press **Pickup field** on the floating panel. Then tap the field. The collector enters the configured pickup search and records that field.
5. Select the correct pickup result. Before tapping the editable destination search, press **Dropoff field**, then tap that field. Select the correct destination result.
6. Reach the fare screen and press **Finish** on the panel. Both route fields must have been marked. The saved workflow stops here.
7. Read and review the prices. Then return the ride app to its home screen and test **Run learned workflow**.

The pilot matches accessible controls by identifiers and labels. It does not replay blind screen coordinates. If a screen cannot be matched uniquely, collection pauses. A blocked booking, payment or confirmation control is not automatically pressed. If an app requires one of those controls even for route setup, report the screen so its adapter can be reviewed.

After all three workflows work individually, test **Run all three apps for this route**. It collects sequentially on your one phone and presents a combined review. Each captured quote keeps its real timestamp. Start with one route; this pilot does not automatically iterate all routes or run scheduled background jobs.

## 6. How to read the results

- Fare ranges retain separate low and high values.
- Each three-app run has a shared Run ID. Separate runs are not silently combined.
- FC Comparison flags missing categories, capture gaps over 60 seconds, unspecified payment or promotion details, and differing payment or promotion states.
- Blank categories mean no accepted observation; they are not zero prices.
- Results are observations from the signed-in test accounts, not guaranteed fares for every customer.
- No cheapest-provider ranking is computed across mismatched vehicle classes or overlapping fare ranges.

Upload queued quotes before changing route IDs, route search names or product configuration. Reload routes on the phone after editing FC Routes or FC Products.

## What is ready, and what still needs proof

Included: source for the Android accessibility collector, guided workflow training and replay, three-app sequential collection, review, encrypted token storage, offline upload queue, screen inspection, the sheet receiver, validation and deduplication, comparison logic, local checks, Windows build script and GitHub build workflow.

Still to verify: installation on your phone, each provider's actual controls and price labels, an end-to-end phone upload, fresh-quote behavior and timing. This version reads accessible text. Screenshot OCR, scheduled unattended jobs, automatic route-list iteration and live multi-phone synchronization are not included in 0.1.

## Technical references

- Android accessibility services: https://developer.android.com/guide/topics/ui/accessibility/views/service
- Google Play's rule-based automation policy: https://support.google.com/googleplay/android-developer/answer/10964491
- Apps Script web apps: https://developers.google.com/apps-script/guides/web
- Android build tooling compatibility: https://developer.android.com/build/releases/agp-8-7-0-release-notes
- Android restricted settings: https://support.google.com/android/answer/12623953
