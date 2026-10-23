# Pilot validation — 23 October 2026

Result: Android debug APK built successfully and Android lint passed in [GitHub Actions build run 2](https://github.com/Daunt256/kampala-fare-collector/actions/runs/40008272989), at source commit `cf8d91fe0ad944232f5f5247c448abcf969debf0`.

| Check | Result |
| --- | --- |
| Java fare parsing, product association, route checks, action guards and currency checks | 44 checks passed |
| Apps Script validation, authentication, ingestion, retries, conflicts, comparison and installer boundaries | 34 checks passed using an in-memory Sheets test double |
| All Java source syntax | Passed |
| XML resources and manifest | Parsed; package scope and permissions checked |
| Android SDK compilation | `:app:assembleDebug` passed for API 35, minimum API 26 |
| Android lint | `:app:lintDebug` passed |
| Generated artifact | Signed debug test APK; GitHub artifact checksum verified after download |
| Windows build script | Prepared; not executed in the Linux development environment |
| Live phone and provider workflows | Pending APK installation and calibration on a real phone |
| Live Apps Script deployment | Prepared; requires Editor access to the target Google Sheet |

APK SHA-256: `8c9247a2021205a0bf69b01e815a44c23b9484d290a0b94be4f78c4c3178b661`.

Build and logic checks do not establish actual provider screen matching, live price accuracy or fare-refresh timing. Calibrate SafeBoda, Bolt and Faras on the test phone before relying on comparisons. The script receiver has been exercised with a Sheets test double; live Google authorization, deployment and an end-to-end phone upload remain pending.

The installer targets the spreadsheet chosen privately during installation. It creates four FC tabs and does not edit existing monthly analysis tabs. Private spreadsheet IDs, route lists, deployment URLs and pairing tokens are not included in the source or APK.
