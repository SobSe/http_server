import org.apache.http.NameValuePair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Request {
    private String typeRequest;
    private String method;
    private Map<String, List<String>> params = new HashMap<>();

    public Request(String typeRequest, String method, List<NameValuePair> params) {
        this.typeRequest = typeRequest;
        this.method = method;
        for (NameValuePair param : params) {
            List<String> listParams = this.params
                    .computeIfAbsent(param.getName(), k -> new ArrayList<>());
            listParams.add(param.getValue());
        }
    }

    public String getTypeRequest() {
        return typeRequest;
    }

    public String getMethod() {
        return method;
    }

    public List<String> getQueryParam(String name) {
        return new ArrayList<>(params.getOrDefault(name, new ArrayList<>()));
    }

    public Map<String, List<String>> getQueryParams() {
        return new HashMap<>(params);
    }

}
