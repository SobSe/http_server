import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
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
                out.write((
                        "HTTP/1.1 400 Bad request\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
                return;
            }

            if (handlers.get(parts[0]) == null) {
                // just close socket
                out.write((
                        "HTTP/1.1 400 Bad request\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
                return;
            }

            Request request = new Request(parts[0],parts[1]);
            if (handlers.get(request.getTypeRequest()).get(request.getMethod()) == null) {
                out.write((
                        "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
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

    public void addHandler(String typeRequest, String method, Handler handler) {
        Map<String, Handler> currentHandlers = handlers.get(typeRequest);
        if (currentHandlers == null) {
            currentHandlers = new ConcurrentHashMap<>();
            handlers.put(typeRequest, currentHandlers);
        }
        currentHandlers.put(method, handler);
    }
}
