# Notes for Plugin Hub review

Ranked NH matches two invited players for default Max/Med NH fights in Emir's Arena. The plugin observes those fights and submits reports to an external service. The service runs the queue, compares paired reports and records best-of-three results. The beta is currently unrated.

## Network requests

All service calls use RuneLite's injected OkHttp and Gson instances, outside the game and Swing threads. The default endpoint is `https://ranked-nh.vercel.app`. HTTPS is required, with loopback HTTP allowed for development. Requests have bounded timeouts. Changing the endpoint invalidates the previous connection.

The sidebar requires an NH key and the **Share match observations** checkbox before connecting. See [PRIVACY.md](PRIVACY.md) for the data fields and retention. The Hub manifest includes an external-service warning covering IP address, character name, loadout and fight observations.

Polling is every 15 seconds for an idle logged-in participant, 30 seconds when logged out, and 3 seconds while queued, matched or retrying a report. User actions bypass that timer and are serialized with status requests. Rank membership refreshes every five minutes; name lookups are limited to enrolled members and batched up to 25 at a time.

`FightRecorder`, `ArenaRoundGuard`, `ArenaKit` and `RankedNhApi` contain the observation and reporting code. Readiness is a service message after both assigned players are seen in the correct world. It does not accept or start an Arena challenge. Client reports are observations, not trusted game-server proof.

## Game interaction and overlays

- `ArenaWorldHopper` calls `Client.openWorldHopper` and `Client.hopToWorld` only after a user clicks the assigned-world button. It validates the destination against RuneLite's world list, region, Arena type and capacity. Ordinary game restrictions and confirmation dialogs still apply.
- `OpponentLocator` limits the tile, nameplate and minimap marker to the assigned opponent in the loaded scene, on the assigned Arena world, during preparation, active play or reporting. It does not locate players outside the loaded scene.
- `RankMenuIcons` changes the rank image in a player's menu target on Arena worlds. It leaves the menu action and ordering intact. Other players' ranks are queried only after a local enrolled-member check.
- Setup overlays identify differences from the default kit. They do not equip items, copy a loadout, choose a build, accept a duel or make combat decisions.
- The match report is hidden during Arena combat. Previews use synthetic data and cannot submit results or change ratings.

The native setup/menu reads are limited to the supported game revision. Unsupported revisions disable those checks rather than interpreting unknown values.

## Bundled resources and size

The JAR is about 21 MiB. The manifest requests `jarSizeLimitMiB=25`, above the default 10 MiB limit, for maintainer review.

Most of the size is 30 locally bundled rank-turn PNG sheets. Each contains 48 frames at 192 by 192 pixels, for reveals drawn at up to 184 pixels. Only the current rank is decoded, off the render thread, and its 6.75 MiB decoded sheet is released after the reveal. Reduced motion skips the animation. Missing or failed resources fall back to a still badge.

The sheets use a shared palette where it meets the chosen quality floor, with RGBA retained for turns that lose too much colour detail when quantized. Reducing the original 256-pixel frames to 192 pixels brought the JAR down from about 31 MiB without dropping frames or changing the timing. The plugin loads no remote code, 3D runtime or video decoder. Rank images and reveal audio are classpath resources. [ASSET-LICENSE.md](ASSET-LICENSE.md) records permission to redistribute them with Ranked NH.

## Local files and dependencies

Normal plugin use saves display preferences and bounded hashes of dismissed reports through RuneLite's config manager. The optional, user-clicked kit snapshot uses RuneLite's `Filepath` utility and a newly named file under the plugin directory. NH keys are never saved to configuration or sent in URLs.

The production plugin has no dependencies beyond those supplied by RuneLite. JUnit and the development client dependencies are test-only. Plugin Hub uses its standard build and includes `src/main`; the independent backend and beta launcher are absent from this repository.

## Validation and current limits

`./gradlew test jar` runs the public plugin tests. Coverage includes request serialization, repeated status refreshes during queue clicks, default-kit comparisons across worn and inventory slots, stale report handling and all rank resources.

Earlier two-account Arena testing covered ordinary fights, setup differences, opponent indicators and reports. Hosted API tests covered pairing, paired round reports, result retries, settlement and history. Automated API tests are not a substitute for an Arena fight. The full disconnect, double-death and game-update matrix is still being tested in the beta. The latest queue-control changes have regression coverage; a fresh two-player in-game check is pending.

Ratings stay disabled during the beta checks. Disputed or incomplete evidence goes to review. Closing the client can lose an unsent report. Matching client reports cannot prevent collusion or prove an untampered game result.
