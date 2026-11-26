package org.example.posinventorysystem.repo;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only reporting queries for dashboard statistics, sales, top-selling
 * products, and gross profit. All queries use the shared JdbcExecutor so
 * connections are borrowed from Hikari and JDBC resources are closed centrally.
 */
public class ReportRepository {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    /**
     * A snapshot of the main dashboard figures. Sales and purchase figures are
     * limited to the supplied date range; inventory/customer figures represent
     * the current state of the database.
     */
    public record DashboardSummary(
            BigDecimal totalSales,
            long salesCount,
            BigDecimal totalPurchases,
            long purchasesCount,
            long activeProducts,
            long lowStockProducts,
            long customers
    ) {}

    /** Aggregated sales quantity and revenue for one product. */
    public record TopSellingProduct(
            int productId,
            String productName,
            long quantitySold,
            BigDecimal revenue
    ) {}

    /** Sales grouped by calendar date, cashier, and payment method. */
    public record SalesReportRow(
            LocalDate saleDate,
            int cashierId,
            String cashierName,
            String paymentMethod,
            long transactionCount,
            BigDecimal amountCollected
    ) {}

    /** Gross-profit figures for the requested date range. */
    public record ProfitSummary(
            BigDecimal revenue,
            BigDecimal costOfGoods,
            BigDecimal grossProfit
    ) {}

    /**
     * Returns dashboard totals. The date range is inclusive at both ends.
     * Inventory, low-stock, and customer counts are current-state figures.
     */
    public DashboardSummary getDashboardSummary(LocalDateTime from, LocalDateTime to) {
        validateDateRange(from, to);

        return JdbcExecutor.withConnection(
                "Failed to fetch dashboard summary",
                conn -> {
                    String sql = "SELECT "
                            + "COALESCE((SELECT SUM(total) FROM sale "
                            + "WHERE deleted_at IS NULL AND status = 'completed' AND sale_date BETWEEN ? AND ?), 0) AS total_sales, "
                            + "(SELECT COUNT(*) FROM sale "
                            + "WHERE deleted_at IS NULL AND status = 'completed' AND sale_date BETWEEN ? AND ?) AS sales_count, "
                            + "COALESCE((SELECT SUM(total) FROM purchase "
                            + "WHERE deleted_at IS NULL AND status = 'completed' AND purchase_date BETWEEN ? AND ?), 0) AS total_purchases, "
                            + "(SELECT COUNT(*) FROM purchase "
                            + "WHERE deleted_at IS NULL AND status = 'completed' AND purchase_date BETWEEN ? AND ?) AS purchases_count, "
                            + "(SELECT COUNT(*) FROM product WHERE deleted_at IS NULL AND is_active = 1) AS active_products, "
                            + "(SELECT COUNT(*) FROM product WHERE deleted_at IS NULL AND is_active = 1 "
                            + "AND quantity_in_stock <= reorder_level) AS low_stock_products, "
                            + "(SELECT COUNT(*) FROM customer WHERE deleted_at IS NULL) AS customers";

                    return JdbcExecutor.queryOne(conn, sql, ps -> {
                        ps.setTimestamp(1, Timestamp.valueOf(from));
                        ps.setTimestamp(2, Timestamp.valueOf(to));
                        ps.setTimestamp(3, Timestamp.valueOf(from));
                        ps.setTimestamp(4, Timestamp.valueOf(to));
                        ps.setTimestamp(5, Timestamp.valueOf(from));
                        ps.setTimestamp(6, Timestamp.valueOf(to));
                        ps.setTimestamp(7, Timestamp.valueOf(from));
                        ps.setTimestamp(8, Timestamp.valueOf(to));
                    }, rs -> new DashboardSummary(
                            getAmount(rs, "total_sales"),
                            rs.getLong("sales_count"),
                            getAmount(rs, "total_purchases"),
                            rs.getLong("purchases_count"),
                            rs.getLong("active_products"),
                            rs.getLong("low_stock_products"),
                            rs.getLong("customers")
                    )).orElseThrow(() -> new SQLException("Dashboard summary query returned no row."));
                }
        );
    }

    /**
     * Returns the top-selling products in the inclusive date range, ordered by
     * quantity sold and then revenue. Soft-deleted products and line items are
     * excluded, as are cancelled/refunded or soft-deleted sales.
     */
    public List<TopSellingProduct> getTopSellingProducts(
            LocalDateTime from,
            LocalDateTime to,
            int limit
    ) {
        validateDateRange(from, to);
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be greater than zero.");
        }

