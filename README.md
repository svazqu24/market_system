# Market System

A simulated stock market order-book system written in Java 17.

## Architecture

```
src/main/java/
├── Main.java                        # Entry point / demo driver
├── market/                          # Market data & Observer pattern
│   ├── CurrentMarketObserver.java   # Observer interface
│   ├── CurrentMarketPublisher.java  # Manages subscriptions, dispatches updates
│   ├── CurrentMarketSide.java       # Snapshot of one side of the market (price × volume)
│   └── CurrentMarketTracker.java    # Computes spread, drives Publisher
├── price/                           # Monetary value types
│   ├── Price.java                   # Immutable cent-based price value
│   ├── PriceFactory.java            # Flyweight cache for Price instances
│   └── InvalidPriceException.java
├── product/                         # Order-book domain model
│   ├── BookSide.java                # BUY / SELL enum
│   ├── Tradable.java                # Interface for Order & QuoteSide
│   ├── TradableDTO.java             # Immutable record snapshot of a Tradable
│   ├── Order.java                   # Single-sided market order
│   ├── Quote.java                   # Two-sided quote (BUY + SELL)
│   ├── QuoteSide.java               # One side of a Quote (implements Tradable)
│   ├── ProductBook.java             # Full BUY+SELL book for one symbol
│   └── ProductBookSide.java         # One side of the order book (price-priority queue)
└── tradingsystem/                   # System-level managers (singletons)
    ├── DataValidationException.java
    ├── ProductManager.java          # Manages all ProductBooks
    ├── User.java                    # Holds a user's active orders & market data
    └── UserManager.java             # Registry of all Users
```

## Key Design Patterns

| Pattern | Where used |
|---|---|
| Singleton (holder idiom) | `ProductManager`, `UserManager`, `CurrentMarketPublisher`, `CurrentMarketTracker` |
| Observer | `CurrentMarketObserver` / `CurrentMarketPublisher` |
| Flyweight | `PriceFactory` – caches `Price` instances by cent value |
| DTO | `TradableDTO` record – safe snapshot passed to `User` |

## Building & Running

```bash
# Compile and run with Maven
mvn compile
mvn exec:java -Dexec.mainClass="Main"

# Or build a fat jar and run directly
mvn package
java -jar target/market-system-1.0.0.jar
```

## Bug Fixes Applied (v1.0.0)

- **Double user-update**: `ProductBookSide` was calling `UserManager.updateTradable()` and `ProductManager` was calling it again for the same event. Removed the redundant calls from `ProductManager`.
- **`removeQuotesForUser` ConcurrentModificationException**: Iterating `bookEntries` while calling `cancel()` which modifies it. Fixed by collecting the ID first, then cancelling outside the loop.
- **`Price.compareTo()` overflow**: `this.cents - p.cents` overflows for extreme values. Fixed with `Integer.compare()`.
- **Raw `Exception` throws**: `Order`, `Quote`, `QuoteSide` now throw `DataValidationException` instead of plain `Exception`.
- **`tryTrade()` visibility**: Was `public`; made `private` since it is an internal `ProductBook` concern.
- **Non-thread-safe singletons**: All four singletons now use the initialization-on-demand holder idiom (thread-safe, no locking overhead).
- **Non-thread-safe `PriceFactory` cache**: Replaced `HashMap` with `ConcurrentHashMap`.
- **Opaque `NoSuchMethodError` catch** in `CurrentMarketTracker`: Removed; width is now computed cleanly.
- **Non-unique IDs**: `Order` and `QuoteSide` IDs now use `UUID.randomUUID()` instead of `System.nanoTime()`.
