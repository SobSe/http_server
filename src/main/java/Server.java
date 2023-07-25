import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class Server {
    private final ServerSocket serverSocket;
    private final Map<String, Map<String, Handler>> handlers = new ConcurrentHashMap<>();

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void start() {
        final var threadPool = Executors.newFixedThreadPool(64);
        while (true) {

            try {
                Socket client = serverSocket.accept();
                threadPool.submit(() -> executeRequest(client));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //threadPool.shutdown();
    }

    private void executeRequest(Socket client) {
        try (
                final var in = new BufferedInputStream(client.getInputStream());
                final var out = new BufferedOutputStream(client.getOutputStream())
        ) {
            // лимит на request line + заголовки
            final var limit = 4096;

            in.mark(limit);
            final var buffer = new byte[limit];
            final var read = in.read(buffer);

            // ищем request line
            final var requestLineDelimiter = new byte[]{'\r', '\n'};
            final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
            if (requestLineEnd == -1) {
                badRequest(out);
                return;
            }

            // read only request line for simplicity
            // must be in form GET /path HTTP/1.1
            final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd));
            final var parts = requestLine.split(" ");

            if (parts.length != 3) {
                // just close socket
                badRequest(out);
                return;
            }

            if (handlers.get(parts[0]) == null) {
                // just close socket
                badRequest(out);
                return;
            }

            final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
            final var headersStart = requestLineEnd + requestLineDelimiter.length;
            final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
            if (headersEnd == -1) {
                badRequest(out);
                return;
            }

            // отматываем на начало буфера
            in.reset();
            // пропускаем requestLine
            in.skip(headersStart);

            final var headersBytes = in.readNBytes(headersEnd - headersStart);
            final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));

            Optional<String> contentType = extractHeader(headers, "Content-Type");
            Optional<String> contentLength = extractHeader(headers, "Content-Length");
            URI path = URI.create(parts[1]);
            List<NameValuePair> paramsQuery = URLEncodedUtils.parse(path, StandardCharsets.UTF_8);
            List<NameValuePair> paramsPost = getParamsPost(in
                    , contentType.orElse(null)
                    , contentLength.orElse(null)
                    , headersDelimiter
            );

            Request request = new Request(parts[0],
                    path.getPath(),
                    paramsQuery,
                    paramsPost,
                    in,
                    contentType.orElse(null),
                    contentLength.orElse(null)
            );

            if (handlers.get(request.getTypeRequest()).get(request.getMethod()) == null) {
                resorseNotFound(out);
                return;
            }
            handlers.get(request.getTypeRequest())
                    .get(request.getMethod())
                    .handle(request, out);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static List<NameValuePair> getParamsPost(BufferedInputStream in,
                                                     String contentType,
                                                     String contentLength,
                                                     byte[] headersDelimiter
    ) throws IOException {
        List<NameValuePair> paramsPost = null;
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

    private Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    private static void resorseNotFound(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    public void addHandler(String typeRequest, String method, Handler handler) {
        Map<String, Handler> currentHandlers = handlers.get(typeRequest);
        if (currentHandlers == null) {
            currentHandlers = new ConcurrentHashMap<>();
            handlers.put(typeRequest, currentHandlers);
        }
        currentHandlers.put(method, handler);
    }

    // from Google guava with modifications
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
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
}
