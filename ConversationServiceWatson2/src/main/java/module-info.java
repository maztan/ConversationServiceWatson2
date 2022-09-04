module ConversationServiceWatson {
    requires org.json;

    requires sdk.core;
    //requires ibm.watson;
    requires assistant;
    requires jakarta.servlet;

    requires org.apache.httpcomponents.client5.httpclient5;
    requires org.apache.httpcomponents.client5.httpclient5.fluent;
    requires org.apache.httpcomponents.core5.httpcore5;
}