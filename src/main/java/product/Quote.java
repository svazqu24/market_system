package product;

import tradingsystem.DataValidationException;

/**
 * A two-sided quote submitted by a market-maker, consisting of one
 * {@link QuoteSide} for the BUY side and one for the SELL side.
 *
 * FIX: throws {@link DataValidationException} instead of raw {@link Exception}.
 */
public class Quote {

    private final String user;
    private final String product;
    private final QuoteSide buySide;
    private final QuoteSide sellSide;

    /**
     * @param symbol     1–5 character product ticker
     * @param buyPrice   limit price for the BUY quote side
     * @param buyVolume  volume for the BUY side (1–9999)
     * @param sellPrice  limit price for the SELL quote side
     * @param sellVolume volume for the SELL side (1–9999)
     * @param userName   3-uppercase-letter user code
     * @throws DataValidationException if any argument fails validation
     */
    public Quote(String symbol,
                 price.Price buyPrice, int buyVolume,
                 price.Price sellPrice, int sellVolume,
                 String userName) throws DataValidationException {

        if (userName == null || !userName.matches("^[A-Z]{3}$")) {
            throw new DataValidationException("Invalid user code: must be 3 uppercase letters");
        }
        if (symbol == null || !symbol.matches("^[A-Za-z0-9.]{1,5}$")) {
            throw new DataValidationException("Invalid product symbol: " + symbol);
        }
        if (buyPrice == null || sellPrice == null) {
            throw new DataValidationException("Buy and sell prices cannot be null");
        }
        if (buyVolume <= 0 || buyVolume >= 10000 || sellVolume <= 0 || sellVolume >= 10000) {
            throw new DataValidationException("Invalid volume: must be between 1 and 9999");
        }

        this.user = userName;
        this.product = symbol;
        this.buySide  = new QuoteSide(userName, symbol, buyPrice,  buyVolume,  BookSide.BUY);
        this.sellSide = new QuoteSide(userName, symbol, sellPrice, sellVolume, BookSide.SELL);
    }

    public String getSymbol() {
        return product;
    }

    public String getUser() {
        return user;
    }

    /**
     * Returns the {@link QuoteSide} for the requested {@code side},
     * or {@code null} if an unrecognised side is passed.
     */
    public QuoteSide getQuoteSide(BookSide side) {
        if (side == BookSide.BUY)  return buySide;
        if (side == BookSide.SELL) return sellSide;
        return null;
    }
}
