package com.autoservice;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.*;

public class ServiceOrderHandler implements HttpHandler {
    private static final String BASE = "/api/orders";

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (Server.handleOptions(ex)) return;
        String method = ex.getRequestMethod().toUpperCase();
        String path   = ex.getRequestURI().getPath();
        String query  = ex.getRequestURI().getQuery();

        // sub-paths: /api/orders/{id}/assign, /api/orders/{id}/services,
        //            /api/orders/{id}/parts,   /api/orders/{id}/complete,
        //            /api/orders/{id}/bill
        try {
            if (method.equals("GET") && path.equals(BASE)) {
                listAll(ex, query);
            } else if (method.equals("GET") && path.matches(BASE + "/\\d+")) {
                getOne(ex, extractId(path));
            } else if (method.equals("GET") && path.matches(BASE + "/\\d+/services")) {
                getOrderServices(ex, extractId(path));
            } else if (method.equals("GET") && path.matches(BASE + "/\\d+/parts")) {
                getOrderParts(ex, extractId(path));
            } else if (method.equals("POST") && path.equals(BASE)) {
                create(ex);
            } else if (method.equals("POST") && path.matches(BASE + "/\\d+/assign")) {
                assignMechanic(ex, extractId(path));
            } else if (method.equals("POST") && path.matches(BASE + "/\\d+/services")) {
                addService(ex, extractId(path));
            } else if (method.equals("POST") && path.matches(BASE + "/\\d+/parts")) {
                addPart(ex, extractId(path));
            } else if (method.equals("POST") && path.matches(BASE + "/\\d+/complete")) {
                completeOrder(ex, extractId(path));
            } else if (method.equals("POST") && path.matches(BASE + "/\\d+/bill")) {
                generateBill(ex, extractId(path));
            } else if (method.equals("DELETE") && path.matches(BASE + "/\\d+/services/\\d+")) {
                deleteService(ex, path);
            } else if (method.equals("DELETE") && path.matches(BASE + "/\\d+/parts/\\d+")) {
                deletePart(ex, path);
            } else {
                Server.sendResponse(ex, 404, "{\"error\":\"Not found\"}");
            }
        } catch (Exception e) {
            Server.sendResponse(ex, 500, "{\"error\":\"" + e.getMessage().replace("\"","'") + "\"}");
        }
    }

    // ── List all orders ────────────────────────────────────────────────────
    private void listAll(HttpExchange ex, String query) throws Exception {
        String sql = """
            SELECT so.*, v.vehicle_number, v.brand, v.model,
                   c.name as customer_name, m.name as mechanic_name
            FROM Service_Orders so
            JOIN Vehicles v ON so.vehicle_id = v.vehicle_id
            JOIN Customers c ON v.customer_id = c.customer_id
            LEFT JOIN Mechanics m ON so.mechanic_id = m.mechanic_id
            ORDER BY so.service_date DESC
            """;
        try (Connection c = DB.get(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery(sql);
            JSONArray arr = new JSONArray();
            while (rs.next()) arr.put(orderRow(rs));
            Server.sendResponse(ex, 200, arr.toString());
        }
    }

    // ── Get single order ───────────────────────────────────────────────────
    private void getOne(HttpExchange ex, int id) throws Exception {
        String sql = """
            SELECT so.*, v.vehicle_number, v.brand, v.model,
                   c.name as customer_name, m.name as mechanic_name
            FROM Service_Orders so
            JOIN Vehicles v ON so.vehicle_id = v.vehicle_id
            JOIN Customers c ON v.customer_id = c.customer_id
            LEFT JOIN Mechanics m ON so.mechanic_id = m.mechanic_id
            WHERE so.order_id = ?
            """;
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) Server.sendResponse(ex, 200, orderRow(rs).toString());
            else Server.sendResponse(ex, 404, "{\"error\":\"Not found\"}");
        }
    }

    // ── Create order via stored procedure ─────────────────────────────────
    private void create(HttpExchange ex) throws Exception {
        JSONObject b = new JSONObject(Server.readBody(ex));
        try (Connection c = DB.get(); CallableStatement cs = c.prepareCall("{CALL Create_Service_Order(?,?,?)}")) {
            cs.setInt(1, b.getInt("vehicle_id"));
            cs.setString(2, b.getString("service_date"));
            cs.setString(3, b.optString("problem_description",""));
            cs.execute();
            // Get last inserted id
            Statement s = c.createStatement();
            ResultSet rs = s.executeQuery("SELECT LAST_INSERT_ID() as id");
            rs.next();
            Server.sendResponse(ex, 201, "{\"order_id\":" + rs.getInt("id") + "}");
        }
    }

    // ── Assign mechanic via stored procedure ───────────────────────────────
    private void assignMechanic(HttpExchange ex, int orderId) throws Exception {
        JSONObject b = new JSONObject(Server.readBody(ex));
        try (Connection c = DB.get(); CallableStatement cs = c.prepareCall("{CALL Assign_Mechanic(?,?)}")) {
            cs.setInt(1, orderId);
            cs.setInt(2, b.getInt("mechanic_id"));
            cs.execute();
            Server.sendResponse(ex, 200, "{\"success\":true}");
        }
    }

    // ── Add service to order ───────────────────────────────────────────────
    private void addService(HttpExchange ex, int orderId) throws Exception {
        JSONObject b = new JSONObject(Server.readBody(ex));
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO Service_Order_Details(order_id,service_id,service_cost) VALUES(?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, orderId);
            ps.setInt(2, b.getInt("service_id"));
            ps.setDouble(3, b.getDouble("service_cost"));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys(); keys.next();
            Server.sendResponse(ex, 201, "{\"detail_id\":" + keys.getInt(1) + "}");
        }
    }

    // ── Add parts via stored procedure ────────────────────────────────────
    private void addPart(HttpExchange ex, int orderId) throws Exception {
        JSONObject b = new JSONObject(Server.readBody(ex));
        try (Connection c = DB.get(); CallableStatement cs = c.prepareCall("{CALL Add_Part_Usage(?,?,?)}")) {
            cs.setInt(1, orderId);
            cs.setInt(2, b.getInt("part_id"));
            cs.setInt(3, b.getInt("quantity"));
            cs.execute();
            Server.sendResponse(ex, 201, "{\"success\":true}");
        }
    }

    // ── Complete order via stored procedure ───────────────────────────────
    private void completeOrder(HttpExchange ex, int orderId) throws Exception {
        try (Connection c = DB.get(); CallableStatement cs = c.prepareCall("{CALL Complete_Service(?)}")) {
            cs.setInt(1, orderId);
            cs.execute();
            Server.sendResponse(ex, 200, "{\"success\":true}");
        }
    }

    // ── Generate bill via stored procedure ────────────────────────────────
    private void generateBill(HttpExchange ex, int orderId) throws Exception {
        JSONObject b = new JSONObject(Server.readBody(ex));
        double taxRate = b.optDouble("tax_rate", 18.0); // default 18% GST
        try (Connection c = DB.get(); CallableStatement cs = c.prepareCall("{CALL Generate_Bill(?,?)}")) {
            cs.setInt(1, orderId);
            cs.setDouble(2, taxRate);
            cs.execute();
            // Fetch the bill just created
            PreparedStatement ps = c.prepareStatement(
                "SELECT b.*, so.order_id FROM Bills b JOIN Service_Orders so ON b.order_id = so.order_id WHERE b.order_id=?");
            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                JSONObject bill = new JSONObject()
                    .put("bill_id",        rs.getInt("bill_id"))
                    .put("order_id",       rs.getInt("order_id"))
                    .put("labor_total",    rs.getDouble("labor_total"))
                    .put("parts_total",    rs.getDouble("parts_total"))
                    .put("tax",            rs.getDouble("tax"))
                    .put("grand_total",    rs.getDouble("grand_total"))
                    .put("payment_status", rs.getString("payment_status"));
                Server.sendResponse(ex, 201, bill.toString());
            } else {
                Server.sendResponse(ex, 500, "{\"error\":\"Bill not created\"}");
            }
        }
    }

    // ── Get services for order ────────────────────────────────────────────
    private void getOrderServices(HttpExchange ex, int orderId) throws Exception {
        String sql = "SELECT sod.*, s.service_name FROM Service_Order_Details sod JOIN Services s USING(service_id) WHERE sod.order_id=?";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();
            JSONArray arr = new JSONArray();
            while (rs.next()) {
                arr.put(new JSONObject()
                    .put("detail_id",    rs.getInt("detail_id"))
                    .put("order_id",     rs.getInt("order_id"))
                    .put("service_id",   rs.getInt("service_id"))
                    .put("service_name", rs.getString("service_name"))
                    .put("service_cost", rs.getDouble("service_cost")));
            }
            Server.sendResponse(ex, 200, arr.toString());
        }
    }

    // ── Get parts for order ───────────────────────────────────────────────
    private void getOrderParts(HttpExchange ex, int orderId) throws Exception {
        String sql = "SELECT pu.*, sp.part_name FROM Parts_Used pu JOIN Spare_Parts sp USING(part_id) WHERE pu.order_id=?";
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();
            JSONArray arr = new JSONArray();
            while (rs.next()) {
                arr.put(new JSONObject()
                    .put("usage_id",  rs.getInt("usage_id"))
                    .put("order_id",  rs.getInt("order_id"))
                    .put("part_id",   rs.getInt("part_id"))
                    .put("part_name", rs.getString("part_name"))
                    .put("quantity",  rs.getInt("quantity"))
                    .put("line_cost", rs.getDouble("line_cost")));
            }
            Server.sendResponse(ex, 200, arr.toString());
        }
    }

    // ── Delete service from order ─────────────────────────────────────────
    private void deleteService(HttpExchange ex, String path) throws Exception {
        String[] parts = path.split("/");
        int detailId = Integer.parseInt(parts[parts.length - 1]);
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(
                "DELETE FROM Service_Order_Details WHERE detail_id=?")) {
            ps.setInt(1, detailId);
            ps.executeUpdate();
            Server.sendResponse(ex, 200, "{\"success\":true}");
        }
    }

    // ── Delete part from order ────────────────────────────────────────────
    private void deletePart(HttpExchange ex, String path) throws Exception {
        String[] parts = path.split("/");
        int usageId = Integer.parseInt(parts[parts.length - 1]);
        // First restore stock
        try (Connection c = DB.get()) {
            PreparedStatement get = c.prepareStatement("SELECT part_id, quantity FROM Parts_Used WHERE usage_id=?");
            get.setInt(1, usageId);
            ResultSet rs = get.executeQuery();
            if (rs.next()) {
                int partId = rs.getInt("part_id");
                int qty = rs.getInt("quantity");
                PreparedStatement restore = c.prepareStatement("UPDATE Spare_Parts SET stock_qty = stock_qty + ? WHERE part_id=?");
                restore.setInt(1, qty);
                restore.setInt(2, partId);
                restore.executeUpdate();
            }
            PreparedStatement del = c.prepareStatement("DELETE FROM Parts_Used WHERE usage_id=?");
            del.setInt(1, usageId);
            del.executeUpdate();
        }
        Server.sendResponse(ex, 200, "{\"success\":true}");
    }

    private int extractId(String path) {
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("orders") && i + 1 < parts.length) {
                try { return Integer.parseInt(parts[i + 1]); } catch (NumberFormatException ignored) {}
            }
        }
        return -1;
    }

    private JSONObject orderRow(ResultSet rs) throws SQLException {
        JSONObject o = new JSONObject()
                .put("order_id",            rs.getInt("order_id"))
                .put("vehicle_id",          rs.getInt("vehicle_id"))
                .put("service_date",        rs.getString("service_date"))
                .put("problem_description", rs.getString("problem_description"))
                .put("status",              rs.getString("status"))
                .put("vehicle_number",      rs.getString("vehicle_number"))
                .put("brand",               rs.getString("brand"))
                .put("model",               rs.getString("model"))
                .put("customer_name",       rs.getString("customer_name"));
        try {
            String mn = rs.getString("mechanic_name");
            o.put("mechanic_id",   rs.getInt("mechanic_id"));
            o.put("mechanic_name", mn != null ? mn : JSONObject.NULL);
        } catch (SQLException ignored) {}
        return o;
    }
}
