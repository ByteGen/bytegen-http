package com.bytegen.common.http;

import com.bytegen.common.http.template.HttpTemplate;
import okhttp3.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * User: xiang
 * Date: 2018/10/19
 * Desc:
 */
public class HttpTemplateTest {

    class TestHttp extends HttpTemplate {
        @Override
        public String getServiceName() {
            return "test service";
        }

        @Override
        public String getServiceHost() {
            return "http://www.test-sign.com";
        }

        @Override
        public String getAppId() {
            return "10000";
        }

        @Override
        public String getAppSecret() {
            return "secret";
        }

        public boolean isEnableLog() {
            return true;
        }
    }


    private HttpTemplate template;

    @Before
    public void before() {
        template = new TestHttp();
    }

    @Test
    public void get() throws Exception {
        Request request = template.newRequestBuilder(HttpMethod.GET, "/test/verify_sign")
                .addHeader("AuthorizatiON", "Bearer abcde")
                .addUrlParam("nonce", RandomStringUtils.random(30))
                .addUrlParam("nonce", RandomStringUtils.random(30))
                .build();

        String response = template.applyContent(request);
        Assert.assertTrue(null != response && response.contains("success"));
    }

    @Test
    public void post() throws Exception {

        FormBody formBody = new FormBody.Builder()
                .add("nonce", RandomStringUtils.random(30))
                .add("nonce", RandomStringUtils.random(30))
                .build();

        Request request = template.newRequestBuilder(HttpMethod.POST, "/test/verify_sign")
                .addHeader("AuthorizatiON", "Bearer abcde")
                .addUrlParam("timestamp", "1526250988")
                .requestBody(formBody)
                .build();

        String response = template.applyContent(request);
        Assert.assertTrue(null != response && response.contains("success"));
    }

    @Test
    public void putJson() throws Exception {
        RequestBody requestBody = RequestBody.create(HttpMediaType.APPLICATION_JSON, "{\n" +
                "    \"result_code\": \"success\",\n" +
                "    \"data\": {\n" +
                "        \"server_sign\": \"0b44f949f6539705dae889cf59f2d011\",\n" +
                "        \"client_sign\": \"0b44f949f6539705dae889cf59f2d011\"\n" +
                "    },\n" +
                "    \"server_time\": 1526350470\n" +
                "}");

        Request request = template.newRequestBuilder(HttpMethod.PUT, "/test/verify_sign")
                .addHeader("AuthorizatiON", "Bearer abcde")
                .addUrlParam("timestamp", "1526250988")
                .requestBody(requestBody)
                .build();

        String response = template.applyContent(request);
        Assert.assertTrue(null != response && response.contains("success"));
    }

    @Test
    public void postMultiPart() throws Exception {

        RequestBody requestBody = new MultipartBody.Builder()
                .addPart(MultipartBody.create(HttpMediaType.APPLICATION_JSON, "{\n" +
                        "    \"result_code\": \"success\",\n" +
                        "    \"data\": {\n" +
                        "        \"server_sign\": \"0b44f949f6539705dae889cf59f2d011\",\n" +
                        "        \"client_sign\": \"0b44f949f6539705dae889cf59f2d011\"\n" +
                        "    },\n" +
                        "    \"server_time\": 1526350470\n" +
                        "}"))
                .addFormDataPart("AuthorizatiON", "Bearer abcde")
                .addPart(MultipartBody.create(MediaType.parse("application/pdf"), "content".getBytes()))
                .build();

        Request request = template.newRequestBuilder(HttpMethod.POST, "/test/verify_sign")
                .addUrlParam("test", "true")
                .requestBody(requestBody)
                .build();

        String response = template.applyContent(request);
        Assert.assertTrue(null != response && response.contains("success"));
    }

}