package jordermatching.core;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import jordermatching.collections.AbstractOrderList;
import jordermatching.collections.OrderArrayDeque;
import jordermatching.collections.OrderArrayList;
import jordermatching.collections.OrderCustomLinkedList;
import jordermatching.collections.OrderLinkedList;
import jordermatching.core.ImplementSelector.MarketDataStoreType;
import jordermatching.core.ImplementSelector.OrderListTreeMapType;
import jordermatching.core.ImplementSelector.OrderListType;
import jordermatching.core.Order.Side;
import jordermatching.marketdata.AtomicMarketDataStore;
import jordermatching.marketdata.IMarketDataStore;
import jordermatching.marketdata.ReadWriteLockMarketData;
import jordermatching.marketdata.ReentrantLockMarketDataStore;
import jordermatching.marketdata.VolatileMarketDataStore;

//Assume to be used by a single thread only
public class OrderTree {
  public final HashMap<String/*internalOrderId*/, Order> orderMap = new HashMap<String, Order>(10000);
  private final HashMap<Double/*price*/, AbstractOrderList> jdkHashMap = new HashMap<Double, AbstractOrderList>(10000);
  private final TreeMap<Double/*price*/, AbstractOrderList> jdkTreeMap = new TreeMap<Double, AbstractOrderList>();
  private StringBuilder sb = new StringBuilder();
  private Side side;
  private OrderListType listType;
  private OrderListTreeMapType treeType;
  private IMarketDataStore mdStore;
  
  public OrderTree(Side side, OrderListType listType, OrderListTreeMapType treeType, MarketDataStoreType mdStoreType) {
    this.side = side;
    this.listType = listType;
    this.treeType = treeType;
    this.mdStore = mdStoreType == MarketDataStoreType.CompareAndSwap? new AtomicMarketDataStore():
    	mdStoreType == MarketDataStoreType.ReadWriteLock? new ReadWriteLockMarketData():
        mdStoreType == MarketDataStoreType.ReentrantLock? new ReentrantLockMarketDataStore():
    	new VolatileMarketDataStore();
  }
  
  public void insertOrder(Order order) {
    if (orderMap.containsKey(order.getOrderId())) {
	throw new RuntimeException("Duplicated order id!");
    }
    orderMap.put(order.getOrderId(), order);
    insertOrderToPriceMap(order);
  }
  
  private void insertOrderToPriceMap(Order order) {
    final double price = order.getPrice();
    AbstractOrderList list;
    /*if (hashMapType == OrderListHashMapType.CustomPrimitive) {
    	list = customHashMap.get(price);
    } else {*/
    	list = jdkHashMap.get(price);
    //}
    if (list == null) {
      switch(listType) {
        case Linked: list = new OrderLinkedList(order.getSide(), order.getSymbol(), price); break;
        case Array10: list = new OrderArrayList(order.getSide(), order.getSymbol(), price, 10); break;
        case Array100: list = new OrderArrayList(order.getSide(), order.getSymbol(), price, 100); break;
        case ArrayDeque: list = new OrderArrayDeque(order.getSide(), order.getSymbol(), price); break;
        default: list = new OrderCustomLinkedList(order.getSide(), order.getSymbol(), price); break;
      }
      /*if (treeType == OrderListTreeMapType.CustomPrimitive) {
		  //customTreeMap.insert(price, list);
    	  customTreeMap.put(price, list);
      } else {*/
		  jdkTreeMap.put(price, list);
      //}
      
      /*if (hashMapType == OrderListHashMapType.CustomPrimitive) {
    	  customHashMap.insert(price, list);
      } else {*/
    	  jdkHashMap.put(price, list);
      //}
    }
    list.append(order);
    order.setOrderList(list);
  }
  
  public Order getOrderByID(String internalOrderId) {
    return orderMap.get(internalOrderId);
  }
  
  public String updateOrderByID(String internalOrderId, String newClientOrderID, long quantity, double price) {
    if (quantity == 0) {
	removeOrderByID(internalOrderId);
	return null;
    }
    final Order order = orderMap.remove(internalOrderId);
    if (order != null) {
      order.setClientOrderID(newClientOrderID);
      orderMap.put(order.getOrderId(), order);
      if (Double.compare(order.getPrice(), price) !=0) {//Change Price
        if (order.getQty() != quantity) {//Change Qty first if needed
          order.setQty(quantity);
        }
        removeOrderFromList(order.getOrderList(), order);
        order.setPrice(price);
        insertOrderToPriceMap(order);
      } else if (order.getQty() != quantity) {//Change Qty only
        order.setQty(quantity);
      }
      return null;
    }
    return "Cannot find the order";
  }
  
