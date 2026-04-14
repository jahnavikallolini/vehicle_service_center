package com.autoservice;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.*;

// ─── Bills ────────────────────────────────────────────────────────────────────
class BillHandler implements HttpHandler {
    private static final String BASE = "/api/bills";
    @Override public void handle(HttpExchange ex) throws IOException {
        if (Server.handleOptions(ex)) return;
        String method = ex.getRequestMethod().toUpperCase();
        String path   = ex.getRequestURI().getPath();
        String id     = Server.getPathParam(ex, BASE);
        try {
            if ("GET".equals(method) && id == null) listAll(ex);
            else if ("GET".equals(method))          getOne(ex, id);
            else if ("PUT".equals(method) && path.matches(BASE + "/\\d+/pay")) markPaid(ex, id.replace("/pay",""));
            else Server.sendResponse(ex, 405, "{}");
        } catch (Exception e) { Server.sendResponse(ex, 500, "{\"error\":\"" + e.getMessage().replace("\"","'") + "\"}"); }
    }

    private void listAll(HttpExchange ex) throws Exception {
        String sql = """
            SELECT b.*, so.order_id, v.vehicle_number, c.name as customer_name
            FROM Bills b
            JOIN Service_Orders so ON b.order_id = so.order_id
            JOIN Vehicles v ON so.vehicle_id = v.vehicle_id
            JOIN Customers c ON v.customer_id = c.customer_id
            ORDER BY b.bill_id DESC
            """;
        try (Connection c = DB.get(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery(sql);
            JSONArray arr = new JSONArray();
            while (rs.next()) arr.put(row(rs));
            Server.sendResponse(ex, 200, arr.toString());
        }
    }

    private void getOne(HttpExchange ex, String id) throws Exception {
        String sql = """
            SELECT b.*, v.vehicle_number, c.name as customer_name
            FROM Bills b
            JOIN Service_Orders so ON b.order_id = so.order_id
            JOIN Vehicles v ON so.vehicle_id = v.vehicle_id
            JOIN Customers c ON v.customer_id = c.customer_id
            WHERE b.bill_id = ? OR b.order_id = ?
            """;
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(id));
            ps.setInt(2, Integer.parseInt(id));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) Server.sendResponse(ex, 200, row(rs).toString());
            else Server.sendResponse(ex, 404, "{\"error\":\"Not found\"}");
        }
    }

    private void markPaid(HttpExchange ex, String id) throws Exception {
        try (Connection c = DB.get(); PreparedStatement ps = c.prepareStatement(
                "UPDATE Bills SET payment_status='Paid' WHERE bill_id=?")) {
            ps.setInt(1, Integer.parseInt(id));
            ps.executeUpdate();
            Server.sendResponse(ex, 200, "{\"success\":true}");
        }
    }

    private JSONObject row(ResultSet rs) throws SQLException {
        JSONObject o = new JSONObject()
            .put("bill_id",        rs.getInt("bill_id"))
            .put("order_id",       rs.getInt("order_id"))
            .put("labor_total",    rs.getDouble("labor_total"))
            .put("parts_total",    rs.getDouble("parts_total"))
            .put("tax",            rs.getDouble("tax"))
            .put("grand_total",    rs.getDouble("grand_total"))
            .put("payment_status", rs.getString("payment_status"));
        try { o.put("vehicle_number",  rs.getString("vehicle_number")); } catch (SQLException ignored) {}
        try { o.put("customer_name",   rs.getString("customer_name")); }  catch (SQLException ignored) {}
        return o;
    }
}

// ─── Reports ──────────────────────────────────────────────────────────────────
class ReportHandler implements HttpHandler {
    private static final String BASE = "/api/reports";
    @Override public void handle(HttpExchange ex) throws IOException {
        if (Server.handleOptions(ex)) return;
        String path = ex.getRequestURI().getPath();
        try {
            if (path.endsWith("/summary"))          summary(ex);
            else if (path.endsWith("/revenue"))     revenue(ex);
            else if (path.endsWith("/mechanic"))    mechanicStats(ex);
            else if (path.endsWith("/parts"))       partsUsage(ex);
            else if (path.endsWith("/pending"))     pendingOrders(ex);
            else Server.sendResponse(ex, 404, "{\"error\":\"Unknown report\"}");
        } catch (Exception e) { Server.sendResponse(ex, 500, "{\"error\":\"" + e.getMessage().replace("\"","'") + "\"}"); }
    }

    private void summary(HttpExchange ex) throws Exception {
        try (Connection c = DB.get(); Statement s = c.createStatement()) {
            JSONObject res = new JSONObject();
            res.put("total_customers",    scalar(s, "SELECT COUNT(*) FROM Customers"));
            res.put("total_vehicles",     scalar(s, "SELECT COUNT(*) FROM Vehicles"));
            res.put("total_orders",       scalar(s, "SELECT COUNT(*) FROM Service_Orders"));
            res.put("open_orders",        scalar(s, "SELECT COUNT(*) FROM Service_Orders WHERE status='Open'"));
            res.put("in_progress_orders", scalar(s, "SELECT COUNT(*) FROM Service_Orders WHERE status='In Progress'"));
            res.put("completed_orders",   scalar(s, "SELECT COUNT(*) FROM Service_Orders WHERE status='Completed'"));
            res.put("total_revenue",      scalar(s, "SELECT COALESCE(SUM(grand_total),0) FROM Bills WHERE payment_status='Paid'"));
            res.put("pending_revenue",    scalar(s, "SELECT COALESCE(SUM(grand_total),0) FROM Bills WHERE payment_status='Pending'"));
            res.put("low_stock_parts",    scalar(s, "SELECT COUNT(*) FROM Spare_Parts WHERE stock_qty < 5"));
            Server.sendResponse(ex, 200, res.toString());
        }
    }

