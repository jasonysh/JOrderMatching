package jordermatching.core;

import java.util.HashMap;

import jordermatching.collections.AbstractOrderList;

public class Order {
  public enum Side {
    BUY, SELL;
  }

  public enum OrdType {
    MARKET, LIMIT;
  }

  // private String orderId;
  private String clientOrderId;
  private String symbol;
  private String sender;
  private String target;
  private Side side;
  private OrdType type;

  private double price;
  private double avgExecutedPrice, lastExecutedPrice;
  private long timestamp, qty, openQty, executedQty, lastExecutedQty;

  // Self-implemented Doubly-linked List
  private AbstractOrderList list;
  private Order nextOrder, prevOrder;

  public Order() {
  }

  public Order(Order order) {
    this(order.getClientOrderId(), order.getSymbol(), order.getSender(), order.getTarget(), order.getSide(),
        order.getType(), order.getPrice(), order.getQty());
  }

  // for Testing
  public Order(HashMap<String, String> map) {
    this(map.get("clientOrderId"), map.get("symbol"), map.get("sender"), map.get("target"), map.get("side").equals("BUY")? Side.BUY: /*'1'*/Side.SELL,
        map.get("type").equals("LIMIT")? OrdType.LIMIT:OrdType.MARKET, Double.parseDouble(map.get("price")), Long.parseLong(map.get("quantity")));
  }

  public Order(String clientOrderId, String symbol, String sender, String target, Side side, OrdType type, double price,
      long quantity) {
    super();
    set(clientOrderId, symbol, sender, target, side, type, price, quantity);
  }

  public void set(String clientOrderId, String symbol, String sender, String target, Side side, OrdType type, double price,
      long quantity) {
    this.clientOrderId = clientOrderId;
    this.symbol = symbol;
    this.sender = sender;
    this.target = target;
    this.side = side;
    this.type = type;
    this.price = roundPrice(price);
    this.avgExecutedPrice = 0d;
    this.lastExecutedPrice = 0d;
    this.qty = quantity;
    openQty = quantity;
    executedQty = 0l;
    lastExecutedQty = 0l;
    this.timestamp = System.nanoTime();
    list = null;
    nextOrder = null;
    prevOrder = null;

  }

  public static String getOrderId(String sender, String clientOrderId) {
    return sender+"_"+clientOrderId;
  }
  
  public String getOrderId() {
    return getOrderId(sender, clientOrderId);
  }

  public String getClientOrderId() {
    return clientOrderId;
  }

  public String getSymbol() {
    return symbol;
  }

  public String getSender() {
    return sender;
  }

  public String getTarget() {
    return target;
  }

  public Side getSide() {
    return side;
  }

  public OrdType getType() {
    return type;
  }

  public double getPrice() {
    return price;
  }

  public double getAvgExecutedPrice() {
    return avgExecutedPrice;
  }

  public double getLastExecutedPrice() {
    return lastExecutedPrice;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public long getQty() {
    return qty;
  }

  public long getOpenQty() {
    return openQty;
  }

  public long getExecutedQty() {
    return executedQty;
  }

  public long getLastExecutedQty() {
    return lastExecutedQty;
  }

  public boolean isClosed() {
    return openQty == 0;
  }

  public AbstractOrderList getOrderList() {
    return list;
  }

  public Order getNextOrder() {
    return nextOrder;
  }

  public Order getPrevOrder() {
    return prevOrder;
  }

  private static double roundPrice(double price) {
    final long factor = (long) Math.pow(10, 2);
    return ((double) Math.round(price * factor)) / factor;
  }

  public static double calcAvgPx(long lastQty, double lastPx, long executedQty, double avgPx) {
    return roundPrice(((lastQty * lastPx) + (avgPx * executedQty)) / (lastQty + executedQty));
  }

  public void execute(double price, long quantity) {
    avgExecutedPrice = calcAvgPx(quantity, price, executedQty, avgExecutedPrice);
    executedQty += quantity;
    openQty -= quantity;
    lastExecutedPrice = price;
    lastExecutedQty = quantity;
    if (type == OrdType.LIMIT && list != null) {
      list.setVolume(list.getVolume() - quantity);
    }
  }

  public void cancel() {
    openQty = 0;
  }

  // public void setOrderId(String orderId) { this.orderId = orderId; }
  public void setNextOrder(Order order) {
    this.nextOrder = order;
  }

  public void setPrevOrder(Order order) {
    this.prevOrder = order;
  }

  public void setOrderList(AbstractOrderList list) {
    this.list = list;
  }

  public void setClientOrderID(String clientOrderId) {
    this.clientOrderId = clientOrderId;
  }

  public void setPrice(double price) {// Modify order request
    this.price = roundPrice(price);
    this.timestamp = System.nanoTime();
  }

  public void setQty(long qty) {// Modify order request
    if (this.qty < qty && list.tail() != this) {// Move order to the end of the list. i.e. loses time priority
      list.moveToTail(this);
      this.timestamp = System.nanoTime();
    }
    list.setVolume(list.getVolume() - (this.qty - qty));
    this.qty = qty;
    openQty = qty;
  }

  public String toString() {
    return qty + (" (oid: "+getOrderId()+" S:" + (side) + ",P:" + price + ",OQ:" + openQty + ",EQ:" + executedQty + ",AP:$" + avgExecutedPrice
        + ",LP:$" + lastExecutedPrice + ",LQ:" + lastExecutedQty + ")");
  }
}
