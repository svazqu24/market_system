package product;

/**
 * Immutable snapshot of a {@link Tradable} at a point in time.
 * Passed to {@link tradingsystem.User} so the user's view of its orders
 * cannot accidentally mutate the live book entry.
 */
public record TradableDTO(
        String user,
        String product,
        price.Price price,
        int originalVolume,
        int remainingVolume,
        int cancelledVolume,
        int filledVolume,
        BookSide side,
        String tradableId
) {
    /** Convenience constructor that snapshots a live {@link Tradable}. */
    public TradableDTO(Tradable t) {
        this(
                t.getUser(),
                t.getProduct(),
                t.getPrice(),
                t.getOriginalVolume(),
                t.getRemainingVolume(),
                t.getCancelledVolume(),
                t.getFilledVolume(),
                t.getSide(),
                t.getId()
        );
    }
}
