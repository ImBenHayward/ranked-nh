# Notes for Plugin Hub review

Ranked NH matches two invited players for default Max/Med NH fights in Emir's Arena. The plugin observes those fights and submits reports to an external service. The service runs the queue, compares paired reports and records best-of-three results. The invitation-only Beta season awards ratings and placement progress. Those ratings reset before public Season 1.

## Network requests

All service calls use RuneLite's injected OkHttp and Gson instances, outside the game and Swing threads. The default endpoint is `https://ranked-nh.vercel.app`. HTTPS is required, with loopback HTTP allowed for development. Requests have bounded timeouts. Changing the endpoint invalidates the previous connection.

The sidebar requires an NH key and the **Share match observations** checkbox before connecting. See [PRIVACY.md](PRIVACY.md) for the data fields and retention. The Hub manifest includes an external-service warning covering IP address, character name, loadout and fight observations.

Polling is every 15 seconds for an idle logged-in participant, 30 seconds when logged out, and 3 seconds while queued, matched or retrying a report. User actions bypass that timer and are serialized with status requests. Rank membership refreshes every five minutes; name lookups are limited to enrolled members and batched up to 25 at a time.

`FightRecorder`, `ArenaRoundGuard`, `ArenaKit` and `RankedNhApi` contain the observation and reporting code. Readiness is a service message after both assigned players are seen in the correct world. It does not accept or start an Arena challenge. Client reports are observations, not trusted game-server proof.

## Game interaction and overlays

- `ArenaWorldGuide` validates the suggested destination against RuneLite's world list, region, Arena type and capacity. After a user clicks **Open world switcher**, it opens the native picker with `Client.openWorldHopper`. The player selects the displayed world manually. The plugin never calls `Client.hopToWorld`.
- `OpponentLocator` limits the tile, nameplate and minimap marker to the assigned opponent in the loaded scene, on the assigned Arena world, during preparation, active play or reporting. It does not locate players outside the loaded scene.
- `RankMenuIcons` changes the rank image in a player's menu target on Arena worlds. It leaves the menu action and ordering intact. Other players' ranks are queried only after a local enrolled-member check.
- Setup overlays identify differences from the default kit. They do not equip items, copy a loadout, choose a build, accept a duel or make combat decisions.
- The match report is hidden during Arena combat. Previews use synthetic data and cannot submit results or change ratings.

The native setup/menu reads are limited to the supported game revision. Unsupported revisions disable those checks rather than interpreting unknown values.

## Bundled resources and size

The JAR is about 8.9 MiB, within the default 10 MiB limit. No size allowance is requested.

Most of the size is 30 locally bundled rank turns, each with 32 separate PNG frames at 128 by 128 pixels. Each image decodes to 64 KiB; no large sprite sheets are bundled. Only the current rank is decoded, off the render thread. Its frames total 2 MiB and are released after the reveal. Reveals draw at up to 184 pixels. Reduced motion skips the animation. Missing or failed resources fall back to a still badge.

Each turn uses a shared palette where it meets the chosen quality floor, with RGBA retained for turns that lose too much colour detail when quantized. The turn keeps its original duration, easing and final orientation. Still badges are 256px, above the largest 184px reveal. All frames and badges are generated from the retained original artwork. The plugin loads no remote code, 3D runtime or video decoder. Rank images and reveal audio are classpath resources. The entire repository, including these resources, is covered by [BSD 2-Clause](LICENSE). [ASSET-LICENSE.md](ASSET-LICENSE.md) records origins and attribution.

Reveal audio uses RuneLite's injected `AudioPlayer` on its executor. The finite hum and chime are scheduled against the reveal clock; reduced motion plays just the chime. Closing or muting a report cancels pending cues. RuneLite exposes no stop handle, so a cue already playing finishes, at most 1.8 seconds later. Volume scaling and short fades are applied to the bundled PCM data before passing WAV streams to RuneLite. The production plugin does not use `javax.sound` directly.

## Local files and dependencies

Normal plugin use saves display preferences and bounded hashes of dismissed reports through RuneLite's config manager. The optional, user-clicked kit snapshot uses RuneLite's `Filepath` utility and a newly named file under the plugin directory. NH keys are never saved to configuration or sent in URLs.

The production plugin has no dependencies beyond those supplied by RuneLite. JUnit and the development client dependencies are test-only. Plugin Hub uses its standard build and includes `src/main`; the independent backend and beta launcher are absent from this repository.

## Validation and current limits

`./gradlew test jar` runs the public plugin tests. Coverage includes request serialization, repeated status refreshes during queue clicks, default-kit comparisons across worn and inventory slots, stale report handling and all rank resources.

Earlier two-account Arena testing covered ordinary fights, setup differences, opponent indicators and reports. Hosted API tests covered pairing, paired round reports, result retries, settlement and history. Automated API tests are not a substitute for an Arena fight. The full disconnect, double-death and game-update matrix is still being tested in the beta. The latest queue-control changes have regression coverage; a fresh two-player in-game check is pending.

The hosted Beta season awards ratings after accepted paired reports; earlier unrated pilot matches stay archived. Disputed or incomplete evidence goes to review. Closing the client can lose an unsent report. Matching client reports cannot prevent collusion or prove an untampered game result.
