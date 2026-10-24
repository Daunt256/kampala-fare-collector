# Pilot validation — 24 October 2026

Version 0.2.1 fixes SafeBoda tilde fare ranges, adds Insured Plus aliases and Electric Boda, and keeps nine providers with 32 categories. It installs as **Kampala Fare Collector 2.1** beside the previous pilot.

| Check | Result |
| --- | --- |
| Java fare parsing, six observed ranges, category association, metadata labels and guards | 121 local checks passed |
| Receiver validation, ingestion and range-preserving comparison writes | 50 local checks passed |
| Java source syntax, XML, package and permission scope | Passed locally |
| Android SDK build and lint | Passed in GitHub Actions |
| Eight Android emulator checks on Android 15 / API 35 | All passed |
| Existing deployed receiver compatibility | Upload payload normalizes only tilde separators; original row text remains evidence |
| Live phone confirmation | Updated pilot still needs a fresh capture on the user's phone |

The new Android tests verify that all six ranges survive queue storage and upload preparation, that source evidence is retained, and that the review screen displays both endpoints. The original six connection persistence tests remain enabled.

Regression fixtures reproduce the reported Boda and Car fare formats without using private routes or pairing settings. Receiver tests use an in-memory spreadsheet; they do not append artificial observations to the live workbook. Existing live quote records are not rewritten. Only FC Products needs the Insured Plus alias and appended Electric Boda row; the existing receiver reads that configuration directly.

Validated build: https://github.com/Daunt256/kampala-fare-collector/actions/runs/40084429473 (source commit c8d8bbf028ad184c63d27b5c2b867e6470d9a30a).

Distributed APK: Kampala-Fare-Collector-2.1.apk, 58,062 bytes. SHA-256: 20860038367c98bcd8c745f3a19f3396d9ab5622255b6dca05316bd641f5b7ca. Its signature, application ID, label and category strings were verified after signing.

The live catalog now contains the Insured Plus alias and Electric Boda, with 32 active products from nine providers. The four previous quote observations and all 35 original tab properties were verified unchanged. Final native visual fit was not checked; this content-only update retained the existing cell structure and checkbox validation.
