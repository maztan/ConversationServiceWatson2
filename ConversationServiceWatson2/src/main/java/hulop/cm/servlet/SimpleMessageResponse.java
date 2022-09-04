package hulop.cm.servlet;

import com.ibm.watson.assistant.v2.model.MessageContext;
import com.ibm.watson.assistant.v2.model.MessageOutput;
import com.ibm.watson.assistant.v2.model.MessageResponse;

public class SimpleMessageResponse extends MessageResponse {

    public  SimpleMessageResponse(MessageOutput output, MessageContext context, String userId){
        this.output = output;
        this.context = context;
        this.userId = userId;
    }
    public MessageOutput getOutput() {
        return this.output;
    }

    public MessageContext getContext() {
        return this.context;
    }

    public String getUserId() {
        return this.userId;
    }
}
