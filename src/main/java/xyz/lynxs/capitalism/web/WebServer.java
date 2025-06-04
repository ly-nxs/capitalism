package xyz.lynxs.capitalism.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import xyz.lynxs.capitalism.Capitalism;
import xyz.lynxs.capitalism.Config;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.Executors;

public class WebServer {
    private static com.sun.net.httpserver.HttpServer server;
    private static boolean isRunning = false;
    private static PriceTracker priceTracker;

    public static synchronized void start(PriceTracker tracker) {
        if (isRunning) {
            Capitalism.LOGGER.warn("Web server is already running!");
            return;
        }

        priceTracker = tracker;
        int port = Config.getWebPort();

        try {
            server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(Executors.newFixedThreadPool(Config.getThreads())); // Increased thread pool

            // API endpoints
            server.createContext("/api/prices", WebServer::handlePricesRequest);
            server.createContext("/api/history", WebServer::handleHistoryRequest);
            server.createContext("/api/item/", WebServer::handleSingleItemRequest);
            server.createContext("/api/status", WebServer::handleStatusRequest);

            server.createContext("/api/reload", exchange -> {
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.getResponseHeaders().set("Upgrade", "websocket");
                    exchange.getResponseHeaders().set("Connection", "Upgrade");
                    exchange.sendResponseHeaders(101, -1); // Switching Protocols

                    //TODO: properly handle the WebSocket handshake
                }
            });
            // Static files from resources
            server.createContext("/", WebServer::handleResourceRequest);

            // Add error handler
            server.setExecutor(Executors.newCachedThreadPool());

            server.start();
            isRunning = true;
            Capitalism.LOGGER.info("Web UI available at http://localhost:{}/index.html", port);
        } catch (IOException e) {
            Capitalism.LOGGER.error("Failed to start web server on port {}", port, e);
        }
    }

    private static void handlePricesRequest(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        try (exchange) {
            JsonObject response = new JsonObject();
            response.add("prices", new Gson().toJsonTree(priceTracker.getAllPrices()));
            response.addProperty("lastUpdated", priceTracker.getLastUpdateTime().toString());

            sendJsonResponse(exchange, response);
        }
    }

    private static void handleHistoryRequest(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        try (exchange) {
            String query = exchange.getRequestURI().getQuery();
            String itemParam = getQueryParameter(query, "item");
            String limitParam = getQueryParameter(query, "limit");

            if (itemParam == null) {
                sendErrorResponse(exchange, 400, "Missing 'item' parameter");
                return;
            }

            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemParam));
            if (item == null) {
                sendErrorResponse(exchange, 404, "Item not found");
                return;
            }

            int limit = 50; // default
            try {
                if (limitParam != null) {
                    limit = Integer.parseInt(limitParam);
                    limit = Math.min(1000, Math.max(1, limit)); // clamp between 1-1000
                }
            } catch (NumberFormatException e) {
                sendErrorResponse(exchange, 400, "Invalid limit parameter");
                return;
            }

            JsonObject response = new JsonObject();
            response.add("history", new Gson().toJsonTree(
                    priceTracker.getRecentPriceHistory(item, limit)
            ));
            sendJsonResponse(exchange, response);
        }
    }

    private static void handleSingleItemRequest(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            String itemId = path.substring("/api/item/".length());

            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
            if (item == null) {
                sendErrorResponse(exchange, 404, "Item not found");
                return;
            }

            JsonObject response = new JsonObject();
            response.addProperty("item", itemId);
            response.addProperty("currentPrice", priceTracker.getCurrentPrice(item));
            response.add("recentHistory", new Gson().toJsonTree(
                    priceTracker.getRecentPriceHistory(item, 10)
            ));

            sendJsonResponse(exchange, response);
        }
    }

    private static void handleStatusRequest(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        try (exchange) {
            JsonObject response = new JsonObject();
            response.addProperty("status", "online");
            response.addProperty("serverTime", Instant.now().toString());
            response.addProperty("itemsTracked", priceTracker.getAllPrices().size());
            response.addProperty("lastUpdate", priceTracker.getLastUpdateTime().toString());

            sendJsonResponse(exchange, response);
        }
    }

    private static void handleResourceRequest(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }

        // Security check
        if (path.contains("..") || path.startsWith("/api")) {
            sendErrorResponse(exchange, 403, "Forbidden");
            return;
        }

        Path resourcePath = Path.of(Config.getWebPath() + path);


            String contentType = getContentType(path);
            try(InputStream stream = resourcePath.toUri().toURL().openStream()) {

                byte[] bytes =  stream.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }

    }

    private static String getQueryParameter(String query, String paramName) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 0 && pair[0].equals(paramName)) {
                return pair.length > 1 ? pair[1] : "";
            }
        }
        return null;
    }

    private static String getContentType(String path) {
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".json")) return "application/json";
        return "text/html";
    }

    private static void sendJsonResponse(com.sun.net.httpserver.HttpExchange exchange, JsonObject response) throws IOException {
        String json = new Gson().toJson(response);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, json.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void sendErrorResponse(com.sun.net.httpserver.HttpExchange exchange, int code, String message) throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        error.addProperty("status", code);

        String json = new Gson().toJson(error);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, json.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static synchronized void stop() {
        if (server != null) {
            server.stop(0);
            isRunning = false;
            Capitalism.LOGGER.info("Web server stopped");
        }
    }

    public static synchronized void reload(PriceTracker priceTracker) {
        stop();
        start(priceTracker);
    }

    public static boolean isRunning() {
        return isRunning;
    }
}