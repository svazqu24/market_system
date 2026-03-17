package market;

import price.Price;

/**
 * Computes the bid–ask spread and forwards current-market snapshots to
 * {@link CurrentMarketPublisher} for distribution to subscribers.
 *
 * FIX: removed the nonsensical {@code catch (NoSuchMethodError | Exception)} block;
 * spread width is now computed directly from {@link Price#getCents()}.
 *
 * FIX: singleton uses the initialization-on-demand holder idiom.
 */
public class CurrentMarketTracker {

    private CurrentMarketTracker() {}

    private static class Holder {
        private static final CurrentMarketTracker INSTANCE = new CurrentMarketTracker();
    }

    public static CurrentMarketTracker getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Prints the current market for {@code symbol}, then notifies all subscribers.
     *
     * @param symbol     product ticker
     * @param buyPrice   best bid price  (null = empty book)
     * @param buyVolume  total volume at best bid
     * @param sellPrice  best ask price  (null = empty book)
     * @param sellVolume total volume at best ask
     */
    public void updateMarket(String symbol,
                             Price buyPrice, int buyVolume,
                             Price sellPrice, int sellVolume) {

        String spreadStr = computeSpread(buyPrice, sellPrice);

        CurrentMarketSide buySide  = new CurrentMarketSide(buyPrice,  buyVolume);
        CurrentMarketSide sellSide = new CurrentMarketSide(sellPrice, sellVolume);

        System.out.println("*********** Current Market ***********");
        System.out.printf("* %s   %s - %s %s%n", symbol, buySide, sellSide, spreadStr);
        System.out.println("**************************************");

        CurrentMarketPublisher.getInstance().acceptCurrentMarket(symbol, buySide, sellSide);
    }

    /** Returns the formatted spread string, e.g. "[$0.15]". */
    private String computeSpread(Price buyPrice, Price sellPrice) {
        if (buyPrice == null || sellPrice == null) {
            return "[$0.00]";
        }
        int diffCents = Math.abs(sellPrice.getCents() - buyPrice.getCents());
        return String.format("[$%.2f]", diffCents / 100.0);
    }
}
