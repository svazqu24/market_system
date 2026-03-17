package product;

import tradingsystem.DataValidationException;
import java.util.UUID;

/**
 * One side (BUY or SELL) of a two-sided {@link Quote}.
 * Implements {@link Tradable} so it can be placed directly into a
 * {@link ProductBookSide}.
 *
 * FIX: throws {@link DataValidationException} instead of raw {@link Exception}.
 * FIX: ID uses {@link UUID#randomUUID()} for uniqueness.
 */
public class QuoteSide implements Tradable {

    private final String user;
    private final String product;
    private final price.Price price;
    private final BookSide side;
    private final int originalVolume;
    private final String id;

    private int remainingVolume;
    private int filledVolume;
    private int cancelledVolume;

    public QuoteSide(String user, String product, price.Price price, int originalVolume, BookSide side)
            throws DataValidationException {

        if (user == null || !user.matches("^[A-Z]{3}$")) {
            throw new DataValidationException("Invalid user code: must be 3 uppercase letters");
        }
        if (product == null || !product.matches("^[A-Za-z0-9.]{1,5}$")) {
            throw new DataValidationException("Invalid product symbol: " + product);
        }
        if (price == null) {
            throw new DataValidationException("Price cannot be null");
        }
        if (side == null) {
            throw new DataValidationException("Book side cannot be null");
        }
        if (originalVolume <= 0 || originalVolume >= 10000) {
            throw new DataValidationException("Invalid volume: must be between 1 and 9999");
        }

        this.user = user;
        this.product = product;
        this.price = price;
        this.side = side;
        this.originalVolume = originalVolume;
        this.remainingVolume = originalVolume;
        this.filledVolume = 0;
        this.cancelledVolume = 0;
        this.id = user + "-" + product + "-" + side + "-" + UUID.randomUUID();
    }

    @Override public String getId()               { return id; }
    @Override public String getUser()             { return user; }
    @Override public String getProduct()          { return product; }
    @Override public price.Price getPrice()       { return price; }
    @Override public BookSide getSide()           { return side; }
    @Override public int getOriginalVolume()      { return originalVolume; }
    @Override public int getRemainingVolume()     { return remainingVolume; }
    @Override public int getFilledVolume()        { return filledVolume; }
    @Override public int getCancelledVolume()     { return cancelledVolume; }

    @Override
    public void setRemainingVolume(int newVol) {
        remainingVolume = Math.max(newVol, 0);
    }

    @Override
    public void setFilledVolume(int newVol) {
        filledVolume = Math.max(newVol, 0);
    }

    @Override
    public void setCancelledVolume(int newVol) {
        cancelledVolume = Math.max(newVol, 0);
    }

    @Override
    public TradableDTO makeTradableDTO() {
        return new TradableDTO(this);
    }

    @Override
    public String toString() {
        return String.format(
                "%s %s quote side for %s: %s, Orig: %d, Rem: %d, Fill: %d, CXL: %d, ID: %s",
                user, side, product, price,
                originalVolume, remainingVolume, filledVolume, cancelledVolume, id
        );
    }
}
