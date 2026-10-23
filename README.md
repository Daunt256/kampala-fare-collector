# Kampala Fare Collector

Start with **START-HERE.md**.

Native Android pilot for SafeBoda, Bolt and Faras with a Google Apps Script receiver. Application ID: `com.abaho.farecollector`. Minimum Android 8.0; compile and target API 35; JDK 17; Gradle 8.9; Android Gradle Plugin 8.7.3. No third-party runtime dependencies.

The phone implements user-started, rule-based accessibility workflows, local fare extraction, human review and upload. It neither calls undocumented ride-provider APIs nor generates fares. Screenshot OCR is not part of this build. A real device calibration is required before treating a workflow as reliable.

Build with `Build-APK.ps1`, Android Studio, or the included GitHub Actions workflow. The package has not yet been compiled into an APK because SDK downloads were blocked in the development environment. Do not treat source or syntax checks as an Android build result.

Run the offline checks with JDK 17, Python 3 and Node.js:

```bash
python3 tools/check_project.py
```

The checks exercise fare parsing, product association, action guards, route validation, Apps Script authentication, ingestion, idempotent retries, conflicting-ID rejection, comparison timing, blank versus zero, and preservation of an unrelated worksheet using an in-memory test double. They parse all Java source and XML files but do not replace Android SDK type checking, lint or device tests.

The script creates only the `FC Routes`, `FC Products`, `FC Quotes`, and `FC Comparison` tabs. Attach it through the target sheet’s **Extensions → Apps Script** menu; setup saves that spreadsheet’s ID privately in Script properties. Existing monthly tabs are not edited. Routes, pairing tokens and deployment URLs stay out of this public repository. The phone loads routes only after connecting to the receiver.
