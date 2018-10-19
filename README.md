# Http

借助 OkHttp 简单的包装请求发送方法, 简化日志/打点/方法调用的工作.

## Usage

1. 核心文件为 `HttpTemplate.java`, 需要注意里面的 common parameter 和 signature 方法.

- Bean parameter 的支持, 示例如下

```java
public class HttpSample {

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
    
    private HttpTemplate template = new TestHttp();

    public void getTest() throws Exception {
        Request request = template.newRequestBuilder(HttpMethod.GET, "/test/verify_sign")
                .addHeader("AuthorizatiON", "Bearer abcde")
                .addUrlParam("nonce", RandomStringUtils.random(30))
                .addUrlParam("nonce", RandomStringUtils.random(30))
                .build();

        String response = template.applyContent(request);
        Assert.assertTrue(null != response && response.contains("success"));
    }
}
```

