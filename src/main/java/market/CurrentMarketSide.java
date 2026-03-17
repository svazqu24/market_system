package market;

import price.Price;

/**
 * An immutable snapshot of one side (BUY or SELL) of the current market:
 * the best available price and the total volume available at that price.
 */
public class CurrentMarketSide {

    private final Price price;
    private final int volume;

    public CurrentMarketSide(Price price, int volume) {
        this.price = price;
        this.volume = volume;
    }

    public Price getPrice() {
        return price;
    }

    public int getVolume() {
        return volume;
    }

    /** Returns "{price}x{volume}", e.g. "$12.34x500". Null price displays as "$0.00". */
    @Override
    public String toString() {
        if (price == null) return "$0.00x" + volume;
        return price.toString() + "x" + volume;
    }
}
