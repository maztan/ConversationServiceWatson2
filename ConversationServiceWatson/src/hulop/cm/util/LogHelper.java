package hulop.cm.util;

import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

public class LogHelper {
	private JSONObject mConfig = CommonUtil.getConfig();
	private String mEndpoint, mApiKey;

	public LogHelper() {
		try {
			JSONObject config = mConfig.getJSONObject("logging_config");
			mEndpoint = config.getString("endpoint");
			mApiKey = config.getString("api_key");
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public boolean saveLog(String clientId, JSONObject log) throws Exception {
		JSONObject data = new JSONObject();
		data.put("event", "conversation");
		data.put("client", clientId);
		data.put("timestamp", System.currentTimeMillis());
		data.put("log", log);
		Request request = Request.Post(new URI(mEndpoint)).bodyForm(Form.form().add("action", "insert")
				.add("auditor_api_key", mApiKey).add("data", new JSONArray().put(data).toString()).build());
		HttpResponse response = request.execute().returnResponse();
		return response.getStatusLine().getStatusCode() == 200;
	}

	public JSONArray getLog(String clientId, String start, String end, String skip, String limit) throws Exception {
		Form form = Form.form().add("action", "get").add("auditor_api_key", mApiKey).add("event", "conversation");
		if (clientId != null) {
			form.add("clientId", clientId);
		}
		if (start != null) {
			form.add("start", start);
		}
		if (end != null) {
			form.add("end", end);
		}
		if (skip != null) {
			form.add("skip", skip);
		}
		if (limit != null) {
			form.add("limit", limit);
		}
		Request request = Request.Post(new URI(mEndpoint)).bodyForm(form.build());
		return (new JSONArray(request.execute().returnContent().asString()));
	}

}
