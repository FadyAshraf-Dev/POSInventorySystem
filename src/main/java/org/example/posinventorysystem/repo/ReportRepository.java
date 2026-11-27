package org.example.posinventorysystem.repo;

import org.example.posinventorysystem.data.JdbcExecutor;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

/**
 * Read-only reporting queries for the Reports screen. All queries use the
 * shared JdbcExecutor so connections are borrowed from Hikari and JDBC
 * resources are closed centrally.
 *
 * Conventions used throughout this class:
 *   - every query ignores soft-deleted transactional rows (deleted_at IS NULL
 *     on sale, sale_item, purchase, purchase_item). Historical reports do NOT
 *     filter on soft-deleted master data (product, category, supplier, user):
 *     a product/category/supplier that was soft-deleted later still keeps its
 *     past sales/purchases in the figures, so grouped totals stay consistent
 *     with the overall totals. Only the current-state snapshots
 *     (inventoryValue, findOutOfStock) exclude soft-deleted products;
 *   - every sales figure counts only status = 'completed' sales (held,
 *     cancelled and refunded sales are excluded);
 *   - every date-range parameter is treated as half-open: [from, to). The
 *     caller is responsible for passing "to" as the start of the day/period
 *     after the one they want included;
 *   - profit is always computed from sale_item.unit_cost (the cost snapshot
 *     taken when the line was sold), never from product.cost_price, so later
 *     changes to a product's cost don't rewrite the profit on past sales.
 */
public class ReportRepository {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    // ------------------------------------------------------------------
    // DTOs
    // ------------------------------------------------------------------

    /** Gross-profit figures for a date range. */
    public record ProfitSummary(BigDecimal revenue, BigDecimal cost, BigDecimal profit) {}

    /** One calendar day's sales (for a per-day bar/line chart). */
    public record DailySales(LocalDate date, long invoiceCount, BigDecimal total) {}

    /** One calendar month's sales within a single year (for a per-month bar chart). */
    public record MonthlySales(int month, BigDecimal total) {}

    /** One product's aggregated sales and profit (for a top-products bar chart). */
    public record TopSellingProduct(
            int productId,
            String name,
            long qtySold,
            BigDecimal revenue,
            BigDecimal profit
    ) {}

    /** Sales total and count for one payment method (for a pie/bar chart). */
    public record SalesByPaymentMethod(String method, BigDecimal total, long count) {}

    /** Sales total and invoice count for one cashier. */
    public record SalesByCashier(int userId, String fullName, long invoiceCount, BigDecimal total) {}

    /** Sales total for one product category (for a bar chart). */
    public record SalesByCategory(int categoryId, String name, BigDecimal total) {}

    /** Purchases total for one supplier. */
    public record PurchasesBySupplier(int supplierId, String name, BigDecimal total) {}

    /** Current stock valuation: total cost-basis value and number of distinct items. */
    public record InventoryValue(BigDecimal totalValue, long itemCount) {}

    /** A product that is currently out of stock. */
    public record OutOfStockProduct(int productId, String name, int quantityInStock, int reorderLevel) {}

    // ------------------------------------------------------------------
    // totalSales
    // ------------------------------------------------------------------

    /** Total of completed, non-deleted sales in [from, to). Used for the dashboard "today's sales". */
    public BigDecimal totalSales(LocalDateTime from, LocalDateTime to) {
        return JdbcExecutor.withConnection("Failed to fetch total sales", conn -> totalSales(conn, from, to));
    }

    public BigDecimal totalSales(Connection conn, LocalDateTime from, LocalDateTime to) throws SQLException {
        validateRange(from, to);
        String sql = "SELECT COALESCE(SUM(total), 0) AS total_sales FROM sale "
                + "WHERE deleted_at IS NULL AND status = 'completed' AND sale_date >= ? AND sale_date < ?";
        return JdbcExecutor.queryOne(conn, sql, ps -> {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
        }, rs -> getAmount(rs, "total_sales")).orElse(ZERO);
    }

    // ------------------------------------------------------------------
    // profitSummary
    // ------------------------------------------------------------------

