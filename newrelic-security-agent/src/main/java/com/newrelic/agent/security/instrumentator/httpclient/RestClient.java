package com.newrelic.agent.security.instrumentator.httpclient;

import com.newrelic.agent.security.AgentInfo;
import com.newrelic.agent.security.intcodeagent.filelogging.FileLoggerThreadPool;
import com.newrelic.agent.security.intcodeagent.filelogging.LogLevel;
import com.newrelic.agent.security.intcodeagent.models.javaagent.FuzzFailEvent;
import com.newrelic.agent.security.intcodeagent.websocket.EventSendPool;
import com.newrelic.api.agent.security.instrumentation.helpers.ServletHelper;
import okhttp3.*;
import okhttp3.OkHttpClient.Builder;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

public class RestClient {


    public static final String REQUEST_SUCCESS_S_RESPONSE_S_S = "Request success : %s :: response : %s : %s";
    public static final String CALL_FAILED_REQUEST_S_REASON = "Call failed : request %s reason : ";
    public static final String FIRING_REQUEST_METHOD_S = "Firing request :: Method : %s";
    public static final String FIRING_REQUEST_URL_S = "Firing request :: URL : %s";
    public static final String FIRING_REQUEST_HEADERS_S = "Firing request :: Headers : %s";

    private static final FileLoggerThreadPool logger = FileLoggerThreadPool.getInstance();

    public static RestClient instance;

    private boolean isConnected = true;

    private static final Object lock = new Object();

    private final X509TrustManager x509TrustManager = new X509TrustManager() {
        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
                throws CertificateException {
        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
                throws CertificateException {
        }

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[] {};
        }
    };

    // Create a trust manager that does not validate certificate chains
    private final TrustManager[] trustAllCerts = new TrustManager[]{
            x509TrustManager
    };
    
    private final ThreadLocal<OkHttpClient> clientThreadLocal = new ThreadLocal<OkHttpClient>() {
        @Override
        protected OkHttpClient initialValue() {

            Builder builder = new OkHttpClient.Builder();
            try {
                ConnectionPool connectionPool = new ConnectionPool(1, 5, TimeUnit.MINUTES);
                builder = builder.connectionPool(connectionPool);
                builder = builder.callTimeout(10, TimeUnit.SECONDS);

                // Install the all-trusting trust manager
                final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                // Create an ssl socket factory with our all-trusting manager
                final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                builder = builder.sslSocketFactory(sslSocketFactory, x509TrustManager);
                builder.addInterceptor(new Interceptor() {
                    @NotNull
                    @Override
                    public Response intercept(@NotNull Chain chain) throws IOException {
                        Request request = chain.request();
                        Response response = chain.proceed(request);
                        RestClient.getInstance().setConnected(!(response.code() == 503 || response.code() == 504));
                        return response;
                    }
                });

                builder = builder.hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return builder.build();
        }
    };

    private RestClient() {
    }

    public static RestClient getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new RestClient();
                }
            }
        }
        return instance;
    }

    public OkHttpClient getClient() {
        return clientThreadLocal.get();
    }

    public void fireRequest(Request request, int repeatCount) throws InterruptedIOException {
        OkHttpClient client = clientThreadLocal.get();

        logger.log(LogLevel.FINER, String.format(FIRING_REQUEST_METHOD_S, request.method()), RestClient.class.getName());
        logger.log(LogLevel.FINER, String.format(FIRING_REQUEST_URL_S, request.url()), RestClient.class.getName());
        logger.log(LogLevel.FINER, String.format(FIRING_REQUEST_HEADERS_S, request.headers()), RestClient.class.getName());

        Call call = client.newCall(request);
        try {
            Response response = call.execute();
            logger.log(LogLevel.FINER, String.format(REQUEST_SUCCESS_S_RESPONSE_S_S, request, response, response.body().string()), RestClient.class.getName());
            response.body().close();
            if (client.connectionPool() != null) {
                client.connectionPool().evictAll();
            }
        } catch (InterruptedIOException e){
            if(repeatCount >= 0){
                fireRequest(request, --repeatCount);
            }
        } catch (IOException e) {
            logger.log(LogLevel.FINER, String.format(CALL_FAILED_REQUEST_S_REASON, request), e, RestClient.class.getName());
            FuzzFailEvent fuzzFailEvent = new FuzzFailEvent(AgentInfo.getInstance().getApplicationUUID());
            fuzzFailEvent.setFuzzHeader(request.header(ServletHelper.CSEC_IAST_FUZZ_REQUEST_ID));
            EventSendPool.getInstance().sendEvent(fuzzFailEvent);
        }

    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }
}