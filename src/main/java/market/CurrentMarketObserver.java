package market;

/**
 * Observer interface for receiving current-market (top-of-book) updates.
 * Implement this interface and subscribe via {@link CurrentMarketPublisher}
 * to receive live price/volume snapshots for a symbol.
 */
public interface CurrentMarketObserver {

    /**
     * Called whenever the best bid or offer for {@code symbol} changes.
     *
     * @param symbol   the product symbol (e.g. "WMT")
     * @param buySide  the current best bid; may be null if the buy book is empty
     * @param sellSide the current best offer; may be null if the sell book is empty
     */
    void updateCurrentMarket(String symbol, CurrentMarketSide buySide, CurrentMarketSide sellSide);
}