    /**
     * Gross profit for completed, non-deleted sales in [from, to), AFTER
     * invoice-level discounts and BEFORE tax:
     *   revenue = SUM(sale_item.line_total) - SUM(sale.discount)
     *   cost    = SUM(quantity * unit_cost), using the unit_cost snapshot
     *             stored on each sale item
     *   profit  = revenue - cost
     * sale_item.line_total already includes any line-level discount; the
     * invoice-level discount (sale.discount) is subtracted once per sale.
     * Tax is excluded, so revenue here differs from totalSales (which is
     * sale.total, i.e. includes tax).
     */
    public ProfitSummary profitSummary(LocalDateTime from, LocalDateTime to) {
        return JdbcExecutor.withConnection("Failed to fetch profit summary", conn -> profitSummary(conn, from, to));
    }

    public ProfitSummary profitSummary(Connection conn, LocalDateTime from, LocalDateTime to) throws SQLException {
        validateRange(from, to);
        // Invoice-level discounts are summed in a separate subquery so they are
        // counted once per sale, not once per sale_item row.
        String sql = "SELECT "
                + "COALESCE(SUM(si.line_total), 0) - "
                + "COALESCE((SELECT SUM(d.discount) FROM sale d "
                + "WHERE d.deleted_at IS NULL AND d.status = 'completed' "
                + "AND d.sale_date >= ? AND d.sale_date < ?), 0) AS revenue, "
                + "COALESCE(SUM(si.quantity * si.unit_cost), 0) AS cost, "
                + "COALESCE(SUM(si.line_total - si.quantity * si.unit_cost), 0) - "
                + "COALESCE((SELECT SUM(d2.discount) FROM sale d2 "
                + "WHERE d2.deleted_at IS NULL AND d2.status = 'completed' "
                + "AND d2.sale_date >= ? AND d2.sale_date < ?), 0) AS profit "
                + "FROM sale_item si "
                + "INNER JOIN sale s ON s.sale_id = si.sale_id "
                + "WHERE s.deleted_at IS NULL AND s.status = 'completed' "
                + "AND si.deleted_at IS NULL "
                + "AND s.sale_date >= ? AND s.sale_date < ?";
        return JdbcExecutor.queryOne(conn, sql, ps -> {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
            ps.setTimestamp(3, Timestamp.valueOf(from));
            ps.setTimestamp(4, Timestamp.valueOf(to));
            ps.setTimestamp(5, Timestamp.valueOf(from));
            ps.setTimestamp(6, Timestamp.valueOf(to));
        }, rs -> new ProfitSummary(
                getAmount(rs, "revenue"),
                getAmount(rs, "cost"),
                getAmount(rs, "profit")
        )).orElse(new ProfitSummary(ZERO, ZERO, ZERO));
    }

    // ------------------------------------------------------------------
    // salesByDay
    // ------------------------------------------------------------------

    /** Completed, non-deleted sales in [from, to), grouped by calendar date. */
    public List<DailySales> salesByDay(LocalDateTime from, LocalDateTime to) {
        return JdbcExecutor.withConnection("Failed to fetch sales by day", conn -> salesByDay(conn, from, to));
    }

    public List<DailySales> salesByDay(Connection conn, LocalDateTime from, LocalDateTime to) throws SQLException {
        validateRange(from, to);
        String sql = "SELECT DATE(sale_date) AS sale_day, COUNT(*) AS invoice_count, SUM(total) AS total "
                + "FROM sale "
                + "WHERE deleted_at IS NULL AND status = 'completed' AND sale_date >= ? AND sale_date < ? "
                + "GROUP BY DATE(sale_date) "
                + "ORDER BY sale_day";
        return JdbcExecutor.query(conn, sql, ps -> {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
        }, rs -> new DailySales(
                rs.getDate("sale_day").toLocalDate(),
                rs.getLong("invoice_count"),
                getAmount(rs, "total")
        ));
    }

    // ------------------------------------------------------------------
    // salesByMonth
    // ------------------------------------------------------------------

