# Ranked NH data policy

Updated 13 October 2026.

Ranked NH uses `https://ranked-nh.vercel.app`, operated by ImBenHayward, for its invited-player queue and match records. The API runs on Render, Vercel forwards requests from the existing address, and records are stored in Supabase Postgres. This service is separate from RuneLite and Jagex.

## When requests start

The plugin makes no Ranked NH service requests until you tick **Share match observations** and click **Connect**. Connecting sends the NH access key to the configured service to identify the enrolled character. The same key authenticates later requests.

Once connected, the plugin polls for status. Queue actions require the enrolled RuneScape character to be logged in. The checkbox is part of the connection screen and is hidden after connecting. To stop Ranked NH requests, disable the plugin or close RuneLite. Changing the service address also disconnects it.

## What the service receives

| Data | Purpose |
| --- | --- |
| NH access key | Authenticate the enrolled participant. The service stores a hash of the key. |
| Enrolled character and internal player ID | Match the invitation to the account and attach its results. |
| Queue region, acceptance, readiness, forfeit and review actions | Run the queue and best-of-three series. |
| Assigned Arena world, ruleset and round identifiers | Associate reports with the agreed match. |
| Both matched players' saved Arena kits | Check the default Max/Med setup, including item IDs, quantities, build and spellbook. |
| Observed winner or draw, timing and hitsplats | Compare the two clients' accounts of the round. Hitsplats identify self or opponent, amount and relative game tick. |
| Names of enrolled participants whose ranks are requested | Show NH rank badges in Arena player menus. |

The service sends connected players a list of active enrolled names. The plugin checks that list locally before requesting another player's rank. Names absent from the list are not sent for lookup. The matched opponent and local player's ranks are already included in match status.

Ranked NH does not upload chat, friends lists, general world scans, unrelated players' equipment, Jagex passwords or game-login files. Game observations are scoped to the assigned duel. The HTTP connection exposes the player's IP address to the hosting provider. The application does not explicitly store raw IP addresses in match records; shared request limiting uses a hash. Hosting infrastructure may keep request logs.

## Who can see records

Your opponent sees the matchup, your enrolled character name and NH rank. Enrolled players can request other enrolled players' ranks. Authenticated players can read their own recent match history. Service operators can inspect reports and review disputes.

The public leaderboard is disabled for the current beta. If enabled later, it can show eligible participants' names, ranks, ratings, wins and losses.

## Retention

The service has a daily cleanup job configured to delete raw reports more than 30 days old when their match is complete or void. These reports contain the submitted kits, hitsplats, timing and observed winner. Expired access-key sessions and expired rate-limit buckets are also removed. Reports for unresolved matches are kept for review. The settled match result, rating history, player records and operator decisions currently have no automatic expiry.

There is no self-service data export or deletion tool yet. Contact ImBenHayward in the [Ranked NH Discord](https://discord.gg/3fsrUg6VR) about your records. Do not post a key or private match evidence in a public channel.

## What stays on your computer

The plugin keeps the NH access key in memory and does not save it in RuneLite settings. Paste it again after restarting RuneLite. Display preferences and hashes of already-dismissed reports are stored in RuneLite settings, so closed results stay closed.

Unsent round reports remain in memory while the plugin retries. Closing RuneLite before a report is sent can lose that report. It does not award a win for a disconnected player.

The optional **Kit diagnostics** setting reveals a **Save kit snapshot** button. Only clicking that button writes a JSON kit snapshot under RuneLite's plugin directory. It does not approve or upload a new default kit.

The Plugin Hub source does not contain the separate beta launcher's game-login setup. Development login exports are handled through RuneLite's documented development workflow, outside the plugin.
