package com.rankednh;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Small persistent history, scoped by service, enrolled player, match and result state. */
final class ReportSeenStore {
    private final Supplier<String> read;
    private final Consumer<String> write;
    ReportSeenStore(Supplier<String> read,Consumer<String> write){this.read=read;this.write=write;}
    private LinkedHashSet<String> entries(){
        String raw=read.get();return new LinkedHashSet<>(raw==null || raw.isEmpty()?java.util.List.of():Arrays.asList(raw.split(",")));
    }
    synchronized boolean contains(String key){return entries().contains(hash(key));}
    synchronized void remember(String key){
        LinkedHashSet<String> values=entries();String digest=hash(key);
        if(!values.add(digest))return;
        while(values.size()>128)values.remove(values.iterator().next());
        write.accept(String.join(",",values));
    }
    private static String hash(String value){
        try{return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
}
