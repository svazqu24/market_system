package market;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Manages per-symbol subscriptions and dispatches current-market updates
 * to all registered {@link CurrentMarketObserver}s.
 *
 * FIX: singleton now uses the initialization-on-demand holder idiom for
 * guaranteed thread safety with zero synchronization overhead on the fast path.
 */
public class CurrentMarketPublisher {

    /** Maps product symbol → list of subscribed observers. */
    private final HashMap<String, ArrayList<CurrentMarketObserver>> subscriptions = new HashMap<>();

    private CurrentMarketPublisher() {}

    // --- Initialization-on-demand holder (thread-safe, lazy) ---
    private static class Holder {
        private static final CurrentMarketPublisher INSTANCE = new CurrentMarketPublisher();
    }

    public static CurrentMarketPublisher getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Subscribes {@code observer} to current-market updates for {@code symbol}.
     * Does nothing if either argument is null.
     */
    public void subscribeCurrentMarket(String symbol, CurrentMarketObserver observer) {
        if (symbol == null || observer == null) return;
        subscriptions.computeIfAbsent(symbol, k -> new ArrayList<>()).add(observer);
    }

    /**
     * Unsubscribes {@code observer} from current-market updates for {@code symbol}.
     * Removes the symbol entry entirely if the subscriber list becomes empty.
     */
    public void unSubscribeCurrentMarket(String symbol, CurrentMarketObserver observer) {
        if (symbol == null || observer == null) return;
        ArrayList<CurrentMarketObserver> list = subscriptions.get(symbol);
        if (list == null) return;
        list.remove(observer);
        if (list.isEmpty()) {
            subscriptions.remove(symbol);
        }
    }

    /**
     * Dispatches a market update to every observer subscribed to {@code symbol}.
     * Iterates over a snapshot so that observer callbacks may safely
     * subscribe or unsubscribe without causing a ConcurrentModificationException.
     */
    public void acceptCurrentMarket(String symbol, CurrentMarketSide buySide, CurrentMarketSide sellSide) {
        ArrayList<CurrentMarketObserver> list = subscriptions.get(symbol);
        if (list == null) return;

        // Snapshot prevents ConcurrentModificationException if a callback mutates subscriptions
        for (CurrentMarketObserver observer : new ArrayList<>(list)) {
            observer.updateCurrentMarket(symbol, buySide, sellSide);
        }
    }
}
