# Pilot signing

The distributed 0.2.1 APK uses application ID com.abaho.farecollector.pilot3 and the label **Kampala Fare Collector 2.1**. It installs beside earlier pilots, preserving their local records. Pair this installation and enable its accessibility service separately.

The distribution APK is signed after CI using a persistent private pilot key. The private signing backup is held outside this public repository. Future distributed updates must retain that application ID and signing key so Android can update them without removing local settings.

GitHub Actions builds, lints and tests a debug APK. Its temporary debug signature is for CI verification. The signing utility included in the artifact can verify and sign the distribution copy; the artifact contains no private signing key.

Never commit the signing backup, keystore, pairing token, deployed receiver URL or private route list.
