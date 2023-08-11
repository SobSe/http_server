import org.apache.http.NameValuePair;

import java.util.*;
import java.util.stream.Collectors;

public class Request {
    private final String typeRequest;
    private final String method;
    private final List<NameValuePair> paramsQuery;
    private final List<NameValuePair> paramsPost;
    private final Map<String, List<Part>> parts;

    public Request(String typeRequest, String method,
                   List<NameValuePair> paramsQuery,
                   List<NameValuePair> paramsPost,
                   Map<String, List<Part>> paramsPart
    ) {
        this.typeRequest = typeRequest;
        this.method = method;
        this.paramsQuery = paramsQuery;
        this.paramsPost = paramsPost;
        this.parts = paramsPart;
    }

    public String getTypeRequest() {
        return typeRequest;
    }

    public String getMethod() {
        return method;
    }

    public List<NameValuePair> getQueryParam(String name) {
        return paramsQuery.stream()
                .filter(p -> (p.getName().equals(name)))
                .collect(Collectors.toList());
    }

    public List<NameValuePair> getQueryParams() {
        return Collections.unmodifiableList(paramsQuery);
    }

    public List<NameValuePair> getPostParam(String name) {
        return paramsPost.stream()
                .filter(p -> (p.getName().equals(name)))
                .collect(Collectors.toList());
    }

    public List<NameValuePair> getPostParams() {
        return Collections.unmodifiableList(paramsPost);
    }

    public List<Part> getPart(String name) {
        return Collections.unmodifiableList((parts.getOrDefault("name", new ArrayList<>())));
    }

    public Map<String, List<Part>> getParts() {
        return Collections.unmodifiableMap(parts);
    }
}
