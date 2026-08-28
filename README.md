# 👋🧩 Morphe Patches template

Template repository for Morphe Patches.

## ❓ About

Patches for apps I like.

<!-- TODO: Update this about section with a brief introduction/summary about this repo and what it offers. -->

### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=SamuelBMartins/morphe-patches

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.0.0-dev.2](https://github.com/SamuelBMartins/morphe-patches/releases/tag/v1.0.0-dev.2)**&nbsp;&nbsp;•&nbsp;&nbsp;`dev`&nbsp;&nbsp;•&nbsp;&nbsp;8 patches total
<details open>
<summary>📦 Tapas&nbsp;&nbsp;•&nbsp;&nbsp;4 patches</summary>
<br>

**🎯 Supported versions:**

| 7.13.0 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Disable ad mediation SDK](#disable-ad-mediation-sdk) | Prevents the Unity LevelPlay, AppLovin, ironSource, Vungle and Tapjoy SDKs from starting up, which also stops their tracking. Rewarded ads can no longer be watched to earn Ink or unlock episodes. |  |
| [Hide ads](#hide-ads) | Hides banner ads in the episode viewer, comments and series pages, and hides Tapas' own in-house promo ads. |  |
| [Hide promo popups](#hide-promo-popups) | Hides the full screen promo popups shown on the home tab, including the "Free Ink offer" popup, and stops Braze marketing overlays from being shown. Push notifications are not affected. |  |
| [Open Library on startup](#open-library-on-startup) | Opens the Library tab on its Subscribed list at startup, instead of the Home tab. Opening the app from a notification or a link is not affected. |  |

</details>

<details open>
<summary>📦 WEBTOON&nbsp;&nbsp;•&nbsp;&nbsp;4 patches</summary>
<br>

**🎯 Supported versions:**

| 3.9.11 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Hide ads](#hide-ads) | Hides the ad on the splash screen and the display ads on the Home tab, the More tab, the episode list of a series, and the top and the end of the episode viewer. Rewarded ads can still be watched to earn coins or unlock episodes. |  |
| [Hide promo popups](#hide-promo-popups) | Hides the full screen promotion popups, both the ones shown when the Home tab opens and the ones that appear after liking, subscribing, reading or purchasing, and stops Braze marketing overlays from being shown. Push notifications are not affected. |  |
| [Hide recommend popups](#hide-recommend-popups) | Hides the popup suggesting other series that is shown after the last episode of a series, in both the comic and the short drama viewer. Covers the full screen popup, the bottom sheet and the web popup variants. |  |
| [Open Subscribed on startup](#open-subscribed-on-startup) | Opens the MY tab on its Subscribed list instead of the Home tab whenever the app opens its main screen, which includes the cold start from the launcher. A notification or a link that points at a specific tab, title or episode is not affected, and neither is the Home tab of the bottom bar. |  |

</details>

<!-- PATCHES_END -->

### 🛠️ Building locally

- Run `./gradlew buildAndroid`
- The built patches .mpp file is found in `patches/build/libs/patches-*.mpp`
- Patch the mpp file using [Morphe-Desktop](https://github.com/MorpheApp/morphe-desktop)
  like any other patch bundle.

See the [Morphe documentation](https://github.com/MorpheApp/morphe-documentation) for more information.

## 📜 License

UserXYZ Patches are licensed under the [GNU General Public License v3.0](LICENSE)
