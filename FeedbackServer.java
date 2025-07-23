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

        server.createContext("/feedback", exchange -> {
    if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(204, -1);
        return;
    }

    if ("GET".equals(exchange.getRequestMethod())) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Content-Type", "application/json");

        String json = getFeedbackAsJson(); // Get DB results as JSON string
        exchange.sendResponseHeaders(200, json.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(json.getBytes());
        os.close();
    }
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
                      "INSERT INTO feedback (name, message, course, rating) VALUES (?, ?, ?, ?)"
                    );
                   stmt.setString(1, data.get("name"));
                   stmt.setString(2, data.get("message"));
                   stmt.setString(3, data.get("course"));
                   stmt.setInt(4, Integer.parseInt(data.get("rating")));

                   stmt.executeUpdate();
                   conn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Response
String response = """
<!DOCTYPE html>
<html>
<head>
  <title>Thank You</title>
  <style>
    body {
      display: flex;
      justify-content: center;
      align-items: center;
      height: 100vh;
      margin: 0;
      font-family: Arial, sans-serif;
      background-color: #f0f4f8;
    }
    h1 {
      color:rgb(20, 132, 245);
    }
  </style>
</head>
<body>
  <h1>Thank you for your feedback!</h1>
</body>
</html>
""";
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

    private static String getFeedbackAsJson() {
    StringBuilder json = new StringBuilder("[");
    try {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection conn = DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/feedbackdb", "root", "passw06d"
        );
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT name, message, course, rating FROM feedback");

        boolean first = true;
        while (rs.next()) {
            if (!first) json.append(",");
            json.append("{");
            json.append("\"name\":\"").append(rs.getString("name")).append("\",");
            json.append("\"message\":\"").append(rs.getString("message")).append("\",");
            json.append("\"course\":\"").append(rs.getString("course")).append("\",");
            json.append("\"rating\":").append(rs.getInt("rating"));
            json.append("}");
            first = false;
        }

        conn.close();
    } catch (Exception e) {
        e.printStackTrace();
    }

    json.append("]");
    return json.toString();
}


    // Method to parse form data
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
