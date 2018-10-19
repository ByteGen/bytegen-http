package com.bytegen.common.http.util;

import com.bytegen.common.http.HttpParam;
import okhttp3.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * User: xiang
 * Date: 2018/10/19
 * Desc:
 */
public class HttpUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtil.class);

    public static <T> List<NameValuePair> flatMapToPair(Map<String, T> map) {
        if (null == map || map.isEmpty()) {
            return Collections.emptyList();
        }

        List<NameValuePair> pairs = new ArrayList<>();
        map.forEach((k, v) -> {
            if (null != v) {
                if (v instanceof Collection) {
                    for (Object vv : (Collection) v) {
                        if (null != vv) {
                            pairs.add(new BasicNameValuePair(k, String.valueOf(vv)));
                        }
                    }
                } else if (v.getClass().isArray()) {
                    for (Object vv : (Object[]) v) {
                        if (null != vv) {
                            pairs.add(new BasicNameValuePair(k, String.valueOf(vv)));
                        }
                    }
                } else {
                    pairs.add(new BasicNameValuePair(k, String.valueOf(v)));
                }
            }
        });
        return pairs;
    }

    public static String concatUrl(String host, String uri, List<NameValuePair> queryParam) {
        Args.check(!StringUtils.isAllBlank(host, uri), "Host/uri can not be blank");

        String url;
        if (StringUtils.isBlank(host)) {
            url = uri;
        } else if (StringUtils.isBlank(uri)) {
            url = host;
        } else {
            if (uri.startsWith("/")) {
                if (host.endsWith("/")) {
                    url = host.substring(0, host.length() - 1) + uri;
                } else {
                    url = host + uri;
                }
            } else if (host.endsWith("/")) {
                url = host + uri;
            } else {
                url = host + "/" + uri;
            }
        }
        Args.check(url.trim().startsWith("http"), "Url invalid: " + url);

        if (queryParam != null && !queryParam.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            queryParam.stream()
                    .filter(nameValuePair -> StringUtils.isNotEmpty(nameValuePair.getValue()))
                    .forEach(p -> sb.append(p.getName()).append("=").append(urlEncode(p.getValue())).append("&"));

            if (sb.length() > 0) {
                sb.deleteCharAt(sb.length() - 1);
            }

            if (!url.contains("?")) {
                url = url + "?" + sb.toString();
            } else if (url.indexOf("?") == (url.length() - 1)) {
                url = url + sb.toString();
            } else {
                url = url + "&" + sb.toString();
            }
        }
        return url;
    }

    public static String linkSortedNoneEmptyArgs(List<NameValuePair> params, String... excludeKeys) {
        if (null == params || params.isEmpty()) {
            return "";
        }

        params.sort((a, b) -> {
            if (a.getName().equals(b.getName())) {
                return a.getValue().compareTo(b.getValue());
            }
            return a.getName().compareTo(b.getName());
        });

        List<String> exclude = null == excludeKeys ? Collections.emptyList() : Arrays.asList(excludeKeys);

        StringBuilder sb = new StringBuilder();
        params.stream()
                .filter(p -> !exclude.contains(p.getName()) && !StringUtils.isEmpty(p.getValue()))
                .forEach(p -> sb.append(p.getName()).append("=").append(p.getValue()).append("&"));

        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    public static String signURLAndRequestParams(String method, String uri, List<NameValuePair> params, String token, String secret) throws NoSuchAlgorithmException {

        StringBuilder beforeSign = new StringBuilder().append(method).append(uri);

        String paramString = HttpUtil.linkSortedNoneEmptyArgs(params, HttpParam.SIGN);
        if (!StringUtils.isEmpty(paramString)) {
            beforeSign.append("?").append(paramString).append(secret);
        } else {
            beforeSign.append("?").append(secret);
        }
        if (token != null) {
            beforeSign.append(token);
        }
        return signInput(beforeSign.toString());
    }

    public static String signInput(String input) throws NoSuchAlgorithmException {
        // TODO your signature method
        return "[mock signature]";
    }

    private static String urlEncode(String v) {
        try {
            return URLEncoder.encode(v, "utf-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Url encode failed: " + v, e);
        }
        return v;
    }

    public static boolean isURLEncodedForm(final MediaType contentType) {
        return (contentType != null) && contentType.toString().toLowerCase().startsWith("application/x-www-form-urlencoded");
    }

    public static boolean isJson(final MediaType contentType) {
        return (contentType != null) && contentType.toString().toLowerCase().startsWith("application/json");
    }

    public static boolean isMultipartFormData(final MediaType contentType) {
        return (contentType != null) && (contentType.toString().toLowerCase().startsWith("multipart/"));
    }
}
