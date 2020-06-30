package jordermatching.core;

import java.util.HashMap;
import java.util.List;

import jordermatching.core.ImplementSelector.MarketDataStoreType;
import jordermatching.core.ImplementSelector.MatchListType;
import jordermatching.core.ImplementSelector.OrderListTreeMapType;
import jordermatching.core.ImplementSelector.OrderListType;
import jordermatching.core.Order.Side;

public class OrderMatchingEngine {

  private final HashMap<String/* Symbol */, OrderBook> obMap = new HashMap<String/* Symbol */, OrderBook>();

  private boolean verbose;
  private long clock = 0L;
  private OrderListType listType;
  private OrderListTreeMapType treeType;
  private MarketDataStoreType mdStoreType;
  private MatchListType matchListType;

  public OrderMatchingEngine() {
    this(false, OrderListType.CustomLinked, MarketDataStoreType.ReentrantLock, MatchListType.Preallocate);
  }

  public OrderMatchingEngine(boolean verbose, OrderListType listType, 
      MarketDataStoreType mdStoreType, MatchListType matchListType) {
    this.verbose = verbose;
    this.listType = listType;
    this.treeType = OrderListTreeMapType.JDK;
    this.mdStoreType = mdStoreType;
    this.matchListType = matchListType;
  }

  // only one single thread should access only at the same time
  public OrderBook getOrderBook(String symbol) {
    OrderBook ob = obMap.get(symbol);
    if (ob == null) {
      ob = new OrderBook(listType, treeType, mdStoreType, matchListType);
      obMap.put(symbol, ob);
    }
    return ob;
  }

  /**
   * 
   * @param order
   * @param matchedList matching orders will be added to matched, the input order
   *                    will not be included if matched.
   */
  public List<Order> placeOrder(Order order, List<Order> matchedList) {
    if (order.isClosed()) {
      if (this.verbose) {
        System.out.println("order size should not be 0.");
      }
      return matchedList;
    }
    List<Order> ret = getOrderBook(order.getSymbol()).placeOrder(order, matchedList);
    if (verbose) {
      clock++;
      for (String symbol : obMap.keySet()) {
        double[] prices = new double[10];
        long[] volumes = new long[10];
        readBids(symbol, prices, volumes);
        for (int i = 0; i < 10; i++) {
          if (volumes[i] != 0 || prices[i] != 0) {
            System.out.println(clock + " Level " + (i + 1) + " BID - symbol: " + symbol + " price: " + prices[i]
                + " volume: " + volumes[i]);
          }
        }

        prices = new double[10];
        volumes = new long[10];
        readAsks(symbol, prices, volumes);
        for (int i = 0; i < 10; i++) {
          if (volumes[i] != 0 || prices[i] != 0) {
            System.out.println(clock + " Level " + (i + 1) + " ASK - symbol: " + symbol + " price: " + prices[i]
                + " volume: " + volumes[i]);
          }
        }
      }
    }
    return ret;
  }

  public String modifyOrder(String symbol, Side side, String internalOrderId, String newInternalOrderId, long quantity,
      double price) {
    return getOrderBook(symbol).modifyOrder(side, internalOrderId, newInternalOrderId, quantity, price);
  }

  /**
   * @return the cancelled order, or {@code null} if order book does not contain
   *         the order.
   */
  public Order cancelOrder(String symbol, Side side, String internalOrderId) {
    return getOrderBook(symbol).cancelOrder(side, internalOrderId);
  }

  public Order getOrder(String symbol, Side side, String internalOrderId) {
    return getOrderBook(symbol).getOrder(side, internalOrderId);
  }

  public double getBestBid(String symbol) {
    return getOrderBook(symbol).getBestBid();
  }

  public double getBestAsk(String symbol) {
    return getOrderBook(symbol).getBestAsk();
  }

  public void readBids(String symbol, double[] prices, long[] volumes) {
    getOrderBook(symbol).readBids(prices, volumes);
  }

  public void readAsks(String symbol, double[] prices, long[] volumes) {
    getOrderBook(symbol).readAsks(prices, volumes);
  }

  public int size() {
    int size = 0;
    for (OrderBook ob : obMap.values()) {
      size += ob.size();
    }
    return size;
  }

  public String getTypeDesc() {
    return listType.name() + " " + treeType.name() + " " + mdStoreType.name();
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (OrderBook ob : obMap.values()) {
      sb.append(ob.toStatString());
    }
    for (OrderBook ob : obMap.values()) {
      sb.append(ob.toString());
    }
    return sb.toString();
  }
}
