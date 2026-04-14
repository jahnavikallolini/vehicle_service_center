package com.autoservice;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.*;

public class VehicleHandler implements HttpHandler {
    private static final String BASE = "/api/vehicles";

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (Server.handleOptions(ex)) return;
        String method = ex.getRequestMethod().toUpperCase();
        String path   = ex.getRequestURI().getPath(); // e.g. /api/vehicles or /api/vehicles/5
        String query  = ex.getRequestURI().getQuery(); // e.g. customer_id=3
        String id     = Server.getPathParam(ex, BASE);

        try {
            switch (method) {
                case "GET"  -> { if (id == null) listAll(ex, query); else getOne(ex, id); }
                case "POST" -> create(ex);
                case "PUT"  -> update(ex, id);
                default     -> Server.sendResponse(ex, 405, "{\"error\":\"Method not allowed\"}");
            }
        } catch (Exception e) {
            Server.sendResponse(ex, 500, "{\"error\":\"" + e.getMessage().replace("\"","'") + "\"}");
        }
    }

    private void listAll(HttpExchange ex, String query) throws Exception {
        String sql = "SELECT v.*, c.name as customer_name FROM Vehicles v JOIN Customers c USING(customer_id)";
        if (query != null && query.startsWith("customer_id=")) {
            sql += " WHERE v.customer_id=" + Integer.parseInt(query.split("=")[1]);
        }
        sql += " ORDER BY v.vehicle_number";
        try (Connection c = DB.get(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery(sql);
            JSONArray arr = new JSONArray();
            while (rs.next()) arr.put(rowToJson(rs));
            Server.sendResponse(ex, 200, arr.toString());
        }
    }

    private void getOne(HttpExchange ex, String id) throws Exception {
        String sql = "SELECT v.*, c.name as customer_name FROM Vehicles v JOIN Customers c USING(customer_id) WHERE v.vehicle_id=?";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(id));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) Server.sendResponse(ex, 200, rowToJson(rs).toString());
            else Server.sendResponse(ex, 404, "{\"error\":\"Not found\"}");
        }
    }

    private void create(HttpExchange ex) throws Exception {
        JSONObject body = new JSONObject(Server.readBody(ex));
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO Vehicles(customer_id,vehicle_number,brand,model,fuel_type,purchase_year) VALUES(?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, body.getInt("customer_id"));
            ps.setString(2, body.getString("vehicle_number"));
            ps.setString(3, body.getString("brand"));
            ps.setString(4, body.getString("model"));
            ps.setString(5, body.getString("fuel_type"));
            ps.setInt(6, body.getInt("purchase_year"));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            keys.next();
            Server.sendResponse(ex, 201, "{\"vehicle_id\":" + keys.getInt(1) + "}");
        }
    }

    private void update(HttpExchange ex, String id) throws Exception {
        JSONObject body = new JSONObject(Server.readBody(ex));
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE Vehicles SET customer_id=?,vehicle_number=?,brand=?,model=?,fuel_type=?,purchase_year=? WHERE vehicle_id=?")) {
            ps.setInt(1, body.getInt("customer_id"));
            ps.setString(2, body.getString("vehicle_number"));
            ps.setString(3, body.getString("brand"));
            ps.setString(4, body.getString("model"));
            ps.setString(5, body.getString("fuel_type"));
            ps.setInt(6, body.getInt("purchase_year"));
            ps.setInt(7, Integer.parseInt(id));
            ps.executeUpdate();
            Server.sendResponse(ex, 200, "{\"success\":true}");
        }
    }

    private JSONObject rowToJson(ResultSet rs) throws SQLException {
        JSONObject o = new JSONObject()
                .put("vehicle_id",     rs.getInt("vehicle_id"))
                .put("customer_id",    rs.getInt("customer_id"))
                .put("vehicle_number", rs.getString("vehicle_number"))
                .put("brand",          rs.getString("brand"))
                .put("model",          rs.getString("model"))
                .put("fuel_type",      rs.getString("fuel_type"))
                .put("purchase_year",  rs.getInt("purchase_year"));
        try { o.put("customer_name", rs.getString("customer_name")); } catch (SQLException ignored) {}
        return o;
    }
}
