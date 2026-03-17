package price;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Flyweight factory for Price instances.
 * Caches every Price by its cent value so callers sharing the same price
 * always receive the same object reference.
 *
 * FIX: replaced HashMap with ConcurrentHashMap for thread safety.
 */
public abstract class PriceFactory {

    private static final ConcurrentHashMap<Integer, Price> priceCache = new ConcurrentHashMap<>();

    /** Returns a cached (or newly created) Price for the given cent value. */
    public static Price makePrice(int cents) {
        return priceCache.computeIfAbsent(cents, Price::new);
    }

    /**
     * Parses a price string such as "$1,234.56" or "-56.78" into a Price.
     *
     * @param stringValueIn dollar-formatted price string
     * @return cached Price instance
     * @throws InvalidPriceException if the string is null, empty, or malformed
     */
    public static Price makePrice(String stringValueIn) throws InvalidPriceException {
        if (stringValueIn == null || stringValueIn.trim().isEmpty()) {
            throw new InvalidPriceException("Price string cannot be null or empty");
        }

        String s = stringValueIn.trim();

        if (s.startsWith("$")) {
            s = s.substring(1);
        }

        boolean isNegative = false;
        if (s.startsWith("-")) {
            isNegative = true;
            s = s.substring(1);
        }

        s = s.replace(",", "");

        if (s.endsWith(".")) {
            s = s + "00";
        }

        if (s.chars().filter(c -> c == '.').count() > 1) {
            throw new InvalidPriceException("Price string contains multiple decimal points");
        }

        try {
            int cents;
            if (s.contains(".")) {
                String[] parts = s.split("\\.");
                if (parts.length == 2 && parts[1].length() != 2) {
                    throw new InvalidPriceException("Cents portion must be exactly 2 digits");
                }
                cents = (int) Math.round(Double.parseDouble(s) * 100);
            } else {
                cents = Integer.parseInt(s) * 100;
            }
            return makePrice(isNegative ? -cents : cents);
        } catch (NumberFormatException e) {
            throw new InvalidPriceException("Malformed price string: \"" + stringValueIn + "\"");
        }
    }
}
