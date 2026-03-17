package product;

/**
 * The full order book for a single product symbol, containing one BUY-side
 * and one SELL-side {@link ProductBookSide}.
 *
 * FIX: {@link #tryTrade()} changed from {@code public} to {@code private} —
 *      it is an internal concern of the book and should not be called externally.
 * FIX: Removed duplicate {@link tradingsystem.UserManager#updateTradable} calls
 *      that were being issued by both this class and {@link tradingsystem.ProductManager}.
 *      {@link ProductBookSide} is now the single source of user-update notifications.
 */
public class ProductBook {

    private final String product;
    private final ProductBookSide buySide;
    private final ProductBookSide sellSide;

    /**
     * @param product 1–5 character alphanumeric product symbol
     * @throws IllegalArgumentException if the symbol is null or malformed
     */
    public ProductBook(String product) {
        if (product == null || !product.matches("^[A-Za-z0-9.]{1,5}$")) {
            throw new IllegalArgumentException(
                    "Product must be 1–5 alphanumeric characters (letters, digits, or '.'): " + product);
        }
        this.product = product;
        this.buySide  = new ProductBookSide(BookSide.BUY);
        this.sellSide = new ProductBookSide(BookSide.SELL);
        updateMarket();
    }

    public String getProduct() {
        return product;
    }

    /**
     * Adds a single-sided {@link Order} or {@link QuoteSide} to the book,
     * then attempts to trade and refreshes the market.
     */
    public TradableDTO add(Tradable t) {
        if (t == null) {
            throw new IllegalArgumentException("Tradable cannot be null");
        }
        System.out.println("**ADD: " + t);

        TradableDTO dto = (t.getSide() == BookSide.BUY)
                ? buySide.add(t)
                : sellSide.add(t);

        tryTrade();
        updateMarket();
        return dto;
    }

    /**
     * Replaces any existing quotes for the quote's user, then adds the new
     * BUY and SELL sides to their respective book sides.
     */
    public TradableDTO[] add(Quote q) {
        if (q == null) {
            throw new IllegalArgumentException("Quote cannot be null");
        }

        removeQuotesForUser(q.getUser());

        Tradable buyQuoteSide  = q.getQuoteSide(BookSide.BUY);
        Tradable sellQuoteSide = q.getQuoteSide(BookSide.SELL);

        TradableDTO buyDto  = null;
        TradableDTO sellDto = null;

        if (buyQuoteSide != null) {
            System.out.println("**ADD " + buyQuoteSide);
            buyDto = buySide.add(buyQuoteSide);
        }
        if (sellQuoteSide != null) {
            System.out.println("**ADD " + sellQuoteSide);
            sellDto = sellSide.add(sellQuoteSide);
        }

        tryTrade();
        updateMarket();
        return new TradableDTO[]{buyDto, sellDto};
    }

    /**
     * Cancels the entry with the given {@code orderId} on the given {@code side}.
     *
     * @return the cancelled entry's DTO, or {@code null} if not found
     */
    public TradableDTO cancel(BookSide side, String orderId) {
        if (side == null || orderId == null) return null;

        TradableDTO dto = (side == BookSide.BUY)
                ? buySide.cancel(orderId)
                : sellSide.cancel(orderId);

        updateMarket();
        return dto;
    }

    /**
     * Removes all quote entries belonging to {@code username} from both sides.
     *
     * @return a two-element array: [buyDTO, sellDTO], either may be null
     */
    public TradableDTO[] removeQuotesForUser(String username) {
        TradableDTO buyDto  = buySide.removeQuotesForUser(username);
        TradableDTO sellDto = sellSide.removeQuotesForUser(username);
        updateMarket();
        return new TradableDTO[]{buyDto, sellDto};
    }

    /** Pushes the current top-of-book snapshot to the market tracker. */
    private void updateMarket() {
        price.Price buyPrice  = buySide.topOfBookPrice();
        int         buyVol    = buySide.topOfBookVolume();
        price.Price sellPrice = sellSide.topOfBookPrice();
        int         sellVol   = sellSide.topOfBookVolume();

        market.CurrentMarketTracker.getInstance()
                .updateMarket(product, buyPrice, buyVol, sellPrice, sellVol);
    }

    /**
     * Continuously matches the top of the BUY side against the top of the SELL side
     * until no crossing exists or either side is empty.
     *
     * FIX: changed from public to private — this is an internal book mechanic.
     */
    private void tryTrade() {
        while (true) {
            price.Price topBuy  = buySide.topOfBookPrice();
            price.Price topSell = sellSide.topOfBookPrice();

            if (topBuy == null || topSell == null) break;
            if (topSell.compareTo(topBuy) > 0) break; // no crossing

            int buyVol  = buySide.topOfBookVolume();
            int sellVol = sellSide.topOfBookVolume();
            int toTrade = Math.min(buyVol, sellVol);

            if (toTrade <= 0) break;

            buySide.tradeOut(topBuy, toTrade);
            sellSide.tradeOut(topSell, toTrade);
        }
    }

    public String getTopOfBookString(BookSide side) {
        if (side == null) return null;
        ProductBookSide pbs = (side == BookSide.BUY) ? buySide : sellSide;
        price.Price top = pbs.topOfBookPrice();
        if (top == null) {
            return "Top of " + side + " book: <Empty>";
        }
        return "Top of " + side + " book: " + top + " x " + pbs.topOfBookVolume();
    }

    @Override
    public String toString() {
        return "Product: " + product + "\n" +
                buySide.toString()  + "\n" +
                sellSide.toString();
    }
}
