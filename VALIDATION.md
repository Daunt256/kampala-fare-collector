# Pilot validation — 23 October 2026

Result: source and offline logic checks passed. No APK has been generated yet.

| Check | Result |
| --- | --- |
| Pure Java fare parsing, product association, route checks, action guards, currency checks | 44 checks passed |
| Apps Script validation, authentication, POST ingestion, duplicate retries, conflicts, comparisons, installer boundaries | 34 checks passed using an in-memory Sheets test double |
| All Java source syntax | Passed with JDK 17 compiler parser |
| XML resources and manifest | Parsed; package allowlist and requested permissions checked |
| Android SDK compilation and lint | Not run; SDK and build-tool downloads returned HTTP 403 in the development environment |
| Windows build script | Prepared; not executed in this Linux environment |
| GitHub Actions build | Workflow prepared; build result is available under Actions |
| Live phone and provider workflows | Not run; requires APK installation and calibration on your phone |
| Live Apps Script deployment | Not deployed; source and setup instructions supplied |

The offline tests do not establish Android API type compatibility, installability, live screen matching, actual price accuracy, refresh timing, or native Google Sheets behavior. The Android build workflow fails if compilation or lint fails. Live collection must be checked against the three apps before relying on the comparisons.

Only SafeBoda, Bolt and Faras are included. Existing workbook tabs and the uploaded XLSX snapshot were not modified during development. Running the supplied installer later creates the four explicitly named FC tabs in the spreadsheet chosen privately during installation.
