package com.rankednh;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.runelite.client.callback.ClientThread;
import okhttp3.OkHttpClient;
import org.junit.Test;
import static org.junit.Assert.*;

public class MatchActionDispatchTest {
    @Test public void findMatchSurvivesAnInFlightPollAndDuplicateClicks()throws Exception{
        try(Fixture f=new Fixture()){
            f.plugin.poll();
            f.join();f.join();
            assertTrue("Find a match must be queued behind the status request",f.requests.actionPending());
            f.tasks.remove().run();
            assertTrue(f.requests.actionPending());
            f.tasks.remove().run();
            assertEquals(List.of("GET /queue/status","POST /queue/join"),f.received);
            JsonObject body=new Gson().fromJson(f.bodies.get(1),JsonObject.class);
            assertEquals("EU",body.get("region").getAsString());
            assertEquals("nh-max-126-v1",body.get("rulesetId").getAsString());
            assertTrue(body.get("consent").getAsBoolean());
            assertTrue(f.plugin.status().queued);
            assertFalse(f.requests.busy());assertTrue(f.tasks.isEmpty());
        }
    }

    @Test public void leaveQueueImmediatelyAfterPollingReachesTheServer()throws Exception{
        try(Fixture f=new Fixture()){
            f.response.queued=true;
            f.plugin.poll();f.tasks.remove().run();
            f.plugin.leave();
            assertTrue("Leave queue must not wait for the polling interval",f.requests.actionPending());
            f.tasks.remove().run();
            assertEquals(List.of("GET /queue/status","POST /queue/leave"),f.received);
            assertFalse(f.plugin.status().queued);assertFalse(f.requests.busy());
        }
    }

    @Test public void acceptImmediatelyAfterPollingReachesTheServer()throws Exception{
        try(Fixture f=new Fixture()){
            f.response.match=new Models.Match();f.response.match.id="test-match";f.response.match.state="offered";
            f.plugin.poll();f.tasks.remove().run();
            f.plugin.accept(true);
            assertTrue("Accept must not wait for the polling interval",f.requests.actionPending());
            f.tasks.remove().run();
            assertEquals(List.of("GET /queue/status","POST /match/test-match/accept"),f.received);
            assertTrue(new Gson().fromJson(f.bodies.get(1),JsonObject.class).get("accept").getAsBoolean());
            assertTrue(f.plugin.status().match.accepted);assertFalse(f.requests.busy());
        }
    }

    private static final class Fixture implements AutoCloseable {
        final RankedNhPlugin plugin=new RankedNhPlugin();
        final ArrayDeque<Runnable> tasks=new ArrayDeque<>();
        final ServiceRequests requests=new ServiceRequests(tasks::add,()->{},()->0L);
        final List<String> received=new ArrayList<>(),bodies=new ArrayList<>();
        final Models.Status response=new Models.Status();
        final OkHttpClient http=new OkHttpClient();
        final HttpServer server;
        Fixture()throws Exception{
            server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            server.createContext("/",exchange->{
                try{
                    String path=exchange.getRequestURI().getPath();
                    received.add(exchange.getRequestMethod()+" "+path);
                    bodies.add(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
                    if(path.equals("/queue/join"))response.queued=true;
                    if(path.equals("/queue/leave"))response.queued=false;
                    if(path.endsWith("/accept"))response.match.accepted=true;
                    byte[] json=new Gson().toJson(response).getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type","application/json");
                    exchange.sendResponseHeaders(200,json.length);exchange.getResponseBody().write(json);
                }finally{exchange.close();}
            });
            RankedNhApi api=new RankedNhApi();set(api,"http",http);set(api,"gson",new Gson());
            set(plugin,"api",api);set(plugin,"requests",requests);set(plugin,"running",true);
            set(plugin,"token","synthetic-test-key");set(plugin,"accountMatches",true);set(plugin,"status",response);
            set(plugin,"baseUrl","http://127.0.0.1:"+server.getAddress().getPort());
            // Keep game/UI callbacks deferred, as they would be until the next client tick.
            set(plugin,"clientThread",new ClientThread(){@Override public void invoke(Runnable task){}});
            server.start();
        }
        void join()throws Exception{
            Method command=RankedNhPlugin.class.getDeclaredMethod("command",String.class,Object.class);command.setAccessible(true);
            command.invoke(plugin,"/queue/join",Map.of("rulesetId","nh-max-126-v1","consent",true,"region","EU"));
        }
        @Override public void close(){server.stop(0);http.connectionPool().evictAll();http.dispatcher().executorService().shutdown();}
    }
    private static void set(Object target,String name,Object value)throws Exception{
        Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);field.set(target,value);
    }
}
