# Tao Patches

[Morphe](https://morphe.software) patches for the FanFiction.Net Android app (`com.fictionpress.fanfiction`).
They make the app send far fewer requests to the server, remove its telemetry, and keep it responsive.

### How to use these patches

Click the button below, or manually add this repo URL to Morphe Manager: https://github.com/xyz-user/xyz-patches

<a href="https://morphe.software/add-source?github=xyz-user/xyz-patches">
  <img src=".github/assets/add-to-morphe.svg" alt="Add to Morphe" height="48">
</a>

Patch the APKM bundle of FanFiction.Net 66.5 from APKMirror. The patched app is signed with a different key,
so uninstall the official app first, or apply "Clone app" from the Morphe patches to install it next to the
official app.

## Why

The app's server answers each user's requests one at a time, in order. Every unnecessary request makes the
screen you are waiting for load later. Measured on a real device (see [docs/measurements.md](docs/measurements.md)):

- Reopening the app sends 3 API requests instead of about 25-29.
- Covers and avatars were about half of all requests, because the app downloads every image again each time it
  is shown. They are now cached for 7 days.
- Firebase Analytics, Crashlytics and logging connections drop from about 33 in two minutes to none.

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->

#### A list of the patches will be shown here after the first release.

<!-- PATCHES_END -->

## Known issue: favorites

Since 2026-11-20 the server's favorite stories endpoint is extremely slow for every app user, so large
favorites lists never load. This is a server problem that no patch can fix. As a stopgap, enable both
"Favorites letter filter" and "Network timeout": favorites then open one starting letter at a time.
While a letter loads, other screens wait behind it.

## Building

Requires JDK 17+, the Android SDK, and a GitHub token with `read:packages`
([setup](https://github.com/MorpheApp/morphe-patcher/blob/main/docs/2_1_setup.md#-prepare-the-environment)).

```sh
GITHUB_ACTOR=<user> GITHUB_TOKEN=<token> ./gradlew buildAndroid
```

The bundle is written to `patches/build/libs/patches-*.mpp`. Work happens on the `dev` branch: `feat:` and
`fix:` commits there publish pre-releases, and merging `dev` into `main` publishes a stable release.

## License

GPLv3, see [LICENSE](LICENSE) and [NOTICE](NOTICE). Not affiliated with FanFiction.Net, FictionPress, or the
Morphe project.
