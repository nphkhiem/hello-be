# API 28 on a television cannot be emulated on Apple Silicon, so M1 records the gap

`minSdk` is 28, and both P2-T7 and the M1 exit gate ask for verification on "API 28/current TV".
The current TV half is done, on Television_4K at API 36. The API 28 half is not, and it is not a
matter of installing something.

Google's Android TV system-image manifest lists, per API level, the ABIs it ships. From API 22
through API 30 the only Android TV image is `x86`. `arm64-v8a` Android TV images begin at API 31.
This machine is Apple Silicon, and the emulator cannot run an x86 system image on it. So an API 28
Android TV emulator does not exist for this hardware, and no amount of SDK downloading produces one.

A non-television API 28 image does exist for arm64, `system-images;android-28;google_apis;arm64-v8a`,
which would have covered the platform half of the risk: Media3 decoding, Room, lifecycle callbacks
and coroutines, all of which are indifferent to whether the device is a television. The project
owner reports that image cannot be installed on this machine either, so that route is closed too.

The alternative to writing this down was to leave the criterion unticked with no reason beside it,
or worse, to tick it because the suite passed on the one device that could run it. Both of those
turn a real gap into an invisible one.

## Consequences

M1 closes with the API 28 leg **unmet**, named as unmet, with this as the reason. `minSdk` stays at
28, because the app is meant to run there and nothing about that has changed.

What is unverified is specific rather than general: API 28 is where Media3's decoder selection,
Room's SQLite version and the lifecycle callback ordering differ most from API 36, and none of those
have been exercised. The Compose and D-pad surface is the same code on both, so the television
behaviour verified at API 36 carries.

Closing the gap needs one of three things, in rough order of cost: a physical Android TV device
running API 28, an x86 CI runner that can host the API 28 television image, or a machine that can
install the arm64 API 28 phone image for the platform half. Until one of those exists, any claim
that Hello Bé runs on API 28 rests on the minSdk declaration and on nothing having been observed to
contradict it, which is not the same as evidence.
