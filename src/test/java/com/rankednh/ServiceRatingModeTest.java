package com.rankednh;

import com.google.gson.Gson;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Test;
import static org.junit.Assert.*;

public class ServiceRatingModeTest {
    @Test public void successfulMatchActionsDoNotDependOnTheSeparateRatingModeRequest()throws Exception{
        List<String> paths=new ArrayList<>();AtomicReference<String> health=new AtomicReference<>("{\"rated\":false}");
        OkHttpClient http=new OkHttpClient.Builder().addInterceptor(chain->{
            String path=chain.request().url().encodedPath();paths.add(path);
            return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(ResponseBody.create(MediaType.parse("application/json"),path.equals("/health")?health.get():"{\"queued\":true}")).build();
        }).build();
        RankedNhApi api=new RankedNhApi();set(api,"http",http);set(api,"gson",new Gson());
        assertTrue(api.call("https://example.test","test","/queue/join",java.util.Map.of()).queued);
        assertEquals(List.of("/queue/join"),paths);
        assertEquals(Boolean.FALSE,api.rated("https://example.test","test"));
        health.set("{\"rated\":true}");assertEquals(Boolean.TRUE,api.rated("https://example.test","test"));
        health.set("{}");assertNull(api.rated("https://example.test","test"));
        health.set("{\"rated\":\"false\"}");assertNull(api.rated("https://example.test","test"));
    }
    private static void set(Object target,String name,Object value)throws Exception{
        Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);field.set(target,value);
    }
}