    /** Completed, non-deleted sales for the given calendar year, grouped by month (1-12). */
    public List<MonthlySales> salesByMonth(int year) {
        return JdbcExecutor.withConnection("Failed to fetch sales by month", conn -> salesByMonth(conn, year));
    }

    public List<MonthlySales> salesByMonth(Connection conn, int year) throws SQLException {
        if (year < 1) {
            throw new IllegalArgumentException("Year must be a positive calendar year.");
        }
        LocalDateTime from = LocalDateTime.of(year, Month.JANUARY, 1, 0, 0);
        LocalDateTime to = from.plusYears(1);
        String sql = "SELECT MONTH(sale_date) AS sale_month, SUM(total) AS total "
                + "FROM sale "
                + "WHERE deleted_at IS NULL AND status = 'completed' AND sale_date >= ? AND sale_date < ? "
                + "GROUP BY MONTH(sale_date) "
                + "ORDER BY sale_month";
        return JdbcExecutor.query(conn, sql, ps -> {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
        }, rs -> new MonthlySales(
                rs.getInt("sale_month"),
                getAmount(rs, "total")
        ));
    }

    // ------------------------------------------------------------------
    // topSellingProducts
    // ------------------------------------------------------------------

    /**
     * Top-selling products in [from, to), ranked by quantity sold then
     * revenue. Only completed, non-deleted sales/sale_items count; products
     * that were soft-deleted later are still included, so their past sales
     * don't disappear. Revenue and profit are at line level (before any
     * invoice-level discount); profit uses sale_item.unit_cost.
     */
    public List<TopSellingProduct> topSellingProducts(LocalDateTime from, LocalDateTime to, int limit) {
        return JdbcExecutor.withConnection("Failed to fetch top-selling products", conn -> topSellingProducts(conn, from, to, limit));
    }

