import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class FeedbackServer {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Server HTML form
        server.createContext("/", exchange -> {
            File file = new File("form.html");
            byte[] bytes = Files.readAllBytes(file.toPath());
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        });

        // Server CSS
        server.createContext("/form.css", exchange -> {
            File cssFile = new File("form.css");
            byte[] cssBytes = Files.readAllBytes(cssFile.toPath());
            exchange.getResponseHeaders().add("Content-Type", "text/css");
            exchange.sendResponseHeaders(200, cssBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(cssBytes);
            os.close();
     });

        // form submission
        server.createContext("/submit", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                BufferedReader br = new BufferedReader(isr);
                StringBuilder formData = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    formData.append(line);
                }

                Map<String, String> data = parseForm(formData.toString());

                // Save to DB
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    Connection conn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/feedbackdb", "root", "passw06d"
                    );
                    PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO feedback (name, message) VALUES (?, ?)"
                    );
                    stmt.setString(1, data.get("name"));
                    stmt.setString(2, data.get("message"));
                    stmt.executeUpdate();
                    conn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Response
                String response = "<h1>Thank you!</h1>";
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        });

        server.setExecutor(null); // default executor
        server.start();
        System.out.println("Server started at http://localhost:8080");
    }

    // Helper method to parse form data
    private static Map<String, String> parseForm(String formData) throws UnsupportedEncodingException {
        Map<String, String> map = new HashMap<>();
        String[] pairs = formData.split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=");
            if (parts.length == 2) {
                String key = java.net.URLDecoder.decode(parts[0], "UTF-8");
                String value = java.net.URLDecoder.decode(parts[1], "UTF-8");
                map.put(key, value);
            }
        }
        return map;
    }
}
