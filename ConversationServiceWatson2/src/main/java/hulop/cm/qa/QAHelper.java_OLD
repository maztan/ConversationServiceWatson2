/*******************************************************************************
 * Copyright (c) 2014, 2017  IBM Corporation, Carnegie Mellon University and others
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *******************************************************************************/

package hulop.cm.qa;

import java.security.AuthProvider;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.fluent.Content;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.fluent.Response;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.fluent.Executor;
import javax.net.ssl.SSLContext;

import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.net.Host;
import org.apache.hc.core5.util.Timeout;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.SSLContext;

public abstract class QAHelper {
    private static final int TIMEOUT = 15 * 1000;
    private static final String[] CLIENT_CONTEXT_KEYS = new String[] { "no_welcome", "latitude", "longitude", "floor", "building", "user_mode" };

    public abstract JSONObject postMessage(String clientId, String text, JSONObject context) throws Exception;

    public QAHelper() {
        super();
    }

    protected Object execute(boolean ignoreCert, String username, String password, Request request) throws Exception {
        CloseableHttpClient httpClient = null;
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        if (ignoreCert) {
            final SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                    .setHostnameVerifier(new NoopHostnameVerifier())
                    //.setSslContext(SSLContext.getInstance("https", ))
                    .build();
            final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(sslSocketFactory)
                    .build();
            httpClientBuilder.setConnectionManager(cm);
        }

        httpClient = httpClientBuilder.build();
        // setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)

        Executor executor = Executor.newInstance(httpClient);
        if (username != null) {
            executor.auth(HttpHost.create("ibm.com"), username, password.toCharArray());
        }
        Response response = executor.execute(request.connectTimeout(Timeout.of(TIMEOUT, TimeUnit.MILLISECONDS)).responseTimeout(Timeout.of(TIMEOUT, TimeUnit.MILLISECONDS)));
        Content content = response.returnContent();
        String strJSON = content.getType().getCharset() == null ? new String(content.asBytes(), "UTF-8")
                : content.asString();
        return new JSONObject(strJSON.replaceAll("\\\\u0000", ""));
    }

    protected void addClientContext(JSONObject clientContext, JSONObject requestContext) {
        if (clientContext != null) {
            for (String key : CLIENT_CONTEXT_KEYS) {
                if (clientContext.has(key)) {
                    try {
                        requestContext.put(key, clientContext.get(key));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    requestContext.remove(key);
                }
            }
        }
    }

    protected void removeClientContext(JSONObject responseContext) {
        for (String key : CLIENT_CONTEXT_KEYS) {
            responseContext.remove(key);
        }
    }
}