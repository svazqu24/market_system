package product;

import tradingsystem.DataValidationException;
import tradingsystem.UserManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * One side (BUY or SELL) of a product's order book, ordered by price priority.
 * BUY side: highest price first. SELL side: lowest price first.
 *
 * FIX: {@link #removeQuotesForUser} previously iterated {@code bookEntries} while
 *      calling {@link #cancel}, which modifies {@code bookEntries} — a latent
 *      {@link java.util.ConcurrentModificationException}. Fixed by collecting
 *      the target ID first, then cancelling after the loop.
 * FIX: Removed duplicate {@link UserManager#updateTradable} calls that were
 *      also being made by {@link tradingsystem.ProductManager}. This class is
 *      now the single owner of user-update notifications.
 */
public class ProductBookSide {

    private final BookSide side;

    /** Price → ordered list of tradables at that price level. */
    private final TreeMap<price.Price, ArrayList<Tradable>> bookEntries;

    public ProductBookSide(BookSide side) {
        if (side == null) {
            throw new IllegalArgumentException("Side cannot be null");
        }
        this.side = side;
        this.bookEntries = (side == BookSide.BUY)
                ? new TreeMap<>(Comparator.reverseOrder())
                : new TreeMap<>();
    }

    public BookSide getSide() {
        return side;
    }

    /**
     * Adds a tradable to this book side and notifies the user.
     *
     * @return a DTO snapshot of the tradable as it was added
     */
    public TradableDTO add(Tradable t) {
        if (t == null) {
            throw new IllegalArgumentException("Tradable cannot be null");
        }

        bookEntries.computeIfAbsent(t.getPrice(), k -> new ArrayList<>()).add(t);

        TradableDTO dto = t.makeTradableDTO();
        notifyUser(t.getUser(), dto);
        return dto;
    }

    /**
     * Cancels the tradable with the given ID: sets remaining volume to 0,
     * removes it from the book, and notifies the user.
     *
     * @return the cancelled tradable's DTO, or {@code null} if not found
     */
    public TradableDTO cancel(String tradableId) {
        if (tradableId == null) return null;

        for (Map.Entry<price.Price, ArrayList<Tradable>> entry : bookEntries.entrySet()) {
            Iterator<Tradable> it = entry.getValue().iterator();
            while (it.hasNext()) {
                Tradable t = it.next();
                if (tradableId.equals(t.getId())) {
                    System.out.println("**CANCEL: " + t);
                    t.setCancelledVolume(t.getRemainingVolume());
                    t.setRemainingVolume(0);
                    it.remove();
                    if (entry.getValue().isEmpty()) {
                        bookEntries.remove(entry.getKey());
                    }
                    TradableDTO dto = t.makeTradableDTO();
                    notifyUser(t.getUser(), dto);
                    return dto;
                }
            }
        }
        return null;
    }

    /**
     * Finds and cancels the one quote-side entry belonging to {@code username}.
     *
     * FIX: previously called {@code cancel()} while iterating {@code bookEntries},
     * risking a ConcurrentModificationException. Now collects the ID first,
     * then delegates to {@link #cancel(String)} outside the loop.
     *
     * @return DTO of the removed entry, or {@code null} if none found
     */
    public TradableDTO removeQuotesForUser(String username) {
        if (username == null) return null;

        // Phase 1: find the ID without modifying the map
        String foundId = null;
        outer:
        for (ArrayList<Tradable> list : bookEntries.values()) {
            for (Tradable t : list) {
                if (username.equals(t.getUser())) {
                    foundId = t.getId();
                    break outer;
                }
            }
        }

        // Phase 2: cancel by ID (safely mutates bookEntries outside the iteration)
        return foundId != null ? cancel(foundId) : null;
    }

    /** @return the best price on this side, or {@code null} if the book is empty */
    public price.Price topOfBookPrice() {
        return bookEntries.isEmpty() ? null : bookEntries.firstKey();
    }

    /** @return total volume available at the best price, or 0 if the book is empty */
    public int topOfBookVolume() {
        price.Price top = topOfBookPrice();
        if (top == null) return 0;
        ArrayList<Tradable> list = bookEntries.get(top);
        if (list == null) return 0;
        int sum = 0;
        for (Tradable t : list) {
            sum += t.getRemainingVolume();
        }
        return sum;
    }

    /**
     * Trades {@code vol} shares out of the best price level on this side.
     * If {@code vol} covers the entire level, all entries are fully filled.
     * Otherwise, each entry is partially filled pro-rata.
     */
    public void tradeOut(price.Price tradePrice, int vol) {
        if (tradePrice == null || vol <= 0) return;

        price.Price top = topOfBookPrice();
        if (top == null) return;

        // Only trade if this side's best price is tradable against the given price
        int cmp = top.compareTo(tradePrice);
        boolean tradable = (side == BookSide.BUY) ? (cmp >= 0) : (cmp <= 0);
        if (!tradable) return;

        ArrayList<Tradable> atPrice = bookEntries.get(top);
        if (atPrice == null || atPrice.isEmpty()) return;

        ArrayList<Tradable> snapshot = new ArrayList<>(atPrice);
        int totalVol = snapshot.stream().mapToInt(Tradable::getRemainingVolume).sum();
        if (totalVol <= 0) return;

        if (vol >= totalVol) {
            // Full fill of every entry at this price level
            for (Tradable t : snapshot) {
                int filled = t.getRemainingVolume();
                t.setFilledVolume(t.getFilledVolume() + filled);
                t.setRemainingVolume(0);
                System.out.println("\tFULL FILL: (" + side + " " + filled + ") " + t);
                notifyUser(t.getUser(), t.makeTradableDTO());
            }
            bookEntries.remove(top);
        } else {
            // Partial fill — distribute proportionally
            int remainder = vol;
            for (Tradable t : snapshot) {
                if (remainder <= 0) break;
                int tRem = t.getRemainingVolume();
                if (tRem <= 0) continue;

                double ratio  = (double) tRem / totalVol;
                int toTrade   = Math.min((int) Math.ceil(vol * ratio), Math.min(tRem, remainder));

                t.setFilledVolume(t.getFilledVolume() + toTrade);
                t.setRemainingVolume(tRem - toTrade);
                System.out.println("\tPARTIAL FILL: (" + side + " " + toTrade + ") " + t);
                notifyUser(t.getUser(), t.makeTradableDTO());
                remainder -= toTrade;
            }

            // Clean up fully-filled entries
            atPrice.removeIf(t -> t.getRemainingVolume() <= 0);
            if (atPrice.isEmpty()) {
                bookEntries.remove(top);
            }
        }
    }

    /** Silently notifies the user of a tradable state change; logs errors but does not throw. */
    private void notifyUser(String userId, TradableDTO dto) {
        try {
            UserManager.getInstance().updateTradable(userId, dto);
        } catch (DataValidationException e) {
            System.err.println("Warning: could not update user '" + userId + "': " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Side: ").append(side).append("\n");
        if (bookEntries.isEmpty()) {
            sb.append("  <Empty>");
            return sb.toString();
        }
        for (Map.Entry<price.Price, ArrayList<Tradable>> entry : bookEntries.entrySet()) {
            sb.append("  Price: ").append(entry.getKey()).append("\n");
            for (Tradable t : entry.getValue()) {
                sb.append("    ").append(t).append("\n");
            }
        }
        return sb.toString().trim();
    }
}