        return JdbcExecutor.withConnection(
                "Failed to fetch top-selling products",
                conn -> {
                    String sql = "SELECT p.product_id, p.product_name, "
                            + "SUM(si.quantity) AS quantity_sold, SUM(si.line_total) AS revenue "
                            + "FROM sale_item si "
                            + "INNER JOIN sale s ON s.sale_id = si.sale_id "
                            + "INNER JOIN product p ON p.product_id = si.product_id "
                            + "WHERE s.deleted_at IS NULL AND s.status = 'completed' "
                            + "AND si.deleted_at IS NULL AND p.deleted_at IS NULL "
                            + "AND s.sale_date BETWEEN ? AND ? "
                            + "GROUP BY p.product_id, p.product_name "
                            + "ORDER BY quantity_sold DESC, revenue DESC, p.product_name ASC "
                            + "LIMIT ?";

                    return JdbcExecutor.query(conn, sql, ps -> {
                        ps.setTimestamp(1, Timestamp.valueOf(from));
                        ps.setTimestamp(2, Timestamp.valueOf(to));
                        ps.setInt(3, limit);
                    }, rs -> new TopSellingProduct(
                            rs.getInt("product_id"),
                            rs.getString("product_name"),
                            rs.getLong("quantity_sold"),
                            getAmount(rs, "revenue")
                    ));
                }
        );
    }

    /**
     * Returns sales grouped by date, cashier, and payment method. Pass null for
     * cashierId and/or paymentMethod when that filter should not be applied.
     * A sale with no payment row is reported under the "unpaid" method.
     * Amounts represent collected payment amounts, not invoice totals.
     */
    public List<SalesReportRow> getSalesReport(
            LocalDateTime from,
            LocalDateTime to,
            Integer cashierId,
            String paymentMethod
    ) {
        validateDateRange(from, to);

        String normalizedMethod = paymentMethod == null || paymentMethod.isBlank()
                ? null
                : paymentMethod.trim().toLowerCase();

        return JdbcExecutor.withConnection(
                "Failed to fetch sales report",
                conn -> {
                    String sql = "SELECT DATE(s.sale_date) AS sale_date, u.user_id AS cashier_id, "
                            + "u.full_name AS cashier_name, COALESCE(pay.method, 'unpaid') AS payment_method, "
                            + "COUNT(DISTINCT s.sale_id) AS transaction_count, "
                            + "COALESCE(SUM(pay.amount), 0) AS amount_collected "
                            + "FROM sale s "
                            + "INNER JOIN user u ON u.user_id = s.user_id "
                            + "LEFT JOIN payment pay ON pay.sale_id = s.sale_id "
                            + "WHERE s.deleted_at IS NULL AND s.status = 'completed' "
                            + "AND s.sale_date BETWEEN ? AND ? "
                            + "AND (? IS NULL OR s.user_id = ?) "
                            + "AND (? IS NULL OR COALESCE(pay.method, 'unpaid') = ?) "
                            + "GROUP BY DATE(s.sale_date), u.user_id, u.full_name, COALESCE(pay.method, 'unpaid') "
                            + "ORDER BY sale_date, cashier_name, payment_method";

                    return JdbcExecutor.query(conn, sql, ps -> {
                        ps.setTimestamp(1, Timestamp.valueOf(from));
                        ps.setTimestamp(2, Timestamp.valueOf(to));
                        if (cashierId == null) {
                            ps.setNull(3, Types.INTEGER);
                            ps.setNull(4, Types.INTEGER);
                        } else {
                            ps.setInt(3, cashierId);
                            ps.setInt(4, cashierId);
                        }
                        if (normalizedMethod == null) {
                            ps.setNull(5, Types.VARCHAR);
                            ps.setNull(6, Types.VARCHAR);
                        } else {
                            ps.setString(5, normalizedMethod);
                            ps.setString(6, normalizedMethod);
                        }
                    }, rs -> new SalesReportRow(
                            rs.getDate("sale_date").toLocalDate(),
                            rs.getInt("cashier_id"),
                            rs.getString("cashier_name"),
                            rs.getString("payment_method"),
                            rs.getLong("transaction_count"),
                            getAmount(rs, "amount_collected")
                    ));
                }
        );
    }

    /**
     * Calculates gross profit for completed, non-deleted sales in the inclusive
     * date range. Cost of goods uses the unit_cost snapshot stored on each sale
     * item, so later changes to product.cost_price do not rewrite past profit.
     */
    public ProfitSummary getProfitSummary(LocalDateTime from, LocalDateTime to) {
        validateDateRange(from, to);

        return JdbcExecutor.withConnection(
                "Failed to fetch profit summary",
                conn -> {
                    String sql = "SELECT "
                            + "COALESCE(SUM(si.line_total), 0) AS revenue, "
                            + "COALESCE(SUM(si.unit_cost * si.quantity), 0) AS cost_of_goods "
                            + "FROM sale_item si "
                            + "INNER JOIN sale s ON s.sale_id = si.sale_id "
                            + "WHERE s.deleted_at IS NULL AND s.status = 'completed' "
                            + "AND si.deleted_at IS NULL "
                            + "AND s.sale_date BETWEEN ? AND ?";

                    return JdbcExecutor.queryOne(conn, sql, ps -> {
                        ps.setTimestamp(1, Timestamp.valueOf(from));
                        ps.setTimestamp(2, Timestamp.valueOf(to));
                    }, rs -> {
                        BigDecimal revenue = getAmount(rs, "revenue");
                        BigDecimal costOfGoods = getAmount(rs, "cost_of_goods");
                        return new ProfitSummary(revenue, costOfGoods, revenue.subtract(costOfGoods));
                    }).orElse(new ProfitSummary(ZERO, ZERO, ZERO));
                }
        );
    }

    private static BigDecimal getAmount(ResultSet rs, String column) throws SQLException {
        BigDecimal amount = rs.getBigDecimal(column);
        return amount == null ? ZERO : amount;
    }

    private static void validateDateRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException("Date range must have non-null endpoints and from <= to.");
        }
    }
}
