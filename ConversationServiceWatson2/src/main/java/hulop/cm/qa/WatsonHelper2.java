
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

import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.watson.assistant.v2.Assistant;
import com.ibm.watson.assistant.v2.model.*;
import hulop.cm.util.CommonUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import com.ibm.cloud.sdk.core.http.Response;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;

public class WatsonHelper2 {
    private final String mLang;
    private String sessionId;
    private Map<String, MessageResponse> mLastResultMap = new HashMap<>();
    private Map<String, Long> mLastPostMap = new HashMap<String, Long>();

    private IamAuthenticator authenticator;
    private Assistant assistant;

    private String assistantId;
    private static final Map<String, WatsonHelper2> instances = new HashMap<>();

    public static WatsonHelper2 getInstance(String lang) {
        WatsonHelper2 instance = instances.get(lang);
        if (instance == null) {
            instances.put(lang, instance = new WatsonHelper2(lang));
        }
        return instance;
    }

    protected WatsonHelper2(String lang) {
        super();
        mLang = lang;
        JSONObject config = CommonUtil.getConfig();
        String apiKey = config.getString("api_key");
        authenticator = new IamAuthenticator.Builder().apikey(apiKey).build();
        assistant = new Assistant("2021-11-27", authenticator);
        JSONObject watsonConfig = config.getJSONObject("watson_config");
        assistant.setServiceUrl(watsonConfig.getString("endpoint"));
        assistantId = watsonConfig.getString("assistantId");
        System.out.println("creating session");
        sessionId = assistant.createSession(
                new CreateSessionOptions.Builder().assistantId(assistantId).build())
                .execute().getResult().getSessionId();
    }

    public MessageResponse postMessage(String clientId, String text, JSONObject clientContext) throws Exception {
        MessageInput messageInput = new MessageInput.Builder().messageType("text").text(text).build();
        MessageOptions.Builder messageOptionsBuilder = new MessageOptions.Builder().input(messageInput);
        messageOptionsBuilder.assistantId(assistantId).sessionId(sessionId);

        if(clientContext != null){
            Map<String,Object> context = new HashMap<>();
            for (String key: clientContext.keySet())
            {
                context.put(key, clientContext.get(key));
            }
            messageOptionsBuilder.context(new MessageContext.Builder().integrations(context).build());
        }

        Response<MessageResponse> response = assistant.message(messageOptionsBuilder.build()).execute();
        System.out.println("Response status: " + response.getStatusCode());
        if(response.getStatusCode() == 200){
            return response.getResult();
        }else{
            System.out.println(response);
        }

        return null;
    }

    public MessageResponse getLastResult(String clientId) {
        if (mLastResultMap.containsKey(clientId)) {
            try {
                return mLastResultMap.get(clientId);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void setLastResult(String clientId, MessageResponse result) {
        try {
            mLastResultMap.put(clientId, result);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}

