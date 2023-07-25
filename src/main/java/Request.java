import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.util.Streams;
import org.apache.http.NameValuePair;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class Request {
    private final String typeRequest;
    private final String method;
    private final Map<String, List<String>> paramsQuery = new HashMap<>();
    private final Map<String, List<String>> paramsPost = new HashMap<>();
    private final Map<String, List<Part>> parts = new HashMap<>();
    private final InputStream in;
    private final String contentType;
    private final String contentLength;

    public Request(String typeRequest, String method,
                   List<NameValuePair> paramsQuery,
                   List<NameValuePair> paramsPost,
                   InputStream in,
                   String contentType,
                   String contentLength
    ) {
        this.typeRequest = typeRequest;
        this.method = method;
        this.in = in;
        this.contentLength = contentLength;
        this.contentType= contentType;

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

        if (contentType!= null && contentType.startsWith("multipart/form-data")) {
            parsParts();
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

    public List<Part> getPart(String name) {
        return new ArrayList<>(parts.getOrDefault("name", new ArrayList<>()));
    }

    public Map<String, List<Part>> getParts() {
        return new HashMap<>(parts);
    }

    private void parsParts() {
        RequestFileUpload fileUpload = new RequestFileUpload();
        try {
            FileItemIterator iterStream = fileUpload.getItemIterator(this);
            while (iterStream.hasNext()) {
                FileItemStream item = iterStream.next();
                String name = item.getFieldName();
                InputStream stream = item.openStream();
                Part part;
                if (!item.isFormField()) {
                    byte[] content = stream.readAllBytes();
                    part = new Part(false, content);
                } else {
                    String value = Streams.asString(stream);
                    part = new Part(true, value);
                }
                List<Part> listParts = this.parts
                        .computeIfAbsent(name, k -> new ArrayList<>());
                listParts.add(part);
            }
        } catch (FileUploadException | IOException e) {
            e.printStackTrace();
        }
    }

    public InputStream getIn() {
        return in;
    }

    public String getContentType() {
        return contentType;
    }

    public String getContentLength() {
        return contentLength;
    }
}