  private void removeOrderFromList(AbstractOrderList list, Order order) {
    order.setOrderList(null);
    list.remove(order);
    if (list.size() == 0) {
      final double d = order.getPrice();
	  jdkTreeMap.remove(d);
	  jdkHashMap.remove(d);
    }
  }

  public Order removeOrderByID(String internalOrderId) {
    final Order order = orderMap.remove(internalOrderId);
    if (order != null) {
      removeOrderFromList(order.getOrderList(), order);
    }
    return order;
  }
  
  public AbstractOrderList getMinPriceList() {
      /*if (treeType == OrderListTreeMapType.CustomPrimitive) {
    	  return customTreeMap.size() == 0? null: customTreeMap.firstEntry().getValue();
		  //return customTreeMap.size() == 0? null: (AbstractOrderList)customTreeMap.getValue(customTreeMap.firstEntry());
	  } else {*/
		  return jdkTreeMap.size() == 0? null: jdkTreeMap.firstEntry().getValue();
	  //}
  }
  public AbstractOrderList getMaxPriceList() { 
      /*if (treeType == OrderListTreeMapType.CustomPrimitive) {
    	  return customTreeMap.size() == 0? null: customTreeMap.lastEntry().getValue();
	  } else {*/
		  return jdkTreeMap.size() == 0? null: jdkTreeMap.lastEntry().getValue();
	  //}
  }
  public double getMaxPrice() {
	  AbstractOrderList ol = getMaxPriceList();
	  return ol == null? 0d: ol.getPrice();
  }
  public double getMinPrice() {
	  AbstractOrderList ol = getMinPriceList();
	  return ol == null? 0d: ol.getPrice();
  }
  public int size() {
	  return orderMap.size();
  }
  public int depth() { return /*treeType == OrderListTreeMapType.CustomPrimitive? customTreeMap.size():*/ jdkTreeMap.size(); }
  
  protected void reloadMarketData() {
	  double price0 = 0d, price1 = 0d, price2 = 0d, price3 = 0d, price4 = 0d,
	      price5 = 0d, price6 = 0d, price7 = 0d, price8 = 0d, price9 = 0d;
	  long volume0 = 0L, volume1 = 0L, volume2 = 0L, volume3 = 0L, volume4 = 0L,
	      volume5 = 0L, volume6 = 0L, volume7 = 0L, volume8 = 0L, volume9 = 0L;
	  
	  java.util.NavigableMap<Double, AbstractOrderList> map = jdkTreeMap;
	  if (side == Side.BUY) {
		  map = map.descendingMap();
	  }
      int i = 0;
	  for (Map.Entry<Double, AbstractOrderList> entry: map.entrySet()) {
		  AbstractOrderList value = entry.getValue();
		  switch(i) {
		  	case 0: price0 = value.getPrice(); volume0 = value.getVolume(); break;
		  	case 1: price1 = value.getPrice(); volume1 = value.getVolume(); break;
		  	case 2: price2 = value.getPrice(); volume2 = value.getVolume(); break;
		  	case 3: price3 = value.getPrice(); volume3 = value.getVolume(); break;
		  	case 4: price4 = value.getPrice(); volume4 = value.getVolume(); break;
		  	case 5: price5 = value.getPrice(); volume5 = value.getVolume(); break;
		  	case 6: price6 = value.getPrice(); volume6 = value.getVolume(); break;
		  	case 7: price7 = value.getPrice(); volume7 = value.getVolume(); break;
		  	case 8: price8 = value.getPrice(); volume8 = value.getVolume(); break;
		  	case 9: price9 = value.getPrice(); volume9 = value.getVolume(); break;
		  }
		  i++;
	  }
	  mdStore.set(price0, volume0, price1, volume1, price2, volume2, price3, volume3, price4, volume4,
			  price5, volume5, price6, volume6, price7, volume7, price8, volume8, price9, volume9);
  }
  
  public void read(double[] prices, long[] volumes) {
	  mdStore.read(prices, volumes);
  }
  
  public String toStatString() {
    sb.setLength(0);
    sb.append("\n| Min/Max = ").append(getMinPrice()).append("/").append(getMaxPrice())
      .append("\n| Depth/Orders = ").append(depth()).append("/").append(size()).append("\n");
    return sb.toString();
  } 
  public String toString() {
    sb.setLength(0);
    int level = 1;
    for (AbstractOrderList list : jdkTreeMap.values()) {
      sb.append("| ").append(side==Side.BUY?"BUY ":"SELL").append(" L").append(level++).append(" ");
      sb.append(list.toString());
      sb.append("\n");
    }
    return sb.toString();
  }
}
