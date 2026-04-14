package com.autoservice;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.*;

// ─── Services ────────────────────────────────────────────────────────────────
class ServiceHandler implements HttpHandler {
    private static final String BASE = "/api/services";
    @Override public void handle(HttpExchange ex) throws IOException {
        if (Server.handleOptions(ex)) return;
        String method = ex.getRequestMethod().toUpperCase();
        String id = Server.getPathParam(ex, BASE);
        try {
            switch (method) {
                case "GET"  -> { if (id == null) listAll(ex); else getOne(ex, id); }
                case "POST" -> create(ex);
                case "PUT"  -> update(ex, id);
                default     -> Server.sendResponse(ex, 405, "{}");
            }
        } catch (Exception e) { Server.sendResponse(ex, 500, "{\"error\":\"" + e.getMessage().replace("\"","'") + "\"}"); }
    }
    private void listAll(HttpExchange ex) throws Exception {
        try (Connection c = DB.get(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery("SELECT * FROM Services ORDER BY service_name");
            JSONArray arr = new JSONArray();
            while (rs.next()) arr.put(row(rs));
            Server.sendResponse(ex, 200, arr.toString());
        }
    }
    private void getOne(HttpExchange ex, String id) throws Exception {
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement("SELECT * FROM Services WHERE service_id=?")) {
            ps.setInt(1, Integer.parseInt(id));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) Server.sendResponse(ex, 200, row(rs).toString());
            else Server.sendResponse(ex, 404, "{\"error\":\"Not found\"}");
        }
    }
    private void create(HttpExchange ex) throws Exception {
        JSONObject b = new JSONObject(Server.readBody(ex));
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO Services(service_name,base_cost,estimated_hours) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, b.getString("service_name"));
            ps.setDouble(2, b.getDouble("base_cost"));
            ps.setDouble(3, b.getDouble("estimated_hours"));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys(); keys.next();
            Server.sendResponse(ex, 201, "{\"service_id\":" + keys.getInt(1) + "}");
        }
    }
    private void update(HttpExchange ex, String id) throws Exception {
        JSONObject b = new JSONObject(Server.readBody(ex));
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE Services SET service_name=?,base_cost=?,estimated_hours=? WHERE service_id=?")) {
            ps.setString(1, b.getString("service_name"));
            ps.setDouble(2, b.getDouble("base_cost"));
            ps.setDouble(3, b.getDouble("estimated_hours"));
            ps.setInt(4, Integer.parseInt(id));
            ps.executeUpdate();
            Server.sendResponse(ex, 200, "{\"success\":true}");
        }
    }
    private JSONObject row(ResultSet rs) throws SQLException {
        return new JSONObject()
                .put("service_id",       rs.getInt("service_id"))
                .put("service_name",     rs.getString("service_name"))
                .put("base_cost",        rs.getDouble("base_cost"))
                .put("estimated_hours",  rs.getDouble("estimated_hours"));
    }
}

