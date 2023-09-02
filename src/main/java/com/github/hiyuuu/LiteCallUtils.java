package com.github.hiyuuu;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * リクエストを送信するクラス
 */
public class LiteCallUtils {

    public enum RequestMethod {
        GET, POST, PUT, DELETE, PATCH,
        HEAD, OPTIONS, TRACE, CONNECT
    }

    private URL url;
    private String method;
    private byte[] body;
    private Charset bodyCharset = StandardCharsets.UTF_8;
    private List<Properties> headers;
    private int timeout = 3000;

    /**
     * コンストラクター
     * @param url
     * @param Method
     * @throws MalformedURLException
     */
    public LiteCallUtils(String url, String Method) throws MalformedURLException {
        this.url =  new URL(url);
        this.method = Method;
    }
    /**
     * コンストラクター
     * @param url
     * @param method
     * @throws MalformedURLException
     */
    public LiteCallUtils(String url, RequestMethod method) throws MalformedURLException {
        this.url =  new URL(url);
        this.method = method.name();
    }
    /**
     * コンストラクター
     * @param url
     * @param Method
     * @param headers
     * @throws MalformedURLException
     */
    public LiteCallUtils(String url, String Method, List<Properties> headers) throws MalformedURLException {
        this.url =  new URL(url);
        this.method = Method;
        this.headers = headers;
    }
    /**
     * コンストラクター
     * @param url
     * @param method
     * @param headers
     * @throws MalformedURLException
     */
    public LiteCallUtils(String url, RequestMethod method, List<Properties> headers) throws MalformedURLException {
        this.url =  new URL(url);
        this.method = method.name();
        this.headers = headers;
    }

    /**
     * URLを設定
     * @param url
     * @throws MalformedURLException
     */
    public LiteCallUtils setURL(String url) throws MalformedURLException {
        this.url = new URL(url);
        return this;
    }

    /**
     * URLを取得
     * @return
     */
    public String getURL() { return this.url.toString(); }

    /**
     * タイムアウトを設定
     * @param timeout
     */
    public LiteCallUtils setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * タイムアウトを取得
     * @return
     */
    public int getTimeout() { return this.timeout; }

    /**
     * リクエストメソッドを設定
     * @param method
     */
    public LiteCallUtils setMethod(String method) {
        this.method = method;
        return this;
    }

    /**
     * リクエストメソッドを取得
     * @return
     */
    public String getMethod() { return this.method; }

    /**
     * エンコードを設定
     * @param charset
     */
    public LiteCallUtils setBodyCharset(Charset charset) {
        this.bodyCharset = charset;
        return this;
    }

    /**
     * エンコードを取得
     * @return charset
     */
    public Charset getBodyCharset() { return this.bodyCharset; }

    /**
     * ボディーをBYTE[]型で設定
     * @param body
     */
    public LiteCallUtils setBody(byte[] body) {
        this.body = body;
        return this;
    }

    /**
     * ボディーをSTRING型で設定
     * @param body
     */
    public LiteCallUtils setBody(String body) {
        this.body = body.getBytes(bodyCharset);
        return this;
    }

    /**
     * ボディーをBYTE[]型で取定
     */
    public byte[] getBody() { return this.body; }

    /**
     * ボディーをSTRING型で取得
     * @return
     */
    public String getBodyByString() { return new String(this.body, bodyCharset); }

    /**
     * ヘッダーを設定
     * @param key
     * @param value
     */
    public LiteCallUtils setHeader(String key, String value) {
        if (this.headers == null) {
            this.headers = new ArrayList<>();
        }
        Properties header = new Properties();
        header.setProperty("name", key);
        header.setProperty("value", value);
        this.headers.add(header);

        return this;
    }

    /**
     * ヘッダーを削除
     * @param key
     * @param value
     */
    public LiteCallUtils removeHeader(String key, String value) {
        if (this.headers != null) {
            this.headers.removeIf(f -> f.getProperty("name").equals(key) && f.getProperty("value").equals(value));
        }
        return this;
    }

    /**
     * ヘッダーを全削除
     */
    public LiteCallUtils removeAllHeaders() {
        if (this.headers != null) {
            this.headers.clear();
        }
        return this;
    }

    /**
     * 全ヘッダーを取得
     * @return
     */
    public List<Properties> getHeaders() { return this.headers; }

    /**
     * リクエストを送信
     * @return InputStream型
     * @throws IOException
     */
    public InputStream connect() throws IOException {
        HttpsURLConnection con = (HttpsURLConnection) this.url.openConnection();
        con.setDoOutput(true);
        con.setDoInput(true);

        List<RequestMethod> overrideMethods = Arrays.asList(
                RequestMethod.CONNECT,
                RequestMethod.DELETE,
                RequestMethod.GET,
                RequestMethod.HEAD,
                RequestMethod.OPTIONS,
                RequestMethod.PATCH,
                RequestMethod.PUT,
                RequestMethod.TRACE
        );
        if (overrideMethods.contains(method)) {
            con.setRequestMethod("POST");
            con.setRequestProperty("X-HTTP-Method-Override", method.toUpperCase());
        } else {
            con.setRequestMethod(method);
        }

        con.setReadTimeout(timeout);

        if (headers != null) {
            for (Properties f : headers) {
                con.setRequestProperty(f.getProperty("name"), f.getProperty("value"));
            }
        }

        if (body != null) {
            con.getOutputStream().write(body);
            con.getOutputStream().close();
        }

        return con.getInputStream();
    }

    public String connectString() throws IOException {
        InputStream is = connect();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, bodyCharset));

        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line);
        }
        is.close();

        return sb.toString();
    }

    @Override
    public String toString() {
        return "CallUtils{" +
                "url=" + url +
                ", method='" + method + '\'' +
                ", body='" + body + '\'' +
                ", headers=" + headers +
                ", timeout=" + timeout +
                '}';
    }
}