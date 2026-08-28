# 👋🧩 Morphe Patches template

Template repository for Morphe Patches.

## ❓ About

Patches for apps I like.

<!-- TODO: Update this about section with a brief introduction/summary about this repo and what it offers. -->

### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=SamuelBMartins/morphe-patches

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.0.0-dev.1](https://github.com/SamuelBMartins/morphe-patches/releases/tag/v1.0.0-dev.1)**&nbsp;&nbsp;•&nbsp;&nbsp;`dev`&nbsp;&nbsp;•&nbsp;&nbsp;3 patches total
<details open>
<summary>📦 Tapas&nbsp;&nbsp;•&nbsp;&nbsp;3 patches</summary>
<br>

**🎯 Supported versions:**

| 7.13.0 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Disable ad mediation SDK](#disable-ad-mediation-sdk) | Prevents the Unity LevelPlay, AppLovin, ironSource, Vungle and Tapjoy SDKs from starting up, which also stops their tracking. Rewarded ads can no longer be watched to earn Ink or unlock episodes. |  |
| [Hide ads](#hide-ads) | Hides banner ads in the episode viewer, comments and series pages, and hides Tapas' own in-house promo ads. |  |
| [Hide promo popups](#hide-promo-popups) | Hides the full screen promo popups shown on the home tab, including the "Free Ink offer" popup, and stops Braze marketing overlays from being shown. Push notifications are not affected. |  |

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