    private void revenue(HttpExchange ex) throws Exception {
        String sql = """
            SELECT DATE_FORMAT(so.service_date,'%Y-%m') as month,
                   SUM(b.grand_total) as total,
                   COUNT(b.bill_id) as bills
            FROM Bills b
            JOIN Service_Orders so ON b.order_id = so.order_id
            WHERE b.payment_status = 'Paid'
            GROUP BY month ORDER BY month DESC LIMIT 12
            """;
        try (Connection c = DB.get(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery(sql);
            JSONArray arr = new JSONArray();
            while (rs.next()) arr.put(new JSONObject()
                .put("month", rs.getString("month"))
                .put("total", rs.getDouble("total"))
                .put("bills", rs.getInt("bills")));
            Server.sendResponse(ex, 200, arr.toString());
        }
    }

    private void mechanicStats(HttpExchange ex) throws Exception {
        String sql = """
            SELECT m.name, m.specialization, m.availability_status,
                   COUNT(so.order_id) as total_orders,
                   SUM(CASE WHEN so.status='Completed' THEN 1 ELSE 0 END) as completed
            FROM Mechanics m
            LEFT JOIN Service_Orders so ON m.mechanic_id = so.mechanic_id
            GROUP BY m.mechanic_id
            ORDER BY total_orders DESC
            """;
        try (Connection c = DB.get(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery(sql);
            JSONArray arr = new JSONArray();
            while (rs.next()) arr.put(new JSONObject()
                .put("name",                rs.getString("name"))
                .put("specialization",      rs.getString("specialization"))
                .put("availability_status", rs.getString("availability_status"))
                .put("total_orders",        rs.getInt("total_orders"))
                .put("completed",           rs.getInt("completed")));
            Server.sendResponse(ex, 200, arr.toString());
        }
    }

    private void partsUsage(HttpExchange ex) throws Exception {
        String sql = """
            SELECT sp.part_name, sp.stock_qty, sp.price,
                   COALESCE(SUM(pu.quantity),0) as total_used,
                   COALESCE(SUM(pu.line_cost),0) as total_revenue
            FROM Spare_Parts sp
            LEFT JOIN Parts_Used pu ON sp.part_id = pu.part_id
            GROUP BY sp.part_id
            ORDER BY total_used DESC
            """;
        try (Connection c = DB.get(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery(sql);
            JSONArray arr = new JSONArray();
            while (rs.next()) arr.put(new JSONObject()
                .put("part_name",     rs.getString("part_name"))
                .put("stock_qty",     rs.getInt("stock_qty"))
                .put("price",         rs.getDouble("price"))
                .put("total_used",    rs.getInt("total_used"))
                .put("total_revenue", rs.getDouble("total_revenue")));
            Server.sendResponse(ex, 200, arr.toString());
        }
    }

    private void pendingOrders(HttpExchange ex) throws Exception {
        String sql = """
            SELECT so.order_id, so.service_date, so.status,
                   v.vehicle_number, v.brand, v.model,
                   c.name as customer_name, c.phone,
                   m.name as mechanic_name
            FROM Service_Orders so
            JOIN Vehicles v ON so.vehicle_id = v.vehicle_id
            JOIN Customers c ON v.customer_id = c.customer_id
            LEFT JOIN Mechanics m ON so.mechanic_id = m.mechanic_id
            WHERE so.status IN ('Open','In Progress')
            ORDER BY so.service_date
            """;
        try (Connection c = DB.get(); Statement s = c.createStatement()) {
            ResultSet rs = s.executeQuery(sql);
            JSONArray arr = new JSONArray();
            while (rs.next()) arr.put(new JSONObject()
                .put("order_id",       rs.getInt("order_id"))
                .put("service_date",   rs.getString("service_date"))
                .put("status",         rs.getString("status"))
                .put("vehicle_number", rs.getString("vehicle_number"))
                .put("brand",          rs.getString("brand"))
                .put("model",          rs.getString("model"))
                .put("customer_name",  rs.getString("customer_name"))
                .put("phone",          rs.getString("phone"))
                .put("mechanic_name",  rs.getString("mechanic_name") != null ? rs.getString("mechanic_name") : "Unassigned"));
            Server.sendResponse(ex, 200, arr.toString());
        }
    }

    private double scalar(Statement s, String sql) throws SQLException {
        ResultSet rs = s.executeQuery(sql);
        return rs.next() ? rs.getDouble(1) : 0;
    }
}
