# Pilot validation — 24 October 2026

Version 0.2.0 adds encrypted connection drafts, persistent connection fields, nine-provider coverage and a separate Android package to retain the original pilot’s local data.

| Check | Result |
| --- | --- |
| Java fare parsing, product association, route/action guards and nine-provider catalog | 78 local checks passed |
| Apps Script authentication, ingestion, comparisons, published URL selection and nine-provider configuration | 49 local checks passed |
| Java source syntax, XML, package and permission scope | Passed locally |
| Android SDK compilation and lint for version 0.2.0 | See latest workflow result |
| Six Android emulator connection persistence and encrypted-storage tests | See latest workflow result |
| Apps Script receiver | Existing deployed receiver remains available; expanded catalog rollout follows APK validation |
| Provider screen calibration and phone upload | Pending tests with the installed passenger apps |

The connection tests cover returning from another app, activity recreation, closing/relaunching, saved credentials and masking, invalid URL rejection, and encrypted drafts/saved tokens. Provider coverage includes selector entries, installed-app detection, training/replay, product configuration and reviewed quote validation. Live app navigation and category labels must be checked on the test phone.

The installer targets the spreadsheet chosen privately during setup and modifies only the FC tabs. Private spreadsheet IDs, route lists, deployment URLs and pairing tokens are not included in the source or APK.
