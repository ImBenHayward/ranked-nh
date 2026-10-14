package com.rankednh;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Fixed reference bundled from shared/default-max-med.json, never learned from an opponent. */
@Singleton
final class DefaultArenaKit {
    private static final class Reference { String id; ArenaKit kit; }
    private final Reference reference;
    @Inject DefaultArenaKit(Gson gson){reference=load(gson);}
    private static Reference load(Gson gson){
        try(InputStreamReader reader=new InputStreamReader(DefaultArenaKit.class.getResourceAsStream("default-max-med.json"),StandardCharsets.UTF_8)){
            Reference reference=gson.fromJson(reader,Reference.class);
            if(reference.id==null || reference.kit==null || !reference.kit.complete() || reference.kit.revision!=ArenaSetup.REVISION)
                throw new IllegalStateException("Invalid Arena reference");
            return reference;
        }catch(java.io.IOException error){throw new IllegalStateException("Cannot read Arena reference",error);}
    }
    String id(){return reference.id;}
    ArenaKit kit(){return reference.kit;}
    boolean matches(ArenaKit kit){return kit!=null && kit.allowedBy(reference.kit);}
}
