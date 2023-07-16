import java.io.*;
import java.net.ServerSocket;

public class Main {
    public static void main(String[] args) {
        try (final var serverSocket = new ServerSocket(9999)) {
            var server = new Server(serverSocket);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
