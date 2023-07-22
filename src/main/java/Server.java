import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import javax.lang.model.element.Name;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
                final var in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                final var out = new BufferedOutputStream(client.getOutputStream())
        ) {
            // read only request line for simplicity
            // must be in form GET /path HTTP/1.1
            final var requestLine = in.readLine();
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

            String header = null;
            List<String> headers = new ArrayList<>();
            while(!(header = in.readLine()).equals("")) {
                headers.add(header);
            }

            Optional<String> contentType = extractHeader(headers, "Content-Type");
            Optional<String> contentLength = extractHeader(headers, "Content-Length");
            URI path = URI.create(parts[1]);
            List<NameValuePair> paramsQuery = URLEncodedUtils.parse(path, StandardCharsets.UTF_8);
            List<NameValuePair> paramsPost = getParamsPost(in, contentType, contentLength);

            Request request = new Request(parts[0],path.getPath(), paramsQuery, paramsPost);
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

    private static List<NameValuePair> getParamsPost(BufferedReader in, Optional<String> contentType, Optional<String> contentLength) throws IOException {
        List<NameValuePair> paramsPost = null;
        if (contentType.isPresent()) {
            if (contentType.get().equals("application/x-www-form-urlencoded")) {
                if (contentLength.isPresent()) {
                    char[] cbuf = new char[Integer.parseInt(contentLength.get())];
                    int count = in.read(cbuf);
                    String stringParams = String.valueOf(cbuf);
                    paramsPost = URLEncodedUtils.parse(stringParams, StandardCharsets.UTF_8);
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
}
