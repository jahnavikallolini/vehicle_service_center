package com.autoservice;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.*;

public class CustomerHandler implements HttpHandler {
    private static final String BASE = "/api/customers";

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (Server.handleOptions(ex)) return;
        String method = ex.getRequestMethod().toUpperCase();
        String id = Server.getPathParam(ex, BASE);

        try {
            switch (method) {
                case "GET"    -> { if (id == null) listAll(ex); else getOne(ex, id); }
                case "POST"   -> create(ex);
                case "PUT"    -> update(ex, id);
                case "DELETE" -> delete(ex, id);
                default       -> Server.sendResponse(ex, 405, "{\"error\":\"Method not allowed\"}");
            }
        } catch (Exception e) {
            Server.sendResponse(ex, 500, "{\"error\":\"" + e.getMessage().replace("\"","'") + "\"}");
        }
    }

    private void listAll(HttpExchange ex) throws Exception {
        try (Connection c = DB.get(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery("SELECT * FROM Customers ORDER BY name");
            JSONArray arr = new JSONArray();
            while (rs.next()) arr.put(rowToJson(rs));
            Server.sendResponse(ex, 200, arr.toString());
        }
    }

    private void getOne(HttpExchange ex, String id) throws Exception {
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement("SELECT * FROM Customers WHERE customer_id=?")) {
            ps.setInt(1, Integer.parseInt(id));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) Server.sendResponse(ex, 200, rowToJson(rs).toString());
            else Server.sendResponse(ex, 404, "{\"error\":\"Not found\"}");
        }
    }

    private void create(HttpExchange ex) throws Exception {
        JSONObject body = new JSONObject(Server.readBody(ex));
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO Customers(name,phone,email,address) VALUES(?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, body.getString("name"));
            ps.setString(2, body.getString("phone"));
            ps.setString(3, body.optString("email", ""));
            ps.setString(4, body.optString("address", ""));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            keys.next();
            Server.sendResponse(ex, 201, "{\"customer_id\":" + keys.getInt(1) + "}");
        }
    }

    private void update(HttpExchange ex, String id) throws Exception {
        JSONObject body = new JSONObject(Server.readBody(ex));
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE Customers SET name=?,phone=?,email=?,address=? WHERE customer_id=?")) {
            ps.setString(1, body.getString("name"));
            ps.setString(2, body.getString("phone"));
            ps.setString(3, body.optString("email", ""));
            ps.setString(4, body.optString("address", ""));
            ps.setInt(5, Integer.parseInt(id));
            ps.executeUpdate();
            Server.sendResponse(ex, 200, "{\"success\":true}");
        }
    }

    private void delete(HttpExchange ex, String id) throws Exception {
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM Customers WHERE customer_id=?")) {
            ps.setInt(1, Integer.parseInt(id));
            ps.executeUpdate();
            Server.sendResponse(ex, 200, "{\"success\":true}");
        }
    }

    private JSONObject rowToJson(ResultSet rs) throws SQLException {
        return new JSONObject()
                .put("customer_id", rs.getInt("customer_id"))
                .put("name",        rs.getString("name"))
                .put("phone",       rs.getString("phone"))
                .put("email",       rs.getString("email"))
                .put("address",     rs.getString("address"));
    }
}
