package com.shell.common;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@UtilityClass
@Log
public class HttpUtil {

    private static final long REQUEST_TIMEOUT = TimeUnit.MINUTES.toMillis(1);


    private static SSLConnectionSocketFactory createVerifiedFactory() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        var sslContext = SSLContextBuilder.create().loadTrustMaterial((chain, authType) -> true).build();
        return new SSLConnectionSocketFactory(sslContext, new DefaultHostnameVerifier());
    }

    private static Registry<ConnectionSocketFactory> createTrustAllFactoryRegistry(SSLConnectionSocketFactory sslFactory) {
        return RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https", sslFactory).build();
    }

    private CloseableHttpClient createClient() throws GeneralSecurityException {
        try {
            var connectionConfig = ConnectionConfig.custom()
                    .setConnectTimeout(REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
                    .setSocketTimeout(Math.toIntExact(REQUEST_TIMEOUT), TimeUnit.MILLISECONDS)
                    .build();
            var requestConfig = RequestConfig.custom()
                    .setResponseTimeout(Timeout.ofMilliseconds(REQUEST_TIMEOUT))
                    .setConnectionRequestTimeout(Timeout.ofMilliseconds(REQUEST_TIMEOUT))
                    .build();
            var sslFactory = createVerifiedFactory();
            var poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager(createTrustAllFactoryRegistry(sslFactory));
            poolingHttpClientConnectionManager.setDefaultConnectionConfig(connectionConfig);
            return HttpClientBuilder.create().setDefaultHeaders(getDefaultHeaders())
                    .setDefaultRequestConfig(requestConfig)
                    .setConnectionReuseStrategy((request, response, context) -> true)
                    .setConnectionManager(poolingHttpClientConnectionManager).build();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException ex) {
            log.log(Level.WARNING, "cloud-shell-task >> HttpUtil >> getRequestFactory >> Exception:", ex);
            throw new GeneralSecurityException(ex);
        }
    }


    public ResponseEntity<String> sendRequest(String path, HttpHeaders headers) {
        HttpGet request = new HttpGet(path);
        return sendRequest(path, request, headers);
    }


    @SneakyThrows
    public ResponseEntity<String> sendRequest(String path, HttpUriRequestBase request, HttpHeaders headers) {
        try (CloseableHttpClient client = createClient()) {
            headers.forEach((headerName, headerValue) -> request.setHeader(headerName, headerValue.stream().findFirst().orElse("")));
            return client.execute(request, response -> {
                var code = response.getCode();
                var responseEntity = Optional.ofNullable(response.getEntity());
                if (responseEntity.isPresent()) {
                    return ResponseEntity.status(code).body(EntityUtils.toString(responseEntity.get()));
                }
                return ResponseEntity.status(code).body("");
            });
        } catch (Exception e) {
            log.log(Level.WARNING, MessageFormat.format("cloud-shell-task >> HttpUtil >> sendRequest >> url: {0} >> Exception:", path), e);
            throw e;
        }
    }

    public ResponseEntity<String> sendPostRequest(String path, String json, HttpHeaders headers) {
        var request = new HttpPost(path);
        var entity = new StringEntity(json, ContentType.APPLICATION_JSON);
        request.setEntity(entity);
        return sendRequest(path, request, headers);
    }

    public static Map<String, String> getDefaultHeaderValue() {
        var result = new HashMap<String, String>();
        result.put(HttpHeaders.CONTENT_TYPE, "application/json");
        result.put(HttpHeaders.CACHE_CONTROL, "max-age=0");
        return result;
    }

    public static List<BasicHeader> getDefaultHeaders() {
        return getDefaultHeaderValue().entrySet().stream().map(it -> new BasicHeader(it.getKey(), it.getValue()))
                .toList();
    }

    public HttpHeaders getHeaderPostRequest() {
        var header = new HttpHeaders();
        getDefaultHeaderValue().forEach(header::add);
        return header;
    }

    @SneakyThrows
    public ResponseEntity<String> downloadFile(String outputFilePath, HttpUriRequestBase request, HttpHeaders headers) {
        try (CloseableHttpClient client = createClient()) {
            headers.forEach((headerName, headerValue) -> request.setHeader(headerName, headerValue.stream().findFirst().orElse("")));
            return client.execute(request, response -> {
                var inputStream = response.getEntity().getContent();
                try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(outputFilePath), 10485760)) {
                    byte[] buffer = new byte[10485760];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        bufferedOutputStream.write(buffer, 0, bytesRead);
                    }
                    EntityUtils.consume(response.getEntity());
                }
                if (response.getCode() != 200) {
                    throw new HttpResponseException(response.getCode(), "Failed to download file");
                }
                return ResponseEntity.status(response.getCode()).body("");
            });
        } catch (Exception e) {
            log.log(Level.WARNING, "cloud-shell-task >> HttpUtil >> sendRequest >> url: {0} >> Exception:", e);
            throw e;
        }
    }


}
