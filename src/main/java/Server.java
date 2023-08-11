
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
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
            Request request = new RequestParser(handlers, in).parse();
            if (request == null) {
                badRequest(out);
                return;
            }

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
