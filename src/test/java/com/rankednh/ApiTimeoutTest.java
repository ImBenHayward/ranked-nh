package com.rankednh;

import com.google.gson.Gson;
import java.lang.reflect.Field;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import okhttp3.OkHttpClient;
import static org.junit.Assert.*;

public class ApiTimeoutTest {
    @Test public void aServerThatAcceptsButNeverRespondsCannotHoldTheControlsIndefinitely()throws Exception{
        try(ServerSocket server=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"))){
            CountDownLatch accepted=new CountDownLatch(1),finish=new CountDownLatch(1);
            Thread worker=new Thread(()->{
                try(Socket socket=server.accept()){accepted.countDown();finish.await(12,TimeUnit.SECONDS);}
                catch(Exception ignored){}
            });worker.setDaemon(true);worker.start();
            RankedNhApi api=new RankedNhApi();OkHttpClient shared=new OkHttpClient.Builder().readTimeout(0,TimeUnit.MILLISECONDS).build();
            Field http=RankedNhApi.class.getDeclaredField("http");http.setAccessible(true);http.set(api,shared);
            Field gson=RankedNhApi.class.getDeclaredField("gson");gson.setAccessible(true);gson.set(api,new Gson());
            long start=System.nanoTime();
            try{api.request("http://127.0.0.1:"+server.getLocalPort(),"synthetic-test-key","/queue/status",null);fail("Request should time out");}
            catch(InterruptedIOException expected){assertEquals(0,accepted.getCount());assertTrue(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()-start)<11);}
            finally{finish.countDown();worker.join(1000);shared.connectionPool().evictAll();shared.dispatcher().executorService().shutdown();}
        }
    }
}
