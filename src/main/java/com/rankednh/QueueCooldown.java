package com.rankednh;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Server owns eligibility; local time is used only to explain the remaining wait. */
final class QueueCooldown {
    final String remaining,detail;
    private QueueCooldown(String remaining,String detail){this.remaining=remaining;this.detail=detail;}
    static QueueCooldown from(Models.Status status,long serverNow,ZoneId zone){
        if(status.cooldownUntil==null)return null;
        String reason=reason(status.cooldownReason);
        try{
            Instant until=Instant.parse(status.cooldownUntil);
            long seconds=Math.max(0,(long)Math.ceil((until.toEpochMilli()-serverNow)/1000.0));
            String remaining=seconds==0?"Checking queue availability…":"Available in "+duration(seconds);
            LocalDate today=Instant.ofEpochMilli(serverNow).atZone(zone).toLocalDate();ZonedDateTime local=until.atZone(zone);
            String day=local.toLocalDate().equals(today)?"Today":local.toLocalDate().equals(today.plusDays(1))?"Tomorrow":
                local.format(DateTimeFormatter.ofPattern(local.getYear()==today.getYear()?"d MMM":"d MMM yyyy",Locale.UK));
            String time=local.format(DateTimeFormatter.ofPattern("HH:mm z",Locale.ENGLISH));
            return new QueueCooldown(remaining,day+" at "+time+"\n"+reason);
        }catch(java.time.DateTimeException invalid){return new QueueCooldown("Checking queue availability…",reason);}
    }
    private static String duration(long seconds){
        if(seconds>=3600)return seconds/3600+"h "+seconds%3600/60+"m";
        if(seconds>=60)return seconds/60+"m "+seconds%60+"s";
        return seconds+"s";
    }
    private static String reason(String code){
        if("cancelled".equals(code))return "Cancelled before the first fight.";
        if("declined".equals(code))return "Match offer declined.";
        if("missed_accept".equals(code))return "Match offer wasn't accepted in time.";
        if("no_show".equals(code))return "Didn't arrive before the match deadline.";
        return "Temporary matchmaking cooldown.";
    }
}
