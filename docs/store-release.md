# Store records and GitHub delivery

## App identities

| Platform | Listing | Identifier | Console |
| --- | --- | --- | --- |
| Apple | Barge Boss: Towboat | `com.bargeboss.game.ios` | [App Store Connect 6835285833](https://appstoreconnect.apple.com/apps/6835285833/distribution) |
| Google Play | Barge Boss | `com.bargeboss.game` | [Play Console](https://play.google.com/console/u/0/developers/5411064851941850442/app/4975600156932203800/app-dashboard) |

Apple SKU is `bargeboss-ios`; both records use English (U.S.). Play is a free
game. The installed iOS display name remains **Barge Boss**. Neither record is a
published release. Apple's initial store version is 1.0 while the current binary
marketing version is 0.1.0; align them before an App Store submission. TestFlight
can use the current 0.1.0 version.

## Source and boundaries

Adapted from `koniho/dddumpling` at `f787ddc4af7ee8aa2c4dd9ab7c23848e23974008`:
`ios-release.yml`, `fastlane/ios.rb`, the pinned fastlane dependencies and
`build-bundle.sh`. The source game is unchanged. A temporary credential-transfer workflow was later
added to DDDumpling with explicit authorization and removed after completion. Game Center,
Firebase, production publishing, DDDumpling store text and external tester
notifications were not copied.

Release workflows run manually from main. Both default to building artifacts
without uploading. Android upload is restricted to internal testing and defaults
to a draft; activation is a separate input. iOS upload does not distribute to
external testers or submit the app for review. The ordinary check workflow uses
no release credentials. `release-config.yml` validates lane behavior without
credentials or uploads.

## Credential configuration

GitHub's secrets API returns names and timestamps, not secret values. Existing
DDDumpling secrets cannot be downloaded through that API. All ten required
release secrets are now populated in Barge Boss through the authorized transfer
recorded below. For future updates, supply original values directly through
GitHub Settings or `gh secret set` via stdin. Never put plaintext secrets in
commits, shell arguments, logs, chat or workflow artifacts.

### Apple environment: ios-release

Variables:

- `IOS_TEAM_ID`: `QC7LYR5635`
- `IOS_MATCH_GIT_URL`: `git@github.com:koniho/dddumpling-ios-signing.git`

Secrets (same names as the working DDDumpling environment):

- `IOS_MATCH_GIT_PRIVATE_KEY`
- `IOS_MATCH_PASSWORD`
- `IOS_APPSTORE_KEY_ID`
- `IOS_APPSTORE_ISSUER_ID`
- `IOS_APPSTORE_KEY_BASE64`

Barge Boss now has its own active App Store provisioning profile,
`match AppStore com.bargeboss.game.ios` (UUID
`7f75fe9b-079c-4e43-ab81-2834c8c7566e`, expires 2027-09-13).
It reuses distribution certificate `8F45S3FDPB`; no certificate was created,
replaced or revoked. The profile is encrypted in the private match repository
at commit `e4d4f19bc54736ba4001a7f606060aa76a3cc7e1` alongside the unchanged
DDDumpling assets. The normal release lane remains read-only.

The iOS AppIcon catalog supplies opaque 1024px light, dark and grayscale tinted
renditions of the existing Android tugboat mark. Regenerate them with
`swift ios/scripts/generate-icon.swift`. The Release simulator build compiled
the catalog and the installed Home Screen icon was visually checked. Native
signing/export and App Store processing remain separate validation steps.

Build numbers come from `github.run_number.github.run_attempt` and are checked in
the exported IPA. Keep this workflow as their source, or coordinate a marketing
version change before uploading higher manual build numbers.

### Android environment: android-release

Secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_PASSWORD`
- `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` (needed only for upload)

Use an authorized release/upload key from the existing credential source. Do not
substitute DDDumpling's local debug key or regenerate the development phone's key.
The Play service account must have access to **Barge Boss**, including release
access to testing tracks. Existing DDDumpling-only permissions do not establish
that access. A first manual AAB upload/Play App Signing enrollment may be needed
before the Publishing API accepts uploads for the new app.

Android versionCode remains explicit in `AndroidManifest.xml`. Increment it for
every new Play upload; retries with an already-used versionCode will be rejected.
The bundle build forces profiling off and never creates or replaces a debug key.

## Commands

```sh
# Local packaging validation without signing or upload:
./build-bundle.sh --unsigned
python3 tools/verify-release.py android
ruby tools/test-release.rb

# GitHub artifact-only builds (requires signing inputs above):
gh workflow run android-release.yml --ref main
gh workflow run ios-release.yml --ref main

# Explicit uploads once signing, app icons and store access are ready:
gh workflow run android-release.yml --ref main -f upload_to_play=true
# Add -f activate_release=true only when internal distribution is intended.
gh workflow run ios-release.yml --ref main -f upload_to_testflight=true
```

The build artifact checks enforce the Barge Boss identity, expected versions,
production Android settings and unchanged installed iOS name. They do not certify
store policy compliance, screenshots, privacy disclosures or physical-device
behavior. Those remain separate release tasks.

## Setup validation (2026-11-15)

- Created `ios-release` and `android-release` GitHub environments restricted to main;
  populated the two non-secret Apple variables above. The later authorized secret
  transfer is recorded below.
- `build-bundle.sh --unsigned` passed the shared suite and bundletool validation.
  The package/version/permission and developer-class checks passed on that AAB.
- Offline release-boundary tests, shell syntax checks and actionlint passed locally.
- At this initial checkpoint, signed export, store uploads and physical-device
  installation were unverified. Later Android signing validation is recorded below.

### Local credential discovery and hosted checks

The source checkout's ignored `.private/ios-signing/` directory contains
`deploy_key`, `match-password`, and setup scripts with the Apple API key identifiers.
The API private-key file referenced by those scripts is absent. No Android release
keystore or Play service-account JSON was found in the local checkout (debug
keystores are present and are not release substitutes). After explicit user approval, copied `IOS_MATCH_GIT_PRIVATE_KEY`,
`IOS_MATCH_PASSWORD`, `IOS_APPSTORE_KEY_ID`, and `IOS_APPSTORE_ISSUER_ID` directly
into Barge Boss's encrypted `ios-release` environment secrets via stdin. Verified
all four names are present without reading or printing their values.
At that checkpoint the Apple private key and Android credentials were still
missing locally. The later workflow transfer resolved those credential gaps;
the Barge Boss provisioning profile remains a separate app-specific requirement.

[Hosted release configuration check 43797755353](https://github.com/koniho/bargeboss/actions/runs/43797755353)
passed, including pinned fastlane option compatibility and lane loading. This is
configuration validation, not a signed build or upload.

### Authorized GitHub-to-GitHub credential transfer (2026-11-16 UTC)

[Transfer run 43805138520](https://github.com/koniho/dddumpling/actions/runs/43805138520)
succeeded. Its manually dispatched, main-only workflow read exactly the five
Apple and five Android secrets listed above. It encrypted each value with the
corresponding Barge Boss environment's GitHub public key using libsodium sealed
boxes. Only destination-encrypted ciphertext was downloaded and submitted to
GitHub's environment-secret API; no plaintext credentials left the source runner,
were logged, or were committed. No cross-repository access token was created.

Verified all five secret names in each destination environment. The temporary
workflow was disabled and removed (source commits `9ed7a5116d7e` and
`390232cb3d31`), its encrypted transfer artifact was deleted, and the local
ciphertext copy was removed. Existing DDDumpling secrets and release workflows
were preserved. Secret presence alone does not prove Play account permissions
or a valid Barge Boss Apple provisioning profile.

[Android release run 43805246578](https://github.com/koniho/bargeboss/actions/runs/43805246578)
then passed at source commit `4d55fc384cb1`: shared checks, signed AAB build,
bundletool validation, artifact identity/production verification and artifact
upload. Artifact `bargeboss-android-release-1-1` contains the signed bundle.
`upload_to_play=false` and `activate_release=false`; the Play upload step was
skipped. This verifies the transferred Android signing credentials, not the
service account's Barge Boss Play access. iOS signed export is still pending the
Barge Boss profile and app icon. No store release or tester distribution occurred.

### Barge Boss iOS signing assets and icon (2026-11-16 UTC)

[Profile setup run 43806205716](https://github.com/koniho/bargeboss/actions/runs/43806205716)
created the app-specific profile through Apple's API using the existing team
certificate. Only match-encrypted profile data left the runner. The temporary
setup workflow was removed after use. No store upload was requested by this setup.
The simulator app was built, installed and launched without resetting saved data.
No physical iOS or Android device was connected for installation checks.
