package com.bytegen.common.http.template;

import com.bytegen.common.http.HttpParam;
import com.bytegen.common.http.exception.HttpRequestException;
import com.bytegen.common.http.exception.Not2xxException;
import com.bytegen.common.http.magic.TimeTracker;
import com.bytegen.common.http.util.HttpUtil;
import com.bytegen.common.http.util.ServiceMonitor;
import okhttp3.*;
import okhttp3.internal.http.HttpMethod;
import okio.Buffer;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * User: xiang
 * Date: 2018/10/19
 * Desc:
 * <p>
 * common parameters and signature will be added on request build;
 * request and response log will be done automatically;
 * note that:
 * 1. ServiceException will be thrown and should be handled
 * 2. ResponseBody can be consumed only once
 * </p>
 */
public abstract class HttpTemplate {
    private static Logger LOGGER = LoggerFactory.getLogger(HttpTemplate.class);

    protected static final String DEFAULT_USER_AGENT = "Default-HTTP-Client";
    protected static final Charset DEFAULT_CHARSET = Consts.UTF_8;
    protected static final String ERROR_RESPONSE_STRING = "";

    private static final OkHttpClient defaultHttpClient = new OkHttpClient()
            .newBuilder()
            .connectTimeout(3000L, TimeUnit.MILLISECONDS)
            .readTimeout(5000L, TimeUnit.MILLISECONDS)
            .connectionPool(new ConnectionPool(256, 50, TimeUnit.SECONDS))
            .retryOnConnectionFailure(false)
            .addInterceptor(new RetryOn5xx(3))
            .build();

    public static class RetryOn5xx implements Interceptor {
        private int maxRetry;
        private int retryNum = 0;

        public RetryOn5xx(int maxRetry) {
            this.maxRetry = maxRetry;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = chain.proceed(request);
            while (response.code() >= 500 && response.code() <= 600 && retryNum < maxRetry) {
                retryNum++;
                LOGGER.warn("http service retried: [{}]/[{}]. ", retryNum, maxRetry);
                response = chain.proceed(request);
            }
            return response;
        }
    }

    public abstract String getServiceName();

    public OkHttpClient getHttpClient() {
        return defaultHttpClient;
    }

    public boolean isEnableFalcon() {
        return true;
    }

    public boolean isEnableLog() {
        return false; // by default we do not log http request and response
    }

    public abstract String getServiceHost();

    public abstract String getAppId();

    public abstract String getAppSecret();

    protected void ensureGeneralUrlParameter(final List<NameValuePair> urlParam) {
        // add common parameters
        boolean hasTimestamp = false, hasAppId = false, hasNonce = false;
        for (NameValuePair p : urlParam) {
            if (!hasAppId && p.getName().equals(HttpParam.APP_ID) && null != p.getValue()) {
                if (!p.getValue().equals(this.getAppId())) {
                    throw new HttpRequestException(this.getServiceName(), "can not sign with the given appId");
                }
                hasAppId = true;
            }
            if (!hasTimestamp && p.getName().equals(HttpParam.TIMESTAMP) && null != p.getValue()) {
                hasTimestamp = true;
            }
            if (!hasNonce && p.getName().equals(HttpParam.NONCE) && null != p.getValue()) {
                hasNonce = true;
            }
        }

        if (!hasAppId) {
            urlParam.add(new BasicNameValuePair(HttpParam.APP_ID, this.getAppId()));
        }
        if (!hasTimestamp) {
            urlParam.add(new BasicNameValuePair(HttpParam.TIMESTAMP, String.valueOf(System.currentTimeMillis() / 1000L)));
        }
        if (!hasNonce) {
            urlParam.add(new BasicNameValuePair(HttpParam.NONCE, RandomStringUtils.randomAlphanumeric(RandomUtils.nextInt(10, 32))));
        }

        String traceId = MDC.get(HttpParam.TRACE_ID);
        if (StringUtils.isNotBlank(traceId)) {
            urlParam.add(new BasicNameValuePair(HttpParam.TRACE_ID, traceId));
        }
    }

    protected void makeSignature(final RequestBuilder builder) {
        //add sign
        try {
            List<NameValuePair> toSignParam = new ArrayList<>(builder.urlParam);
            if (null != builder.requestBody) {
                if (HttpUtil.isURLEncodedForm(builder.requestBody.contentType()) && builder.requestBody instanceof FormBody) {
                    FormBody body = (FormBody) builder.requestBody;

                    for (int i = 0; i < body.size(); i++) {
                        toSignParam.add(new BasicNameValuePair(body.name(i), body.value(i)));
                    }

                } else if (HttpUtil.isJson(builder.requestBody.contentType())) {
                    String json = bodyToString(builder.requestBody);
                    toSignParam.add(new BasicNameValuePair(HttpParam.JSON_KEY, json));  // for sign

                }
            }

            String token = builder.headers.get(HttpHeaders.AUTHORIZATION);

            String sign = HttpUtil.signURLAndRequestParams(builder.method, builder.uri, toSignParam, token, this.getAppSecret());
            builder.urlParam.add(new BasicNameValuePair(HttpParam.SIGN, sign));

        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("sign error", e);
            throw new HttpRequestException(this.getServiceName(), "sign error");
        }
    }

