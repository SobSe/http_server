import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.util.Streams;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RequestParser {


    private final BufferedInputStream in;
    private final Map<String, Map<String, Handler>> handlers;
    private final List<String> headers;
    private String method;

    public RequestParser(Map<String, Map<String, Handler>> handlers, BufferedInputStream in) {
        this.in = in;
        this.handlers = handlers;
        headers = new ArrayList<>();
    }

    public Request parse() throws IOException{

        final var limit = 4096;

        in.mark(limit);
        final var buffer = new byte[limit];
        final var read = in.read(buffer);

        // ищем request line
        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) {
            //badRequest(out);
            return null;
        }

        // read only request line for simplicity
        // must be in form GET /path HTTP/1.1
        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd));
        final var partsRequestLine = requestLine.split(" ");

        if (partsRequestLine.length != 3) {
            // just close socket
            //badRequest(out);
            return null;
        }
        method = partsRequestLine[0];
        String path = partsRequestLine[1];
        if (handlers.get(partsRequestLine[0]) == null) {
            // just close socket
            //badRequest(out);
            return null;
        }

        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            //badRequest(out);
            return null;
        }

        // отматываем на начало буфера
        in.reset();
        // пропускаем requestLine
        in.skip(headersStart);

        final var headersBytes = in.readNBytes(headersEnd - headersStart);
        headers.addAll(Arrays.asList(new String(headersBytes).split("\r\n")));

        String contentType = extractHeader("Content-Type");
        String contentLength = extractHeader("Content-Length");
        URI uri = URI.create(path);
        List<NameValuePair> paramsQuery = URLEncodedUtils.parse(path, StandardCharsets.UTF_8);
        List<NameValuePair> paramsPost = getParamsPost(in
                , contentType
                , contentLength
                , headersDelimiter
        );
        Map<String, List<Part>> parts = parsParts(contentType);
        return new Request(method, uri.getPath(), paramsQuery, paramsPost, parts);
    }

    private static List<NameValuePair> getParamsPost(BufferedInputStream in,
                                                     String contentType,
                                                     String contentLength,
                                                     byte[] headersDelimiter
    ) throws IOException {
        List<NameValuePair> paramsPost = new ArrayList<>();
        if (contentType != null) {
            if (contentType.startsWith("application/x-www-form-urlencoded")) {
                if (contentLength != null) {
                    in.skip(headersDelimiter.length);
                    // вычитываем Content-Length, чтобы прочитать body
                    final var length = Integer.parseInt(contentLength);
                    final var bodyBytes = in.readNBytes(length);
                    final var body = new String(bodyBytes);
                    paramsPost = URLEncodedUtils.parse(body, StandardCharsets.UTF_8);
                }
            }
        }
        return paramsPost;
    }

    private Map<String, List<Part>> parsParts(String contentType) {
        FileUploadImpl fileUpload = new FileUploadImpl();
        Map<String, List<Part>> parts = new HashMap<>();
        if (contentType != null) {
            if (contentType.startsWith("multipart/form-data")) {
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
                        List<Part> listParts = parts
                                .computeIfAbsent(name, k -> new ArrayList<>());
                        listParts.add(part);
                    }
                } catch (FileUploadException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return parts;
    }

    private String extractHeader(String header) {
        return headers
                .stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst().orElse(null);
    }

    private int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    public InputStream getIn() {
        return in;
    }

    public String getContentType() {
        return extractHeader("Content-Type");
    }

    public String getContentLength() {
        return extractHeader("Content-Length");
    }

    public String getMethod() {
        return method;
    }
}
