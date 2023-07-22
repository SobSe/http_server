import org.apache.http.NameValuePair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Request {
    private String typeRequest;
    private String method;
    private Map<String, List<String>> paramsQuery = new HashMap<>();
    private Map<String, List<String>> paramsPost = new HashMap<>();

    public Request(String typeRequest, String method, List<NameValuePair> paramsQuery, List<NameValuePair> paramsPost) {
        this.typeRequest = typeRequest;
        this.method = method;

        for (NameValuePair param : paramsQuery) {
            List<String> listParams = this.paramsQuery
                    .computeIfAbsent(param.getName(), k -> new ArrayList<>());
            listParams.add(param.getValue());
        }

        if (paramsPost != null) {
            for (NameValuePair param : paramsPost) {
                List<String> listParams = this.paramsPost
                        .computeIfAbsent(param.getName(), k -> new ArrayList<>());
                listParams.add(param.getValue());
            }
        }
    }

    public String getTypeRequest() {
        return typeRequest;
    }

    public String getMethod() {
        return method;
    }

    public List<String> getQueryParam(String name) {
        return new ArrayList<>(paramsQuery.getOrDefault(name, new ArrayList<>()));
    }

    public Map<String, List<String>> getQueryParams() {
        return new HashMap<>(paramsQuery);
    }

    public List<String> getPostParam(String name) {
        return new ArrayList<>(paramsPost.getOrDefault(name, new ArrayList<>()));
    }

    public Map<String, List<String>> getPostParams() {
        return new HashMap<>(paramsPost);
    }

}
