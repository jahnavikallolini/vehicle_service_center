package com.autoservice;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StaticHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/") || path.isEmpty()) path = "/index.html";

        InputStream is = getClass().getResourceAsStream("/static" + path);

        if (is != null) {
            byte[] content = is.readAllBytes();
            String mime = getMime(path);

            ex.getResponseHeaders().add("Content-Type", mime);
            ex.sendResponseHeaders(200, content.length);

            try (OutputStream os = ex.getResponseBody()) {
                os.write(content);
            }
        } else {
            String body = "404 Not Found";
            ex.sendResponseHeaders(404, body.length());
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body.getBytes());
            }
        }
    }

    private String getMime(String path) {
        if (path.endsWith(".html")) return "text/html; charset=UTF-8";
        if (path.endsWith(".css"))  return "text/css";
        if (path.endsWith(".js"))   return "application/javascript";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".png"))  return "image/png";
        if (path.endsWith(".svg"))  return "image/svg+xml";
        return "application/octet-stream";
    }
}
