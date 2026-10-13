package com.rankednh;

enum RankPreview {
    RANK_UP("Rank up"),RATING_GAIN("Rating gain"),RATING_LOSS("Rating loss"),PLACEMENTS("Placements complete");
    private final String label;
    RankPreview(String label){this.label=label;}
    @Override public String toString(){return label;}
}
