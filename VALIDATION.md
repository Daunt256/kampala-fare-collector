# Pilot validation — 24 October 2026

Version 0.2.1 fixes SafeBoda tilde fare ranges, adds Insured Plus aliases and Electric Boda, and keeps nine providers with 32 categories. It installs as **Kampala Fare Collector 2.1** beside the previous pilot.

| Check | Result |
| --- | --- |
| Java fare parsing, six observed ranges, category association, metadata labels and guards | 121 local checks passed |
| Receiver validation, ingestion and range-preserving comparison writes | 50 local checks passed |
| Java source syntax, XML, package and permission scope | Passed locally |
| Android SDK build, lint and eight emulator checks | See the workflow for this commit |
| Existing deployed receiver compatibility | Upload payload normalizes only tilde separators; original row text remains evidence |
| Live phone confirmation | Updated pilot still needs a fresh capture on the user's phone |

The new Android tests verify that all six ranges survive queue storage and upload preparation, that source evidence is retained, and that the review screen displays both endpoints. The original six connection persistence tests remain enabled.

Regression fixtures reproduce the reported Boda and Car fare formats without using private routes or pairing settings. Receiver tests use an in-memory spreadsheet; they do not append artificial observations to the live workbook. Existing live quote records are not rewritten. Only FC Products needs the Insured Plus alias and appended Electric Boda row; the existing receiver reads that configuration directly.