// ─── Mechanics ───────────────────────────────────────────────────────────────
class MechanicHandler implements HttpHandler {
    private static final String BASE = "/api/mechanics";
    @Override public void handle(HttpExchange ex) throws IOException {
        if (Server.handleOptions(ex)) return;
        String method = ex.getRequestMethod().toUpperCase();
        String id = Server.getPathParam(ex, BASE);
        try {
            switch (method) {
                case "GET"  -> { if (id == null) listAll(ex, ex.getRequestURI().getQuery()); else getOne(ex, id); }
                case "POST" -> create(ex);
                case "PUT"  -> update(ex, id);
                default     -> Server.sendResponse(ex, 405, "{}");
            }
        } catch (Exception e) { Server.sendResponse(ex, 500, "{\"error\":\"" + e.getMessage().replace("\"","'") + "\"}"); }
    }
    private void listAll(HttpExchange ex, String query) throws Exception {
        String sql = "SELECT * FROM Mechanics";
        if ("available=true".equals(query)) sql += " WHERE availability_status='Available'";
        sql += " ORDER BY name";
        try (Connection c = DB.get(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery(sql);
            JSONArray arr = new JSONArray();
            while (rs.next()) arr.put(row(rs));
            Server.sendResponse(ex, 200, arr.toString());
        }
    }
    private void getOne(HttpExchange ex, String id) throws Exception {
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement("SELECT * FROM Mechanics WHERE mechanic_id=?")) {
            ps.setInt(1, Integer.parseInt(id));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) Server.sendResponse(ex, 200, row(rs).toString());
            else Server.sendResponse(ex, 404, "{\"error\":\"Not found\"}");
        }
    }
    private void create(HttpExchange ex) throws Exception {
        JSONObject b = new JSONObject(Server.readBody(ex));
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO Mechanics(name,specialization,phone,availability_status) VALUES(?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, b.getString("name"));
            ps.setString(2, b.optString("specialization","General"));
            ps.setString(3, b.getString("phone"));
            ps.setString(4, b.optString("availability_status","Available"));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys(); keys.next();
            Server.sendResponse(ex, 201, "{\"mechanic_id\":" + keys.getInt(1) + "}");
        }
    }
    private void update(HttpExchange ex, String id) throws Exception {
        JSONObject b = new JSONObject(Server.readBody(ex));
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE Mechanics SET name=?,specialization=?,phone=?,availability_status=? WHERE mechanic_id=?")) {
            ps.setString(1, b.getString("name"));
            ps.setString(2, b.optString("specialization","General"));
            ps.setString(3, b.getString("phone"));
            ps.setString(4, b.optString("availability_status","Available"));
            ps.setInt(5, Integer.parseInt(id));
            ps.executeUpdate();
            Server.sendResponse(ex, 200, "{\"success\":true}");
        }
    }
    private JSONObject row(ResultSet rs) throws SQLException {
        return new JSONObject()
                .put("mechanic_id",          rs.getInt("mechanic_id"))
                .put("name",                 rs.getString("name"))
                .put("specialization",       rs.getString("specialization"))
                .put("phone",                rs.getString("phone"))
                .put("availability_status",  rs.getString("availability_status"));
    }
}

// ─── Spare Parts ─────────────────────────────────────────────────────────────
class PartsHandler implements HttpHandler {
    private static final String BASE = "/api/parts";
    @Override public void handle(HttpExchange ex) throws IOException {
        if (Server.handleOptions(ex)) return;
        String method = ex.getRequestMethod().toUpperCase();
        String id = Server.getPathParam(ex, BASE);
        try {
            switch (method) {
                case "GET"  -> { if (id == null) listAll(ex); else getOne(ex, id); }
                case "POST" -> create(ex);
                case "PUT"  -> update(ex, id);
                default     -> Server.sendResponse(ex, 405, "{}");
            }
        } catch (Exception e) { Server.sendResponse(ex, 500, "{\"error\":\"" + e.getMessage().replace("\"","'") + "\"}"); }
    }
    private void listAll(HttpExchange ex) throws Exception {
        try (Connection c = DB.get(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery("SELECT * FROM Spare_Parts ORDER BY part_name");
            JSONArray arr = new JSONArray();
            while (rs.next()) arr.put(row(rs));
            Server.sendResponse(ex, 200, arr.toString());
        }
    }
    private void getOne(HttpExchange ex, String id) throws Exception {
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement("SELECT * FROM Spare_Parts WHERE part_id=?")) {
            ps.setInt(1, Integer.parseInt(id));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) Server.sendResponse(ex, 200, row(rs).toString());
            else Server.sendResponse(ex, 404, "{\"error\":\"Not found\"}");
        }
    }
    private void create(HttpExchange ex) throws Exception {
        JSONObject b = new JSONObject(Server.readBody(ex));
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO Spare_Parts(part_name,stock_qty,price) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, b.getString("part_name"));
            ps.setInt(2, b.getInt("stock_qty"));
            ps.setDouble(3, b.getDouble("price"));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys(); keys.next();
            Server.sendResponse(ex, 201, "{\"part_id\":" + keys.getInt(1) + "}");
        }
    }
    private void update(HttpExchange ex, String id) throws Exception {
        JSONObject b = new JSONObject(Server.readBody(ex));
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE Spare_Parts SET part_name=?,stock_qty=?,price=? WHERE part_id=?")) {
            ps.setString(1, b.getString("part_name"));
            ps.setInt(2, b.getInt("stock_qty"));
            ps.setDouble(3, b.getDouble("price"));
            ps.setInt(4, Integer.parseInt(id));
            ps.executeUpdate();
            Server.sendResponse(ex, 200, "{\"success\":true}");
        }
    }
    private JSONObject row(ResultSet rs) throws SQLException {
        return new JSONObject()
                .put("part_id",   rs.getInt("part_id"))
                .put("part_name", rs.getString("part_name"))
                .put("stock_qty", rs.getInt("stock_qty"))
                .put("price",     rs.getDouble("price"));
    }
}
