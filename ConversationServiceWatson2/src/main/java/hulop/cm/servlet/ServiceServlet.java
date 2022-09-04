package hulop.cm.servlet;

import com.ibm.watson.assistant.v2.model.*;
import hulop.cm.qa.WatsonHelper2;
import hulop.cm.util.CommonUtil;
import hulop.cm.util.LogHelper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

@WebServlet("/service")
public class ServiceServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private LogHelper logHelper = new LogHelper();
    private JSONObject mLastResultMap = new JSONObject();

    /**
     * @see HttpServlet#HttpServlet()
     */
    public ServiceServlet() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String id = request.getParameter("id");
        if (id == null || id.isEmpty()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String lang = CommonUtil.langFilter(request.getParameter("lang"));
        try {
            CommonUtil.load("/data/messages/" + lang + ".json");
            if (CommonUtil.getConfig().getJSONObject("watson_config").getString("workspace_" + lang).charAt(0) == '!') {
                lang = "en";
            }
        } catch (Exception e) {
            lang = "en";
        }
        final String clientId = id != null ? id : request.getSession(true).getId();
        final String text = request.getParameter("text");
        final Object bodyObj = CommonUtil.getJSON(request);
        if (bodyObj != null) {
            System.out.println(bodyObj);
        }
        System.out.println("request text: " + text);
        System.out.println("Lang: " + lang);
        WatsonHelper2 watHelper = WatsonHelper2.getInstance(lang);
        //WatsonHelper watHelper = WatsonHelper.getInstance(lang);
        MessageResponse result = null;
        JSONObject context = null;
        try {
            if (bodyObj instanceof JSONObject) {
                try {
                    context = ((JSONObject) bodyObj).optJSONObject("context");
                    MessageResponse lastResult = watHelper.getLastResult(clientId);
                    if (lastResult != null) {
                        MessageContext lastContext = lastResult.getContext();
                        for (Iterator<String> it = context.keys(); it.hasNext();) {
                            String key = it.next();
                            lastContext.integrations().put(key, context.get(key));
                        }
                        watHelper.setLastResult(clientId, lastResult);
                    }
                } catch (Exception ex) {
                    System.err.println(ex);
                }
            }
            try {
                System.out.println("Posting message");
                result = watHelper.postMessage(clientId, text, context);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            int errorCount = 0;
            MessageResponse lastResult = result;
            if (result == null) {
                try {
                    try {
                        errorCount = mLastResultMap.getJSONObject(clientId).getJSONObject("context")
                                .getInt("error_count");
                    } catch (Exception e) {
                    }
                    JSONObject messages = CommonUtil.load("/data/messages/" + lang + ".json");
                    String agent_name = messages.getString("ERROR");
                    if (errorCount++ < 1) {
                        lastResult = simpleResult(messages.getString("TRY_AGAIN"), agent_name, false);
                    } else {
                        errorCount = 0;
                        lastResult = simpleResult(messages.getString("TRY_AGAIN_LATER"), agent_name, true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (lastResult != null) {
                if (lastResult.getContext() != null) {
                    lastResult.getContext().integrations().put("error_count", errorCount);
                }
                CommonUtil.sendJSON(lastResult, response);
                mLastResultMap.put(clientId, lastResult);
                try {
                    logHelper.saveLog(clientId, lastResult);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private SimpleMessageResponse simpleResult(String output_text, String agent_name, boolean finish)
            throws Exception {
        /*MessageInput.Builder messageInputBuilder = new MessageInput.Builder();
        if (input_text != null) {
            messageInputBuilder.text(input_text);
        }*/
        //MessageOutput messageOutput = new MessageOutput;

        MessageContext messageContext = new MessageContext.Builder().integrations(
                Map.ofEntries(
                        Map.entry("conversation_id", ""),
                        Map.entry("agent_name", agent_name),
                        Map.entry("finish", finish),
                        Map.entry("system", new JSONObject().put("dialog_request_counter", 1).put("dialog_turn_counter", 1))
                )
        ).build();

        //JSONObject result = new JSONObject();
        //result.put("output", output);
        MessageOutput messageOutput = new MessageOutput();
        messageOutput.getUserDefined().putAll(Map.ofEntries(
                Map.entry("text", new JSONArray(output_text.split("\n"))),
                Map.entry("log_messages", new String[0]),
                Map.entry("nodes_visited", new String[0])
        ));
        return new SimpleMessageResponse(messageOutput,messageContext,  "");
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

}
