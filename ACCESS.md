# Native Android App Access / Build Guide

Repository: `l86385138-pixel/app-creator-new`

## Outputs
- Release APK: `app-release.apk`
- Release AAB: `app-release.aab`

## Native requirement
This project is a native Android application using Android Activity/View components. It does **not** load the coaching website inside a WebView.

## App identity
- App: GAYAN GANGA COCHING CENTER
- Package: `com.gayangangacoachingcenter.app`
- Orientation: Portrait

## Build
GitHub Actions workflow: `.github/workflows/build-apk.yml`

The workflow builds both `assembleRelease` and `bundleRelease` and uploads the two artifacts separately.

## Firebase access
Firebase configuration must be supplied as `android/app/google-services.json` when Firebase services are connected. Do not commit private service-account keys or passwords into the repository.

## Artifact access
Open GitHub → Actions → Build Release APK and AAB → select a successful run → Artifacts → download the APK or AAB.
