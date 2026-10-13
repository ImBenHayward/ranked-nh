package com.rankednh;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Singleton
final class RankedNhApi
{
    static final class ApiException extends IOException {
        final int code;
        ApiException(int code,String message){super(message);this.code=code;}
    }
    @Inject private OkHttpClient http;
    @Inject private Gson gson;
    static String validateUrl(String input) {
        URI uri=URI.create(input.trim());
        String host=uri.getHost();
        boolean local="localhost".equals(host) || "127.0.0.1".equals(host) || "[::1]".equals(host) || "::1".equals(host);
        if (host==null || uri.getUserInfo()!=null || uri.getQuery()!=null || uri.getFragment()!=null ||
            !("https".equals(uri.getScheme()) || (local && "http".equals(uri.getScheme())))) {
            throw new IllegalArgumentException("Use HTTPS, or HTTP on localhost for development.");
        }
        return uri.toString().replaceAll("/+$", "");
    }
    Models.Status call(String base, String token, String path, Object payload) throws IOException {
        return gson.fromJson(request(base,token,path,payload),Models.Status.class);
    }
    String request(String base,String token,String path,Object payload) throws IOException {
        Request.Builder request=new Request.Builder().url(validateUrl(base)+path).header("Authorization","Bearer "+token);
        if(payload!=null) request.post(RequestBody.create(MediaType.parse("application/json"),gson.toJson(payload)));
        // Bound the whole request, including a server that accepts a connection but stops responding.
        OkHttpClient bounded=http.newBuilder().connectTimeout(3,TimeUnit.SECONDS).readTimeout(5,TimeUnit.SECONDS)
            .writeTimeout(5,TimeUnit.SECONDS).callTimeout(8,TimeUnit.SECONDS).retryOnConnectionFailure(false).build();
        try(Response response=bounded.newCall(request.build()).execute()) {
            if(response.body()==null) throw new IOException("Empty service response. Retry shortly.");
            String body=response.body().string();
            if(!response.isSuccessful()) {
                String message="Service unavailable. Your report will be retried.";
                try { JsonObject error=gson.fromJson(body,JsonObject.class); if(error.has("error")) message=error.get("error").getAsString(); }
                catch(RuntimeException ignored) { }
                throw new ApiException(response.code(),message);
            }
            return body;
        }
    }
}
