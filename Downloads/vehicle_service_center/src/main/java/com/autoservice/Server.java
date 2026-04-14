package com.autoservice;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class Server {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Customer endpoints
        server.createContext("/api/customers", new CustomerHandler());
        // Vehicle endpoints
        server.createContext("/api/vehicles", new VehicleHandler());
        // Service catalog
        server.createContext("/api/services", new ServiceHandler());
        // Mechanics
        server.createContext("/api/mechanics", new MechanicHandler());
        // Service Orders
        server.createContext("/api/orders", new ServiceOrderHandler());
        // Parts
        server.createContext("/api/parts", new PartsHandler());
        // Bills
        server.createContext("/api/bills", new BillHandler());
        // Reports
        server.createContext("/api/reports", new ReportHandler());
        // Static frontend
        server.createContext("/", new StaticHandler());

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("AutoService backend running on http://localhost:8080");
    }

    // ─── CORS + response helpers ──────────────────────────────────────────────
    public static void sendResponse(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    public static boolean handleOptions(HttpExchange ex) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            sendResponse(ex, 204, "");
            return true;
        }
        return false;
    }

    public static String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static String getPathParam(HttpExchange ex, String base) {
        String path = ex.getRequestURI().getPath();
        String suffix = path.substring(base.length());
        if (suffix.startsWith("/")) suffix = suffix.substring(1);
        return suffix.isEmpty() ? null : suffix;
    }
}
