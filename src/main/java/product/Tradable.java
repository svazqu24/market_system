package product;

/**
 * Common contract for all objects that can be placed in an order book:
 * {@link Order} and {@link QuoteSide}.
 */
public interface Tradable {

    String getId();

    int getRemainingVolume();
    void setRemainingVolume(int newVol);

    int getCancelledVolume();
    void setCancelledVolume(int newVol);

    int getFilledVolume();
    void setFilledVolume(int newVol);

    int getOriginalVolume();

    String getUser();
    String getProduct();

    price.Price getPrice();
    BookSide getSide();

    TradableDTO makeTradableDTO();
}