    public RequestBuilder newRequestBuilder(String httpMethod, String uri) {
        return new RequestBuilder(this, httpMethod, uri);
    }

    public class RequestBuilder {
        private HttpTemplate template;
        private String method;
        private String uri;
        private Headers.Builder headers;
        private List<NameValuePair> urlParam;
        private RequestBody requestBody;

        public RequestBuilder(HttpTemplate template, String method, String uri) {
            Args.notBlank(method, "HttpMethod");
            Args.notBlank(uri, "Uri");

            this.template = template;
            this.method = method;
            this.uri = uri;

            this.headers = new Headers.Builder();
            this.urlParam = new ArrayList<>();
            this.requestBody = null;

            headers.add("User-Agent", DEFAULT_USER_AGENT);
        }

        public RequestBuilder addHeader(String name, String value) {
            Args.notNull(name, "HeaderName");
            Args.notEmpty(value, "HeaderValue");
            headers.add(name, value);
            return this;
        }

        public RequestBuilder headers(Map<String, String> header) {
            Args.notNull(header, "Headers");
            header.forEach(this::addHeader);
            return this;
        }

        public RequestBuilder addUrlParam(String name, String value) {
            urlParam.add(new BasicNameValuePair(name, value));
            return this;
        }

        public RequestBuilder urlParams(Map<String, String> params) {
            urlParam.addAll(HttpUtil.flatMapToPair(params));
            return this;
        }

        public RequestBuilder requestBody(RequestBody body) {
            Args.notNull(body, "RequestBody on set");

            if (!HttpMethod.permitsRequestBody(method)) {
                throw new IllegalArgumentException("method " + method + " do not support request body.");
            }
            requestBody = body;
            return this;
        }

        public Request build() throws HttpRequestException {

            template.ensureGeneralUrlParameter(urlParam);
            template.makeSignature(this);

            // final url
            String url = HttpUtil.concatUrl(template.getServiceHost(), uri, urlParam);

            return new Request.Builder()
                    .url(url)
                    .headers(headers.build())
                    .method(method, requestBody)
                    .build();
        }

        public String getMethod() {
            return method;
        }

        public String getUri() {
            return uri;
        }

        public Headers.Builder getHeaders() {
            return headers;
        }

        public List<NameValuePair> getUrlParam() {
            return urlParam;
        }

        public RequestBody getRequestBody() {
            return requestBody;
        }
    }

    private static String bodyToString(final RequestBody request) {
        if (null == request) {
            return "";
        }

        try {
            final RequestBody copy = request;
            final Buffer buffer = new Buffer();
            copy.writeTo(buffer);
            Charset charset = copy.contentType() == null ? DEFAULT_CHARSET :
                    copy.contentType().charset() == null ? DEFAULT_CHARSET :
                            copy.contentType().charset();
            return buffer.readString(charset);
        } catch (final IOException e) {
            return "read request body fail...";
        }
    }


    public Response execute(Request request) throws IOException {
        Args.notNull(request, "Request");

        if (isEnableLog()) {
            LOGGER.info("{} get request: url [{}], body [{}]",
                    getServiceName(), request.url(), bodyToString(request.body()));
        }

        TimeTracker tracker = new TimeTracker();
        int code = -1;
        try {
            Response response = getHttpClient().newCall(request).execute();
            if (null == response) {
                throw new Not2xxException("Empty response");
            }
            code = response.code();

            if (isEnableLog()) {
                LOGGER.info("{} http response: url [{}], code [{}]", getServiceName(), request.url(), code);
            }

            return response;
        } finally {
            //falcon
            if (isEnableFalcon()) {
                ServiceMonitor.count(getServiceName(), request.method(), request.url().encodedPath(), code, tracker.getDuration());
            }
        }
    }

    public String applyContent(Request request) throws IOException {
        Response response = execute(request);
        if (null == response) {
            throw new Not2xxException("Empty response");
        }

        String content;
        try (ResponseBody body = response.body()) {
            content = null == body ? ERROR_RESPONSE_STRING : body.string();
        }

        if (isEnableLog()) {
            LOGGER.info("{} http response: url [{}], code [{}], content [{}]", getServiceName(), request.url(), response.code(), content);
        }

        if (!response.isSuccessful()) {
            throw new Not2xxException("Unexpected response code: " + response);
        }

        return content;
    }

    public String applyContentIOSafe(Request request) throws HttpRequestException {
        try {
            return applyContent(request);

        } catch (Not2xxException e) {
            LOGGER.error("service not 2xx...", e);
            throw new HttpRequestException(getServiceName(), String.format("service [%s] Not2xx", getServiceName()));
        } catch (IOException e) {
            LOGGER.error("service exception in: " + getServiceName(), e);
            throw new HttpRequestException(getServiceName(), String.format("service [%s] unknown error", getServiceName()));
        }
    }

}
