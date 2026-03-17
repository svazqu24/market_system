package tradingsystem;

import product.*;

import java.util.HashMap;
import java.util.Random;

/**
 * System-level registry for all {@link ProductBook}s.
 * Routes orders, quotes, and cancellations to the correct book.
 *
 * FIX: singleton now uses the initialization-on-demand holder idiom
 *      for guaranteed thread safety without synchronization overhead.
 * FIX: removed duplicate {@link UserManager#updateTradable} calls —
 *      {@link product.ProductBookSide} is now the sole source of user-update
 *      notifications, so calling it again here was double-updating user state.
 */
public class ProductManager {

    private final HashMap<String, ProductBook> books  = new HashMap<>();
    private final Random rand = new Random();

    private ProductManager() {}

    // --- Initialization-on-demand holder (thread-safe, lazy) ---
    private static class Holder {
        private static final ProductManager INSTANCE = new ProductManager();
    }

    public static ProductManager getInstance() {
        return Holder.INSTANCE;
    }

    // -------------------------------------------------------------------------
    // Product management
    // -------------------------------------------------------------------------

    /**
     * Registers a new product and creates an empty {@link ProductBook} for it.
     *
     * @param symbol 1–5 uppercase-letter ticker (e.g. "WMT")
     * @throws DataValidationException if the symbol is invalid or already registered
     */
    public void addProduct(String symbol) throws DataValidationException {
        if (symbol == null || !symbol.matches("[A-Z]{1,5}")) {
            throw new DataValidationException("Invalid product symbol: " + symbol);
        }
        if (books.containsKey(symbol)) {
            throw new DataValidationException("Product already registered: " + symbol);
        }
        books.put(symbol, new ProductBook(symbol));
    }

    /**
     * Returns the {@link ProductBook} for the given symbol.
     *
     * @throws DataValidationException if the symbol has not been registered
     */
    public ProductBook getProductBook(String symbol) throws DataValidationException {
        ProductBook pb = books.get(symbol);
        if (pb == null) {
            throw new DataValidationException("Product not found: " + symbol);
        }
        return pb;
    }

    /**
     * Returns a randomly chosen product symbol from the registered products.
     *
     * @throws DataValidationException if no products have been registered yet
     */
    public String getRandomProduct() throws DataValidationException {
        if (books.isEmpty()) {
            throw new DataValidationException("No products have been registered");
        }
        Object[] keys = books.keySet().toArray();
        return (String) keys[rand.nextInt(keys.length)];
    }

    // -------------------------------------------------------------------------
    // Order & quote entry
    // -------------------------------------------------------------------------

    /**
     * Adds a {@link Tradable} (Order or QuoteSide) to the appropriate book.
     * User notification is handled inside {@link product.ProductBookSide#add}.
     *
     * @return DTO snapshot of the tradable as added, or {@code null} if t is null
     */
    public TradableDTO addTradable(Tradable t) throws DataValidationException {
        if (t == null) return null;
        return getProductBook(t.getProduct()).add(t);
    }

    /**
     * Adds a two-sided {@link Quote} to the appropriate book.
     * User notification is handled inside {@link product.ProductBookSide#add}.
     *
     * @return two-element array [buyDTO, sellDTO]; individual elements may be null
     */
    public TradableDTO[] addQuote(Quote q) throws DataValidationException {
        if (q == null) return new TradableDTO[2];
        return getProductBook(q.getSymbol()).add(q);
    }

    // -------------------------------------------------------------------------
    // Cancellation
    // -------------------------------------------------------------------------

    /**
     * Cancels the order identified by {@code dto}, updating the user via
     * {@link product.ProductBookSide#cancel}.
     *
     * @return the cancelled DTO, or {@code null} if not found
     */
    public TradableDTO cancel(TradableDTO dto) throws DataValidationException {
        if (dto == null) return null;
        return getProductBook(dto.product()).cancel(dto.side(), dto.tradableId());
    }

    /**
     * Removes all quotes belonging to {@code user} from the book for {@code symbol}.
     *
     * @return two-element array [buyDTO, sellDTO]; individual elements may be null
     */
    public TradableDTO[] cancelQuote(String symbol, String user) throws DataValidationException {
        if (symbol == null || user == null) {
            throw new DataValidationException("Symbol and user cannot be null");
        }
        return getProductBook(symbol).removeQuotesForUser(user);
    }

    // -------------------------------------------------------------------------
    // Display
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (ProductBook pb : books.values()) {
            sb.append(pb).append("\n");
        }
        return sb.toString();
    }
}