    public List<TopSellingProduct> topSellingProducts(Connection conn, LocalDateTime from, LocalDateTime to, int limit) throws SQLException {
        validateRange(from, to);
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be greater than zero.");
        }
        String sql = "SELECT p.product_id, p.product_name, "
                + "SUM(si.quantity) AS qty_sold, "
                + "SUM(si.line_total) AS revenue, "
                + "SUM(si.line_total - si.quantity * si.unit_cost) AS profit "
                + "FROM sale_item si "
                + "INNER JOIN sale s ON s.sale_id = si.sale_id "
                + "INNER JOIN product p ON p.product_id = si.product_id "
                + "WHERE s.deleted_at IS NULL AND s.status = 'completed' "
                + "AND si.deleted_at IS NULL "
                + "AND s.sale_date >= ? AND s.sale_date < ? "
                + "GROUP BY p.product_id, p.product_name "
                + "ORDER BY qty_sold DESC, revenue DESC, p.product_name ASC "
                + "LIMIT ?";
        return JdbcExecutor.query(conn, sql, ps -> {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
            ps.setInt(3, limit);
        }, rs -> new TopSellingProduct(
                rs.getInt("product_id"),
                rs.getString("product_name"),
                rs.getLong("qty_sold"),
                getAmount(rs, "revenue"),
                getAmount(rs, "profit")
        ));
    }

    // ------------------------------------------------------------------
    // salesByPaymentMethod
    // ------------------------------------------------------------------

    /**
     * Completed, non-deleted sales in [from, to), grouped by payment method.
     * A sale is attributed to a method by summing its payment rows; a sale
     * with no payment rows contributes nothing here (it is not "unpaid" —
     * there is simply no method to group it under).
     *
     * count = number of distinct invoices that used the method (a split-payment
     * invoice counts once under each method it used, not once per payment row).
     *
     * total = payment amounts, but never more than the invoice total: if a
     * sale's payments add up to more than sale.total (e.g. the cashier
     * recorded the amount tendered, 200, for a 150 invoice), each payment is
     * scaled down proportionally so the sale contributes exactly sale.total.
     * Under-paid sales are not scaled up.
     */
    public List<SalesByPaymentMethod> salesByPaymentMethod(LocalDateTime from, LocalDateTime to) {
        return JdbcExecutor.withConnection("Failed to fetch sales by payment method", conn -> salesByPaymentMethod(conn, from, to));
    }

    public List<SalesByPaymentMethod> salesByPaymentMethod(Connection conn, LocalDateTime from, LocalDateTime to) throws SQLException {
        validateRange(from, to);
        String sql = "SELECT pay.method AS method, "
                + "ROUND(SUM(CASE WHEN pt.paid > s.total AND pt.paid > 0 "
                + "THEN pay.amount * s.total / pt.paid ELSE pay.amount END), 2) AS total, "
                + "COUNT(DISTINCT s.sale_id) AS cnt "
                + "FROM payment pay "
                + "INNER JOIN sale s ON s.sale_id = pay.sale_id "
                + "INNER JOIN (SELECT sale_id, SUM(amount) AS paid FROM payment GROUP BY sale_id) pt "
                + "ON pt.sale_id = pay.sale_id "
                + "WHERE s.deleted_at IS NULL AND s.status = 'completed' "
                + "AND s.sale_date >= ? AND s.sale_date < ? "
                + "GROUP BY pay.method "
                + "ORDER BY total DESC";
        return JdbcExecutor.query(conn, sql, ps -> {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
        }, rs -> new SalesByPaymentMethod(
                rs.getString("method"),
                getAmount(rs, "total"),
                rs.getLong("cnt")
        ));
    }

    // ------------------------------------------------------------------
    // salesByCashier
    // ------------------------------------------------------------------

    /** Completed, non-deleted sales in [from, to), grouped by the cashier (user) who made them. */
    public List<SalesByCashier> salesByCashier(LocalDateTime from, LocalDateTime to) {
        return JdbcExecutor.withConnection("Failed to fetch sales by cashier", conn -> salesByCashier(conn, from, to));
    }

    public List<SalesByCashier> salesByCashier(Connection conn, LocalDateTime from, LocalDateTime to) throws SQLException {
        validateRange(from, to);
        String sql = "SELECT u.user_id AS user_id, u.full_name AS full_name, "
                + "COUNT(*) AS invoice_count, SUM(s.total) AS total "
                + "FROM sale s "
                + "INNER JOIN user u ON u.user_id = s.user_id "
                + "WHERE s.deleted_at IS NULL AND s.status = 'completed' "
                + "AND s.sale_date >= ? AND s.sale_date < ? "
                + "GROUP BY u.user_id, u.full_name "
                + "ORDER BY total DESC";
        return JdbcExecutor.query(conn, sql, ps -> {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
        }, rs -> new SalesByCashier(
                rs.getInt("user_id"),
                rs.getString("full_name"),
                rs.getLong("invoice_count"),
                getAmount(rs, "total")
        ));
    }

    // ------------------------------------------------------------------
    // salesByCategory
    // ------------------------------------------------------------------

    /**
     * Completed, non-deleted sales in [from, to), grouped by the product
     * category of each sale line (via sale_item -> product -> category).
     * A multi-category sale contributes to each category it touches.
     * Soft-deleted products/categories are still included so historical
     * sales stay in the report. Totals are sums of line_total (before
     * invoice-level discount and tax), so they will not equal totalSales.
     */
    public List<SalesByCategory> salesByCategory(LocalDateTime from, LocalDateTime to) {
        return JdbcExecutor.withConnection("Failed to fetch sales by category", conn -> salesByCategory(conn, from, to));
    }

    public List<SalesByCategory> salesByCategory(Connection conn, LocalDateTime from, LocalDateTime to) throws SQLException {
        validateRange(from, to);
        String sql = "SELECT c.category_id AS category_id, c.category_name AS category_name, "
                + "SUM(si.line_total) AS total "
                + "FROM sale_item si "
                + "INNER JOIN sale s ON s.sale_id = si.sale_id "
                + "INNER JOIN product p ON p.product_id = si.product_id "
                + "INNER JOIN category c ON c.category_id = p.category_id "
                + "WHERE s.deleted_at IS NULL AND s.status = 'completed' "
                + "AND si.deleted_at IS NULL "
                + "AND s.sale_date >= ? AND s.sale_date < ? "
                + "GROUP BY c.category_id, c.category_name "
                + "ORDER BY total DESC";
        return JdbcExecutor.query(conn, sql, ps -> {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
        }, rs -> new SalesByCategory(
                rs.getInt("category_id"),
                rs.getString("category_name"),
                getAmount(rs, "total")
        ));
    }

    // ------------------------------------------------------------------
    // purchasesBySupplier
    // ------------------------------------------------------------------

    /**
     * Completed, non-deleted purchases in [from, to), grouped by supplier.
     * Soft-deleted suppliers are still included so historical purchases
     * stay in the report.
     */
    public List<PurchasesBySupplier> purchasesBySupplier(LocalDateTime from, LocalDateTime to) {
        return JdbcExecutor.withConnection("Failed to fetch purchases by supplier", conn -> purchasesBySupplier(conn, from, to));
    }

    public List<PurchasesBySupplier> purchasesBySupplier(Connection conn, LocalDateTime from, LocalDateTime to) throws SQLException {
        validateRange(from, to);
        String sql = "SELECT sup.supplier_id AS supplier_id, sup.supplier_name AS supplier_name, "
                + "SUM(p.total) AS total "
                + "FROM purchase p "
                + "INNER JOIN supplier sup ON sup.supplier_id = p.supplier_id "
                + "WHERE p.deleted_at IS NULL AND p.status = 'completed' "
                + "AND p.purchase_date >= ? AND p.purchase_date < ? "
                + "GROUP BY sup.supplier_id, sup.supplier_name "
                + "ORDER BY total DESC";
        return JdbcExecutor.query(conn, sql, ps -> {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
        }, rs -> new PurchasesBySupplier(
                rs.getInt("supplier_id"),
                rs.getString("supplier_name"),
                getAmount(rs, "total")
        ));
    }

    // ------------------------------------------------------------------
    // inventoryValue
    // ------------------------------------------------------------------

    /**
     * Current stock valuation across all non-deleted products (including
     * inactive ones, since they may still hold stock):
     * SUM(quantity_in_stock * cost_price), plus the number of distinct items
     * counted. This is a current-state snapshot, not scoped to a date range.
     */
    public InventoryValue inventoryValue() {
        return JdbcExecutor.withConnection("Failed to fetch inventory value", this::inventoryValue);
    }

    public InventoryValue inventoryValue(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(SUM(quantity_in_stock * cost_price), 0) AS total_value, "
                + "COUNT(*) AS item_count "
                + "FROM product "
                + "WHERE deleted_at IS NULL";
        return JdbcExecutor.queryOne(conn, sql, ps -> { }, rs -> new InventoryValue(
                getAmount(rs, "total_value"),
                rs.getLong("item_count")
        )).orElse(new InventoryValue(ZERO, 0));
    }

    // ------------------------------------------------------------------
    // findOutOfStock
    // ------------------------------------------------------------------

    /** Non-deleted products currently at zero stock, ordered by name. */
    public List<OutOfStockProduct> findOutOfStock() {
        return JdbcExecutor.withConnection("Failed to fetch out-of-stock products", this::findOutOfStock);
    }

    public List<OutOfStockProduct> findOutOfStock(Connection conn) throws SQLException {
        String sql = "SELECT product_id, product_name, quantity_in_stock, reorder_level "
                + "FROM product "
                + "WHERE deleted_at IS NULL AND quantity_in_stock <= 0 "
                + "ORDER BY product_name";
        return JdbcExecutor.query(conn, sql, ps -> { }, rs -> new OutOfStockProduct(
                rs.getInt("product_id"),
                rs.getString("product_name"),
                rs.getInt("quantity_in_stock"),
                rs.getInt("reorder_level")
        ));
    }

    // ------------------------------------------------------------------
    // shared helpers
    // ------------------------------------------------------------------

    private static BigDecimal getAmount(ResultSet rs, String column) throws SQLException {
        BigDecimal amount = rs.getBigDecimal(column);
        return amount == null ? ZERO : amount;
    }

    private static void validateRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new IllegalArgumentException("Date range must have non-null endpoints and from < to.");
        }
    }
}
