package com.rankednh;

import java.util.ArrayList;
import java.util.List;

final class Models
{
    static final class Rank {
        String name; String badge; int placementsRemaining; Integer nextAt;
        String nextRank; Integer divisionStart; Integer pointsToNext; Double progress;
    }
    static final class Player { String id; String rsn; double rating; int games; int wins; Rank rank; }
    static final class Opponent { String id; String rsn; Rank rank; }
    static final class Match {
        String id; String state; int world; String region; String rulesetId; Opponent opponent;
        int[] score; int round; boolean accepted; boolean opponentAccepted; boolean ready; boolean opponentReady;
        String nonce; String startedAt; String createdAt; String endedAt; String deadline; boolean reported; boolean opponentReported;
        String winner; boolean rated; String reason; Integer ratingChange; Integer ratingBefore; Integer ratingAfter;
        Rank rankBefore,rankAfter;
        boolean isOpen() { return !("complete".equals(state) || "void".equals(state)); }
    }
    static final class Status {
        String mode; String serverTime; Player player; boolean queued; String joinedAt; String cooldownUntil; String cooldownReason; Match match;
        List<String> regions=new ArrayList<>(); String queueRegion; int crossRegionAfterSeconds;
    }
    static final class Hit {
        final String target; final int amount; final int tick;
        Hit(String target, int amount, int tick) { this.target=target; this.amount=amount; this.tick=tick; }
    }
    static final class Report {
        int round; String nonce; String winner; int world; int durationTicks;
        String source="death-observed-v1"; String endReason="death";
        List<Hit> hits=new ArrayList<>();
        SetupEvidence setup;
    }
    static final class SetupEvidence {
        final String referenceId;final ArenaKit own,opponent;
        SetupEvidence(String referenceId,ArenaKit own,ArenaKit opponent){this.referenceId=referenceId;this.own=own;this.opponent=opponent;}
    }
    private Models() {}
}
