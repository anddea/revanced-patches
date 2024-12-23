# [3.0.0-dev.7](https://github.com/anddea/revanced-patches/compare/v3.0.0-dev.6...v3.0.0-dev.7) (2024-12-23)


### Bug Fixes

* **Reddit - Hide ads:** `Hide ads` patch fails on `2024.17.0` ([8426f74](https://github.com/anddea/revanced-patches/commit/8426f7471e1a54963cb56131f3e1362068375d1b))
* **YouTube - Shorts components:** Patch failing during building with certain patch selection ([a442ff3](https://github.com/anddea/revanced-patches/commit/a442ff38ca1a4ee9372fb4bf1bbdc8f8c67a1c20))
* **YouTube - Toolbar components:** `Hide voice search button` setting does not work ([cb6868a](https://github.com/anddea/revanced-patches/commit/cb6868a8eb46b75f4741ea58adb684aa9c37b4d3))
* **YouTube Music:** App crashes when including `Hide action bar components` patch ([e7646e2](https://github.com/anddea/revanced-patches/commit/e7646e2410e6e622207ddf8b89011d3507f1ea30))


### Features

* **YouTube - Navigation bar components:** Bring back`Enable translucent navigation bar` setting ([158f8da](https://github.com/anddea/revanced-patches/commit/158f8da6c7432c9df07a3b91d7c25f87ec14aaef))
* **YouTube - Seekbar components:** Bring back `Enable Cairo seekbar` setting (for YouTube 19.23.40-19.32.36) ([fed8915](https://github.com/anddea/revanced-patches/commit/fed89158dd9dd63552362c580d4eebfd57715d68))
* **YouTube Music - Spoof client:** Limit support version to 7.16.53 and change default client preset ([20beb64](https://github.com/anddea/revanced-patches/commit/20beb648ae675e313ff099da4dbdf6d6b91e9285))

# [3.0.0-dev.6](https://github.com/anddea/revanced-patches/compare/v3.0.0-dev.5...v3.0.0-dev.6) (2024-12-21)


### Bug Fixes

* **YouTube - Custom branding icon:** Patch option `restoreOldSplashAnimation` not working in YouTube 19.32.39+ ([b81d32e](https://github.com/anddea/revanced-patches/commit/b81d32eea4ca5ba26c6294d9467f6823d2088ca0))
* **YouTube - Hide feed components:** `Hide carousel shelf` hiding in library in certain situations ([10e4667](https://github.com/anddea/revanced-patches/commit/10e466737ac09f4ab328d520c211a9b1a8e413ab))
* **YouTube - Miniplayer:** Use estimated maximum on screen size for devices with low density screens ([efeb5fb](https://github.com/anddea/revanced-patches/commit/efeb5fba02d660bfd3f19b59fc1ef21ba11ea062))
* **YouTube - Theme:** Splash background color not applied in latest YouTube client ([a8c4462](https://github.com/anddea/revanced-patches/commit/a8c446222b9a75e0c74af78dfe029334d8f3dfa0))
* **YouTube - Video playback:** Applying default video quality to Shorts causes the beginning of the shorts to get stuck in a loop ([9c4c56e](https://github.com/anddea/revanced-patches/commit/9c4c56eefa65b1c3f61e23cf78375272b33e679f))
* **YouTube Music - Hide action bar components:** `Hide Download button` setting not working in YouTube Music 7.25.52 ([85dfb09](https://github.com/anddea/revanced-patches/commit/85dfb09521cff167e9029b25d48c9d84a33ed57b))
* **YouTube Music - SponsorBlock:** `Change segment behavior` and `About` sections are hidden in the settings ([176adb8](https://github.com/anddea/revanced-patches/commit/176adb89102f96c22e8a437d019c688df933e2dd))
* **YouTube:** Splash screen background color does not change in dark mode if `Theme` patch is excluded ([28df1b4](https://github.com/anddea/revanced-patches/commit/28df1b4172262719fcc86b038e4a617bce0dc6e9))
* **YouTube:** When clicking on timestamps in comments, playback speed sometimes changes to 1.0x (unpatched YouTube bug) ([93c4bf8](https://github.com/anddea/revanced-patches/commit/93c4bf8fb63fe7f06f3908bc6e755c568f86667f))


### Features

* **YouTube - Navigation bar components:** Add `Disable translucent status bar` setting ([fe09dbc](https://github.com/anddea/revanced-patches/commit/fe09dbcece9324f224950cd784caabf2db64bf6c))
* **YouTube - Navigation bar components:** Separate `Enable translucent navigation bar` setting into `Disable light translucent bar` and `Disable dark translucent bar` settings ([602de6e](https://github.com/anddea/revanced-patches/commit/602de6e9692c1af4ab892726305ce2e6d4a04f08))
* **YouTube - Shorts components:** Add `Restore old player layout` setting (YouTube 18.29.38 ~ 19.16.39) ([bf8afdd](https://github.com/anddea/revanced-patches/commit/bf8afddae275b9ef7568fb68398749d2b3f47941))
* **YouTube - Shorts components:** Add styles to custom actions dialog ([e95b064](https://github.com/anddea/revanced-patches/commit/e95b0643131bfe20ded2747734c34c603838812c))
* **YouTube - Swipe controls:** Change the setting name `Enable watch panel gestures` to `Disable watch panel gestures`, and change the setting name `Enable swipe to change video` to `Disable swipe to change video` ([375cf3a](https://github.com/anddea/revanced-patches/commit/375cf3ad331c3823ee17cbea450f8e87510e58b0))
* **YouTube Music - Hide action bar components:** Limit the available versions of the `Override Download action button` setting to 7.16.53 ([16ead35](https://github.com/anddea/revanced-patches/commit/16ead35768678e7ac4a3327635a2f6f3ecf6bce2))
* **YouTube Music - Spoof client:** Add `Use old client` and `Default client` settings ([bb3bd2a](https://github.com/anddea/revanced-patches/commit/bb3bd2a9d799c0177ad39c8d59b0884c510b5864))
* **YouTube:** Support version `19.44.39` ([22419fd](https://github.com/anddea/revanced-patches/commit/22419fd9d53bd2234283423a8e9fec30b1fff3fc))

# [3.0.0-dev.5](https://github.com/anddea/revanced-patches/compare/v3.0.0-dev.4...v3.0.0-dev.5) (2024-12-17)


### Bug Fixes

* **YouTube - Enable gradient loading screen:** `Enable gradient loading screen` not working on YouTube 19.34.42+ ([d6b6a42](https://github.com/anddea/revanced-patches/commit/d6b6a427e31e3324163679735b7dceac0234460a))
* **YouTube - Hide ads:** Hide new type of featured promotions ([c1dadc0](https://github.com/anddea/revanced-patches/commit/c1dadc0e2b45149974d8a50c2d7de0e05e1537a2))
* **YouTube - Hide feed components:** `Hide carousel shelf` hiding in library in certain situations ([6323421](https://github.com/anddea/revanced-patches/commit/6323421d7c3a99c278258f19ca36a489b6345875))
* **YouTube - Hide feed components:** `Hide carousel shelf` not hiding in home feed in certain situations ([0c690b1](https://github.com/anddea/revanced-patches/commit/0c690b1ee732764588ef600f37097b662b11e902))
* **YouTube - Hide feed components:** New kind of community posts are not hidden ([a58ed6b](https://github.com/anddea/revanced-patches/commit/a58ed6b19727940f28b8e1d1247dcd742942495c))
* **YouTube - Hide player flyout menu:** `Sleep timer menu` always hidden in YouTube 19.34.42 ([fa42f5f](https://github.com/anddea/revanced-patches/commit/fa42f5f820772fddba720f9895a21b7f21c2182f))
* **YouTube - MaterialYou:** Theme not applied to notification dots in YouTube 19.34.42+ ([fd31d87](https://github.com/anddea/revanced-patches/commit/fd31d87ba5925305882d761f3455c05007988ddf))
* **YouTube - Player components:** `Hide seek message` not working on YouTube 19.34.42 ([8cb3b4b](https://github.com/anddea/revanced-patches/commit/8cb3b4b91ae9531824a124ae897b1d9729f8f340))
* **YouTube - Seekbar components:** `Custom seekbar color` not applied to gradient seekbar in YouTube 19.34.42 ([b3ac64c](https://github.com/anddea/revanced-patches/commit/b3ac64c05a44d8b7d035de94f2d9cec26d7514f3))
* **YouTube - Shorts components:** `Hide Shorts shelves` not hiding in home feed in certain situations ([3481e01](https://github.com/anddea/revanced-patches/commit/3481e01a7c224d262ec7b23e1f132787dc3f838e))
* **YouTube - Spoof streaming data:** On `iOS` clients, livestreams always start from the beginning ([4e60bf5](https://github.com/anddea/revanced-patches/commit/4e60bf514bd5480e35bb102d2f4758f766b311a8))
* **YouTube - Spoof streaming data:** Videos end 1 second early on iOS client ([b2cc033](https://github.com/anddea/revanced-patches/commit/b2cc03320e934532425d852ac5832dffdfefb98c))
* **YouTube - VideoInformation:** Channel name not fetched in YouTube 19.34.42 ([2e19453](https://github.com/anddea/revanced-patches/commit/2e194533eb9c7169fdbd6ec321422dca19d54a34))
* **YouTube & YouTube Music - Custom branding icon:** Patching fails in some environments when the path entered in the patch options contains uppercase letters ([786bc36](https://github.com/anddea/revanced-patches/commit/786bc36e2a99ae9b2c15c7659c3ac1f4c9ee26f6))
* **YouTube Music - Spoof client:** Action bar not loading as of YouTube Music 7.17.51 ([943c288](https://github.com/anddea/revanced-patches/commit/943c28866ab7af53fb986b648c87f8baf0d67ff9))


### Features

* **YouTube - Custom branding icon:** Add `YouTube Black` icon ([e706c5f](https://github.com/anddea/revanced-patches/commit/e706c5fc67c2318505ea1e5588437bba0040c85a))
* **YouTube - Custom branding icon:** Restrict the version that can use the patch option `Restore old splash animation` to 19.16.39 (deprecated) ([8589c5a](https://github.com/anddea/revanced-patches/commit/8589c5afc4b84ef680f56dee7b93ad3e93df9985))
* **YouTube - Navigation bar components:** Add missing resource for Cairo notification icon (YouTube 19.34.42+) ([2982725](https://github.com/anddea/revanced-patches/commit/2982725d8a9fdd81280d9ba79425d0bd05b3431d))
* **YouTube - Player components:** Add `Hide Chat summary in live chat` setting ([963dbe8](https://github.com/anddea/revanced-patches/commit/963dbe89970b64ff99d277bfada6c3dfb403f4bb))
* **YouTube - Remove background playback restrictions:** Add PiP mode support in Shorts ([4fc44b2](https://github.com/anddea/revanced-patches/commit/4fc44b2ba56c78aaa87b28396a0ef72e0e9fe3f9))
* **YouTube - Seekbar components:** Change default seekbar color to match new branding ([26d8ba6](https://github.com/anddea/revanced-patches/commit/26d8ba6c2bc2a81725bbf61a7bf3a4090175e156))
* **YouTube - Seekbar components:** Remove `Enable Cairo seekbar` setting, which is no longer needed (Enabled by default in YouTube 19.34.42) ([c12f4ae](https://github.com/anddea/revanced-patches/commit/c12f4aeddff61a1c2ac00c26c59777bd76e91f69))
* **YouTube - Shorts components:** Add `Change Shorts background repeat state` setting (YouTube 19.34.42+) ([84d6ccc](https://github.com/anddea/revanced-patches/commit/84d6ccc7cf738650bbfbf15b42fc5b1be02f3d98))
* **YouTube - Shorts components:** Add `Custom actions in toolbar` setting (YouTube 18.38.44+) ([6732b2b](https://github.com/anddea/revanced-patches/commit/6732b2b373b6e4208c2966f4ea262ebe49886f92))
* **YouTube - Shorts components:** Add `Custom actions` setting (YouTube 19.05.36+) ([ff5b527](https://github.com/anddea/revanced-patches/commit/ff5b5279d4c21eabeedaf2d6a1e8ae25871ae042))
* **YouTube - Spoof app version:** Add target version `19.26.42 - Disable Cairo icon in navigation and toolbar` and `19.33.37 - Restore old playback speed flyout panel` ([671e809](https://github.com/anddea/revanced-patches/commit/671e8098c516b05af82eb1609e6740e49d79838c))
* **YouTube - Spoof streaming data:** Remove `Skip iOS livestream playback` setting (no longer needed) ([b5e507c](https://github.com/anddea/revanced-patches/commit/b5e507c7a3718843469f7e366f96a53d279c28a0))
* **YouTube & YouTube Music - Settings:** Add `RVX settings summaries` to patch options ([6211b44](https://github.com/anddea/revanced-patches/commit/6211b448c9979218fe566c8c00e69c09ebe37790))
* **YouTube Music - Custom branding icon:** Delete old `Revancify Yellow` icon ([#893](https://github.com/anddea/revanced-patches/issues/893)) ([0c09f4d](https://github.com/anddea/revanced-patches/commit/0c09f4df563c57f9645506f04fb8ca8f1a399334))
* **YouTube Music - Hide player flyout menu:** Add `Hide Speed dial menu` setting ([42b6bd5](https://github.com/anddea/revanced-patches/commit/42b6bd5e994c3fc22457bf79bfbc55cd15de5734))
* **YouTube Music:** Add `Disable DRC audio` patch ([a3b458d](https://github.com/anddea/revanced-patches/commit/a3b458d50f769cd35d7e7b5ff9144aec0f8dc199))
* **YouTube Music:** Add `Spoof streaming data` patch ([ef14e5a](https://github.com/anddea/revanced-patches/commit/ef14e5acc93e9a9ad5d9eef861023f1d5623e4ff))
* **YouTube Music:** Support version `7.25.52` ([d8fac8b](https://github.com/anddea/revanced-patches/commit/d8fac8b82c5983efd8096f533e175befe4ba396a))
* **YouTube:** Support version `19.38.41` ([756e02f](https://github.com/anddea/revanced-patches/commit/756e02f91a642936f5ab502dcfc33d05eff6eabd))

# [3.0.0-dev.4](https://github.com/anddea/revanced-patches/compare/v3.0.0-dev.3...v3.0.0-dev.4) (2024-12-12)


### Bug Fixes

* **YouTube Music - Visual preferences icons:** Custom branding icons did not work ([45fa7fd](https://github.com/anddea/revanced-patches/commit/45fa7fd9169218bd0cee46b8413aee7611212b0b))

# [3.0.0-dev.3](https://github.com/anddea/revanced-patches/compare/v3.0.0-dev.2...v3.0.0-dev.3) (2024-12-12)


### Bug Fixes

* **YouTube - Overlay buttons:** Play all button did not work for all videos when using all content by time ascending ([e4e51f5](https://github.com/anddea/revanced-patches/commit/e4e51f583ebbf2986a1077860503e3e94c3a3f05))
* **YouTube - Seekbr components:** Reverse start and end colors for Cairo seekbar ([e9bd106](https://github.com/anddea/revanced-patches/commit/e9bd106114c1669426d10830b544d7936a0728a1))

# [3.0.0-dev.2](https://github.com/anddea/revanced-patches/compare/v3.0.0-dev.1...v3.0.0-dev.2) (2024-12-12)


### Bug Fixes

* **YouTube - Visual preferences icons:** Add missing icons in the Manager ([a9b443a](https://github.com/anddea/revanced-patches/commit/a9b443a738ca6f94a98ee32d9cd7ad0837ce66a8))

# [3.0.0-dev.1](https://github.com/anddea/revanced-patches/compare/v2.232.0-dev.1...v3.0.0-dev.1) (2024-12-11)


### Bug Fixes

* **YouTube - Return YouTube Dislike:** Show Shorts dislikes with new A/B button icons ([ad0d15e](https://github.com/anddea/revanced-patches/commit/ad0d15e832c0a51997c35780d880f1bcb2a8b495))
* **YouTube - Shorts components:** Do not hide Shorts action buttons on app first launch ([f5cd017](https://github.com/anddea/revanced-patches/commit/f5cd0173a845352da7d4d5d13f5d545eaf8217b1))
* **YouTube - SponsorBlock:** Fix create new segment crash on tablet custom roms ([58b5fbf](https://github.com/anddea/revanced-patches/commit/58b5fbfcc77d46831f61a3099fbf37c01ae2b2ba))
* **YouTube - Spoof streaming data:** Fix memory leak in `ByteArrayOutputStream` ([42d7bbe](https://github.com/anddea/revanced-patches/commit/42d7bbe8da244c73802e37cab646ccad590d7bdb))
* **YouTube - Video playback:** Correctly set default quality when changing from a low quality video ([8cbe976](https://github.com/anddea/revanced-patches/commit/8cbe9766a4f26aeee909a799b834c4c85efd7e9d))


### Code Refactoring

* Bump ReVanced Patcher & merge integrations ([7dde697](https://github.com/anddea/revanced-patches/commit/7dde697995b3fa02749eff52cf50d1f903fc54ef))


### Features

* **YouTube - Overlay buttons:** Replace `Time-ordered playlist` button with `Play all` button ([5a15809](https://github.com/anddea/revanced-patches/commit/5a15809c96c4d6e988b196be8d0a85a828fe1d1b))
* **YouTube - Spoof streaming data:** Rename the `iOS Compatibility mode` setting to `Skip iOS livestream playback` ([efbc77d](https://github.com/anddea/revanced-patches/commit/efbc77d6a0090cc15bec935aa993155cbf8bdd0c))
* **YouTube - Theme:** Add `Pale Blue`, `Pale Green`, `Pale Orange` light colors ([1bed931](https://github.com/anddea/revanced-patches/commit/1bed9310b7343f1d860c4738b512fce91ffc3895))
* **YouTube Music - Hide ads:** Changed the default value of `Hide fullscreen ads` setting to off and added a warning to the setting ([d337d21](https://github.com/anddea/revanced-patches/commit/d337d2115ef78fd1b2c8d2fd2e528d913b7978e9))
* **YouTube Music:** Add `Spoof client` patch ([09c7967](https://github.com/anddea/revanced-patches/commit/09c796784cbd70fde471773d2ecb5f2123855b73))
* **YouTube:** Support version `19.34.42` ([2018306](https://github.com/anddea/revanced-patches/commit/2018306d5f578ac9915f0a6001391999896fdacc))


### BREAKING CHANGES

* Patches and Integrations are now merged

# [2.232.0-dev.1](https://github.com/anddea/revanced-patches/compare/v2.231.0...v2.232.0-dev.1) (2024-11-10)


### Features

* **YouTube - Spoof app version:** Remove obsolete `19.13.37` spoof target ([743999e](https://github.com/anddea/revanced-patches/commit/743999e864892f33ee4153950339edc8ffae2578))
* **YouTube - Spoof streaming data:** Add `iOS Compatibility mode` setting ([48b26eb](https://github.com/anddea/revanced-patches/commit/48b26eb9d2b5076248af96c2342fdcd7f29b8a51))

# [2.231.0](https://github.com/anddea/revanced-patches/compare/v2.230.0...v2.231.0) (2024-11-07)


### Bug Fixes

* **YouTube - Overlay buttons:** Support A/B player layout ([a58c3df](https://github.com/anddea/revanced-patches/commit/a58c3dfbc6573adc56d7ea600bf66f5cb885ac73))
* **YouTube - Spoof app version:** Remove obsolete `17.33.42` spoof target ([1220449](https://github.com/anddea/revanced-patches/commit/1220449f2ac46f2dd5319441f82cc7de56e1efb6))


### Features

* **YouTube - Description components:** Add `Hide AI-generated video summary section` setting ([fef16e8](https://github.com/anddea/revanced-patches/commit/fef16e84d05be391b301e09d7958d685d4d9df38))
* **YouTube - Hide ads:** Add `Hide player shopping shelf` setting ([320aa48](https://github.com/anddea/revanced-patches/commit/320aa485f917ef612e12b5bca27a307c2961a57c))
* **YouTube - Hide feed components:** Add `Hide floating button` setting ([da4dd43](https://github.com/anddea/revanced-patches/commit/da4dd43bb1b3a493fb1f4480fca2f9471e431437))
* **YouTube - Seekbar components:** Add `Enable high quality thumbnails` setting ([a0476b5](https://github.com/anddea/revanced-patches/commit/a0476b59b5c724cc1da232df0757ee9797edf505))

# [2.231.0-dev.2](https://github.com/anddea/revanced-patches/compare/v2.231.0-dev.1...v2.231.0-dev.2) (2024-11-06)


### Bug Fixes

* **YouTube - Overlay buttons:** Support A/B player layout ([a58c3df](https://github.com/anddea/revanced-patches/commit/a58c3dfbc6573adc56d7ea600bf66f5cb885ac73))
* **YouTube - Spoof app version:** Remove obsolete `17.33.42` spoof target ([1220449](https://github.com/anddea/revanced-patches/commit/1220449f2ac46f2dd5319441f82cc7de56e1efb6))


### Features

* **YouTube - Description components:** Add `Hide AI-generated video summary section` setting ([fef16e8](https://github.com/anddea/revanced-patches/commit/fef16e84d05be391b301e09d7958d685d4d9df38))

# [2.231.0-dev.1](https://github.com/anddea/revanced-patches/compare/v2.230.0...v2.231.0-dev.1) (2024-10-29)


### Features

* **YouTube - Hide ads:** Add `Hide player shopping shelf` setting ([320aa48](https://github.com/anddea/revanced-patches/commit/320aa485f917ef612e12b5bca27a307c2961a57c))
* **YouTube - Hide feed components:** Add `Hide floating button` setting ([da4dd43](https://github.com/anddea/revanced-patches/commit/da4dd43bb1b3a493fb1f4480fca2f9471e431437))
* **YouTube - Seekbar components:** Add `Enable high quality thumbnails` setting ([a0476b5](https://github.com/anddea/revanced-patches/commit/a0476b59b5c724cc1da232df0757ee9797edf505))

# [2.230.0](https://github.com/anddea/revanced-patches/compare/v2.229.0...v2.230.0) (2024-10-29)


### Bug Fixes

* **YouTube - Custom branding icon:** Patch failed due to animation files when AFN is selected ([231a468](https://github.com/anddea/revanced-patches/commit/231a4686be7fe5b87690d9cf8c88fae5245971ef))
* **YouTube - Custom Shorts action buttons:** Low icon quality for `Cairo` option on YouTube 18.29.38 ([819189e](https://github.com/anddea/revanced-patches/commit/819189ed9e66e039c540a78c81c84f9b6bf96682))
* **YouTube - Hide feed components:** `Hide carousel shelf` setting sometimes hides the library shelf ([8c3a5d2](https://github.com/anddea/revanced-patches/commit/8c3a5d2fd08cdca5cf66324cc2a44b722e7062cc))
* **YouTube - Hide feed components:** `Hide Latest videos button` setting does not support tablets ([85397f8](https://github.com/anddea/revanced-patches/commit/85397f85d14e86c598f5df95d79cf8de22dfa891))
* **YouTube - Hook YouTube Music actions:** App crashes when first installed ([aee5a18](https://github.com/anddea/revanced-patches/commit/aee5a18b837c2f0d139a4a6f312d9df7f1b6de04))
* **YouTube - Player components:** The `Disable player popup panels` setting disables the engagement panel in Mix playlists on certain YouTube versions ([63c463c](https://github.com/anddea/revanced-patches/commit/63c463ca2bebd43c85e79609275f72bf4176f242))
* **YouTube - Settings:** `Search bar in settings` can't find `RYD` and `SponsorBlock` settings ([ab222a6](https://github.com/anddea/revanced-patches/commit/ab222a672317ec7d00d566b1ca655b7b5c9a78cd))
* **YouTube - Shorts components:** `Hide paused header` setting does not work ([6df82cf](https://github.com/anddea/revanced-patches/commit/6df82cfd72951867babd4e985f51098ff773f310))
* **YouTube - SponsorBlock:** The new segment window never showed up with Bold icons selected due to the missing drag handle ([c736841](https://github.com/anddea/revanced-patches/commit/c736841774e3b527871d6afe4b6d311a2bc2e573))
* **YouTube - Spoof streaming data:** Wrong register used ([ef1908f](https://github.com/anddea/revanced-patches/commit/ef1908ffd3554d787eb4dcc50995e2888a5f3fad))
* **YouTube & YouTube Music - GmsCore support:** Unimplemented service in GmsCore causes memory leak ([cca866b](https://github.com/anddea/revanced-patches/commit/cca866b218749dd55c52b0616c1d015135a44511))
* **YouTube Music - Custom branding icon:** Patch fails on certain versions ([1b8654a](https://github.com/anddea/revanced-patches/commit/1b8654a9779e0c1029da4b25430d6f0ef946f5e4))
* **YouTube Music - Disable Cairo splash animation:** Some versions are recognized as unpatchable even though they can be patched ([1e74ff9](https://github.com/anddea/revanced-patches/commit/1e74ff94811097370e4c0f0d05dc429601e8f05c))
* **YouTube Music - Player components:** `Hide Audio / Video toggle` setting not working in landscape mode ([e171e45](https://github.com/anddea/revanced-patches/commit/e171e452b1bc936079373afcaedeeb7f30394dec))
* **YouTube Music - Player components:** `Remember shuffle state` setting does not remember the correct state ([18482b2](https://github.com/anddea/revanced-patches/commit/18482b2e6b57d2eea95527a05e9d86674446c052))
* **YouTube Music - Player components:** Patches do not apply properly in certain versions ([db80b1d](https://github.com/anddea/revanced-patches/commit/db80b1d0dd30d0f19323541c9018ff5ab62dae2c))
* **YouTube Music - SponsorBlock:**   SponsorBlock does not skip segments at the beginning of a video when in the background ([e895e6f](https://github.com/anddea/revanced-patches/commit/e895e6f5c638df5e2233cc50dc82de9ccfe89df0))
* **YouTube Music - Spoof app version:** ListPreference sometimes selects wrong values ([ca3dba2](https://github.com/anddea/revanced-patches/commit/ca3dba27402448c8a89c3fc3fabb080f8315c303))


### Features

* **Custom branding icon:** Add `MMT Orange`, `MMT Pink`, `MMT Turquoise` ([e203ad1](https://github.com/anddea/revanced-patches/commit/e203ad159e8717a2042a978e1c6cee04fa8ce745))
* **YouTube - Change start page:** Add `Change start page type` setting ([251d5d7](https://github.com/anddea/revanced-patches/commit/251d5d726061aff08ebe190513e10862db7c4856))
* **YouTube - Custom Shorts action buttons:**  Add `Cairo` ([f3b6717](https://github.com/anddea/revanced-patches/commit/f3b67174da909ab07d4b04bef14d5d7ae672cc3b))
* **YouTube - Hide comments components:** Add `Hide highlighted search links` setting ([39ae3b0](https://github.com/anddea/revanced-patches/commit/39ae3b0072b1e2a8716777d146ba0b45ec555c3e))
* **YouTube - Hide feed components:** Add `Hide related videos` setting ([a82c9ba](https://github.com/anddea/revanced-patches/commit/a82c9bac56e875417e8d0632d5e4cb14a3e4c4aa))
* **YouTube - Hide feed components:** Add `Hide UPCOMING video` setting ([ec9d641](https://github.com/anddea/revanced-patches/commit/ec9d6419df2ed3bd13c4171a92216314202802a1))
* **YouTube - Hide feed components:** Remove `Hide UPCOMING video` setting ([55a6953](https://github.com/anddea/revanced-patches/commit/55a695395bff0903d34ca45593d3ff97609fe264))
* **YouTube - Hide player flyout menu:** Add `Hide 1080p Premium menu` setting ([3231f36](https://github.com/anddea/revanced-patches/commit/3231f3614da9a49ae3bdead139849992d6e85c95))
* **YouTube - Navigation bar components:** Add `Hide navigation bar` setting ([5936015](https://github.com/anddea/revanced-patches/commit/593601526bd10a6976d108d1d8689c44cf6331e9))
* **YouTube - Player components:** Add `Sanitize video subtitle` setting ([fc4e293](https://github.com/anddea/revanced-patches/commit/fc4e293afdfef4139ca8a1667c274efd7164ced1))
* **YouTube - Seekbar components:** Add `Disable seekbar chapters` setting ([a297e58](https://github.com/anddea/revanced-patches/commit/a297e58896c6d1bd968d5a33e993c3395321a908))
* **YouTube - Shorts Component:** Add `Disable Like button animation` setting ([f9709d3](https://github.com/anddea/revanced-patches/commit/f9709d3b5661abfe73b0ebc1273e8b521aa695be))
* **YouTube - Shorts components:** Add `Height percentage of empty space` setting ([b214aa5](https://github.com/anddea/revanced-patches/commit/b214aa5a8eed5a5929806f1ddbbd71bfc30effe6))
* **YouTube - Shorts components:** Add `Hide in channel` setting (Hide the Shorts shelf on the channel home tab) ([428616e](https://github.com/anddea/revanced-patches/commit/428616e2f1426e8128afab8f2b684e35cea3f1cb))
* **YouTube - Shorts components:** Add `Hide stickers` setting ([49cf0fd](https://github.com/anddea/revanced-patches/commit/49cf0fd5c1bf1289944b0e5e78444ae9c23ced60))
* **YouTube - Spoof app version:** Add target version `19.13.37 - Restores old style Rolling number animations` ([9d568e0](https://github.com/anddea/revanced-patches/commit/9d568e040bfe7c052e315179793c2f413bed08a7))
* **YouTube - Spoof app version:** Show the dialog when the app is first installed ([9fe964a](https://github.com/anddea/revanced-patches/commit/9fe964a5484b5e9b69b82a9d1128d6f43ed0052f))
* **YouTube - Swipe controls:** Add `Swipe sensitivity` settings ([ef594d0](https://github.com/anddea/revanced-patches/commit/ef594d09621e26a0b4824b7236d8915e0fedf5cf))
* **YouTube - Toolbar components:** Add `Hide YouTube Doodles` setting ([00c6730](https://github.com/anddea/revanced-patches/commit/00c6730ed977274a70bf8cdd75b3d12af6b9f485))
* **YouTube - Visual preferences icons:** Add `RVX Letters` and `RVX Letters Bold` icons for RVX setting ([de30230](https://github.com/anddea/revanced-patches/commit/de30230265b90de1c6ff93c3ec80155381c26ad0))
* **YouTube - Visual preferences icons:** Add `YT Alt` icon for RVX setting ([8bd264e](https://github.com/anddea/revanced-patches/commit/8bd264e783a982fcc6e6270e579b98f033e58c77))
* **YouTube & YouTube Music - GmsCore support:** Add patch option `Disable GmsService Broker` ([b24a63c](https://github.com/anddea/revanced-patches/commit/b24a63c54d946150db3b8181102034d48ed36187))
* **YouTube & YouTube Music - Hide settings menu:** Add switch preferences for each setting entry ([3c9e660](https://github.com/anddea/revanced-patches/commit/3c9e66083aebe89560ded83b36191cf0d73ffc09))
* **YouTube & YouTube Music - Return YouTube Dislike:** Add `Show estimated likes` setting ([00793d8](https://github.com/anddea/revanced-patches/commit/00793d87e1082d0adbf40f38d5cbd7057c68d6c7))
* **YouTube & YouTube Music - Return YouTube Username:** Add `Display format` setting ([aa617ea](https://github.com/anddea/revanced-patches/commit/aa617eaf1beddbabc9239e067027fa76e58378b3))
* **YouTube & YouTube Music:** Add `Return YouTube Username` patch ([3dbe9c4](https://github.com/anddea/revanced-patches/commit/3dbe9c49c984371bf89e8fc13a5acafa0ef83ce2))
* **YouTube Music - Navigation bar components:** Do not use hardcoded color, `Enable black navigation bar` setting is turned off ([2ba1bd5](https://github.com/anddea/revanced-patches/commit/2ba1bd5ee917f8145e207b9194daaa8acfaa839c))
* **YouTube Music - Settings:** Add `Open default app settings` setting ([71b11cf](https://github.com/anddea/revanced-patches/commit/71b11cf0c43439b130cb22331252c5940d9229cb))
* **YouTube Music - Spoof app version:** Add target version `7.16.53` ([f8bf95c](https://github.com/anddea/revanced-patches/commit/f8bf95cc61d79da1241ef76443d5580f347a60f4))
* **YouTube Music - Spoof app version:** App crashes when first installed ([1f36b4e](https://github.com/anddea/revanced-patches/commit/1f36b4e843510e74e52c404beeae193cd314db50))
* **YouTube:** Add `Hide shortcuts` patch ([2435c4f](https://github.com/anddea/revanced-patches/commit/2435c4f781596d0b370e8ca244a30dbdc793c696))
* **YouTube:** Add `Hook YouTube Music actions` patch ([72b61d1](https://github.com/anddea/revanced-patches/commit/72b61d1bc2bac38e38e9ae70a1e7b8a6d0fc03ee))


### Performance Improvements

* **YouTube - GmsCore support:** Improve performance by using hashsets ([1094160](https://github.com/anddea/revanced-patches/commit/10941604fe812b9fa9c965a183426745ed8519f1))

# [2.230.0-dev.6](https://github.com/anddea/revanced-patches/compare/v2.230.0-dev.5...v2.230.0-dev.6) (2024-10-24)


### Bug Fixes

* **YouTube - Hook YouTube Music actions:** App crashes when first installed ([aee5a18](https://github.com/anddea/revanced-patches/commit/aee5a18b837c2f0d139a4a6f312d9df7f1b6de04))
* **YouTube - SponsorBlock:** The new segment window never showed up with Bold icons selected due to the missing drag handle ([c736841](https://github.com/anddea/revanced-patches/commit/c736841774e3b527871d6afe4b6d311a2bc2e573))
* **YouTube - Spoof streaming data:** Wrong register used ([ef1908f](https://github.com/anddea/revanced-patches/commit/ef1908ffd3554d787eb4dcc50995e2888a5f3fad))
* **YouTube & YouTube Music - GmsCore support:** Unimplemented service in GmsCore causes memory leak ([cca866b](https://github.com/anddea/revanced-patches/commit/cca866b218749dd55c52b0616c1d015135a44511))
* **YouTube Music - Custom branding icon:** Patch fails on certain versions ([1b8654a](https://github.com/anddea/revanced-patches/commit/1b8654a9779e0c1029da4b25430d6f0ef946f5e4))


### Features

* **YouTube - Hide comments components:** Add `Hide highlighted search links` setting ([39ae3b0](https://github.com/anddea/revanced-patches/commit/39ae3b0072b1e2a8716777d146ba0b45ec555c3e))
* **YouTube - Hide feed components:** Remove `Hide UPCOMING video` setting ([55a6953](https://github.com/anddea/revanced-patches/commit/55a695395bff0903d34ca45593d3ff97609fe264))
* **YouTube - Player components:** Add `Sanitize video subtitle` setting ([fc4e293](https://github.com/anddea/revanced-patches/commit/fc4e293afdfef4139ca8a1667c274efd7164ced1))
* **YouTube - Shorts components:** Add `Hide in channel` setting (Hide the Shorts shelf on the channel home tab) ([428616e](https://github.com/anddea/revanced-patches/commit/428616e2f1426e8128afab8f2b684e35cea3f1cb))
* **YouTube - Spoof app version:** Show the dialog when the app is first installed ([9fe964a](https://github.com/anddea/revanced-patches/commit/9fe964a5484b5e9b69b82a9d1128d6f43ed0052f))
* **YouTube - Swipe controls:** Add `Swipe sensitivity` settings ([ef594d0](https://github.com/anddea/revanced-patches/commit/ef594d09621e26a0b4824b7236d8915e0fedf5cf))
* **YouTube & YouTube Music - GmsCore support:** Add patch option `Disable GmsService Broker` ([b24a63c](https://github.com/anddea/revanced-patches/commit/b24a63c54d946150db3b8181102034d48ed36187))
* **YouTube & YouTube Music - Return YouTube Username:** Add `Display format` setting ([aa617ea](https://github.com/anddea/revanced-patches/commit/aa617eaf1beddbabc9239e067027fa76e58378b3))
* **YouTube Music - Navigation bar components:** Do not use hardcoded color, `Enable black navigation bar` setting is turned off ([2ba1bd5](https://github.com/anddea/revanced-patches/commit/2ba1bd5ee917f8145e207b9194daaa8acfaa839c))
* **YouTube Music - Spoof app version:** App crashes when first installed ([1f36b4e](https://github.com/anddea/revanced-patches/commit/1f36b4e843510e74e52c404beeae193cd314db50))


### Performance Improvements

* **YouTube - GmsCore support:** Improve performance by using hashsets ([1094160](https://github.com/anddea/revanced-patches/commit/10941604fe812b9fa9c965a183426745ed8519f1))

# [2.230.0-dev.5](https://github.com/anddea/revanced-patches/compare/v2.230.0-dev.4...v2.230.0-dev.5) (2024-10-16)


### Bug Fixes

* **YouTube - Custom Shorts action buttons:** Low icon quality for `Cairo` option on YouTube 18.29.38 ([819189e](https://github.com/anddea/revanced-patches/commit/819189ed9e66e039c540a78c81c84f9b6bf96682))
* **YouTube Music - Player components:** `Hide Audio / Video toggle` setting not working in landscape mode ([e171e45](https://github.com/anddea/revanced-patches/commit/e171e452b1bc936079373afcaedeeb7f30394dec))
* **YouTube Music - Player components:** Patches do not apply properly in certain versions ([db80b1d](https://github.com/anddea/revanced-patches/commit/db80b1d0dd30d0f19323541c9018ff5ab62dae2c))
* **YouTube Music - Spoof app version:** ListPreference sometimes selects wrong values ([ca3dba2](https://github.com/anddea/revanced-patches/commit/ca3dba27402448c8a89c3fc3fabb080f8315c303))


### Features

* **YouTube - Hide player flyout menu:** Add `Hide 1080p Premium menu` setting ([3231f36](https://github.com/anddea/revanced-patches/commit/3231f3614da9a49ae3bdead139849992d6e85c95))
* **YouTube - Seekbar components:** Add `Disable seekbar chapters` setting ([a297e58](https://github.com/anddea/revanced-patches/commit/a297e58896c6d1bd968d5a33e993c3395321a908))
* **YouTube - Spoof app version:** Add target version `19.13.37 - Restores old style Rolling number animations` ([9d568e0](https://github.com/anddea/revanced-patches/commit/9d568e040bfe7c052e315179793c2f413bed08a7))
* **YouTube - Toolbar components:** Add `Hide YouTube Doodles` setting ([00c6730](https://github.com/anddea/revanced-patches/commit/00c6730ed977274a70bf8cdd75b3d12af6b9f485))
* **YouTube & YouTube Music - Hide settings menu:** Add switch preferences for each setting entry ([3c9e660](https://github.com/anddea/revanced-patches/commit/3c9e66083aebe89560ded83b36191cf0d73ffc09))
* **YouTube & YouTube Music - Return YouTube Dislike:** Add `Show estimated likes` setting ([00793d8](https://github.com/anddea/revanced-patches/commit/00793d87e1082d0adbf40f38d5cbd7057c68d6c7))
* **YouTube & YouTube Music:** Add `Return YouTube Username` patch ([3dbe9c4](https://github.com/anddea/revanced-patches/commit/3dbe9c49c984371bf89e8fc13a5acafa0ef83ce2))

# [2.230.0-dev.4](https://github.com/anddea/revanced-patches/compare/v2.230.0-dev.3...v2.230.0-dev.4) (2024-10-04)


### Bug Fixes

* **YouTube - Hide feed components:** `Hide Latest videos button` setting does not support tablets ([85397f8](https://github.com/anddea/revanced-patches/commit/85397f85d14e86c598f5df95d79cf8de22dfa891))
* **YouTube - Shorts components:** `Hide paused header` setting does not work ([6df82cf](https://github.com/anddea/revanced-patches/commit/6df82cfd72951867babd4e985f51098ff773f310))


### Features

* **YouTube - Visual preferences icons:** Add `RVX Letters` and `RVX Letters Bold` icons for RVX setting ([de30230](https://github.com/anddea/revanced-patches/commit/de30230265b90de1c6ff93c3ec80155381c26ad0))
* **YouTube Music - Spoof app version:** Add target version `7.16.53` ([f8bf95c](https://github.com/anddea/revanced-patches/commit/f8bf95cc61d79da1241ef76443d5580f347a60f4))

# [2.230.0-dev.3](https://github.com/anddea/revanced-patches/compare/v2.230.0-dev.2...v2.230.0-dev.3) (2024-10-02)


### Features

* **YouTube - Visual preferences icons:** Add `YT Alt` icon for RVX setting ([8bd264e](https://github.com/anddea/revanced-patches/commit/8bd264e783a982fcc6e6270e579b98f033e58c77))

# [2.230.0-dev.2](https://github.com/anddea/revanced-patches/compare/v2.230.0-dev.1...v2.230.0-dev.2) (2024-10-01)


### Bug Fixes

* **YouTube - Custom branding icon:** Patch failed due to animation files when AFN is selected ([231a468](https://github.com/anddea/revanced-patches/commit/231a4686be7fe5b87690d9cf8c88fae5245971ef))

# [2.230.0-dev.1](https://github.com/anddea/revanced-patches/compare/v2.229.0...v2.230.0-dev.1) (2024-09-30)


### Bug Fixes

* **YouTube - Hide feed components:** `Hide carousel shelf` setting sometimes hides the library shelf ([8c3a5d2](https://github.com/anddea/revanced-patches/commit/8c3a5d2fd08cdca5cf66324cc2a44b722e7062cc))
* **YouTube - Player components:** The `Disable player popup panels` setting disables the engagement panel in Mix playlists on certain YouTube versions ([63c463c](https://github.com/anddea/revanced-patches/commit/63c463ca2bebd43c85e79609275f72bf4176f242))
* **YouTube - Settings:** `Search bar in settings` can't find `RYD` and `SponsorBlock` settings ([ab222a6](https://github.com/anddea/revanced-patches/commit/ab222a672317ec7d00d566b1ca655b7b5c9a78cd))
* **YouTube Music - Disable Cairo splash animation:** Some versions are recognized as unpatchable even though they can be patched ([1e74ff9](https://github.com/anddea/revanced-patches/commit/1e74ff94811097370e4c0f0d05dc429601e8f05c))
* **YouTube Music - Player components:** `Remember shuffle state` setting does not remember the correct state ([18482b2](https://github.com/anddea/revanced-patches/commit/18482b2e6b57d2eea95527a05e9d86674446c052))
* **YouTube Music - SponsorBlock:**   SponsorBlock does not skip segments at the beginning of a video when in the background ([e895e6f](https://github.com/anddea/revanced-patches/commit/e895e6f5c638df5e2233cc50dc82de9ccfe89df0))


### Features

* **Custom branding icon:** Add `MMT Orange`, `MMT Pink`, `MMT Turquoise` ([e203ad1](https://github.com/anddea/revanced-patches/commit/e203ad159e8717a2042a978e1c6cee04fa8ce745))
* **YouTube - Change start page:** Add `Change start page type` setting ([251d5d7](https://github.com/anddea/revanced-patches/commit/251d5d726061aff08ebe190513e10862db7c4856))
* **YouTube - Custom Shorts action buttons:**  Add `Cairo` ([f3b6717](https://github.com/anddea/revanced-patches/commit/f3b67174da909ab07d4b04bef14d5d7ae672cc3b))
* **YouTube - Hide feed components:** Add `Hide related videos` setting ([a82c9ba](https://github.com/anddea/revanced-patches/commit/a82c9bac56e875417e8d0632d5e4cb14a3e4c4aa))
* **YouTube - Hide feed components:** Add `Hide UPCOMING video` setting ([ec9d641](https://github.com/anddea/revanced-patches/commit/ec9d6419df2ed3bd13c4171a92216314202802a1))
* **YouTube - Navigation bar components:** Add `Hide navigation bar` setting ([5936015](https://github.com/anddea/revanced-patches/commit/593601526bd10a6976d108d1d8689c44cf6331e9))
* **YouTube - Shorts Component:** Add `Disable Like button animation` setting ([f9709d3](https://github.com/anddea/revanced-patches/commit/f9709d3b5661abfe73b0ebc1273e8b521aa695be))
* **YouTube - Shorts components:** Add `Height percentage of empty space` setting ([b214aa5](https://github.com/anddea/revanced-patches/commit/b214aa5a8eed5a5929806f1ddbbd71bfc30effe6))
* **YouTube - Shorts components:** Add `Hide stickers` setting ([49cf0fd](https://github.com/anddea/revanced-patches/commit/49cf0fd5c1bf1289944b0e5e78444ae9c23ced60))
* **YouTube Music - Settings:** Add `Open default app settings` setting ([71b11cf](https://github.com/anddea/revanced-patches/commit/71b11cf0c43439b130cb22331252c5940d9229cb))
* **YouTube:** Add `Hide shortcuts` patch ([2435c4f](https://github.com/anddea/revanced-patches/commit/2435c4f781596d0b370e8ca244a30dbdc793c696))
* **YouTube:** Add `Hook YouTube Music actions` patch ([72b61d1](https://github.com/anddea/revanced-patches/commit/72b61d1bc2bac38e38e9ae70a1e7b8a6d0fc03ee))

# [2.229.0](https://github.com/anddea/revanced-patches/compare/v2.228.0...v2.229.0) (2024-09-30)


### Bug Fixes

* Revert bump dependencies ([d8f988b](https://github.com/anddea/revanced-patches/commit/d8f988bd955f31bd6c4874454585a32d666f942e))
* **YouTube - Disable force auto captions:** Patch doesn't work with Shorts ([a01edc8](https://github.com/anddea/revanced-patches/commit/a01edc84ef41134f5b63b9c84e36ecf20b37bd36))
* **YouTube Music - Disable auto captions:** Captions cannot be changed when `Disable forced auto captions` is turned on ([ec8d633](https://github.com/anddea/revanced-patches/commit/ec8d63331c5701979f89be90cf5cdfb746e02905))
* **YouTube Music - GmsCore support:** Can't share Stories to Facebook, Instagram and Snapchat ([01ec72a](https://github.com/anddea/revanced-patches/commit/01ec72a993391d31be06783d2a11a787412dc245))
* **YouTube Music - SponsorBlock:** SponsorBlock segments at the end of a song cause the player to get stuck ([d986a01](https://github.com/anddea/revanced-patches/commit/d986a0166eb925ec334afdf68dee706e8a3337f6))
* **YouTube Music:** Patch failed because of some missing strings ([29a3874](https://github.com/anddea/revanced-patches/commit/29a38742cd8a72357c2a4f2de42e97effbd11e23))


### Features

* **Hide ads:** Remove `Close fullscreen ads` setting ([5dc3f7a](https://github.com/anddea/revanced-patches/commit/5dc3f7a4a8d80140fcef4d2a89b8ae101e3441b7))
* **YouTube - Custom branding icon:** New themed icon for `Xisr Yellow` ([#831](https://github.com/anddea/revanced-patches/issues/831)) ([ac08786](https://github.com/anddea/revanced-patches/commit/ac08786e3248f8d8a994b3941237e515396fd578))
* **YouTube - Hide ads:** Add `Hide promotion alert banner` setting ([2350d94](https://github.com/anddea/revanced-patches/commit/2350d94136b22403b7aecc6aa91d6db687bb7d4b))
* **YouTube - Hide feed components:** Add `Hide expandable shelves` setting ([482d48d](https://github.com/anddea/revanced-patches/commit/482d48d6a495302c14be0926e753548901bd0358))
* **YouTube - Hide feed components:** Selectively hide video by views for Home / Subscription / Search ([c842248](https://github.com/anddea/revanced-patches/commit/c842248e074399350ea73c1067bf1d5bc1f6da42))
* **YouTube - Hide player flyout menu:** Restore `Hide Ambient mode menu` setting ([d99bdef](https://github.com/anddea/revanced-patches/commit/d99bdefeb4ef477c5277b00d2c64497858604b68))
* **YouTube - Overlay buttons:** Add an option to select a different downloader on long press ([6f47b80](https://github.com/anddea/revanced-patches/commit/6f47b80d89abf9ce4b786a9eb7fa3f8db7257edc))
* **YouTube - Player components:** Add `Disable switch mix playlists` setting ([5d8650f](https://github.com/anddea/revanced-patches/commit/5d8650f14d8935ecaf670689e4c8f24042022dc1))
* **YouTube - Seekbar components:** Add patch options to set Cairo seekbar colors ([b956855](https://github.com/anddea/revanced-patches/commit/b956855183f3f6c85a41e0d3812da6ff71ec8157))
* **YouTube - SponsorBlock:** Add patch option to select new segment window alignment ([28fc1d5](https://github.com/anddea/revanced-patches/commit/28fc1d5527181a123129439a3384335a28c98346))
* **YouTube - SponsorBlock:** Make new segment window draggable ([fd5b5e6](https://github.com/anddea/revanced-patches/commit/fd5b5e68a3dde895baa212e805293f0e3f57a1b6))
* **YouTube - Video playback:** Add `Disable playback speed for music` setting ([b77e952](https://github.com/anddea/revanced-patches/commit/b77e9524ef4d8ab6d573e77c516a596ad5ac4652))
* **YouTube - Video playback:** Add `Disable VP9 codec` setting ([559236b](https://github.com/anddea/revanced-patches/commit/559236b9681b24c22901c4a7b1d8df7c2e48de7e))
* **YouTube Music - Custom branding icon:** Add patch option `RestoreOldSplashIcon` ([e272302](https://github.com/anddea/revanced-patches/commit/e27230246fdf82a35fe035f57a3cc02c16b75664))
* **YouTube Music - Custom branding icon:** Update monochrome icon for `afn_red` & `afn_blue` ([88c4da1](https://github.com/anddea/revanced-patches/commit/88c4da1306a99d919ad3b092de937a8800d8c317))
* **YouTube Music - Hide ads:** Add `Hide promotion alert banner` setting ([ca6263c](https://github.com/anddea/revanced-patches/commit/ca6263c18f823507c55d77e15a1aeb268ab90352))
* **YouTube Music:** Add support version `6.20.51` ([6d89cb0](https://github.com/anddea/revanced-patches/commit/6d89cb09228a5dd30bdc00caa040361c84a48032))
* **YouTube Music:** Add support versions `7.16.53` ~ `7.17.51` ([390cabe](https://github.com/anddea/revanced-patches/commit/390cabeff02871b89cd3440de148a82008235f08))
* **YouTube Music:** Drop support version `7.17.51` ([f49cd50](https://github.com/anddea/revanced-patches/commit/f49cd5068b6bf6827353d5c11c9b1e05eaf5f776))
* **YouTube Music:** Rename `Enable Cairo splash animation` to `Disable Cairo splash animation` ([37373c9](https://github.com/anddea/revanced-patches/commit/37373c95f571545e220f125ea8645ade3459e045))

# [2.229.0-dev.7](https://github.com/anddea/revanced-patches/compare/v2.229.0-dev.6...v2.229.0-dev.7) (2024-09-19)


### Features

* **YouTube - SponsorBlock:** Add patch option to select new segment window alignment ([28fc1d5](https://github.com/anddea/revanced-patches/commit/28fc1d5527181a123129439a3384335a28c98346))

# [2.229.0-dev.6](https://github.com/anddea/revanced-patches/compare/v2.229.0-dev.5...v2.229.0-dev.6) (2024-09-17)


### Features

* **YouTube - Custom branding icon:** New themed icon for `Xisr Yellow` ([#831](https://github.com/anddea/revanced-patches/issues/831)) ([ac08786](https://github.com/anddea/revanced-patches/commit/ac08786e3248f8d8a994b3941237e515396fd578))
* **YouTube - Seekbar components:** Add patch options to set Cairo seekbar colors ([b956855](https://github.com/anddea/revanced-patches/commit/b956855183f3f6c85a41e0d3812da6ff71ec8157))
* **YouTube - SponsorBlock:** Make new segment window draggable ([fd5b5e6](https://github.com/anddea/revanced-patches/commit/fd5b5e68a3dde895baa212e805293f0e3f57a1b6))

# [2.229.0-dev.5](https://github.com/anddea/revanced-patches/compare/v2.229.0-dev.4...v2.229.0-dev.5) (2024-09-16)


### Bug Fixes

* **YouTube Music - Disable auto captions:** Captions cannot be changed when `Disable forced auto captions` is turned on ([ec8d633](https://github.com/anddea/revanced-patches/commit/ec8d63331c5701979f89be90cf5cdfb746e02905))
* **YouTube Music - GmsCore support:** Can't share Stories to Facebook, Instagram and Snapchat ([01ec72a](https://github.com/anddea/revanced-patches/commit/01ec72a993391d31be06783d2a11a787412dc245))


### Features

* **Hide ads:** Remove `Close fullscreen ads` setting ([5dc3f7a](https://github.com/anddea/revanced-patches/commit/5dc3f7a4a8d80140fcef4d2a89b8ae101e3441b7))
* **YouTube - Hide feed components:** Add `Hide expandable shelves` setting ([482d48d](https://github.com/anddea/revanced-patches/commit/482d48d6a495302c14be0926e753548901bd0358))
* **YouTube - Hide feed components:** Selectively hide video by views for Home / Subscription / Search ([c842248](https://github.com/anddea/revanced-patches/commit/c842248e074399350ea73c1067bf1d5bc1f6da42))
* **YouTube - Video playback:** Add `Disable VP9 codec` setting ([559236b](https://github.com/anddea/revanced-patches/commit/559236b9681b24c22901c4a7b1d8df7c2e48de7e))
* **YouTube Music - Custom branding icon:** Update monochrome icon for `afn_red` & `afn_blue` ([88c4da1](https://github.com/anddea/revanced-patches/commit/88c4da1306a99d919ad3b092de937a8800d8c317))
* **YouTube Music:** Add support version `6.20.51` ([6d89cb0](https://github.com/anddea/revanced-patches/commit/6d89cb09228a5dd30bdc00caa040361c84a48032))
* **YouTube Music:** Drop support version `7.17.51` ([f49cd50](https://github.com/anddea/revanced-patches/commit/f49cd5068b6bf6827353d5c11c9b1e05eaf5f776))

# [2.229.0-dev.4](https://github.com/anddea/revanced-patches/compare/v2.229.0-dev.3...v2.229.0-dev.4) (2024-09-09)


### Features

* **YouTube - Overlay buttons:** Add an option to select a different downloader on long press ([6f47b80](https://github.com/anddea/revanced-patches/commit/6f47b80d89abf9ce4b786a9eb7fa3f8db7257edc))
* **YouTube Music - Custom branding icon:** Add patch option `RestoreOldSplashIcon` ([e272302](https://github.com/anddea/revanced-patches/commit/e27230246fdf82a35fe035f57a3cc02c16b75664))
* **YouTube Music:** Rename `Enable Cairo splash animation` to `Disable Cairo splash animation` ([37373c9](https://github.com/anddea/revanced-patches/commit/37373c95f571545e220f125ea8645ade3459e045))

# [2.229.0-dev.3](https://github.com/anddea/revanced-patches/compare/v2.229.0-dev.2...v2.229.0-dev.3) (2024-09-06)


### Bug Fixes

* **YouTube Music:** Patch failed because of some missing strings ([29a3874](https://github.com/anddea/revanced-patches/commit/29a38742cd8a72357c2a4f2de42e97effbd11e23))

# [2.229.0-dev.2](https://github.com/anddea/revanced-patches/compare/v2.229.0-dev.1...v2.229.0-dev.2) (2024-09-06)


### Bug Fixes

* Revert bump dependencies ([d8f988b](https://github.com/anddea/revanced-patches/commit/d8f988bd955f31bd6c4874454585a32d666f942e))

# [2.229.0-dev.1](https://github.com/anddea/revanced-patches/compare/v2.228.0...v2.229.0-dev.1) (2024-09-06)


### Bug Fixes

* **YouTube - Disable force auto captions:** Patch doesn't work with Shorts ([a01edc8](https://github.com/anddea/revanced-patches/commit/a01edc84ef41134f5b63b9c84e36ecf20b37bd36))
* **YouTube Music - SponsorBlock:** SponsorBlock segments at the end of a song cause the player to get stuck ([d986a01](https://github.com/anddea/revanced-patches/commit/d986a0166eb925ec334afdf68dee706e8a3337f6))


### Features

* **YouTube - Hide ads:** Add `Hide promotion alert banner` setting ([2350d94](https://github.com/anddea/revanced-patches/commit/2350d94136b22403b7aecc6aa91d6db687bb7d4b))
* **YouTube - Hide player flyout menu:** Restore `Hide Ambient mode menu` setting ([d99bdef](https://github.com/anddea/revanced-patches/commit/d99bdefeb4ef477c5277b00d2c64497858604b68))
* **YouTube - Player components:** Add `Disable switch mix playlists` setting ([5d8650f](https://github.com/anddea/revanced-patches/commit/5d8650f14d8935ecaf670689e4c8f24042022dc1))
* **YouTube - Video playback:** Add `Disable playback speed for music` setting ([b77e952](https://github.com/anddea/revanced-patches/commit/b77e9524ef4d8ab6d573e77c516a596ad5ac4652))
* **YouTube Music - Hide ads:** Add `Hide promotion alert banner` setting ([ca6263c](https://github.com/anddea/revanced-patches/commit/ca6263c18f823507c55d77e15a1aeb268ab90352))
* **YouTube Music:** Add support versions `7.16.53` ~ `7.17.51` ([390cabe](https://github.com/anddea/revanced-patches/commit/390cabeff02871b89cd3440de148a82008235f08))

# [2.228.0](https://github.com/anddea/revanced-patches/compare/v2.227.0...v2.228.0) (2024-09-05)


### Bug Fixes

* **YouTube - Client spoof:** Some side effects of iOS client ([823711e](https://github.com/anddea/revanced-patches/commit/823711e84bc95b88faa620690ee0fb0960b93808))
* **YouTube - Custom branding icon:** Patch failed for some icons ([97bc461](https://github.com/anddea/revanced-patches/commit/97bc461be57e5ed4f37dd56b3df19cfb933b4d39))
* **YouTube - Custom branding:** Fix patch error regarding `Xisr Yellow` ([dae0a79](https://github.com/anddea/revanced-patches/commit/dae0a793acd2c50e8bda2c8809d3e377101d8c41))
* **YouTube - Custom Branding:** Fixed broken splash animation for `Xisr Yellow` ([#758](https://github.com/anddea/revanced-patches/issues/758)) ([b5ea4b1](https://github.com/anddea/revanced-patches/commit/b5ea4b1f41a43d8ab473bea0212d7f42564dd5b5))
* **YouTube - Disable auto captions:** Turning on `Disable forced auto captions` will disable subtitles ([f4dc6d3](https://github.com/anddea/revanced-patches/commit/f4dc6d39b75a61ae954cc7657551535499826f5f))
* **YouTube - Hide feed components:** Sometimes `Hide carousel shelf` setting doesn't work ([f53ce46](https://github.com/anddea/revanced-patches/commit/f53ce46c1461ac8699af767f03f92a20e7c1ed65))
* **YouTube - Hook download actions:** `Override playlist download button` setting does not work in `Download playlist` menu of flyout panel ([2c4d8c3](https://github.com/anddea/revanced-patches/commit/2c4d8c315cae4339331398f708362c4c4e8343fa))
* **YouTube - Hook download actions:** Video download button was not responding ([fa53ea5](https://github.com/anddea/revanced-patches/commit/fa53ea58b6011eb0627d48f6f8bc85d9092b26f3))
* **YouTube - Overlay buttons:** `Always repeat` button doesn't work when the video is minimized ([e03741f](https://github.com/anddea/revanced-patches/commit/e03741f0ac8073186f993a76495ccc86bde1a092))
* **YouTube - Overlay buttons:** Removed unused `Experimental flags` section ([d96ee79](https://github.com/anddea/revanced-patches/commit/d96ee7945b5f97b3e3d4fe94ff5a32f0ec75c8c7))
* **YouTube - Settings:** Update default values for "Hide low views video", "Spoof streaming data" and BottomMargin of "Overlay buttons" ([9bb8ceb](https://github.com/anddea/revanced-patches/commit/9bb8ceb65d4b43e707e7aec8d37b92190df3cbbf))
* **YouTube - SponsorBlock:** Improve create segment manual seek accuracy ([f0cefa8](https://github.com/anddea/revanced-patches/commit/f0cefa85a4288bda0e3f7324629f1c2a0bcc7839))
* **YouTube - Spoof client:** Change default values ([b60c09a](https://github.com/anddea/revanced-patches/commit/b60c09afb2f7d6faa899cc0dee8e063989d25a90))
* **YouTube - Spoof client:** Fix background playback issue with livestream on iOS clients ([0ef9a65](https://github.com/anddea/revanced-patches/commit/0ef9a655ce4a5f4ba5cd72ff3138b7e7d03b2613))
* **YouTube - Spoof client:** Partial fix for watch history issue of brand accounts on iOS clients ([2294ffb](https://github.com/anddea/revanced-patches/commit/2294ffb1640661e4421887eef8b3bd6ddff49fc1))
* **YouTube - Spoof streaming data:** App crashes when loading ads in Shorts ([e3e6a31](https://github.com/anddea/revanced-patches/commit/e3e6a311299d9496e95818a69e7b4867f7da07d9))
* **YouTube - Spoof streaming data:** Display spoofing side effects option ([7fcced7](https://github.com/anddea/revanced-patches/commit/7fcced78cbe85caa898b94b27b5ad151c5ff6e26))
* **YouTube - Toolbar components:** Turning on the `Hide voice search button` setting makes the margin of the searchbar 0 ([ca02dba](https://github.com/anddea/revanced-patches/commit/ca02dba30710decc4ddea07318e72a9966124665))
* **YouTube - Translations:** Fix `Italian` translations ([#796](https://github.com/anddea/revanced-patches/issues/796)) ([66f0d1b](https://github.com/anddea/revanced-patches/commit/66f0d1bfd839c0f597e577856bc9e2537ae2e9a3))
* **YouTube - Translations:** Update Italian translation ([936c740](https://github.com/anddea/revanced-patches/commit/936c74067d896cc779fc15eec61116eb20862f09))
* **YouTube - Video information:** App crash when casting ([ea08ad8](https://github.com/anddea/revanced-patches/commit/ea08ad8845a6b32b1a47995ed607e952c30630a5))
* **YouTube - Video playback:** Update the option to disable toasts when changing the default values ([4d69bbb](https://github.com/anddea/revanced-patches/commit/4d69bbb6cced058650f7917bb1c74db3be90f2fa))
* **YouTube Music - GmsCore support:** `Open GmsCore` setting is not added if user only includes `GmsCore support` patch ([697ef75](https://github.com/anddea/revanced-patches/commit/697ef758c940c7d6e6fc2c3fe0ced98234807e77))
* **YouTube Music - GmsCore support:** YT Music crashes when using the `Open GmsCore` setting ([ea869f4](https://github.com/anddea/revanced-patches/commit/ea869f477841aa74bcab62d73674ecea7ae11519))
* **YouTube Music - Sanitize sharing links:** Tracking parameters are not removed from the system share panel ([5774c2d](https://github.com/anddea/revanced-patches/commit/5774c2d3d98781a6ba871f39383ee56f82eb3fbe))
* **YouTube:** `Overlay buttons` patch fails ([09c8cc5](https://github.com/anddea/revanced-patches/commit/09c8cc5e3d2f315ce1d5182341ea7ff34633495d))


### Features

* **Custom Branding Icons:** Change default icon to `Revancify Yellow` ([#752](https://github.com/anddea/revanced-patches/issues/752)) ([60163db](https://github.com/anddea/revanced-patches/commit/60163dbde9d90035763ed66827515485393e6b4d))
* Merge RVX v4.12.1-dev.1 ([#791](https://github.com/anddea/revanced-patches/issues/791)) ([de1ddcf](https://github.com/anddea/revanced-patches/commit/de1ddcfe050660c044f68e2610590d94eff45b10))
* Merge v4.13.1-dev.3 ([59f0424](https://github.com/anddea/revanced-patches/commit/59f0424e8b43833e4728b570b88c2f1aef1d8926))
* **YouTube - Default Quality & Speed:** Add an option to disable toasts when changing the default values ([cc95f31](https://github.com/anddea/revanced-patches/commit/cc95f316485fe1759621b37471980c0ffe14d7cd))
* **YouTube - Description components:** Add `Hide Contents section` setting ([e8358ff](https://github.com/anddea/revanced-patches/commit/e8358ffc680693e2923e3c03933b9fcfb0874eb7))
* **YouTube - Download Playlist Button:** Add playlist download button ([#761](https://github.com/anddea/revanced-patches/issues/761)) ([449d45a](https://github.com/anddea/revanced-patches/commit/449d45aaff4be67937a89c177f3e6ee8b8e5d489))
* **YouTube - Hide feed components:** Add syntax to match whole keywords and not substrings ([8ac266d](https://github.com/anddea/revanced-patches/commit/8ac266dd7679bd69af8f95faf816c6bdcc8b2e71))
* **YouTube - Hide player flyout menu:** Remove `Hide Ambient mode menu` setting ([62f94f5](https://github.com/anddea/revanced-patches/commit/62f94f50b03dd1dc3b99116f31089c91b65c23e9))
* **YouTube - Hook download actions:** Add package field in `Hook download actions` patch ([d32838a](https://github.com/anddea/revanced-patches/commit/d32838acf3f36bd01e5745516c993d9c69014b87))
* **YouTube - Overlay buttons:** Add `Collapse` button and update thin-type overlay button icons ([ac90242](https://github.com/anddea/revanced-patches/commit/ac902425787b1d5786f4b6034ca1185e4074d0ba))
* **YouTube - Overlay buttons:** Add patch option `Change top buttons` ([504a1fe](https://github.com/anddea/revanced-patches/commit/504a1fe46ae97b7b328c4245941c69be698bb4f3))
* **YouTube - Player components:** Add `Hide zoom overlay` setting ([00640e6](https://github.com/anddea/revanced-patches/commit/00640e661753802439dfba65827307e7e703fb14))
* **YouTube - Settings:** Show categories while searching settings ([177b016](https://github.com/anddea/revanced-patches/commit/177b016c5918ebe3778135c6eab235e7841ebb65))
* **YouTube - Shorts components:** Add `Hide Use this sound button` setting ([aec5326](https://github.com/anddea/revanced-patches/commit/aec5326712262a8a16545a0ef1621a7c77eaa243))
* **YouTube - Shorts components:** Add settings - `Hide floating button`, `Hide Trends button`, `Hide Use template button` ([b9f0bd8](https://github.com/anddea/revanced-patches/commit/b9f0bd8ee2b714f9b3219edcff2b7abc50efb7af))
* **YouTube - Shorts components:** Clarify the description of some settings ([53b690c](https://github.com/anddea/revanced-patches/commit/53b690ca1ea02106e273ccd616dd163f8a2189e6))
* **YouTube - Shorts components:** Move `Change Shorts repeat state` setting to `Experimental Flags` ([112060a](https://github.com/anddea/revanced-patches/commit/112060a75316b293671f1cec07730d7a05ad3a33))
* **YouTube - Spoof client:** Allow forcing AVC codec with iOS ([beb94d1](https://github.com/anddea/revanced-patches/commit/beb94d1d5cecf039c2ede7cdf2c8199a5739b0b9))
* **YouTube & Music - Custom branding icon:** Rename `Revancify Yellow` To `Xisr Yellow` ([#756](https://github.com/anddea/revanced-patches/issues/756)) ([26d7deb](https://github.com/anddea/revanced-patches/commit/26d7debb8d40013b65b66ac1d56f13c01755ea23))
* **YouTube & Music - Custom branding icon:** Update `Xisr Yellow` icon ([#755](https://github.com/anddea/revanced-patches/issues/755)) ([c1dfd1f](https://github.com/anddea/revanced-patches/commit/c1dfd1f20b777828f57c69bebb9d1f85190f8c7c))
* **YouTube Music - Hide layout components:** Add `Hide settings menu` setting ([78b61dc](https://github.com/anddea/revanced-patches/commit/78b61dc799c4a3106a6800b176f464b8465b27d7))
* **YouTube Music - Player components:** Add settings `Disable miniplayer gesture`, `Disable player gesture` ([0bf84f9](https://github.com/anddea/revanced-patches/commit/0bf84f9d4cf3cad63b4cb6adbac1334be966e5ed))
* **YouTube Music - Video playback:** Add an option to disable toasts when changing the default values ([a663655](https://github.com/anddea/revanced-patches/commit/a66365558a0fb489842bdd73324df798c090549b))
* **YouTube Music:** Add `Change share sheet` patch ([f4ebfff](https://github.com/anddea/revanced-patches/commit/f4ebfffa3dbd7fe5947b4d7e77ee80bbaf70a627))
* **YouTube Music:** Add support version `7.10.52` ([a842d17](https://github.com/anddea/revanced-patches/commit/a842d1743d13dac33db8ba0179f18289b4a8a742))
* **YouTube Music:** Add support version `7.11.51` ~ `7.12.51` ([b8a62ae](https://github.com/anddea/revanced-patches/commit/b8a62aeb0528f5ef513330ae5ef1790ffe4cd920))
* **YouTube Music:** Add support version `7.12.52` ~ `7.13.52` ([4670f1d](https://github.com/anddea/revanced-patches/commit/4670f1d040dcf71376b5870b4330052800fadbd7))
* **YouTube Music:** Add support versions `7.15.52` ~ `7.16.52` ([26ff170](https://github.com/anddea/revanced-patches/commit/26ff170d3acb87a2348f2b3e646bc8afc9936185))
* **YouTube:** Add `Change share sheet` patch ([af81ee7](https://github.com/anddea/revanced-patches/commit/af81ee72b1c4e5d2d86753d833e038ce99ae55de))
* **YouTube:** Add `Spoof streaming data` patch ([a792753](https://github.com/anddea/revanced-patches/commit/a792753b2bec84790ee4023c8572f712415097e2))
* **YouTube:** Add `Watch history` patch ([f1fd6e6](https://github.com/anddea/revanced-patches/commit/f1fd6e68f19d9c6ba45efc081e7ac50d42ef8529))
* **YouTube:** Remove `Spoof client` patch ([4c7538e](https://github.com/anddea/revanced-patches/commit/4c7538e921d1c6afef7e4eeb19c2ac1bae9b86d5))

# [2.228.0-dev.15](https://github.com/anddea/revanced-patches/compare/v2.228.0-dev.14...v2.228.0-dev.15) (2024-09-03)


### Features

* **YouTube - Settings:** Show categories while searching settings ([177b016](https://github.com/anddea/revanced-patches/commit/177b016c5918ebe3778135c6eab235e7841ebb65))

# [2.228.0-dev.14](https://github.com/anddea/revanced-patches/compare/v2.228.0-dev.13...v2.228.0-dev.14) (2024-09-02)


### Bug Fixes

* **YouTube - Settings:** Update default values for "Hide low views video", "Spoof streaming data" and BottomMargin of "Overlay buttons" ([9bb8ceb](https://github.com/anddea/revanced-patches/commit/9bb8ceb65d4b43e707e7aec8d37b92190df3cbbf))
* **YouTube - Spoof streaming data:** Display spoofing side effects option ([7fcced7](https://github.com/anddea/revanced-patches/commit/7fcced78cbe85caa898b94b27b5ad151c5ff6e26))

# [2.228.0-dev.13](https://github.com/anddea/revanced-patches/compare/v2.228.0-dev.12...v2.228.0-dev.13) (2024-09-01)


### Bug Fixes

* **YouTube - Spoof streaming data:** App crashes when loading ads in Shorts ([e3e6a31](https://github.com/anddea/revanced-patches/commit/e3e6a311299d9496e95818a69e7b4867f7da07d9))
* **YouTube - Video playback:** Update the option to disable toasts when changing the default values ([4d69bbb](https://github.com/anddea/revanced-patches/commit/4d69bbb6cced058650f7917bb1c74db3be90f2fa))
* **YouTube Music - GmsCore support:** `Open GmsCore` setting is not added if user only includes `GmsCore support` patch ([697ef75](https://github.com/anddea/revanced-patches/commit/697ef758c940c7d6e6fc2c3fe0ced98234807e77))
* **YouTube Music - GmsCore support:** YT Music crashes when using the `Open GmsCore` setting ([ea869f4](https://github.com/anddea/revanced-patches/commit/ea869f477841aa74bcab62d73674ecea7ae11519))


### Features

* Merge v4.13.1-dev.3 ([59f0424](https://github.com/anddea/revanced-patches/commit/59f0424e8b43833e4728b570b88c2f1aef1d8926))
* **YouTube - Hide feed components:** Add syntax to match whole keywords and not substrings ([8ac266d](https://github.com/anddea/revanced-patches/commit/8ac266dd7679bd69af8f95faf816c6bdcc8b2e71))
* **YouTube - Hide player flyout menu:** Remove `Hide Ambient mode menu` setting ([62f94f5](https://github.com/anddea/revanced-patches/commit/62f94f50b03dd1dc3b99116f31089c91b65c23e9))
* **YouTube - Overlay buttons:** Add `Collapse` button and update thin-type overlay button icons ([ac90242](https://github.com/anddea/revanced-patches/commit/ac902425787b1d5786f4b6034ca1185e4074d0ba))
* **YouTube - Shorts components:** Add `Hide Use this sound button` setting ([aec5326](https://github.com/anddea/revanced-patches/commit/aec5326712262a8a16545a0ef1621a7c77eaa243))
* **YouTube - Shorts components:** Add settings - `Hide floating button`, `Hide Trends button`, `Hide Use template button` ([b9f0bd8](https://github.com/anddea/revanced-patches/commit/b9f0bd8ee2b714f9b3219edcff2b7abc50efb7af))
* **YouTube - Shorts components:** Clarify the description of some settings ([53b690c](https://github.com/anddea/revanced-patches/commit/53b690ca1ea02106e273ccd616dd163f8a2189e6))
* **YouTube - Spoof client:** Allow forcing AVC codec with iOS ([beb94d1](https://github.com/anddea/revanced-patches/commit/beb94d1d5cecf039c2ede7cdf2c8199a5739b0b9))
* **YouTube Music - Video playback:** Add an option to disable toasts when changing the default values ([a663655](https://github.com/anddea/revanced-patches/commit/a66365558a0fb489842bdd73324df798c090549b))
* **YouTube Music:** Add support versions `7.15.52` ~ `7.16.52` ([26ff170](https://github.com/anddea/revanced-patches/commit/26ff170d3acb87a2348f2b3e646bc8afc9936185))
* **YouTube:** Add `Spoof streaming data` patch ([a792753](https://github.com/anddea/revanced-patches/commit/a792753b2bec84790ee4023c8572f712415097e2))
* **YouTube:** Remove `Spoof client` patch ([4c7538e](https://github.com/anddea/revanced-patches/commit/4c7538e921d1c6afef7e4eeb19c2ac1bae9b86d5))

# [2.228.0-dev.12](https://github.com/anddea/revanced-patches/compare/v2.228.0-dev.11...v2.228.0-dev.12) (2024-08-16)


### Bug Fixes

* **YouTube - Video information:** App crash when casting ([ea08ad8](https://github.com/anddea/revanced-patches/commit/ea08ad8845a6b32b1a47995ed607e952c30630a5))
* **YouTube Music - Sanitize sharing links:** Tracking parameters are not removed from the system share panel ([5774c2d](https://github.com/anddea/revanced-patches/commit/5774c2d3d98781a6ba871f39383ee56f82eb3fbe))


### Features

* **YouTube:** Add `Change share sheet` patch ([af81ee7](https://github.com/anddea/revanced-patches/commit/af81ee72b1c4e5d2d86753d833e038ce99ae55de))

# [2.228.0-dev.11](https://github.com/anddea/revanced-patches/compare/v2.228.0-dev.10...v2.228.0-dev.11) (2024-08-08)


### Bug Fixes

* **YouTube - Translations:** Update Italian translation ([936c740](https://github.com/anddea/revanced-patches/commit/936c74067d896cc779fc15eec61116eb20862f09))

# [2.228.0-dev.10](https://github.com/anddea/revanced-patches/compare/v2.228.0-dev.9...v2.228.0-dev.10) (2024-08-08)


### Bug Fixes

* **YouTube - Translations:** Fix `Italian` translations ([#796](https://github.com/anddea/revanced-patches/issues/796)) ([66f0d1b](https://github.com/anddea/revanced-patches/commit/66f0d1bfd839c0f597e577856bc9e2537ae2e9a3))

# [2.228.0-dev.9](https://github.com/anddea/revanced-patches/compare/v2.228.0-dev.8...v2.228.0-dev.9) (2024-08-07)


### Bug Fixes

* **YouTube - Overlay buttons:** Removed unused `Experimental flags` section ([d96ee79](https://github.com/anddea/revanced-patches/commit/d96ee7945b5f97b3e3d4fe94ff5a32f0ec75c8c7))

# [2.228.0-dev.8](https://github.com/anddea/revanced-patches/compare/v2.228.0-dev.7...v2.228.0-dev.8) (2024-08-07)


### Bug Fixes

* **YouTube - Hook download actions:** `Override playlist download button` setting does not work in `Download playlist` menu of flyout panel ([2c4d8c3](https://github.com/anddea/revanced-patches/commit/2c4d8c315cae4339331398f708362c4c4e8343fa))
* **YouTube - Hook download actions:** Video download button was not responding ([fa53ea5](https://github.com/anddea/revanced-patches/commit/fa53ea58b6011eb0627d48f6f8bc85d9092b26f3))
* **YouTube - Overlay buttons:** `Always repeat` button doesn't work when the video is minimized ([e03741f](https://github.com/anddea/revanced-patches/commit/e03741f0ac8073186f993a76495ccc86bde1a092))
* **YouTube:** `Overlay buttons` patch fails ([09c8cc5](https://github.com/anddea/revanced-patches/commit/09c8cc5e3d2f315ce1d5182341ea7ff34633495d))

# [2.228.0-dev.7](https://github.com/anddea/revanced-patches/compare/v2.228.0-dev.6...v2.228.0-dev.7) (2024-08-07)


### Bug Fixes

* **YouTube - SponsorBlock:** Improve create segment manual seek accuracy ([f0cefa8](https://github.com/anddea/revanced-patches/commit/f0cefa85a4288bda0e3f7324629f1c2a0bcc7839))
* **YouTube - Spoof client:** Change default values ([b60c09a](https://github.com/anddea/revanced-patches/commit/b60c09afb2f7d6faa899cc0dee8e063989d25a90))
* **YouTube - Spoof client:** Fix background playback issue with livestream on iOS clients ([0ef9a65](https://github.com/anddea/revanced-patches/commit/0ef9a655ce4a5f4ba5cd72ff3138b7e7d03b2613))
* **YouTube - Spoof client:** Partial fix for watch history issue of brand accounts on iOS clients ([2294ffb](https://github.com/anddea/revanced-patches/commit/2294ffb1640661e4421887eef8b3bd6ddff49fc1))


### Features

* Merge RVX v4.12.1-dev.1 ([#791](https://github.com/anddea/revanced-patches/issues/791)) ([de1ddcf](https://github.com/anddea/revanced-patches/commit/de1ddcfe050660c044f68e2610590d94eff45b10))
* **YouTube - Hook download actions:** Add package field in `Hook download actions` patch ([d32838a](https://github.com/anddea/revanced-patches/commit/d32838acf3f36bd01e5745516c993d9c69014b87))
* **YouTube - Shorts components:** Move `Change Shorts repeat state` setting to `Experimental Flags` ([112060a](https://github.com/anddea/revanced-patches/commit/112060a75316b293671f1cec07730d7a05ad3a33))
* **YouTube Music:** Add `Change share sheet` patch ([f4ebfff](https://github.com/anddea/revanced-patches/commit/f4ebfffa3dbd7fe5947b4d7e77ee80bbaf70a627))
* **YouTube Music:** Add support version `7.12.52` ~ `7.13.52` ([4670f1d](https://github.com/anddea/revanced-patches/commit/4670f1d040dcf71376b5870b4330052800fadbd7))

# [2.228.0-dev.6](https://github.com/anddea/revanced-patches/compare/v2.228.0-dev.5...v2.228.0-dev.6) (2024-08-04)


### Features

* **YouTube - Default Quality & Speed:** Add an option to disable toasts when changing the default values ([cc95f31](https://github.com/anddea/revanced-patches/commit/cc95f316485fe1759621b37471980c0ffe14d7cd))

# [2.228.0-dev.5](https://github.com/anddea/revanced-patches/compare/v2.228.0-dev.4...v2.228.0-dev.5) (2024-08-04)


### Bug Fixes

* **YouTube - Custom branding:** Fix patch error regarding `Xisr Yellow` ([dae0a79](https://github.com/anddea/revanced-patches/commit/dae0a793acd2c50e8bda2c8809d3e377101d8c41))

# [2.228.0-dev.4](https://github.com/anddea/revanced-patches/compare/v2.228.0-dev.3...v2.228.0-dev.4) (2024-08-04)


### Bug Fixes

* **YouTube - Custom Branding:** Fixed broken splash animation for `Xisr Yellow` ([#758](https://github.com/anddea/revanced-patches/issues/758)) ([b5ea4b1](https://github.com/anddea/revanced-patches/commit/b5ea4b1f41a43d8ab473bea0212d7f42564dd5b5))


### Features

* **YouTube - Download Playlist Button:** Add playlist download button ([#761](https://github.com/anddea/revanced-patches/issues/761)) ([449d45a](https://github.com/anddea/revanced-patches/commit/449d45aaff4be67937a89c177f3e6ee8b8e5d489))

# [2.228.0-dev.3](https://github.com/anddea/revanced-patches/compare/v2.228.0-dev.2...v2.228.0-dev.3) (2024-07-31)


### Bug Fixes

* **YouTube - Hide feed components:** Sometimes `Hide carousel shelf` setting doesn't work ([f53ce46](https://github.com/anddea/revanced-patches/commit/f53ce46c1461ac8699af767f03f92a20e7c1ed65))


### Features

* **YouTube Music - Player components:** Add settings `Disable miniplayer gesture`, `Disable player gesture` ([0bf84f9](https://github.com/anddea/revanced-patches/commit/0bf84f9d4cf3cad63b4cb6adbac1334be966e5ed))
* **YouTube Music:** Add support version `7.11.51` ~ `7.12.51` ([b8a62ae](https://github.com/anddea/revanced-patches/commit/b8a62aeb0528f5ef513330ae5ef1790ffe4cd920))

# [2.228.0-dev.2](https://github.com/anddea/revanced-patches/compare/v2.228.0-dev.1...v2.228.0-dev.2) (2024-07-29)


### Bug Fixes

* **YouTube - Client spoof:** Some side effects of iOS client ([823711e](https://github.com/anddea/revanced-patches/commit/823711e84bc95b88faa620690ee0fb0960b93808))
* **YouTube - Disable auto captions:** Turning on `Disable forced auto captions` will disable subtitles ([f4dc6d3](https://github.com/anddea/revanced-patches/commit/f4dc6d39b75a61ae954cc7657551535499826f5f))
* **YouTube - Toolbar components:** Turning on the `Hide voice search button` setting makes the margin of the searchbar 0 ([ca02dba](https://github.com/anddea/revanced-patches/commit/ca02dba30710decc4ddea07318e72a9966124665))


### Features

* **YouTube - Description components:** Add `Hide Contents section` setting ([e8358ff](https://github.com/anddea/revanced-patches/commit/e8358ffc680693e2923e3c03933b9fcfb0874eb7))
* **YouTube - Overlay buttons:** Add patch option `Change top buttons` ([504a1fe](https://github.com/anddea/revanced-patches/commit/504a1fe46ae97b7b328c4245941c69be698bb4f3))
* **YouTube - Player components:** Add `Hide zoom overlay` setting ([00640e6](https://github.com/anddea/revanced-patches/commit/00640e661753802439dfba65827307e7e703fb14))
* **YouTube & Music - Custom branding icon:** Rename `Revancify Yellow` To `Xisr Yellow` ([#756](https://github.com/anddea/revanced-patches/issues/756)) ([26d7deb](https://github.com/anddea/revanced-patches/commit/26d7debb8d40013b65b66ac1d56f13c01755ea23))
* **YouTube & Music - Custom branding icon:** Update `Xisr Yellow` icon ([#755](https://github.com/anddea/revanced-patches/issues/755)) ([c1dfd1f](https://github.com/anddea/revanced-patches/commit/c1dfd1f20b777828f57c69bebb9d1f85190f8c7c))
* **YouTube Music - Hide layout components:** Add `Hide settings menu` setting ([78b61dc](https://github.com/anddea/revanced-patches/commit/78b61dc799c4a3106a6800b176f464b8465b27d7))
* **YouTube Music:** Add support version `7.10.52` ([a842d17](https://github.com/anddea/revanced-patches/commit/a842d1743d13dac33db8ba0179f18289b4a8a742))
* **YouTube:** Add `Watch history` patch ([f1fd6e6](https://github.com/anddea/revanced-patches/commit/f1fd6e68f19d9c6ba45efc081e7ac50d42ef8529))

# [2.228.0-dev.1](https://github.com/anddea/revanced-patches/compare/v2.227.1-dev.1...v2.228.0-dev.1) (2024-07-26)


### Features

* **Custom Branding Icons:** Change default icon to `Revancify Yellow` ([#752](https://github.com/anddea/revanced-patches/issues/752)) ([60163db](https://github.com/anddea/revanced-patches/commit/60163dbde9d90035763ed66827515485393e6b4d))

## [2.227.1-dev.1](https://github.com/anddea/revanced-patches/compare/v2.227.0...v2.227.1-dev.1) (2024-07-26)


### Bug Fixes

* **YouTube - Custom branding icon:** Patch failed for some icons ([97bc461](https://github.com/anddea/revanced-patches/commit/97bc461be57e5ed4f37dd56b3df19cfb933b4d39))

# [2.227.0](https://github.com/anddea/revanced-patches/compare/v2.226.0...v2.227.0) (2024-07-25)


### Bug Fixes

* **Change version code:** Change to a universal patch ([11a00ca](https://github.com/anddea/revanced-patches/commit/11a00ca5c046806124b6441904ce66df3ce002a6))
* Patch error due to invalid strings ([5069303](https://github.com/anddea/revanced-patches/commit/5069303f9dcc08a56a3d8582832db8f869e7245b))
* **Reddit - Change version code:** `Version code` option is not available in RVX Manager ([9a4788d](https://github.com/anddea/revanced-patches/commit/9a4788d1c0dc089088990e8d561cd8c976b4ed55))
* **Shorts components:** `Hide sound button` doesn't work (A/B tests) ([234feb7](https://github.com/anddea/revanced-patches/commit/234feb786b9b391815a2555c0a49aff66b66e5cf))
* Update broken Translations ([f2c2b21](https://github.com/anddea/revanced-patches/commit/f2c2b21821a497c8a88de9085f47b9c20c93a2e9))
* **YouTube - Custom branding icon:** MMT splash icon were missing ([6f29eec](https://github.com/anddea/revanced-patches/commit/6f29eecd0a98016e65492df8a47942ab1f0e2cb9))
* **YouTube - Custom Branding:** Patch fails when selecting some custom icons ([3660723](https://github.com/anddea/revanced-patches/commit/36607234cb8755d9e703d342674284dc2dbd4c8c))
* **YouTube - Custom header for YouTube:** Custom headers do not change (A/B tests) ([7b3e52d](https://github.com/anddea/revanced-patches/commit/7b3e52d4fefa5f67add79b47cbca84b62108070c))
* **YouTube - Disable forced auto captions:** Subtitles don't work after playing Shorts ([b1d091f](https://github.com/anddea/revanced-patches/commit/b1d091f96107936cd48b56cf20cb257c2e9de61e))
* **YouTube - Overlay buttons:** `Mute Volume` button was black when light theme is enabled ([ad99667](https://github.com/anddea/revanced-patches/commit/ad99667ab1b5dc6fa8256978c024f05bbbcc552c))
* **YouTube - Overlay buttons:** Image of the `Mute video` button is reversed ([9b5ebab](https://github.com/anddea/revanced-patches/commit/9b5ebab8e857fb163e26019353a3001f7eb3457f))
* **YouTube - Settings:** Some settings were duplicated ([4e7d994](https://github.com/anddea/revanced-patches/commit/4e7d994ff8805aa602326357135669a2a9e37375))
* **YouTube - Settings:** Toolbar added twice to RVX settings ([6346a5e](https://github.com/anddea/revanced-patches/commit/6346a5e7687cf37f7ce175a30c32c2c9df584f3a))
* **YouTube - SponsorBlock:** Skip segments when casting ([a40bbcb](https://github.com/anddea/revanced-patches/commit/a40bbcb85447673c52eef94874148ebcf3bb6c16))
* **YouTube - Theme:** Reverts background color of `More comments` icon in live chats ([3c7f976](https://github.com/anddea/revanced-patches/commit/3c7f9764c18eed33b6577e20f86c45df94a82091))
* **YouTube - Translations:** Language `de-rDE` was giving warnings during compilation ([20617d5](https://github.com/anddea/revanced-patches/commit/20617d51937c6117de0bd4735bf65e6cd4fa5ef4))
* **YouTube - Video playback:** Default video quality does not apply ([04fa20d](https://github.com/anddea/revanced-patches/commit/04fa20da4dfff3772904be3e8be634a1f3c492e0))
* **YouTube - Visual preferences icons:** Add missing `Custom branding icons` ([27efd27](https://github.com/anddea/revanced-patches/commit/27efd27bc0296ba43445a0ece90dbf81bf69706d))
* **YouTube - VIsual preferences icons:** Revert ([c002788](https://github.com/anddea/revanced-patches/commit/c00278891f01b2a197674c79a9bf27341edfd47b))
* **YouTube Music - Visual preferences icons:** Add missing `Custom branding icons` ([52d581c](https://github.com/anddea/revanced-patches/commit/52d581cc48567b46fa496375b7e079774bd7942c))
* **YouTube:** Bring back missing strings ([d47016a](https://github.com/anddea/revanced-patches/commit/d47016aa954d9dc9c308f7207818dbf29a8a5d80))


### Features

* Merge RVX v4.10.1 ([#691](https://github.com/anddea/revanced-patches/issues/691)) ([a305ca9](https://github.com/anddea/revanced-patches/commit/a305ca95c32ba69a513c9ce9bf681d3a414f16f9))
* Merge RVX v4.10.2-dev.1 ([eb5f944](https://github.com/anddea/revanced-patches/commit/eb5f9449daf1dcbe8ce1c51b39e3371e7f2146b2))
* Merge the `Custom package name` patch into the `GmsCore support` patch and add patch options `GmsCoreVendorGroupId`, `CheckGmsCore`, `PackageNameYouTube`, `PackageNameYouTubeMusic` ([2b4931a](https://github.com/anddea/revanced-patches/commit/2b4931a2151984c57b0f9d3f660ccadd502609ca))
* **YouTube - Alternative thumbnails:** Add alternative domain ([5a86268](https://github.com/anddea/revanced-patches/commit/5a86268e7d5ae785f8cf6b81d4a1c120dfd0c542))
* **YouTube - Bypass image region restrictions:** Bring back setting to change alternative domain ([a037281](https://github.com/anddea/revanced-patches/commit/a037281dd1da76fd55ee5e3f537e99c8ffe1199f))
* **YouTube - Custom branding icon:** Add old splash animation for `Revancify Red` and `Revancify Blue` ([ed2da7e](https://github.com/anddea/revanced-patches/commit/ed2da7e36a64a4b2aa653281ed7e762f7a0dd7b7))
* **YouTube - Custom branding icon:** Custom header support for `Revancify Yellow` ([#656](https://github.com/anddea/revanced-patches/issues/656)) ([72dfa7e](https://github.com/anddea/revanced-patches/commit/72dfa7e2d00e65d027eb0ff423764f79d04f1018))
* **YouTube - Custom branding icon:** Update old splash animation for `Revancify Red` and `Revancify Blue` ([ff70195](https://github.com/anddea/revanced-patches/commit/ff7019555f536e530d5406469d22ac21dced11dd))
* **YouTube - Custom Shorts action buttons:** Add `YouTube` (unchanged) to selectable patch options ([0b77187](https://github.com/anddea/revanced-patches/commit/0b77187699f5c045f8f316dc5cad0c09ccf1917a))
* **YouTube - Force player buttons background:** Add an option to change the color and opacity of the player buttons' background ([eab9795](https://github.com/anddea/revanced-patches/commit/eab979534b5b98fb532a3c6ad79d338e8f23ab4c))
* **YouTube - Hide feed components:** Add `Match full word` option for keyword filtering ([cd12a83](https://github.com/anddea/revanced-patches/commit/cd12a838b1e1f87c18af0f06fd0b1cd09974a8fb))
* **YouTube - Miniplayer:** Add `Enable double-tap action` setting ([157f25a](https://github.com/anddea/revanced-patches/commit/157f25a94a7d5aa0be46d9c39efd8b088a713806))
* **YouTube - Overlay buttons:** Add `Mute Video` button ([#684](https://github.com/anddea/revanced-patches/issues/684)) ([fb26c7a](https://github.com/anddea/revanced-patches/commit/fb26c7a1d9044780158db771cc05dc50115bb3a8))
* **YouTube - Player components:** `Hide player popup panels` setting now hides the products panel ([e96317b](https://github.com/anddea/revanced-patches/commit/e96317b703bc645b657ae697ed2790645bd54d7c))
* **YouTube - Searchbar:** Change cursor color dynamically ([#675](https://github.com/anddea/revanced-patches/issues/675)) ([e5babda](https://github.com/anddea/revanced-patches/commit/e5babda4e8f1fd054ad08bee948f22c8a735cb84))
* **YouTube - Settings:** Change cursor color dynamically in searchbar ([5133265](https://github.com/anddea/revanced-patches/commit/51332654c1e8419ccaa802f90e5bdb9e40fe9f96))
* **YouTube - Settings:** Improved sorting of settings ([fc8bb8c](https://github.com/anddea/revanced-patches/commit/fc8bb8c350955ffed7d1850b8b94719577a24891))
* **YouTube - Settings:** Make `InsertPosition` case-insensitive and interchangeable between Setting name and Setting key ([#713](https://github.com/anddea/revanced-patches/issues/713)) ([269dc50](https://github.com/anddea/revanced-patches/commit/269dc50f707e6e15ff95a956dad34b4151860649))
* **YouTube - Settings:** Show AlertDialog when changing some settings value (matches ReVanced) ([3f301e0](https://github.com/anddea/revanced-patches/commit/3f301e0c4226d77098424b74b44dcb5ebf0ba189))
* **YouTube - Shorts components:** Add `Double-tap animation` settings ([a1868ec](https://github.com/anddea/revanced-patches/commit/a1868ecae8f74ca9d14a616707a5c94058d79591))
* **YouTube - Shorts components:** Add `Enable timestamps`, `Timestamp long press action`, `Meta panel bottom margin` settings ([ad087ff](https://github.com/anddea/revanced-patches/commit/ad087ff2bc184b2cf9eb76e02c4dad719f2c6896))
* **YouTube - Shorts components:** Add `Hide paused header` setting ([b044b61](https://github.com/anddea/revanced-patches/commit/b044b61242c7e4200373debc57975482b81c070b))
* **YouTube - Shorts:** Add Original Outline Icons ([#663](https://github.com/anddea/revanced-patches/issues/663)) ([35a65f5](https://github.com/anddea/revanced-patches/commit/35a65f5e873bebc8ddf151cb194fe0d43f53022b))
* **YouTube - Shorts:** Rename ShortsOverlayButtonsPatch to ShortsActionButtonsPatch ([c3cf914](https://github.com/anddea/revanced-patches/commit/c3cf914c1b6c55d0e005d82077e639d59fb627d1))
* **YouTube - Sponsorblock:** Add content descriptions to `New Segment` buttons ([1deec38](https://github.com/anddea/revanced-patches/commit/1deec386beded1e2d685c3306a61ec8601691cb1))
* **YouTube - Swipe controls:** Add `Enable save and restore brightness` setting (Match to ReVanced) ([4fb9334](https://github.com/anddea/revanced-patches/commit/4fb9334c1597fea75aecd7873ee57d4a5360473a))
* **YouTube - Toolbar components:** Add `Hide image search button` settings ([457df98](https://github.com/anddea/revanced-patches/commit/457df9848340eec17c15f0fa6b156afe63c0975e))
* **YouTube - Translations:** Update `Chinese Traditional` ([#666](https://github.com/anddea/revanced-patches/issues/666)) ([4f383eb](https://github.com/anddea/revanced-patches/commit/4f383eb45aa0610b1b27f039a718288ee193564a))
* **YouTube - Visual preferences icons:** RVX settings icon now changes even when the `Custom branding icon for YouTube` patch option is set to `YouTube` (unchanged) ([770b39b](https://github.com/anddea/revanced-patches/commit/770b39b6c08896041d7b227f6cb9cb8ddb2be1a5))
* **YouTube & YouTube Music - Custom branding icon:** Updated `Revancify Yellow` icon  ([#689](https://github.com/anddea/revanced-patches/issues/689)) ([b86e084](https://github.com/anddea/revanced-patches/commit/b86e084b47da31aea5349d424902f5be11d4da99))
* **YouTube Music - Custom branding icon:** Add `Revancify Yellow` header ([#670](https://github.com/anddea/revanced-patches/issues/670)) ([3dfcadb](https://github.com/anddea/revanced-patches/commit/3dfcadb2f1cdb7dafefe9f1337fc31f4d829a184))
* **YouTube Music - Custom branding icon:** Updated `AFN` icons ([#724](https://github.com/anddea/revanced-patches/issues/724)) ([700af02](https://github.com/anddea/revanced-patches/commit/700af022f47a70d9f611dc0e9555cfc2840992f6))
* **YouTube Music - Thumbnails:** Add `Alternative domain` patch ([e51c9a4](https://github.com/anddea/revanced-patches/commit/e51c9a4d766cd8928449743dd8c6b09e30d0b945))
* **YouTube Music:** Add support version `7.08.53` ([259682b](https://github.com/anddea/revanced-patches/commit/259682b7c0f59ecac0468ee0c7a067c85d5f6bdc))
* **YouTube Music:** Add support versions `7.06.54` ~ `7.07.51` ([dc1e29a](https://github.com/anddea/revanced-patches/commit/dc1e29a77d06b72e6e48a3732b7f9ee305c53244))
* **YouTube Music:** Add support versions `7.07.52` ~ `7.08.52` ([11a668b](https://github.com/anddea/revanced-patches/commit/11a668b52a3469dc64bc1a253711ca213272e4cf))
* **YouTube Music:** Add support versions `7.08.54` ~ `7.10.51` ([0098528](https://github.com/anddea/revanced-patches/commit/009852881590e4a83038af29ce0f34b230d848a2))
* **YouTube Music:** Integrate `Hide double tap overlay filter` patch into the `Player components` patch ([9bd92bb](https://github.com/anddea/revanced-patches/commit/9bd92bb25e610c347f6b3c6712ade0fb2b553271))
* **YouTube:** Add content descriptions to improve accessibility ([468978c](https://github.com/anddea/revanced-patches/commit/468978c77d0b25ab1b70e70f80b3cb2ec678b546))
* **YouTube:** Add support version `19.25.39`, drop support version `19.23.40` ([7a5d939](https://github.com/anddea/revanced-patches/commit/7a5d9394cd7ddf3a05363bdb43611504c5872e97))
* **YouTube:** DeArrow alternative domain ([#672](https://github.com/anddea/revanced-patches/issues/672)) ([ac46f0a](https://github.com/anddea/revanced-patches/commit/ac46f0af2f317629bb4e08bbcba16bb53516a975))
* **YouTube:** Drop support versions `19.17.41` ~ `19.25.39` ([e6b589b](https://github.com/anddea/revanced-patches/commit/e6b589b68ae7774b5104743670c4eb8be1d140cc))
* **YouTube:** Integrate `Change Shorts repeat state` patch into the `Shorts components` patch ([ccc69ec](https://github.com/anddea/revanced-patches/commit/ccc69ec8b501f3877db9f658f637dd272dccafb2))
* **YouTube:** Integrate `Hide double tap overlay filter` patch into the `Player components` patch ([d7ccd0d](https://github.com/anddea/revanced-patches/commit/d7ccd0dd352339d44f3e6330d6396876150b53c2))
* **YouTube:** Separate the `Bypass image region restrictions` patch from the `Alternative thumbnails` patch (Reflecting changes in ReVanced) ([5d41bff](https://github.com/anddea/revanced-patches/commit/5d41bffbeb93e22cc348ebd9f928b67ea506c92c))

# [2.227.0-dev.24](https://github.com/anddea/revanced-patches/compare/v2.227.0-dev.23...v2.227.0-dev.24) (2024-07-22)


### Features

* **YouTube - Bypass image region restrictions:** Bring back setting to change alternative domain ([a037281](https://github.com/anddea/revanced-patches/commit/a037281dd1da76fd55ee5e3f537e99c8ffe1199f))
* **YouTube - Force player buttons background:** Add an option to change the color and opacity of the player buttons' background ([eab9795](https://github.com/anddea/revanced-patches/commit/eab979534b5b98fb532a3c6ad79d338e8f23ab4c))

# [2.227.0-dev.23](https://github.com/anddea/revanced-patches/compare/v2.227.0-dev.22...v2.227.0-dev.23) (2024-07-18)


### Bug Fixes

* **Change version code:** Change to a universal patch ([11a00ca](https://github.com/anddea/revanced-patches/commit/11a00ca5c046806124b6441904ce66df3ce002a6))


### Features

* Merge the `Custom package name` patch into the `GmsCore support` patch and add patch options `GmsCoreVendorGroupId`, `CheckGmsCore`, `PackageNameYouTube`, `PackageNameYouTubeMusic` ([2b4931a](https://github.com/anddea/revanced-patches/commit/2b4931a2151984c57b0f9d3f660ccadd502609ca))
* **YouTube - Player components:** `Hide player popup panels` setting now hides the products panel ([e96317b](https://github.com/anddea/revanced-patches/commit/e96317b703bc645b657ae697ed2790645bd54d7c))
* **YouTube - Swipe controls:** Add `Enable save and restore brightness` setting (Match to ReVanced) ([4fb9334](https://github.com/anddea/revanced-patches/commit/4fb9334c1597fea75aecd7873ee57d4a5360473a))
* **YouTube Music:** Add support versions `7.08.54` ~ `7.10.51` ([0098528](https://github.com/anddea/revanced-patches/commit/009852881590e4a83038af29ce0f34b230d848a2))
* **YouTube:** Separate the `Bypass image region restrictions` patch from the `Alternative thumbnails` patch (Reflecting changes in ReVanced) ([5d41bff](https://github.com/anddea/revanced-patches/commit/5d41bffbeb93e22cc348ebd9f928b67ea506c92c))

# [2.227.0-dev.22](https://github.com/anddea/revanced-patches/compare/v2.227.0-dev.21...v2.227.0-dev.22) (2024-07-15)


### Bug Fixes

* **YouTube - VIsual preferences icons:** Revert ([c002788](https://github.com/anddea/revanced-patches/commit/c00278891f01b2a197674c79a9bf27341edfd47b))

# [2.227.0-dev.21](https://github.com/anddea/revanced-patches/compare/v2.227.0-dev.20...v2.227.0-dev.21) (2024-07-15)


### Features

* **YouTube - Sponsorblock:** Add content descriptions to `New Segment` buttons ([1deec38](https://github.com/anddea/revanced-patches/commit/1deec386beded1e2d685c3306a61ec8601691cb1))
* **YouTube & YouTube Music - Custom branding icon:** Updated `Revancify Yellow` icon  ([#689](https://github.com/anddea/revanced-patches/issues/689)) ([b86e084](https://github.com/anddea/revanced-patches/commit/b86e084b47da31aea5349d424902f5be11d4da99))
* **YouTube Music - Custom branding icon:** Updated `AFN` icons ([#724](https://github.com/anddea/revanced-patches/issues/724)) ([700af02](https://github.com/anddea/revanced-patches/commit/700af022f47a70d9f611dc0e9555cfc2840992f6))

# [2.227.0-dev.20](https://github.com/anddea/revanced-patches/compare/v2.227.0-dev.19...v2.227.0-dev.20) (2024-07-14)


### Bug Fixes

* **YouTube - Disable forced auto captions:** Subtitles don't work after playing Shorts ([b1d091f](https://github.com/anddea/revanced-patches/commit/b1d091f96107936cd48b56cf20cb257c2e9de61e))
* **YouTube - Overlay buttons:** Image of the `Mute video` button is reversed ([9b5ebab](https://github.com/anddea/revanced-patches/commit/9b5ebab8e857fb163e26019353a3001f7eb3457f))
* **YouTube - SponsorBlock:** Skip segments when casting ([a40bbcb](https://github.com/anddea/revanced-patches/commit/a40bbcb85447673c52eef94874148ebcf3bb6c16))
* **YouTube - Video playback:** Default video quality does not apply ([04fa20d](https://github.com/anddea/revanced-patches/commit/04fa20da4dfff3772904be3e8be634a1f3c492e0))


### Features

* Merge RVX v4.10.2-dev.1 ([eb5f944](https://github.com/anddea/revanced-patches/commit/eb5f9449daf1dcbe8ce1c51b39e3371e7f2146b2))
* **YouTube - Custom branding icon:** Update old splash animation for `Revancify Red` and `Revancify Blue` ([ff70195](https://github.com/anddea/revanced-patches/commit/ff7019555f536e530d5406469d22ac21dced11dd))
* **YouTube - Shorts components:** Add `Hide paused header` setting ([b044b61](https://github.com/anddea/revanced-patches/commit/b044b61242c7e4200373debc57975482b81c070b))
* **YouTube Music - Thumbnails:** Add `Alternative domain` patch ([e51c9a4](https://github.com/anddea/revanced-patches/commit/e51c9a4d766cd8928449743dd8c6b09e30d0b945))
* **YouTube Music:** Add support version `7.08.53` ([259682b](https://github.com/anddea/revanced-patches/commit/259682b7c0f59ecac0468ee0c7a067c85d5f6bdc))
* **YouTube:** Drop support versions `19.17.41` ~ `19.25.39` ([e6b589b](https://github.com/anddea/revanced-patches/commit/e6b589b68ae7774b5104743670c4eb8be1d140cc))

# [2.227.0-dev.19](https://github.com/anddea/revanced-patches/compare/v2.227.0-dev.18...v2.227.0-dev.19) (2024-07-11)


### Bug Fixes

* **YouTube:** Bring back missing strings ([d47016a](https://github.com/anddea/revanced-patches/commit/d47016aa954d9dc9c308f7207818dbf29a8a5d80))

# [2.227.0-dev.19](https://github.com/anddea/revanced-patches/compare/v2.227.0-dev.18...v2.227.0-dev.19) (2024-07-11)


### Bug Fixes

* **YouTube:** Bring back missing strings ([d47016a](https://github.com/anddea/revanced-patches/commit/d47016aa954d9dc9c308f7207818dbf29a8a5d80))

# [2.227.0-dev.18](https://github.com/anddea/revanced-patches/compare/v2.227.0-dev.17...v2.227.0-dev.18) (2024-07-10)


### Bug Fixes

* **YouTube - Custom Branding:** Patch fails when selecting some custom icons ([3660723](https://github.com/anddea/revanced-patches/commit/36607234cb8755d9e703d342674284dc2dbd4c8c))

# [2.227.0-dev.17](https://github.com/anddea/revanced-patches/compare/v2.227.0-dev.16...v2.227.0-dev.17) (2024-07-09)


### Features

* **YouTube - Settings:** Make `InsertPosition` case-insensitive and interchangeable between Setting name and Setting key ([#713](https://github.com/anddea/revanced-patches/issues/713)) ([269dc50](https://github.com/anddea/revanced-patches/commit/269dc50f707e6e15ff95a956dad34b4151860649))

# [2.227.0-dev.16](https://github.com/anddea/revanced-patches/compare/v2.227.0-dev.15...v2.227.0-dev.16) (2024-07-07)


### Bug Fixes

* **YouTube - Translations:** Language `de-rDE` was giving warnings during compilation ([20617d5](https://github.com/anddea/revanced-patches/commit/20617d51937c6117de0bd4735bf65e6cd4fa5ef4))

# [2.227.0-dev.15](https://github.com/anddea/revanced-patches/compare/v2.227.0-dev.14...v2.227.0-dev.15) (2024-07-07)


### Bug Fixes

* Update broken Translations ([f2c2b21](https://github.com/anddea/revanced-patches/commit/f2c2b21821a497c8a88de9085f47b9c20c93a2e9))

# [2.227.0-dev.14](https://github.com/anddea/revanced-patches/compare/v2.227.0-dev.13...v2.227.0-dev.14) (2024-07-06)


### Bug Fixes

* Patch error due to invalid strings ([5069303](https://github.com/anddea/revanced-patches/commit/5069303f9dcc08a56a3d8582832db8f869e7245b))

# [2.227.0-dev.13](https://github.com/anddea/revanced-patches/compare/v2.227.0-dev.12...v2.227.0-dev.13) (2024-07-06)


### Bug Fixes

* **YouTube - Custom branding icon:** MMT splash icon were missing ([6f29eec](https://github.com/anddea/revanced-patches/commit/6f29eecd0a98016e65492df8a47942ab1f0e2cb9))

# [2.227.0-dev.12](https://github.com/anddea/revanced-patches/compare/v2.227.0-dev.11...v2.227.0-dev.12) (2024-07-06)


### Bug Fixes

* **YouTube - Settings:** Some settings were duplicated ([4e7d994](https://github.com/anddea/revanced-patches/commit/4e7d994ff8805aa602326357135669a2a9e37375))


### Features

* **YouTube - Settings:** Improved sorting of settings ([fc8bb8c](https://github.com/anddea/revanced-patches/commit/fc8bb8c350955ffed7d1850b8b94719577a24891))

# [2.227.0-dev.11](https://github.com/anddea/revanced-patches/compare/v2.227.0-dev.10...v2.227.0-dev.11) (2024-07-06)


### Features

* Merge RVX v4.10.1 ([#691](https://github.com/anddea/revanced-patches/issues/691)) ([a305ca9](https://github.com/anddea/revanced-patches/commit/a305ca95c32ba69a513c9ce9bf681d3a414f16f9))
* **YouTube - Alternative thumbnails:** Add alternative domain ([5a86268](https://github.com/anddea/revanced-patches/commit/5a86268e7d5ae785f8cf6b81d4a1c120dfd0c542))
* **YouTube - Custom branding icon:** Add old splash animation for `Revancify Red` and `Revancify Blue` ([ed2da7e](https://github.com/anddea/revanced-patches/commit/ed2da7e36a64a4b2aa653281ed7e762f7a0dd7b7))
* **YouTube - Miniplayer:** Add `Enable double-tap action` setting ([157f25a](https://github.com/anddea/revanced-patches/commit/157f25a94a7d5aa0be46d9c39efd8b088a713806))
* **YouTube - Settings:** Change cursor color dynamically in searchbar ([5133265](https://github.com/anddea/revanced-patches/commit/51332654c1e8419ccaa802f90e5bdb9e40fe9f96))
* **YouTube - Shorts components:** Add `Enable timestamps`, `Timestamp long press action`, `Meta panel bottom margin` settings ([ad087ff](https://github.com/anddea/revanced-patches/commit/ad087ff2bc184b2cf9eb76e02c4dad719f2c6896))
* **YouTube Music:** Add support versions `7.07.52` ~ `7.08.52` ([11a668b](https://github.com/anddea/revanced-patches/commit/11a668b52a3469dc64bc1a253711ca213272e4cf))
* **YouTube:** Add support version `19.25.39`, drop support version `19.23.40` ([7a5d939](https://github.com/anddea/revanced-patches/commit/7a5d9394cd7ddf3a05363bdb43611504c5872e97))
* **YouTube:** Integrate `Change Shorts repeat state` patch into the `Shorts components` patch ([ccc69ec](https://github.com/anddea/revanced-patches/commit/ccc69ec8b501f3877db9f658f637dd272dccafb2))

# [2.227.0-dev.10](https://github.com/anddea/revanced-patches/compare/v2.227.0-dev.9...v2.227.0-dev.10) (2024-07-05)


### Bug Fixes

* **YouTube - Overlay buttons:** `Mute Volume` button was black when light theme is enabled ([ad99667](https://github.com/anddea/revanced-patches/commit/ad99667ab1b5dc6fa8256978c024f05bbbcc552c))

# [2.227.0-dev.9](https://github.com/anddea/revanced-patches/compare/v2.227.0-dev.8...v2.227.0-dev.9) (2024-07-05)


### Features

* **YouTube - Overlay buttons:** Add `Mute Video` button ([#684](https://github.com/anddea/revanced-patches/issues/684)) ([fb26c7a](https://github.com/anddea/revanced-patches/commit/fb26c7a1d9044780158db771cc05dc50115bb3a8))

# [2.227.0-dev.8](https://github.com/anddea/revanced-patches/compare/v2.227.0-dev.7...v2.227.0-dev.8) (2024-07-04)


### Bug Fixes

* **YouTube Music - Visual preferences icons:** Add missing `Custom branding icons` ([52d581c](https://github.com/anddea/revanced-patches/commit/52d581cc48567b46fa496375b7e079774bd7942c))


### Features

* **YouTube:** Add content descriptions to improve accessibility ([468978c](https://github.com/anddea/revanced-patches/commit/468978c77d0b25ab1b70e70f80b3cb2ec678b546))

# [2.227.0-dev.7](https://github.com/anddea/revanced-patches/compare/v2.227.0-dev.6...v2.227.0-dev.7) (2024-07-03)


### Bug Fixes

* **YouTube - Visual preferences icons:** Add missing `Custom branding icons` ([27efd27](https://github.com/anddea/revanced-patches/commit/27efd27bc0296ba43445a0ece90dbf81bf69706d))


### Features

* **YouTube - Hide feed components:** Add `Match full word` option for keyword filtering ([cd12a83](https://github.com/anddea/revanced-patches/commit/cd12a838b1e1f87c18af0f06fd0b1cd09974a8fb))

# [2.227.0-dev.6](https://github.com/anddea/revanced-patches/compare/v2.227.0-dev.5...v2.227.0-dev.6) (2024-07-01)


### Reverts

* Revert "fix(YouTube - Shorts): Restore `TikTok` icons (#676)" ([ad814d4](https://github.com/anddea/revanced-patches/commit/ad814d4b4a8329c43642648e4dfb4827fa45e07f)), closes [#676](https://github.com/anddea/revanced-patches/issues/676)

# [2.227.0-dev.5](https://github.com/anddea/revanced-patches/compare/v2.227.0-dev.4...v2.227.0-dev.5) (2024-07-01)


### Bug Fixes

* **YouTube - Shorts:** Restore `TikTok` icons ([#676](https://github.com/anddea/revanced-patches/issues/676)) ([8f6c985](https://github.com/anddea/revanced-patches/commit/8f6c9850c632247ae5feec2815358912e53f9c2f))

# [2.227.0-dev.4](https://github.com/anddea/revanced-patches/compare/v2.227.0-dev.3...v2.227.0-dev.4) (2024-07-01)


### Features

* **YouTube - Searchbar:** Change cursor color dynamically ([#675](https://github.com/anddea/revanced-patches/issues/675)) ([e5babda](https://github.com/anddea/revanced-patches/commit/e5babda4e8f1fd054ad08bee948f22c8a735cb84))

# [2.227.0-dev.3](https://github.com/anddea/revanced-patches/compare/v2.227.0-dev.2...v2.227.0-dev.3) (2024-07-01)


### Bug Fixes

* **Reddit - Change version code:** `Version code` option is not available in RVX Manager ([9a4788d](https://github.com/anddea/revanced-patches/commit/9a4788d1c0dc089088990e8d561cd8c976b4ed55))
* **Shorts components:** `Hide sound button` doesn't work (A/B tests) ([234feb7](https://github.com/anddea/revanced-patches/commit/234feb786b9b391815a2555c0a49aff66b66e5cf))
* **YouTube - Custom header for YouTube:** Custom headers do not change (A/B tests) ([7b3e52d](https://github.com/anddea/revanced-patches/commit/7b3e52d4fefa5f67add79b47cbca84b62108070c))
* **YouTube - Settings:** Toolbar added twice to RVX settings ([6346a5e](https://github.com/anddea/revanced-patches/commit/6346a5e7687cf37f7ce175a30c32c2c9df584f3a))
* **YouTube - Theme:** Reverts background color of `More comments` icon in live chats ([3c7f976](https://github.com/anddea/revanced-patches/commit/3c7f9764c18eed33b6577e20f86c45df94a82091))


### Features

* **YouTube - Custom Shorts action buttons:** Add `YouTube` (unchanged) to selectable patch options ([0b77187](https://github.com/anddea/revanced-patches/commit/0b77187699f5c045f8f316dc5cad0c09ccf1917a))
* **YouTube - Settings:** Show AlertDialog when changing some settings value (matches ReVanced) ([3f301e0](https://github.com/anddea/revanced-patches/commit/3f301e0c4226d77098424b74b44dcb5ebf0ba189))
* **YouTube - Shorts components:** Add `Double-tap animation` settings ([a1868ec](https://github.com/anddea/revanced-patches/commit/a1868ecae8f74ca9d14a616707a5c94058d79591))
* **YouTube - Shorts:** Rename ShortsOverlayButtonsPatch to ShortsActionButtonsPatch ([c3cf914](https://github.com/anddea/revanced-patches/commit/c3cf914c1b6c55d0e005d82077e639d59fb627d1))
* **YouTube - Toolbar components:** Add `Hide image search button` settings ([457df98](https://github.com/anddea/revanced-patches/commit/457df9848340eec17c15f0fa6b156afe63c0975e))
* **YouTube - Visual preferences icons:** RVX settings icon now changes even when the `Custom branding icon for YouTube` patch option is set to `YouTube` (unchanged) ([770b39b](https://github.com/anddea/revanced-patches/commit/770b39b6c08896041d7b227f6cb9cb8ddb2be1a5))
* **YouTube Music - Custom branding icon:** Add `Revancify Yellow` header ([#670](https://github.com/anddea/revanced-patches/issues/670)) ([3dfcadb](https://github.com/anddea/revanced-patches/commit/3dfcadb2f1cdb7dafefe9f1337fc31f4d829a184))
* **YouTube Music:** Add support versions `7.06.54` ~ `7.07.51` ([dc1e29a](https://github.com/anddea/revanced-patches/commit/dc1e29a77d06b72e6e48a3732b7f9ee305c53244))
* **YouTube Music:** Integrate `Hide double tap overlay filter` patch into the `Player components` patch ([9bd92bb](https://github.com/anddea/revanced-patches/commit/9bd92bb25e610c347f6b3c6712ade0fb2b553271))
* **YouTube:** DeArrow alternative domain ([#672](https://github.com/anddea/revanced-patches/issues/672)) ([ac46f0a](https://github.com/anddea/revanced-patches/commit/ac46f0af2f317629bb4e08bbcba16bb53516a975))
* **YouTube:** Integrate `Hide double tap overlay filter` patch into the `Player components` patch ([d7ccd0d](https://github.com/anddea/revanced-patches/commit/d7ccd0dd352339d44f3e6330d6396876150b53c2))

# [2.227.0-dev.2](https://github.com/anddea/revanced-patches/compare/v2.227.0-dev.1...v2.227.0-dev.2) (2024-06-29)


### Features

* **YouTube - Shorts:** Add Original Outline Icons ([#663](https://github.com/anddea/revanced-patches/issues/663)) ([35a65f5](https://github.com/anddea/revanced-patches/commit/35a65f5e873bebc8ddf151cb194fe0d43f53022b))
* **YouTube - Translations:** Update `Chinese Traditional` ([#666](https://github.com/anddea/revanced-patches/issues/666)) ([4f383eb](https://github.com/anddea/revanced-patches/commit/4f383eb45aa0610b1b27f039a718288ee193564a))

# [2.227.0-dev.1](https://github.com/anddea/revanced-patches/compare/v2.226.0...v2.227.0-dev.1) (2024-06-26)


### Features

* **YouTube - Custom branding icon:** Custom header support for `Revancify Yellow` ([#656](https://github.com/anddea/revanced-patches/issues/656)) ([72dfa7e](https://github.com/anddea/revanced-patches/commit/72dfa7e2d00e65d027eb0ff423764f79d04f1018))

# [2.226.0](https://github.com/anddea/revanced-patches/compare/v2.225.0...v2.226.0) (2024-06-26)


### Bug Fixes

* **Custom branding icon:** Failed because of missing resources ([5e0723e](https://github.com/anddea/revanced-patches/commit/5e0723e52856a93f06f798b1fc18e18bbfa56490))
* **GmsCore support:** Spoof package name ([9c4e70c](https://github.com/anddea/revanced-patches/commit/9c4e70cf896e1ca9efc6c9b3766eca7c6869a413))
* **Hide ads:** app crashes in the old client ([2a61d08](https://github.com/anddea/revanced-patches/commit/2a61d08cb59d76115d0f0ddd7deecc8e02a245dc))
* **Reddit - Settings:** Patch option `RVX settings menu name` does not apply to header in ReVanced Extended settings ([4827a0f](https://github.com/anddea/revanced-patches/commit/4827a0fa0b1ee8f576fa1020f9a0378b79cdf9cc))
* **Settings:** Failed with custom RVXSettingsMenuName ([c0d3ed0](https://github.com/anddea/revanced-patches/commit/c0d3ed0b377f018913d217962088e935084c167c))
* **YouTube - Custom branding icon:** Add `Revancify Yellow` to patch options ([aea7060](https://github.com/anddea/revanced-patches/commit/aea70604ae326ec86ddec5b37d03a06673292545))
* **YouTube - Miniplayer:** `Hide expand and close buttons` setting is not disabled in `Modern 1` on YouTube 19.20.35+ ([b7c330a](https://github.com/anddea/revanced-patches/commit/b7c330ace90f9761cd06322eb0840b593d1e26d1))
* **YouTube - Settings:** `CairoSettings` is applied due to A/B testing ([bad0f92](https://github.com/anddea/revanced-patches/commit/bad0f929853890eaac7a21a4faaad5cf43077f74))
* **YouTube - Spoof client:** Player gestures not working when spoofing with `Android VR` client ([0cdc4f3](https://github.com/anddea/revanced-patches/commit/0cdc4f33c98cc16f5a302f2ca46a93434165bfd3))
* **YouTube - Spoof client:** Restore playback speed menu when spoofing to an iOS, Android TV, Android Testsuite client ([06ffb15](https://github.com/anddea/revanced-patches/commit/06ffb15d8bf5b91fa98910d975d3ad887e609789))
* **YouTube - Spoof client:** Seekbar thumbnail not shown in `Android Testsuite` client ([74d50b1](https://github.com/anddea/revanced-patches/commit/74d50b1b5ad5860a4f341039e953a7c38b457afb))
* **YouTube - Translations:** Removed some languages from UI ([93279da](https://github.com/anddea/revanced-patches/commit/93279da02684887dbb1f25668f64f49f2ca55391))
* **YouTube - Visual preferences icons:** Typo in QUIC ([409e283](https://github.com/anddea/revanced-patches/commit/409e2831b3c1ab9db1023e5f6257d4d4d327a592))
* **YouTube Music - Visual preferences icons:** Add missing custom branding icons ([1071471](https://github.com/anddea/revanced-patches/commit/10714717b69710f7c5eb364cb7d0a604c00dae55))
* **YouTube/Overlay buttons:** in devices that do not use `xxhdpi`, some buttons are not replaced correctly ([030b2f6](https://github.com/anddea/revanced-patches/commit/030b2f6fa43ce280266ce45a2f2272aa4d1c1eda))
* **YouTube:** `Hide animated button background` patch doesn't work ([37fac01](https://github.com/anddea/revanced-patches/commit/37fac01c5b173fbbdfa9c44043b086836cbb36cd))


### Features

* **Custom branding icon:** Add `Revancify Yellow` icon ([#533](https://github.com/anddea/revanced-patches/issues/533)) ([d3bdd97](https://github.com/anddea/revanced-patches/commit/d3bdd97c8afc205fca85627740d3da498afb2121))
* **GmsCore support:** Add `Don't show again` option for battery optimization dialog ([3069354](https://github.com/anddea/revanced-patches/commit/3069354e5e736dbbebc1a85cdde2762a436f1338))
* **Hide ads:** Add `Close fullscreen ads` settings ([a9fc181](https://github.com/anddea/revanced-patches/commit/a9fc181507e8e56f9d29e1ab97061fec1455100a))
* **Reddit/Settings for Reddit:** add patch option `RVX settings menu name` ([5d51d81](https://github.com/anddea/revanced-patches/commit/5d51d815a7d4948f17ee4133d818492cb406f61d))
* **Reddit:** Add `Change version code` patch ([ec7da6d](https://github.com/anddea/revanced-patches/commit/ec7da6d5c7f46cfc51a6b7121e712d1db3670b13))
* **Reddit:** Add `Hide recommended communities shelf` patch ([3a07f0a](https://github.com/anddea/revanced-patches/commit/3a07f0a379e500b03dcdb265136d79360fc313b6))
* **YouTube - Custom branding icon:** Add `MMT Blue`, `MMT Green`, `MMT Yellow` ([941216e](https://github.com/anddea/revanced-patches/commit/941216eff18351580ac665fe49300432729aba26))
* **YouTube - Custom branding icon:** Add new splash animations for all `Revancify` icons ([#629](https://github.com/anddea/revanced-patches/issues/629)) ([2d2e504](https://github.com/anddea/revanced-patches/commit/2d2e5045c94f409559e46f6871eabb64407d9443))
* **YouTube - Custom branding icon:** Add splash animation for `Revancify Blue`, `Revancify Red` and `Revancify Yellow` ([95fed8c](https://github.com/anddea/revanced-patches/commit/95fed8c986966f187e016d56d1c969db0ce9141b))
* **YouTube - Custom branding icon:** Add splash animations for AFN Blue and AFN Red ([f5c0178](https://github.com/anddea/revanced-patches/commit/f5c0178befc0d38998bd7f9d660d1147786bf549))
* **YouTube - Hide player flyout menu:** Add `Hide quality menu header` ([99af80b](https://github.com/anddea/revanced-patches/commit/99af80be0c5d84433cae983fa694e6ad48906e4a))
* **YouTube - Miniplayer:** Add `Enable drag and drop` setting (YouTube 19.23.40+) ([fa0ac0f](https://github.com/anddea/revanced-patches/commit/fa0ac0fc5dd710ed50f232c67820527e23cd9838))
* **YouTube - Navigation bar components:** Add `Enable translucent navigation bar` setting ([e9ff3c9](https://github.com/anddea/revanced-patches/commit/e9ff3c9aa61ea0d4090e02e1fe9722d96cee9841))
* **YouTube - Overlay buttons:** Add content descriptions to overlay buttons for improved accessibility ([4d61417](https://github.com/anddea/revanced-patches/commit/4d6141776c6255bf4fc31173dbedd4fe9cec188f))
* **YouTube - Searchbar:** Complete restyle ([#652](https://github.com/anddea/revanced-patches/issues/652)) ([8458e1a](https://github.com/anddea/revanced-patches/commit/8458e1ae561622d2a49a4dc3e71bb9157f2e153e))
* **YouTube - Seekbar components:** Add `Enable Cairo seekbar` setting (YouTube 19.23.40+) ([95ed714](https://github.com/anddea/revanced-patches/commit/95ed714c4ef0bf3c88ab534ebb00e7b6e83b41d4))
* **YouTube - Shorts components:** Add `Hide Super Thanks button` setting ([b0482c7](https://github.com/anddea/revanced-patches/commit/b0482c73271a8cae1c7da83153c935c061ab1431))
* **YouTube - Shorts overlay buttons:** Update `Outline` and `OutlineCircle` icons ([527681c](https://github.com/anddea/revanced-patches/commit/527681c2572231dd15660e765c7408efd5cef4f1))
* **YouTube - Spoof client:** Add `Show in Stats for nerds` setting ([b287a61](https://github.com/anddea/revanced-patches/commit/b287a61e480027689ea2ef792a84dcf1c9b002ab))
* **YouTube - Spoof client:** Selectively spoof client for general video / livestreams / Shorts / fallback (unplayable video) ([d92de62](https://github.com/anddea/revanced-patches/commit/d92de62fc05ba4df1343ddf270e78bd3f4e53e58))
* **YouTube - Start page:** Add `Playables` and `Courses / Learning` ([6c2c5b0](https://github.com/anddea/revanced-patches/commit/6c2c5b0060261dfaee47bf6c359b8db2ce9ce7ac))
* **YouTube - Translations:** Remove unselected languages from UI ([b8d3946](https://github.com/anddea/revanced-patches/commit/b8d3946094caca123a6661ac17b01c28792683f1))
* **YouTube - Translations:** Update `Ukrainian` ([#611](https://github.com/anddea/revanced-patches/issues/611)) ([dab2892](https://github.com/anddea/revanced-patches/commit/dab2892ca34dad17b5e21ce8a0516c9ac75897b9))
* **YouTube Music - Custom branding icon:** Add `MMT Blue`, `MMT Green` and `MMT Yellow` ([27b8aa7](https://github.com/anddea/revanced-patches/commit/27b8aa779642ef979ebd9448943cb8931dde647d))
* **YouTube Music/Settings for YouTube Music:** add patch option `RVX settings menu name` ([e62e2d3](https://github.com/anddea/revanced-patches/commit/e62e2d375175bf40aac8135b40b554aa69466c65))
* **YouTube Music:** Add `Enable Cairo splash animation` patch (YouTube Music 7.06.53+) ([a09dd4c](https://github.com/anddea/revanced-patches/commit/a09dd4c47e6bebb50dfa35b7da0b1b76d073d8bc))
* **YouTube Music:** Add `Visual preferences icons` ([8672b10](https://github.com/anddea/revanced-patches/commit/8672b1001a3ccd34102261738cdeb031f97cf572))
* **YouTube Music:** Add support version `7.04.51` ([62b19b8](https://github.com/anddea/revanced-patches/commit/62b19b8ca17aab73852ef261458a31b27fbf6748))
* **YouTube Music:** add support version `7.05.52` ([91f9dd8](https://github.com/anddea/revanced-patches/commit/91f9dd8db139c16ff573702d54cb7c40315b3c5f))
* **YouTube Music:** add support version `7.05.53` ([9c1c0c9](https://github.com/anddea/revanced-patches/commit/9c1c0c9a65de4738fe505ece707386dbf83885ef))
* **YouTube Music:** Add support versions `7.02.52` ~ `7.03.51` ([40df716](https://github.com/anddea/revanced-patches/commit/40df7164f7e465d03744b3a7e24fd8f9f1ac3584))
* **YouTube Music:** Add support versions `7.05.54` ~ `7.06.53` ([cc2bd1f](https://github.com/anddea/revanced-patches/commit/cc2bd1f060ee5c0282baaeedf64ba3473d50c4c2))
* **YouTube Music:** Remove `Replace Cast button` patch ([b65a118](https://github.com/anddea/revanced-patches/commit/b65a118ffab3af71822295b746b332602f2c5373))
* **YouTube/Hide action buttons:** add `Disable Like and Dislike button glow` setting ([34d8bb1](https://github.com/anddea/revanced-patches/commit/34d8bb1abe32d3e56cd220ba0cf52555a85fabfe))
* **YouTube:** Add `Enable OPUS codec` patch ([f4684df](https://github.com/anddea/revanced-patches/commit/f4684dfd3d46eb19844761ff7818fa691cdbc543))
* **YouTube:** add `Miniplayer` patch (Replaces `Enable tablet mini player` patch) ([0e873ba](https://github.com/anddea/revanced-patches/commit/0e873bac39b5aba5072001744c8e9920b45a4d23))
* **YouTube:** Add support version `19.20.35` ([45b5c5d](https://github.com/anddea/revanced-patches/commit/45b5c5dcb5cf7c6880ee4872a994373640e5b637))
* **YouTube:** add support version `19.21.40` ([5dfb419](https://github.com/anddea/revanced-patches/commit/5dfb4194030df04b163f017071489f6d22f31f46))
* **YouTube:** Add support version `19.23.40` ([19a14cd](https://github.com/anddea/revanced-patches/commit/19a14cd5d24c8867e1ee4c131f1e7a51763a1f11))
* **YouTube:** Remove `Spoof format stream data` patch ([35aeec3](https://github.com/anddea/revanced-patches/commit/35aeec3e12c8adb64dcfe1396980b9722cf4ccaa))

# [2.226.0-dev.20](https://github.com/anddea/revanced-patches/compare/v2.226.0-dev.19...v2.226.0-dev.20) (2024-06-26)


### Features

* **YouTube - Shorts overlay buttons:** Update `Outline` and `OutlineCircle` icons ([527681c](https://github.com/anddea/revanced-patches/commit/527681c2572231dd15660e765c7408efd5cef4f1))

# [2.226.0-dev.19](https://github.com/anddea/revanced-patches/compare/v2.226.0-dev.18...v2.226.0-dev.19) (2024-06-26)


### Features

* **YouTube - Searchbar:** Complete restyle ([#652](https://github.com/anddea/revanced-patches/issues/652)) ([8458e1a](https://github.com/anddea/revanced-patches/commit/8458e1ae561622d2a49a4dc3e71bb9157f2e153e))

# [2.226.0-dev.18](https://github.com/anddea/revanced-patches/compare/v2.226.0-dev.17...v2.226.0-dev.18) (2024-06-24)


### Bug Fixes

* **Settings:** Failed with custom RVXSettingsMenuName ([c0d3ed0](https://github.com/anddea/revanced-patches/commit/c0d3ed0b377f018913d217962088e935084c167c))
* **YouTube Music - Visual preferences icons:** Add missing custom branding icons ([1071471](https://github.com/anddea/revanced-patches/commit/10714717b69710f7c5eb364cb7d0a604c00dae55))

# [2.226.0-dev.17](https://github.com/anddea/revanced-patches/compare/v2.226.0-dev.16...v2.226.0-dev.17) (2024-06-23)


### Bug Fixes

* **GmsCore support:** Spoof package name ([9c4e70c](https://github.com/anddea/revanced-patches/commit/9c4e70cf896e1ca9efc6c9b3766eca7c6869a413))
* **Reddit - Settings:** Patch option `RVX settings menu name` does not apply to header in ReVanced Extended settings ([4827a0f](https://github.com/anddea/revanced-patches/commit/4827a0fa0b1ee8f576fa1020f9a0378b79cdf9cc))
* **YouTube - Miniplayer:** `Hide expand and close buttons` setting is not disabled in `Modern 1` on YouTube 19.20.35+ ([b7c330a](https://github.com/anddea/revanced-patches/commit/b7c330ace90f9761cd06322eb0840b593d1e26d1))
* **YouTube - Spoof client:** Seekbar thumbnail not shown in `Android Testsuite` client ([74d50b1](https://github.com/anddea/revanced-patches/commit/74d50b1b5ad5860a4f341039e953a7c38b457afb))
* **YouTube:** `Hide animated button background` patch doesn't work ([37fac01](https://github.com/anddea/revanced-patches/commit/37fac01c5b173fbbdfa9c44043b086836cbb36cd))


### Features

* **Reddit:** Add `Change version code` patch ([ec7da6d](https://github.com/anddea/revanced-patches/commit/ec7da6d5c7f46cfc51a6b7121e712d1db3670b13))
* **Reddit:** Add `Hide recommended communities shelf` patch ([3a07f0a](https://github.com/anddea/revanced-patches/commit/3a07f0a379e500b03dcdb265136d79360fc313b6))
* **YouTube - Miniplayer:** Add `Enable drag and drop` setting (YouTube 19.23.40+) ([fa0ac0f](https://github.com/anddea/revanced-patches/commit/fa0ac0fc5dd710ed50f232c67820527e23cd9838))
* **YouTube - Navigation bar components:** Add `Enable translucent navigation bar` setting ([e9ff3c9](https://github.com/anddea/revanced-patches/commit/e9ff3c9aa61ea0d4090e02e1fe9722d96cee9841))
* **YouTube - Seekbar components:** Add `Enable Cairo seekbar` setting (YouTube 19.23.40+) ([95ed714](https://github.com/anddea/revanced-patches/commit/95ed714c4ef0bf3c88ab534ebb00e7b6e83b41d4))
* **YouTube Music:** Add `Enable Cairo splash animation` patch (YouTube Music 7.06.53+) ([a09dd4c](https://github.com/anddea/revanced-patches/commit/a09dd4c47e6bebb50dfa35b7da0b1b76d073d8bc))
* **YouTube Music:** Add support versions `7.05.54` ~ `7.06.53` ([cc2bd1f](https://github.com/anddea/revanced-patches/commit/cc2bd1f060ee5c0282baaeedf64ba3473d50c4c2))
* **YouTube:** Add support version `19.23.40` ([19a14cd](https://github.com/anddea/revanced-patches/commit/19a14cd5d24c8867e1ee4c131f1e7a51763a1f11))

# [2.226.0-dev.16](https://github.com/anddea/revanced-patches/compare/v2.226.0-dev.15...v2.226.0-dev.16) (2024-06-21)


### Features

* **YouTube - Custom branding icon:** Add new splash animations for all `Revancify` icons ([#629](https://github.com/anddea/revanced-patches/issues/629)) ([2d2e504](https://github.com/anddea/revanced-patches/commit/2d2e5045c94f409559e46f6871eabb64407d9443))

# [2.226.0-dev.15](https://github.com/anddea/revanced-patches/compare/v2.226.0-dev.14...v2.226.0-dev.15) (2024-06-19)


### Features

* **YouTube Music - Custom branding icon:** Add `MMT Blue`, `MMT Green` and `MMT Yellow` ([27b8aa7](https://github.com/anddea/revanced-patches/commit/27b8aa779642ef979ebd9448943cb8931dde647d))

# [2.226.0-dev.14](https://github.com/anddea/revanced-patches/compare/v2.226.0-dev.13...v2.226.0-dev.14) (2024-06-18)


### Features

* **YouTube - Custom branding icon:** Add splash animation for `Revancify Blue`, `Revancify Red` and `Revancify Yellow` ([95fed8c](https://github.com/anddea/revanced-patches/commit/95fed8c986966f187e016d56d1c969db0ce9141b))

# [2.226.0-dev.13](https://github.com/anddea/revanced-patches/compare/v2.226.0-dev.12...v2.226.0-dev.13) (2024-06-17)


### Features

* **YouTube - Translations:** Update `Ukrainian` ([#611](https://github.com/anddea/revanced-patches/issues/611)) ([dab2892](https://github.com/anddea/revanced-patches/commit/dab2892ca34dad17b5e21ce8a0516c9ac75897b9))

# [2.226.0-dev.12](https://github.com/anddea/revanced-patches/compare/v2.226.0-dev.11...v2.226.0-dev.12) (2024-06-17)


### Features

* **YouTube - Custom branding icon:** Add `MMT Blue`, `MMT Green`, `MMT Yellow` ([941216e](https://github.com/anddea/revanced-patches/commit/941216eff18351580ac665fe49300432729aba26))
* **YouTube - Start page:** Add `Playables` and `Courses / Learning` ([6c2c5b0](https://github.com/anddea/revanced-patches/commit/6c2c5b0060261dfaee47bf6c359b8db2ce9ce7ac))

# [2.226.0-dev.11](https://github.com/anddea/revanced-patches/compare/v2.226.0-dev.10...v2.226.0-dev.11) (2024-06-14)


### Bug Fixes

* **YouTube - Translations:** Removed some languages from UI ([93279da](https://github.com/anddea/revanced-patches/commit/93279da02684887dbb1f25668f64f49f2ca55391))

# [2.226.0-dev.10](https://github.com/anddea/revanced-patches/compare/v2.226.0-dev.9...v2.226.0-dev.10) (2024-06-14)


### Features

* **YouTube - Custom branding icon:** Add splash animations for AFN Blue and AFN Red ([f5c0178](https://github.com/anddea/revanced-patches/commit/f5c0178befc0d38998bd7f9d660d1147786bf549))

# [2.226.0-dev.9](https://github.com/anddea/revanced-patches/compare/v2.226.0-dev.8...v2.226.0-dev.9) (2024-06-14)


### Bug Fixes

* **Hide ads:** app crashes in the old client ([2a61d08](https://github.com/anddea/revanced-patches/commit/2a61d08cb59d76115d0f0ddd7deecc8e02a245dc))
* **YouTube/Overlay buttons:** in devices that do not use `xxhdpi`, some buttons are not replaced correctly ([030b2f6](https://github.com/anddea/revanced-patches/commit/030b2f6fa43ce280266ce45a2f2272aa4d1c1eda))


### Features

* **Reddit/Settings for Reddit:** add patch option `RVX settings menu name` ([5d51d81](https://github.com/anddea/revanced-patches/commit/5d51d815a7d4948f17ee4133d818492cb406f61d))
* **YouTube Music/Settings for YouTube Music:** add patch option `RVX settings menu name` ([e62e2d3](https://github.com/anddea/revanced-patches/commit/e62e2d375175bf40aac8135b40b554aa69466c65))
* **YouTube Music:** add support version `7.05.52` ([91f9dd8](https://github.com/anddea/revanced-patches/commit/91f9dd8db139c16ff573702d54cb7c40315b3c5f))
* **YouTube Music:** add support version `7.05.53` ([9c1c0c9](https://github.com/anddea/revanced-patches/commit/9c1c0c9a65de4738fe505ece707386dbf83885ef))
* **YouTube/Hide action buttons:** add `Disable Like and Dislike button glow` setting ([34d8bb1](https://github.com/anddea/revanced-patches/commit/34d8bb1abe32d3e56cd220ba0cf52555a85fabfe))
* **YouTube:** add `Miniplayer` patch (Replaces `Enable tablet mini player` patch) ([0e873ba](https://github.com/anddea/revanced-patches/commit/0e873bac39b5aba5072001744c8e9920b45a4d23))
* **YouTube:** add support version `19.21.40` ([5dfb419](https://github.com/anddea/revanced-patches/commit/5dfb4194030df04b163f017071489f6d22f31f46))

# [2.226.0-dev.8](https://github.com/anddea/revanced-patches/compare/v2.226.0-dev.7...v2.226.0-dev.8) (2024-06-12)


### Features

* **YouTube - Translations:** Remove unselected languages from UI ([b8d3946](https://github.com/anddea/revanced-patches/commit/b8d3946094caca123a6661ac17b01c28792683f1))

# [2.226.0-dev.7](https://github.com/anddea/revanced-patches/compare/v2.226.0-dev.6...v2.226.0-dev.7) (2024-06-11)


### Bug Fixes

* **Custom branding icon:** Failed because of missing resources ([5e0723e](https://github.com/anddea/revanced-patches/commit/5e0723e52856a93f06f798b1fc18e18bbfa56490))

# [2.226.0-dev.6](https://github.com/anddea/revanced-patches/compare/v2.226.0-dev.5...v2.226.0-dev.6) (2024-06-11)


### Bug Fixes

* **YouTube - Spoof client:** Restore playback speed menu when spoofing to an iOS, Android TV, Android Testsuite client ([06ffb15](https://github.com/anddea/revanced-patches/commit/06ffb15d8bf5b91fa98910d975d3ad887e609789))


### Features

* **Hide ads:** Add `Close fullscreen ads` settings ([a9fc181](https://github.com/anddea/revanced-patches/commit/a9fc181507e8e56f9d29e1ab97061fec1455100a))
* **YouTube - Hide player flyout menu:** Add `Hide quality menu header` ([99af80b](https://github.com/anddea/revanced-patches/commit/99af80be0c5d84433cae983fa694e6ad48906e4a))
* **YouTube - Shorts components:** Add `Hide Super Thanks button` setting ([b0482c7](https://github.com/anddea/revanced-patches/commit/b0482c73271a8cae1c7da83153c935c061ab1431))
* **YouTube Music:** Add support version `7.04.51` ([62b19b8](https://github.com/anddea/revanced-patches/commit/62b19b8ca17aab73852ef261458a31b27fbf6748))
* **YouTube Music:** Remove `Replace Cast button` patch ([b65a118](https://github.com/anddea/revanced-patches/commit/b65a118ffab3af71822295b746b332602f2c5373))
* **YouTube:** Add `Enable OPUS codec` patch ([f4684df](https://github.com/anddea/revanced-patches/commit/f4684dfd3d46eb19844761ff7818fa691cdbc543))
* **YouTube:** Add support version `19.20.35` ([45b5c5d](https://github.com/anddea/revanced-patches/commit/45b5c5dcb5cf7c6880ee4872a994373640e5b637))

# [2.226.0-dev.5](https://github.com/anddea/revanced-patches/compare/v2.226.0-dev.4...v2.226.0-dev.5) (2024-06-05)


### Features

* **GmsCore support:** Add `Don't show again` option for battery optimization dialog ([3069354](https://github.com/anddea/revanced-patches/commit/3069354e5e736dbbebc1a85cdde2762a436f1338))

# [2.226.0-dev.4](https://github.com/anddea/revanced-patches/compare/v2.226.0-dev.3...v2.226.0-dev.4) (2024-06-01)


### Bug Fixes

* **YouTube - Custom branding icon:** Add `Revancify Yellow` to patch options ([aea7060](https://github.com/anddea/revanced-patches/commit/aea70604ae326ec86ddec5b37d03a06673292545))

# [2.226.0-dev.3](https://github.com/anddea/revanced-patches/compare/v2.226.0-dev.2...v2.226.0-dev.3) (2024-06-01)


### Features

* **Custom branding icon:** Add `Revancify Yellow` icon ([#533](https://github.com/anddea/revanced-patches/issues/533)) ([d3bdd97](https://github.com/anddea/revanced-patches/commit/d3bdd97c8afc205fca85627740d3da498afb2121))
* **YouTube - Overlay buttons:** Add content descriptions to overlay buttons for improved accessibility ([4d61417](https://github.com/anddea/revanced-patches/commit/4d6141776c6255bf4fc31173dbedd4fe9cec188f))
* **YouTube Music:** Add `Visual preferences icons` ([8672b10](https://github.com/anddea/revanced-patches/commit/8672b1001a3ccd34102261738cdeb031f97cf572))

# [2.226.0-dev.2](https://github.com/anddea/revanced-patches/compare/v2.226.0-dev.1...v2.226.0-dev.2) (2024-05-31)


### Bug Fixes

* **YouTube - Visual preferences icons:** Typo in QUIC ([409e283](https://github.com/anddea/revanced-patches/commit/409e2831b3c1ab9db1023e5f6257d4d4d327a592))

# [2.226.0-dev.1](https://github.com/anddea/revanced-patches/compare/v2.225.0...v2.226.0-dev.1) (2024-05-31)


### Bug Fixes

* **YouTube - Settings:** `CairoSettings` is applied due to A/B testing ([bad0f92](https://github.com/anddea/revanced-patches/commit/bad0f929853890eaac7a21a4faaad5cf43077f74))
* **YouTube - Spoof client:** Player gestures not working when spoofing with `Android VR` client ([0cdc4f3](https://github.com/anddea/revanced-patches/commit/0cdc4f33c98cc16f5a302f2ca46a93434165bfd3))


### Features

* **YouTube - Spoof client:** Add `Show in Stats for nerds` setting ([b287a61](https://github.com/anddea/revanced-patches/commit/b287a61e480027689ea2ef792a84dcf1c9b002ab))
* **YouTube - Spoof client:** Selectively spoof client for general video / livestreams / Shorts / fallback (unplayable video) ([d92de62](https://github.com/anddea/revanced-patches/commit/d92de62fc05ba4df1343ddf270e78bd3f4e53e58))
* **YouTube Music:** Add support versions `7.02.52` ~ `7.03.51` ([40df716](https://github.com/anddea/revanced-patches/commit/40df7164f7e465d03744b3a7e24fd8f9f1ac3584))
* **YouTube:** Remove `Spoof format stream data` patch ([35aeec3](https://github.com/anddea/revanced-patches/commit/35aeec3e12c8adb64dcfe1396980b9722cf4ccaa))

# [2.225.0](https://github.com/anddea/revanced-patches/compare/v2.224.0...v2.225.0) (2024-05-29)


### Bug Fixes

* Patching failed because of RVX CLI incompatibility ([922d364](https://github.com/anddea/revanced-patches/commit/922d36408b74dc7f9b085e656564df9f01a406ab))
* **Reddit - Hide toolbar button:** Latest version support information ([#378](https://github.com/anddea/revanced-patches/issues/378)) ([0c0752d](https://github.com/anddea/revanced-patches/commit/0c0752d6df50549578dcd0b3ea5c0288e8a6d8d0))
* **YouTube - Custom Branding Icon:** Add missing strings to prevent patch failure ([84e2cb5](https://github.com/anddea/revanced-patches/commit/84e2cb52c250fb6c2fb69bc3a0462ca880a8c9a5))
* **YouTube - Default video quality:** Fix `Skip preloaded buffer` ([fb54626](https://github.com/anddea/revanced-patches/commit/fb54626ee6c0d7b7033b796b5317f0abdd299893))
* **YouTube - Disable splash animation:** Did not work on YouTube v19.17.41+ ([d2182be](https://github.com/anddea/revanced-patches/commit/d2182be8e8feeb2640fe9aea1e90b7a265aa0d3b))
* **YouTube - Hide ads:** `Hide ads` patch does not work on YouTube 18.29.38 ([c150ab2](https://github.com/anddea/revanced-patches/commit/c150ab2be85aa56de28cbff9f32eed2a69fa8688))
* **YouTube - Hide Shorts components:** Exclude `Hide sound metadata label` and `Hide video link label` from `Hide info panel` ([4f5bdcc](https://github.com/anddea/revanced-patches/commit/4f5bdccacee834782525fdcc9367ed39106fa4a8))
* **YouTube - Return YouTube Dislike:** Dislike count sometimes not shown in Shorts ([d821384](https://github.com/anddea/revanced-patches/commit/d82138474e4a544deb5fc37ddd77b87e35b14bbe))
* **YouTube - Return YouTube Dislike:** Fix some RYD issues ([32d6c25](https://github.com/anddea/revanced-patches/commit/32d6c254ffffbc8b09bbf45890cd39488f396123))
* **YouTube - Settings Custom Name:** Fix resource files not found ([#416](https://github.com/anddea/revanced-patches/issues/416)) ([cc4566a](https://github.com/anddea/revanced-patches/commit/cc4566a5499ce7fbe95c82d4c5d27e12fc671c1b))
* **YouTube - SponsorBlock:** `Submit a new segment` button didn't work ([c7e39b3](https://github.com/anddea/revanced-patches/commit/c7e39b3312b4863085eddf6534736c40afd3691a))
* **YouTube - Spoof format stream data:** Check audio tags first ([5589897](https://github.com/anddea/revanced-patches/commit/5589897793d6a6f7e34980ed9b32d051267af48d))
* **YouTube - Spoof format stream data:** Incorrect url is used ([928b459](https://github.com/anddea/revanced-patches/commit/928b459fbd98ed5cc3b63260caef268c882f50a9))
* **YouTube - Spoof format stream data:** Some Uris are not hooked ([e682654](https://github.com/anddea/revanced-patches/commit/e6826546533a2c1adb767b29875663291362fab0))
* **YouTube - Spoof test client:** Spoofing does not work correctly when the `Spoof app version` setting is turned on ([33292a0](https://github.com/anddea/revanced-patches/commit/33292a07279602ab7f8e35fd9a23c5172a4b9b9a))
* **Youtube - Tablet Miniplayer:** Broken controls ([#386](https://github.com/anddea/revanced-patches/issues/386)) ([1b60113](https://github.com/anddea/revanced-patches/commit/1b601130b5bcb3dfa03f590fbc512dbf6a4b841d))
* **YouTube - Translations:** Remove duplicates caused error ([5e904c0](https://github.com/anddea/revanced-patches/commit/5e904c005cb881e8113eb7d41a87c7e738620eb6))
* **YouTube - Video playback:** Default video quality does not apply to Shorts ([eb53500](https://github.com/anddea/revanced-patches/commit/eb53500fb6dc5f07bb36bb6abf531ee61545419f))
* **YouTube Music - Hide ads:** `Hide fullscreen ads` setting also closes non-ad dialogs ([8d03e3f](https://github.com/anddea/revanced-patches/commit/8d03e3f99d40410def975db79761bfb00c56fa01))
* **YouTube:** `Ambient mode control` patch does not work correctly on certain versions ([0f9f79e](https://github.com/anddea/revanced-patches/commit/0f9f79e5c4e7645ae9d788f92a966b59043b5c52))
* **YouTube:** Fix app crashing for some people by reverting fix Shorts looping for some people ([a6f5dd5](https://github.com/anddea/revanced-patches/commit/a6f5dd5d997bd25c8bac130ed5135e57fc74bd9f))
* **YouTube:** Separate `Hide info panels` and `Hide Shorts info panels` ([808d3fb](https://github.com/anddea/revanced-patches/commit/808d3fb89cb27a2d317be2d1f54933aef95b32dc))
* **YouTube:** Speed toast showed fixed value on reset ([2aae719](https://github.com/anddea/revanced-patches/commit/2aae7197aa517f36f0d614b7b419382191d9f1e8))
* **YT Music - Translations:** Use correct resource directory ([78667fa](https://github.com/anddea/revanced-patches/commit/78667fa54654fc68d26042c45cdbb9f5f0ccc187))


### Features

* Refactor and match ReVanced and inotia ([5b4dca0](https://github.com/anddea/revanced-patches/commit/5b4dca03970f887f6caaf3da072ff77276754a61))
* **YouTube - Collapse Button:** Hide collapse button margin ([#415](https://github.com/anddea/revanced-patches/issues/415)) ([6419978](https://github.com/anddea/revanced-patches/commit/6419978ce76fa48cbf157bf4a752af7c291a63c8))
* **YouTube - Force snackbar theme:** Add options for corner radius, background and stroke colors ([eafb7b2](https://github.com/anddea/revanced-patches/commit/eafb7b222c2a39ff2a301a3975838e3e675529a8))
* **YouTube - Fullscreen components:** `Hide related videos in quick actions` setting has been merged into the `Hide related video overlay` setting ([bdcee27](https://github.com/anddea/revanced-patches/commit/bdcee27c001b3e738c7c7c7dde3c7c5888be14d7))
* **YouTube - Hide layout components:** Add `Hide videos based on duration` ([ebd6955](https://github.com/anddea/revanced-patches/commit/ebd69550544d5446bdd98a4257912bd18ec478ef))
* **YouTube - Hide layout components:** Add an option to hide videos with views greater than specified value ([98c2f99](https://github.com/anddea/revanced-patches/commit/98c2f9942dd03f48aee00013307533430306152f))
* **YouTube - Hide shorts components:** Add separate option to hide in player ([7b60297](https://github.com/anddea/revanced-patches/commit/7b60297b0ed5096a9f7533837691d96df4809630))
* **YouTube - Hide shorts components:** Hide comments button if disabled or with label "0" ([299e20b](https://github.com/anddea/revanced-patches/commit/299e20b774408d6f8ec9dc3c6e4274cf43db0bc5))
* **YouTube - Overlay buttons:** Add `Rounded` (old curvy) buttons ([73ab950](https://github.com/anddea/revanced-patches/commit/73ab950ce251e673860826cf32239c451de64122))
* **YouTube - Overlay buttons:** Add `Whitelist` overlay button ([358af9e](https://github.com/anddea/revanced-patches/commit/358af9ee3914bb6912c8f56934cc0d8418a52ce8))
* **YouTube - Player components:** Add setting toggle for `Hide suggested video end screen` ([f4a9c22](https://github.com/anddea/revanced-patches/commit/f4a9c221e128e399e9667b3474d542b84ba9252e))
* **YouTube - Settings:** Add custom name for Extended preference ([#389](https://github.com/anddea/revanced-patches/issues/389)) ([8804b92](https://github.com/anddea/revanced-patches/commit/8804b924ac5db841e458e62cfe69be8424e3b968))
* **YouTube - Settings:** Add search bar for settings ([85cf0c0](https://github.com/anddea/revanced-patches/commit/85cf0c016a20b4fb67c0a7aa1176fdce87d3ad48))
* **YouTube - Spoof format stream data:** improve hook method, fetch to `ANDROID_TESTSUITE` client ([5a066dd](https://github.com/anddea/revanced-patches/commit/5a066ddafcfa459e5b2870bf4f307292e490a070))
* **YouTube - Toolbar components:** Remove `Hide trending searches` setting ([5d2e732](https://github.com/anddea/revanced-patches/commit/5d2e732b20f19fb95b9f91fe9d3409395543586c))
* **YouTube - Translations:** Add compile time option to select app languages ([6356ddc](https://github.com/anddea/revanced-patches/commit/6356ddc5d082c17f39b4f5cf92c2a31a5295aeed))
* **YouTube - Translations:** Add compile time option to select custom language file ([a65679e](https://github.com/anddea/revanced-patches/commit/a65679e2c345065b7fa96fa62f0901085cd3472d))
* **YouTube - Translations:** Add compile time option to select languages ([046a05e](https://github.com/anddea/revanced-patches/commit/046a05e5697b8a7f531e709803e9cb5709e5bbc3))
* **YouTube - Translations:** Update `Chinese Simplified` ([#471](https://github.com/anddea/revanced-patches/issues/471)) ([eb2fe8e](https://github.com/anddea/revanced-patches/commit/eb2fe8ec894ec6d0397ad06bbe7ad71f554dafcd))
* **YouTube - Translations:** Update `Chinese Simplified` ([#492](https://github.com/anddea/revanced-patches/issues/492)) ([e1113d9](https://github.com/anddea/revanced-patches/commit/e1113d91cc8af769988fc3a8c2df3062b06a7f3b))
* **YouTube - Translations:** Update `Chinese Simplified` ([#503](https://github.com/anddea/revanced-patches/issues/503)) ([ee4324b](https://github.com/anddea/revanced-patches/commit/ee4324bf39dfd2eca62044461cb3d7e845ab17d7))
* **YouTube - Translations:** Update `Chinese Traditional` ([#501](https://github.com/anddea/revanced-patches/issues/501)) ([0167c5d](https://github.com/anddea/revanced-patches/commit/0167c5d01d5974af16fc4d2a3fe3a5d0f3a5018d))
* **YouTube - Translations:** Update `Greek` ([#374](https://github.com/anddea/revanced-patches/issues/374)) ([5af0858](https://github.com/anddea/revanced-patches/commit/5af085861592182d23cfe5a451a41a5b70e9f908))
* **YouTube - Translations:** Update `Japanese` ([#457](https://github.com/anddea/revanced-patches/issues/457)) ([b993a5d](https://github.com/anddea/revanced-patches/commit/b993a5dc39380bb54e1b1dcac76cb94a2fa58394))
* **YouTube - Translations:** Update `Japanese` ([#467](https://github.com/anddea/revanced-patches/issues/467)) ([860073b](https://github.com/anddea/revanced-patches/commit/860073b48384dfcbedaf8de7e4f36261fa3e436f))
* **YouTube - Translations:** Update `Japanese` ([#504](https://github.com/anddea/revanced-patches/issues/504)) ([39a1863](https://github.com/anddea/revanced-patches/commit/39a1863cedbdc700fb70b38dd1e04475c539fdee))
* **YouTube - Translations:** Update `Korean` ([#464](https://github.com/anddea/revanced-patches/issues/464)) ([0770f09](https://github.com/anddea/revanced-patches/commit/0770f0944be9cc6bf26cd1218e056dade2990b83))
* **YouTube - Translations:** Update `Spanish` ([#436](https://github.com/anddea/revanced-patches/issues/436)) ([6e384d3](https://github.com/anddea/revanced-patches/commit/6e384d39ea8472b8f103045bb93f7e7bba80c668))
* **YouTube - Translations:** Update `Ukrainian` ([#400](https://github.com/anddea/revanced-patches/issues/400)) ([67c2895](https://github.com/anddea/revanced-patches/commit/67c289576459971d004e1cbb2d2b773918895d73))
* **YouTube - Translations:** Update `Ukrainian` ([#474](https://github.com/anddea/revanced-patches/issues/474)) ([70d9386](https://github.com/anddea/revanced-patches/commit/70d9386b3eb1a4774f9f3f2f394d3d02d29bea90))
* **YouTube - Translations:** Update `Ukrainian` ([#499](https://github.com/anddea/revanced-patches/issues/499)) ([2ec3c02](https://github.com/anddea/revanced-patches/commit/2ec3c02a4fd9a5280f621bfd065be53fe79a0a17))
* **YouTube - VIsual preferences icons:** Add icons to `Player buttons` ([a07b8ec](https://github.com/anddea/revanced-patches/commit/a07b8ecd1184cc54d5c8bb0ae316496e62b27a42))
* **YouTube - Visual preferences icons:** Added more icons ([9dd62ce](https://github.com/anddea/revanced-patches/commit/9dd62ceacaeca8ab1b0b7c7c221e2cebc19a563f))
* **YouTube Music - Player components:** Add `Enable black player background` settings ([c91be5a](https://github.com/anddea/revanced-patches/commit/c91be5a30b99bd46638f8a179c835c89e5c67a1f))
* **YouTube Music - Player components:** Add `Hide audio video switch toggle` setting ([9611f38](https://github.com/anddea/revanced-patches/commit/9611f38db290099f041eefe33896c423e1a1fecc))
* **YouTube Music:** Add support versions `7.01.53` - `7.02.51` ([71019ab](https://github.com/anddea/revanced-patches/commit/71019abba8c66f323b4cc948364bfa2a8d4c7fc8))
* **YouTube:** Add `Force disable Shorts dim` ([8a529ff](https://github.com/anddea/revanced-patches/commit/8a529ffe2efea270348d27d8c1970b02c12adea5))
* **YouTube:** Add `Force snackbar theme` ([2606187](https://github.com/anddea/revanced-patches/commit/2606187a0dca6fa3390a4d66931d70c927e893c9))
* **YouTube:** Add `Spoof client` patch ([5161192](https://github.com/anddea/revanced-patches/commit/5161192ba2fbd3b25e081e573bf4428ec0d1b6c1))
* **YouTube:** Add support versions `19.19.39` - `19.20.34` ([ee43c3e](https://github.com/anddea/revanced-patches/commit/ee43c3e25e1926e9963b9866bdb0c9e8bfe00a42))
* **YouTube:** Hide live chat replay button/label in fullscreen ([0cd9b96](https://github.com/anddea/revanced-patches/commit/0cd9b96c70a1eb5e17d9b3ee6ca81ce313a89eee))
* **YouTube:** Missing patches in the new RVX code ([#488](https://github.com/anddea/revanced-patches/issues/488)) ([3106111](https://github.com/anddea/revanced-patches/commit/310611110a198b7126b5f8037ccddbface263dc3))
* **YouTube:** Remove `Spoof test client` patch ([379e2ed](https://github.com/anddea/revanced-patches/commit/379e2ed903c537b674257251d71a132606ebf6b5))
* **YT Music - Translations:** Add compile time options to select custom language, RVX and app languages ([074d3e2](https://github.com/anddea/revanced-patches/commit/074d3e22c3a16e5c71ea6c7d514bfc11b4576406))


### Reverts

* default landscape mode timeout ([0d91401](https://github.com/anddea/revanced-patches/commit/0d914015accf4ef8a8d98fc607aead33620460ff))

# [2.225.0-dev.25](https://github.com/anddea/revanced-patches/compare/v2.225.0-dev.24...v2.225.0-dev.25) (2024-05-28)


### Bug Fixes

* **YouTube - Translations:** Remove duplicates caused error ([5e904c0](https://github.com/anddea/revanced-patches/commit/5e904c005cb881e8113eb7d41a87c7e738620eb6))

# [2.225.0-dev.24](https://github.com/anddea/revanced-patches/compare/v2.225.0-dev.23...v2.225.0-dev.24) (2024-05-28)


### Bug Fixes

* **YouTube - Disable splash animation:** Did not work on YouTube v19.17.41+ ([d2182be](https://github.com/anddea/revanced-patches/commit/d2182be8e8feeb2640fe9aea1e90b7a265aa0d3b))
* **YouTube - SponsorBlock:** `Submit a new segment` button didn't work ([c7e39b3](https://github.com/anddea/revanced-patches/commit/c7e39b3312b4863085eddf6534736c40afd3691a))


### Features

* **YouTube - Player components:** Add setting toggle for `Hide suggested video end screen` ([f4a9c22](https://github.com/anddea/revanced-patches/commit/f4a9c221e128e399e9667b3474d542b84ba9252e))
* **YouTube Music - Player components:** Add `Enable black player background` settings ([c91be5a](https://github.com/anddea/revanced-patches/commit/c91be5a30b99bd46638f8a179c835c89e5c67a1f))

# [2.225.0-dev.23](https://github.com/anddea/revanced-patches/compare/v2.225.0-dev.22...v2.225.0-dev.23) (2024-05-27)


### Bug Fixes

* **YT Music - Translations:** Use correct resource directory ([78667fa](https://github.com/anddea/revanced-patches/commit/78667fa54654fc68d26042c45cdbb9f5f0ccc187))

# [2.225.0-dev.22](https://github.com/anddea/revanced-patches/compare/v2.225.0-dev.21...v2.225.0-dev.22) (2024-05-27)


### Features

* **YouTube - Fullscreen components:** `Hide related videos in quick actions` setting has been merged into the `Hide related video overlay` setting ([bdcee27](https://github.com/anddea/revanced-patches/commit/bdcee27c001b3e738c7c7c7dde3c7c5888be14d7))
* **YouTube - Overlay buttons:** Add `Whitelist` overlay button ([358af9e](https://github.com/anddea/revanced-patches/commit/358af9ee3914bb6912c8f56934cc0d8418a52ce8))
* **YouTube - Toolbar components:** Remove `Hide trending searches` setting ([5d2e732](https://github.com/anddea/revanced-patches/commit/5d2e732b20f19fb95b9f91fe9d3409395543586c))
* **YouTube Music:** Add support versions `7.01.53` - `7.02.51` ([71019ab](https://github.com/anddea/revanced-patches/commit/71019abba8c66f323b4cc948364bfa2a8d4c7fc8))
* **YouTube:** Add `Spoof client` patch ([5161192](https://github.com/anddea/revanced-patches/commit/5161192ba2fbd3b25e081e573bf4428ec0d1b6c1))
* **YouTube:** Add support versions `19.19.39` - `19.20.34` ([ee43c3e](https://github.com/anddea/revanced-patches/commit/ee43c3e25e1926e9963b9866bdb0c9e8bfe00a42))
* **YouTube:** Remove `Spoof test client` patch ([379e2ed](https://github.com/anddea/revanced-patches/commit/379e2ed903c537b674257251d71a132606ebf6b5))

# [2.225.0-dev.21](https://github.com/anddea/revanced-patches/compare/v2.225.0-dev.20...v2.225.0-dev.21) (2024-05-21)


### Features

* **YouTube - Translations:** Update `Chinese Simplified` ([#503](https://github.com/anddea/revanced-patches/issues/503)) ([ee4324b](https://github.com/anddea/revanced-patches/commit/ee4324bf39dfd2eca62044461cb3d7e845ab17d7))
* **YouTube - Translations:** Update `Chinese Traditional` ([#501](https://github.com/anddea/revanced-patches/issues/501)) ([0167c5d](https://github.com/anddea/revanced-patches/commit/0167c5d01d5974af16fc4d2a3fe3a5d0f3a5018d))
* **YouTube - Translations:** Update `Japanese` ([#504](https://github.com/anddea/revanced-patches/issues/504)) ([39a1863](https://github.com/anddea/revanced-patches/commit/39a1863cedbdc700fb70b38dd1e04475c539fdee))

# [2.225.0-dev.20](https://github.com/anddea/revanced-patches/compare/v2.225.0-dev.19...v2.225.0-dev.20) (2024-05-20)


### Features

* **YouTube:** Missing patches in the new RVX code ([#488](https://github.com/anddea/revanced-patches/issues/488)) ([3106111](https://github.com/anddea/revanced-patches/commit/310611110a198b7126b5f8037ccddbface263dc3))

# [2.225.0-dev.19](https://github.com/anddea/revanced-patches/compare/v2.225.0-dev.18...v2.225.0-dev.19) (2024-05-20)


### Features

* **YouTube - Translations:** Update `Ukrainian` ([#499](https://github.com/anddea/revanced-patches/issues/499)) ([2ec3c02](https://github.com/anddea/revanced-patches/commit/2ec3c02a4fd9a5280f621bfd065be53fe79a0a17))

# [2.225.0-dev.18](https://github.com/anddea/revanced-patches/compare/v2.225.0-dev.17...v2.225.0-dev.18) (2024-05-20)


### Features

* **YouTube - Settings:** Add search bar for settings ([85cf0c0](https://github.com/anddea/revanced-patches/commit/85cf0c016a20b4fb67c0a7aa1176fdce87d3ad48))


### Reverts

* default landscape mode timeout ([0d91401](https://github.com/anddea/revanced-patches/commit/0d914015accf4ef8a8d98fc607aead33620460ff))

# [2.225.0-dev.17](https://github.com/anddea/revanced-patches/compare/v2.225.0-dev.16...v2.225.0-dev.17) (2024-05-19)


### Features

* **YouTube - Translations:** Update `Chinese Simplified` ([#492](https://github.com/anddea/revanced-patches/issues/492)) ([e1113d9](https://github.com/anddea/revanced-patches/commit/e1113d91cc8af769988fc3a8c2df3062b06a7f3b))

# [2.225.0-dev.16](https://github.com/anddea/revanced-patches/compare/v2.225.0-dev.15...v2.225.0-dev.16) (2024-05-18)


### Bug Fixes

* **YouTube - Custom Branding Icon:** Add missing strings to prevent patch failure ([84e2cb5](https://github.com/anddea/revanced-patches/commit/84e2cb52c250fb6c2fb69bc3a0462ca880a8c9a5))

# [2.225.0-dev.15](https://github.com/anddea/revanced-patches/compare/v2.225.0-dev.14...v2.225.0-dev.15) (2024-05-17)


### Features

* **YouTube - Translations:** Add compile time option to select app languages ([6356ddc](https://github.com/anddea/revanced-patches/commit/6356ddc5d082c17f39b4f5cf92c2a31a5295aeed))
* **YT Music - Translations:** Add compile time options to select custom language, RVX and app languages ([074d3e2](https://github.com/anddea/revanced-patches/commit/074d3e22c3a16e5c71ea6c7d514bfc11b4576406))

# [2.225.0-dev.14](https://github.com/anddea/revanced-patches/compare/v2.225.0-dev.13...v2.225.0-dev.14) (2024-05-16)


### Features

* **YouTube - Translations:** Update `Ukrainian` ([#474](https://github.com/anddea/revanced-patches/issues/474)) ([70d9386](https://github.com/anddea/revanced-patches/commit/70d9386b3eb1a4774f9f3f2f394d3d02d29bea90))

# [2.225.0-dev.13](https://github.com/anddea/revanced-patches/compare/v2.225.0-dev.12...v2.225.0-dev.13) (2024-05-16)


### Bug Fixes

* **YouTube:** Speed toast showed fixed value on reset ([2aae719](https://github.com/anddea/revanced-patches/commit/2aae7197aa517f36f0d614b7b419382191d9f1e8))

# [2.225.0-dev.12](https://github.com/anddea/revanced-patches/compare/v2.225.0-dev.11...v2.225.0-dev.12) (2024-05-16)


### Bug Fixes

* **YouTube - Hide ads:** `Hide ads` patch does not work on YouTube 18.29.38 ([c150ab2](https://github.com/anddea/revanced-patches/commit/c150ab2be85aa56de28cbff9f32eed2a69fa8688))
* **YouTube - Return YouTube Dislike:** Dislike count sometimes not shown in Shorts ([d821384](https://github.com/anddea/revanced-patches/commit/d82138474e4a544deb5fc37ddd77b87e35b14bbe))
* **YouTube - Spoof format stream data:** Check audio tags first ([5589897](https://github.com/anddea/revanced-patches/commit/5589897793d6a6f7e34980ed9b32d051267af48d))
* **YouTube - Spoof format stream data:** Incorrect url is used ([928b459](https://github.com/anddea/revanced-patches/commit/928b459fbd98ed5cc3b63260caef268c882f50a9))
* **YouTube - Spoof format stream data:** Some Uris are not hooked ([e682654](https://github.com/anddea/revanced-patches/commit/e6826546533a2c1adb767b29875663291362fab0))
* **YouTube - Spoof test client:** Spoofing does not work correctly when the `Spoof app version` setting is turned on ([33292a0](https://github.com/anddea/revanced-patches/commit/33292a07279602ab7f8e35fd9a23c5172a4b9b9a))
* **YouTube - Video playback:** Default video quality does not apply to Shorts ([eb53500](https://github.com/anddea/revanced-patches/commit/eb53500fb6dc5f07bb36bb6abf531ee61545419f))
* **YouTube Music - Hide ads:** `Hide fullscreen ads` setting also closes non-ad dialogs ([8d03e3f](https://github.com/anddea/revanced-patches/commit/8d03e3f99d40410def975db79761bfb00c56fa01))
* **YouTube:** `Ambient mode control` patch does not work correctly on certain versions ([0f9f79e](https://github.com/anddea/revanced-patches/commit/0f9f79e5c4e7645ae9d788f92a966b59043b5c52))


### Features

* **YouTube - Spoof format stream data:** improve hook method, fetch to `ANDROID_TESTSUITE` client ([5a066dd](https://github.com/anddea/revanced-patches/commit/5a066ddafcfa459e5b2870bf4f307292e490a070))
* **YouTube - Translations:** Update `Chinese Simplified` ([#471](https://github.com/anddea/revanced-patches/issues/471)) ([eb2fe8e](https://github.com/anddea/revanced-patches/commit/eb2fe8ec894ec6d0397ad06bbe7ad71f554dafcd))
* **YouTube - Translations:** Update `Japanese` ([#467](https://github.com/anddea/revanced-patches/issues/467)) ([860073b](https://github.com/anddea/revanced-patches/commit/860073b48384dfcbedaf8de7e4f36261fa3e436f))
* **YouTube - Translations:** Update `Korean` ([#464](https://github.com/anddea/revanced-patches/issues/464)) ([0770f09](https://github.com/anddea/revanced-patches/commit/0770f0944be9cc6bf26cd1218e056dade2990b83))
* **YouTube Music - Player components:** Add `Hide audio video switch toggle` setting ([9611f38](https://github.com/anddea/revanced-patches/commit/9611f38db290099f041eefe33896c423e1a1fecc))

# [2.225.0-dev.11](https://github.com/anddea/revanced-patches/compare/v2.225.0-dev.10...v2.225.0-dev.11) (2024-05-14)


### Features

* **YouTube - Translations:** Add compile time option to select custom language file ([a65679e](https://github.com/anddea/revanced-patches/commit/a65679e2c345065b7fa96fa62f0901085cd3472d))
* **YouTube - Translations:** Add compile time option to select languages ([046a05e](https://github.com/anddea/revanced-patches/commit/046a05e5697b8a7f531e709803e9cb5709e5bbc3))
* **YouTube - Translations:** Update `Greek` ([#374](https://github.com/anddea/revanced-patches/issues/374)) ([5af0858](https://github.com/anddea/revanced-patches/commit/5af085861592182d23cfe5a451a41a5b70e9f908))
* **YouTube - Translations:** Update `Japanese` ([#457](https://github.com/anddea/revanced-patches/issues/457)) ([b993a5d](https://github.com/anddea/revanced-patches/commit/b993a5dc39380bb54e1b1dcac76cb94a2fa58394))
* **YouTube - Translations:** Update `Spanish` ([#436](https://github.com/anddea/revanced-patches/issues/436)) ([6e384d3](https://github.com/anddea/revanced-patches/commit/6e384d39ea8472b8f103045bb93f7e7bba80c668))
* **YouTube - Translations:** Update `Ukrainian` ([#400](https://github.com/anddea/revanced-patches/issues/400)) ([67c2895](https://github.com/anddea/revanced-patches/commit/67c289576459971d004e1cbb2d2b773918895d73))
* **YouTube - VIsual preferences icons:** Add icons to `Player buttons` ([a07b8ec](https://github.com/anddea/revanced-patches/commit/a07b8ecd1184cc54d5c8bb0ae316496e62b27a42))

# [2.225.0-dev.10](https://github.com/anddea/revanced-patches/compare/v2.225.0-dev.9...v2.225.0-dev.10) (2024-05-12)


### Features

* Refactor and match ReVanced and inotia ([5b4dca0](https://github.com/anddea/revanced-patches/commit/5b4dca03970f887f6caaf3da072ff77276754a61))
* **YouTube - Visual preferences icons:** Added more icons ([9dd62ce](https://github.com/anddea/revanced-patches/commit/9dd62ceacaeca8ab1b0b7c7c221e2cebc19a563f))
