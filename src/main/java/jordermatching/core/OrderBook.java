package jordermatching.core;

import java.util.ArrayList;
import java.util.List;

import jordermatching.core.ImplementSelector.MarketDataStoreType;
import jordermatching.core.ImplementSelector.MatchListType;
import jordermatching.core.ImplementSelector.OrderListTreeMapType;
import jordermatching.core.ImplementSelector.OrderListType;
import jordermatching.core.Order.OrdType;
import jordermatching.core.Order.Side;

//Not thread safe
//Refer to https://github.com/DrAshBooth/JavaLOB/blob/master/src/lob/OrderTree.java
public class OrderBook {
  
  private MatchListType matchListType;
  private OrderTree bidTree;
  private OrderTree askTree;
  private long totalExecutedQty = 0;
  private long tradeCnt = 0;
  private double high;
  private double low;
  
  public OrderBook(OrderListType listType, OrderListTreeMapType treeType, MarketDataStoreType mdStoreType, MatchListType matchListType) {
      this.matchListType = matchListType;
      bidTree = new OrderTree(Side.BUY, listType, treeType, mdStoreType);
      askTree = new OrderTree(Side.SELL, listType, treeType, mdStoreType);
  }
  
  public List<Order> placeOrder(Order order, List<Order> matchedList) {
    List<Order> ret = match(order, matchedList);
    bidTree.reloadMarketData();
    askTree.reloadMarketData();
    return ret;
  }
  
  public void readBids(double[] prices, long[] volumes) {
	  bidTree.read(prices, volumes);
  }
  
  public void readAsks(double[] prices, long[] volumes) {
	  askTree.read(prices, volumes);
  }
  
  public List<Order> match(Order order, List<Order> _matches) {
      List<Order> matches = matchListType == MatchListType.Preallocate? _matches: new ArrayList<Order>();
      
	try {
	    while (true) {
	      if (order.getOpenQty() == 0) {
	        return matches;
	      }
	      Order bidOrder;
	      Order askOrder;
	      if (order.getSide() == Side.BUY) {
	        if (askTree.size() == 0) {
		   return matches;
	        }
	        bidOrder = order;
	        askOrder = askTree.getMinPriceList().head();
	      } else {
	        if (bidTree.size() == 0) {
	          return matches;
	        }
	        bidOrder = bidTree.getMaxPriceList().head();
	        askOrder = order;
	      }
	      if (bidOrder.getType() == OrdType.MARKET ||
	          askOrder.getType() == OrdType.MARKET || 
	              (Double.compare(bidOrder.getPrice(), askOrder.getPrice())>=0)) {
	          match(bidOrder, askOrder);
	          if (bidOrder != order && !matches.contains(bidOrder)) {
	              matches.add(0, bidOrder);
	          }
	          if (askOrder != order && !matches.contains(askOrder)) {
	              matches.add(0, askOrder);
	          }
	
	          if (bidOrder != order && bidOrder.isClosed()) {
	              bidTree.removeOrderByID(bidOrder.getOrderId());
	          }
	          if (askOrder != order && askOrder.isClosed()) {
	              askTree.removeOrderByID(askOrder.getOrderId());
	          }
	      } else {
	          return matches;
	      }
	    }
	} finally {
          if (order.getType() == OrdType.LIMIT && order.getOpenQty() > 0) {
        	  (order.getSide() == Side.BUY? bidTree: askTree).insertOrder(order);
          }
	}
  }

  private void match(Order bid, Order ask) {
      final double price;
      if (ask.getType() == OrdType.LIMIT) {
        if (bid.getType() == OrdType.MARKET) {
          price = ask.getPrice();
        } else {//both are limit
          price = bid.getTimestamp() < ask.getTimestamp()? bid.getPrice(): ask.getPrice();
        } 
      } else {//ask is Market, so bid is Limit 
        price = bid.getPrice();
      }
      final long quantity = bid.getOpenQty() >= ask.getOpenQty() ? ask.getOpenQty() : bid.getOpenQty();
      bid.execute(price, quantity);
      ask.execute(price, quantity);
      
      totalExecutedQty+=quantity;
      tradeCnt++;
      if (high < price) {
    	  high = price;
      } else if (low > price) {
    	  low = price;
      }
  }
  
  public String modifyOrder(Side side, String internalOrderId, String newInternalOrderId, long quantity, double price) {
    String ret = (side == Side.BUY? bidTree:askTree).updateOrderByID(internalOrderId, newInternalOrderId, quantity, price);
    bidTree.reloadMarketData();
    askTree.reloadMarketData();
    return ret;
  }
  
  public Order getOrder(Side side, String internalOrderId) {
    return (side == Side.BUY? bidTree:askTree).getOrderByID(internalOrderId);
  }

  public String toStatString() {
    return "|   ----- Trade Stat -----   |"+
           "\n| Total Executed Qty = "+totalExecutedQty+
           "\n| Trade Count = "+tradeCnt+
           "\n|   ----- Bid Stat -----   |"+
           bidTree.toStatString() +
           "|   ----- Ask Stat -----   |"+
           askTree.toStatString();
  }
  
  @Override
  public String toString() {
    return bidTree.toString() +
           askTree.toString();
  }

  public double getBestBid() { return bidTree.getMaxPrice(); }
  public double getBestAsk() { return askTree.getMinPrice(); }
  public double getHigh() { return high; }
  public double getLow() { return low; }
  public int size() { return bidTree.size()+askTree.size(); }
  public int getBidSize() { return bidTree.size(); }
  public int getAskSize() { return askTree.size(); }

  public Order cancelOrder(Side side, String internalOrderId) {
    Order order = (side == Side.BUY? bidTree:askTree).removeOrderByID(internalOrderId);
    if (order != null) {
      order.cancel();
    }
    bidTree.reloadMarketData();
    askTree.reloadMarketData();
    return order;
  }
}
