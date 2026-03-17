package tradingsystem;

import market.CurrentMarketObserver;
import market.CurrentMarketSide;
import product.TradableDTO;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a market participant.
 *
 * <p>Each user holds:
 * <ul>
 *   <li>A map of active/historical {@link TradableDTO}s keyed by tradable ID.</li>
 *   <li>A map of the latest current-market snapshots keyed by product symbol,
 *       populated via the {@link CurrentMarketObserver} callback.</li>
 * </ul>
 */
public class User implements CurrentMarketObserver {

    private final String userId;

    /** tradableId → latest DTO snapshot */
    private final HashMap<String, TradableDTO> tradables = new HashMap<>();

    /** symbol → [buySide, sellSide] */
    private final HashMap<String, CurrentMarketSide[]> currentMarkets = new HashMap<>();

    /**
     * @param userId exactly 3 uppercase letters (e.g. "ANA")
     * @throws DataValidationException if the userId is invalid
     */
    public User(String userId) throws DataValidationException {
        if (userId == null || !userId.matches("[A-Z]{3}")) {
            throw new DataValidationException("User ID must be exactly 3 uppercase letters (A–Z)");
        }
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    // -------------------------------------------------------------------------
    // CurrentMarketObserver
    // -------------------------------------------------------------------------

    /**
     * Receives and stores the latest top-of-book snapshot for {@code symbol}.
     * Called by {@link market.CurrentMarketPublisher} whenever the market changes.
     */
    @Override
    public void updateCurrentMarket(String symbol, CurrentMarketSide buySide, CurrentMarketSide sellSide) {
        currentMarkets.put(symbol, new CurrentMarketSide[]{buySide, sellSide});
    }

    /**
     * Returns a multi-line string of every current-market snapshot this user holds.
     * Format per line: "{symbol}   {bid} - {ask}"
     */
    public String getCurrentMarkets() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, CurrentMarketSide[]> entry : currentMarkets.entrySet()) {
            CurrentMarketSide buy  = entry.getValue()[0];
            CurrentMarketSide sell = entry.getValue()[1];
            sb.append(entry.getKey())
              .append("   ")
              .append(buy  == null ? "$0.00x0" : buy.toString())
              .append(" - ")
              .append(sell == null ? "$0.00x0" : sell.toString())
              .append("\n");
        }
        return sb.toString().trim();
    }

    // -------------------------------------------------------------------------
    // Tradable tracking
    // -------------------------------------------------------------------------

    /**
     * Stores or updates the DTO for a tradable owned by this user.
     * Silently ignores {@code null} DTOs.
     */
    public void updateTradable(TradableDTO dto) {
        if (dto == null) return;
        tradables.put(dto.tradableId(), dto);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("User Id: ").append(userId).append("\n");
        for (TradableDTO dto : tradables.values()) {
            sb.append("  Product: ").append(dto.product())
              .append(", Price: ").append(dto.price())
              .append(", OrigVol: ").append(dto.originalVolume())
              .append(", RemVol: ").append(dto.remainingVolume())
              .append(", CxlVol: ").append(dto.cancelledVolume())
              .append(", FillVol: ").append(dto.filledVolume())
              .append(", User: ").append(dto.user())
              .append(", Side: ").append(dto.side())
              .append(", Id: ").append(dto.tradableId())
              .append("\n");
        }
        return sb.toString();
    }
}
