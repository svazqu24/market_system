package price;

import java.util.Objects;

/**
 * Immutable monetary value stored in whole cents.
 * All arithmetic methods return a new Price instance.
 */
public class Price implements Comparable<Price> {

    private final int cents;

    public Price(int cents) {
        this.cents = cents;
    }

    public int getCents() {
        return cents;
    }

    public boolean isNegative() {
        return cents < 0;
    }

    public Price add(Price p) throws InvalidPriceException {
        if (p == null) {
            throw new InvalidPriceException("Price cannot be null");
        }
        return new Price(cents + p.cents);
    }

    public Price subtract(Price p) throws InvalidPriceException {
        if (p == null) {
            throw new InvalidPriceException("Price cannot be null");
        }
        return new Price(cents - p.cents);
    }

    public Price multiply(int n) {
        return new Price(cents * n);
    }

    public boolean greaterOrEqual(Price p) throws InvalidPriceException {
        if (p == null) throw new InvalidPriceException("Price cannot be null");
        return this.cents >= p.cents;
    }

    public boolean lessOrEqual(Price p) throws InvalidPriceException {
        if (p == null) throw new InvalidPriceException("Price cannot be null");
        return this.cents <= p.cents;
    }

    public boolean greaterThan(Price p) throws InvalidPriceException {
        if (p == null) throw new InvalidPriceException("Price cannot be null");
        return this.cents > p.cents;
    }

    public boolean lessThan(Price p) throws InvalidPriceException {
        if (p == null) throw new InvalidPriceException("Price cannot be null");
        return this.cents < p.cents;
    }

    /**
     * FIX: was "this.cents - p.cents" which silently overflows for large values.
     * Integer.compare() is safe and correct.
     */
    @Override
    public int compareTo(Price p) {
        if (p == null) return 1;
        return Integer.compare(this.cents, p.cents);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        return cents == ((Price) o).cents;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(cents);
    }

    @Override
    public String toString() {
        int absCents = Math.abs(cents);
        int dollars = absCents / 100;
        int remainderCents = absCents % 100;
        String formatted = String.format("$%,d.%02d", dollars, remainderCents);
        return cents < 0 ? formatted.replace("$", "$-") : formatted;
    }
}
