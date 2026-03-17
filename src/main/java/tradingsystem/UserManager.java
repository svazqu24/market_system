package tradingsystem;

import product.TradableDTO;

import java.util.TreeMap;

/**
 * System-level registry of all {@link User}s, keyed by user ID.
 * Uses a {@link TreeMap} so users are always listed in alphabetical order.
 *
 * FIX: singleton now uses the initialization-on-demand holder idiom
 *      for guaranteed thread safety without synchronization overhead.
 * FIX: consistent indentation throughout.
 */
public class UserManager {

    /** Sorted by userId for deterministic output. */
    private final TreeMap<String, User> users = new TreeMap<>();

    private UserManager() {}

    // --- Initialization-on-demand holder (thread-safe, lazy) ---
    private static class Holder {
        private static final UserManager INSTANCE = new UserManager();
    }

    public static UserManager getInstance() {
        return Holder.INSTANCE;
    }

    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------

    /**
     * Populates the registry with the supplied user IDs.
     * Must be called once before any other method.
     *
     * @param userIds array of 3-uppercase-letter user IDs; must not be null
     * @throws DataValidationException if the array is null or any ID is blank/invalid
     */
    public void init(String[] userIds) throws DataValidationException {
        if (userIds == null) {
            throw new DataValidationException("User ID array cannot be null");
        }
        for (String id : userIds) {
            if (id == null || id.trim().isEmpty()) {
                throw new DataValidationException("User ID list contains a null or blank entry");
            }
            String trimmed = id.trim();
            users.put(trimmed, new User(trimmed));
        }
    }

    // -------------------------------------------------------------------------
    // Lookup
    // -------------------------------------------------------------------------

    /**
     * Returns the {@link User} registered under {@code userId}.
     *
     * @throws DataValidationException if userId is null or not registered
     */
    public User getUser(String userId) throws DataValidationException {
        if (userId == null) {
            throw new DataValidationException("User ID cannot be null");
        }
        User u = users.get(userId);
        if (u == null) {
            throw new DataValidationException("User not found: " + userId);
        }
        return u;
    }

    // -------------------------------------------------------------------------
    // Tradable update
    // -------------------------------------------------------------------------

    /**
     * Forwards a {@link TradableDTO} update to the correct user.
     *
     * @throws DataValidationException if userId is null/blank or the user is not registered
     */
    public void updateTradable(String userId, TradableDTO dto) throws DataValidationException {
        if (userId == null || userId.trim().isEmpty()) {
            throw new DataValidationException("User ID cannot be null or empty");
        }
        if (dto == null) return;

        User user = users.get(userId);
        if (user == null) {
            throw new DataValidationException("Cannot update tradable — user not found: " + userId);
        }
        user.updateTradable(dto);
    }

    // -------------------------------------------------------------------------
    // Display
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (User u : users.values()) {
            sb.append(u).append("\n");
        }
        return sb.toString();
    }
}
