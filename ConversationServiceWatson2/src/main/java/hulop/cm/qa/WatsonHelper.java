
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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ContentType;

import hulop.cm.util.CommonUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WatsonHelper extends QAHelper {
    private final String mLang;
    private JSONObject mLastResultMap = new JSONObject();
    private Map<String, Long> mLastPostMap = new HashMap<String, Long>();

    private String mEndpoint, mUsername, mPassword, mWorkspace;
    private boolean mIgnoreCert;

    private static final Map<String, WatsonHelper> instances = new HashMap<String, WatsonHelper>();

    public static WatsonHelper getInstance(String lang) {
        WatsonHelper instance = instances.get(lang);
        if (instance == null) {
            instances.put(lang, instance = new WatsonHelper(lang));
        }
        return instance;
    }

    protected WatsonHelper(String lang) {
        super();
        mLang = lang;
        try {
            JSONObject config = CommonUtil.getConfig().getJSONObject("watson_config");
            mEndpoint = System.getenv("CONV_WATSON_ENDPOINT");
            if (mEndpoint == null) {
                mEndpoint = config.getString("endpoint");
            }
            mUsername = config.getString("username");
            mPassword = config.getString("password");
            mWorkspace = config.getString("workspace_" + lang);
            mIgnoreCert = config.getBoolean("ignoreCert");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public JSONObject postMessage(String clientId, String text, JSONObject clientContext) throws Exception {
        JSONObject input = new JSONObject();
        if (text != null) {
            input.put("text", text);
        }
        JSONObject requestBody = new JSONObject();
        boolean hasText = text != null && text.length() > 0;
        if (hasText && !mLastResultMap.has(clientId)) {
            postMessage(clientId, null, null);
        }
        if (mLastResultMap.has(clientId)) {
            JSONObject lastResult = mLastResultMap.getJSONObject(clientId);
            if (hasText && lastResult.has("context")) {
                try {
                    JSONObject context = lastResult.getJSONObject("context");
                    JSONObject copy = new JSONObject(context.toString()); //clone
                    // context.remove("dest_info");
                    // context.remove("candidates_info");
                    copy.remove("output_pron");
                    copy.remove("navi");
                    requestBody.put("context", copy);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        JSONObject requestContext = requestBody.optJSONObject("context");
        if (requestContext == null) {
            requestBody.put("context", requestContext = new JSONObject());
        }
        long now = System.currentTimeMillis();
        Long lastWelcome = mLastPostMap.put(clientId, now);
        if (lastWelcome != null) {
            requestContext.put("elapsed_time", now - lastWelcome.longValue());
        }
        requestBody.put("alternate_intents", true);
        requestBody.put("input", input);
        addClientContext(clientContext, requestContext);
        String api = String.format(mEndpoint, mWorkspace);
        System.out.println(api);
        System.out.println("---- start of request ----\n" + requestBody.toString(4) + "\n---- end ----");

        if ("$CONTEXT_DEBUG$".equals(text)) {
            return requestBody;
        }
        Request request = Request.post(new URI(api)).bodyString(requestBody.toString(), ContentType.APPLICATION_JSON);

        JSONObject response = (JSONObject) execute(mIgnoreCert, mUsername, mPassword, request);
        JSONObject responseContext = response.optJSONObject("context");
        if (responseContext != null) {
            try {
                removeClientContext(responseContext);
                System.out.println("---- start of response ----\n" + response.toString(4) + "\n---- end ----");
                if (!responseContext.has("output_pron")) {
                    ResponseHandler handler = new ResponseHandler(response);
                    handler.save();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        setLastResult(clientId, response);
        return response;
    }

    public JSONObject getLastResult(String clientId) {
        if (mLastResultMap.has(clientId)) {
            try {
                return mLastResultMap.getJSONObject(clientId);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void setLastResult(String clientId, JSONObject result) {
        try {
            mLastResultMap.put(clientId, result);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private class ResponseHandler {
        private final JSONObject response;
        private String text, pron;

        public ResponseHandler(JSONObject response) throws JSONException {
            JSONArray array = response.getJSONObject("output").getJSONArray("text");
            String join = array.join("\n");
            this.response = response;
            this.text = join.replaceAll("(\\.{3,})", "");
            this.pron = join.replaceAll("(\\.{3,})", "ja".equals(mLang) ? "。\n\n" : "\n\n");
        }

        public void save() throws JSONException {
            response.getJSONObject("output").put("text", new JSONArray(text.split("\n")));
            response.getJSONObject("context").put("output_pron", pron);
        }
    }
}

