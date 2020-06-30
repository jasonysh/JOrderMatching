package jordermatching.core;

import jordermatching.core.Order;
import jordermatching.core.ImplementSelector.MarketDataStoreType;
import jordermatching.core.ImplementSelector.MatchListType;
import jordermatching.core.ImplementSelector.OrderListTreeMapType;
import jordermatching.core.ImplementSelector.OrderListType;
import jordermatching.core.Order.OrdType;
import jordermatching.core.Order.Side;
import jordermatching.util.TestUtility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class OrderBookEngineTest {
  private boolean verbose = true;
  
  private Order createOrder(Side side, Double price, long qty, OrdType ordType) {
    return TestUtility.createOrder(side, price, qty, ordType);
  }
  
  public void assertLastQty(long qty, Order order) {
    assertEquals(qty, order.getLastExecutedQty());
  }
  public void assertLastPrice(double price, Order order) {
    assertEquals(price, order.getLastExecutedPrice(), 0.00001);
  }

  private void testPlaceOrder(final OrderMatchingEngine engine) {
	final double[] prices = new double[10];
	final long[] volumes = new long[10];
	
    List<Order> matches = new ArrayList<Order>();
    Order order = createOrder(Side.BUY, 250.0, 1300, OrdType.LIMIT);
    final String symbol = order.getSymbol();
    matches = engine.placeOrder(order, matches);
    assertEquals(0, matches.size());

  	engine.readBids(symbol, prices, volumes);
    assertEquals(250.0, prices[0], 0.0000000001);
    assertEquals(1300L, volumes[0]);
    assertEquals(0.0, prices[1], 0.0000000001);
    assertEquals(0L, volumes[1]);
    
  	engine.readAsks(symbol, prices, volumes);
    assertEquals(0.0, prices[0], 0.0000000001);
    assertEquals(0L, volumes[0]);
    
    matches = engine.placeOrder(createOrder(Side.SELL, 248.0, 1900, OrdType.LIMIT), matches);
    assertLastQty(1300L, matches.get(0));
    assertLastPrice(250, matches.get(0));
    assertEquals(1, matches.size());
    matches.clear();

  	engine.readBids(symbol, prices, volumes);
    assertEquals(0.0, prices[0], 0.0000000001);
    assertEquals(0L, volumes[0]);
  	
  	engine.readAsks(symbol, prices, volumes);
    assertEquals(248.0, prices[0], 0.0000000001);
    assertEquals(600L, volumes[0]);
    assertEquals(0.0, prices[1], 0.0000000001);
    assertEquals(0L, volumes[1]);

    
    matches = engine.placeOrder(createOrder(Side.BUY, 0.0, 300, OrdType.MARKET), matches);
    assertLastQty(300L, matches.get(0));
    assertLastPrice(248, matches.get(0));
    assertEquals(1, matches.size());
    matches.clear();
    
  	engine.readBids(symbol, prices, volumes);
    assertEquals(0.0, prices[0], 0.0000000001);
    assertEquals(0L, volumes[0]);
  	engine.readAsks(symbol, prices, volumes);
    assertEquals(248.0, prices[0], 0.0000000001);
    assertEquals(300L, volumes[0]);
    assertEquals(0.0, prices[1], 0.0000000001);
    assertEquals(0L, volumes[1]);
    
    matches = engine.placeOrder(createOrder(Side.BUY, 0.0, 900, OrdType.MARKET), matches);
    assertLastQty(300L, matches.get(0));
    assertLastPrice(248, matches.get(0));
    assertEquals(1, matches.size());
    matches.clear();
    
  	engine.readBids(symbol, prices, volumes);
    assertEquals(0.0, prices[0], 0.0000000001);
    assertEquals(0L, volumes[0]);
  	engine.readAsks(symbol, prices, volumes);
    assertEquals(0.0, prices[0], 0.0000000001);
    assertEquals(0L, volumes[0]);
    
    matches = engine.placeOrder(createOrder(Side.BUY, 0.0, 300, OrdType.MARKET), matches);
    assertEquals(0, matches.size());
    
  	engine.readBids(symbol, prices, volumes);
    assertEquals(0.0, prices[0], 0.0000000001);
    assertEquals(0L, volumes[0]);
  	engine.readAsks(symbol, prices, volumes);
    assertEquals(0.0, prices[0], 0.0000000001);
    assertEquals(0L, volumes[0]);
    
    matches = engine.placeOrder(createOrder(Side.BUY, 0.0, 100, OrdType.MARKET), matches);
    assertEquals(0, matches.size());
    
  	engine.readBids(symbol, prices, volumes);
    assertEquals(0.0, prices[0], 0.0000000001);
    assertEquals(0L, volumes[0]);
  	engine.readAsks(symbol, prices, volumes);
    assertEquals(0.0, prices[0], 0.0000000001);
    assertEquals(0L, volumes[0]);
    
    matches = engine.placeOrder(createOrder(Side.BUY, 246.0, 1700, OrdType.LIMIT), matches);
    assertEquals(0, matches.size());
    
  	engine.readBids(symbol, prices, volumes);
    assertEquals(246.0, prices[0], 0.0000000001);
    assertEquals(1700L, volumes[0]);
    assertEquals(0.0, prices[1], 0.0000000001);
    assertEquals(0L, volumes[1]);
  	engine.readAsks(symbol, prices, volumes);
    assertEquals(0.0, prices[0], 0.0000000001);
    assertEquals(0L, volumes[0]);
    
    matches = engine.placeOrder(createOrder(Side.SELL, 0.0, 1200, OrdType.MARKET), matches);
    assertLastQty(1200L, matches.get(0));
    assertLastPrice(246, matches.get(0));
    assertEquals(1, matches.size());
    matches.clear();
    
  	engine.readBids(symbol, prices, volumes);
    assertEquals(246.0, prices[0], 0.0000000001);
    assertEquals(500L, volumes[0]);
    assertEquals(0.0, prices[1], 0.0000000001);
    assertEquals(0L, volumes[1]);
  	engine.readAsks(symbol, prices, volumes);
    assertEquals(0.0, prices[0], 0.0000000001);
    assertEquals(0L, volumes[0]);
    
    matches = engine.placeOrder(createOrder(Side.SELL, 0.0, 1100, OrdType.MARKET), matches);
    assertLastQty(500L, matches.get(0));
    assertLastPrice(246, matches.get(0));
    assertEquals(1, matches.size());
    matches.clear();
    
  	engine.readBids(symbol, prices, volumes);
    assertEquals(0.0, prices[0], 0.0000000001);
    assertEquals(0L, volumes[0]);
  	engine.readAsks(symbol, prices, volumes);
    assertEquals(0.0, prices[0], 0.0000000001);
    assertEquals(0L, volumes[0]);
    
    matches = engine.placeOrder(createOrder(Side.BUY, 246.0, 1700, OrdType.LIMIT), matches);
    assertEquals(0, matches.size());
    
  	engine.readBids(symbol, prices, volumes);
    assertEquals(246.0, prices[0], 0.0000000001);
    assertEquals(1700L, volumes[0]);
    assertEquals(0.0, prices[1], 0.0000000001);
    assertEquals(0L, volumes[1]);
  	engine.readAsks(symbol, prices, volumes);
    assertEquals(0.0, prices[0], 0.0000000001);
    assertEquals(0L, volumes[0]);
    
    matches = engine.placeOrder(createOrder(Side.BUY, 0.0, 1000, OrdType.MARKET), matches);
    assertEquals(0, matches.size());
    
  	engine.readBids(symbol, prices, volumes);
    assertEquals(246.0, prices[0], 0.0000000001);
    assertEquals(1700L, volumes[0]);
    assertEquals(0.0, prices[1], 0.0000000001);
    assertEquals(0L, volumes[1]);
  	engine.readAsks(symbol, prices, volumes);
    assertEquals(0.0, prices[0], 0.0000000001);
    assertEquals(0L, volumes[0]);
    
    matches = engine.placeOrder(createOrder(Side.SELL, 246.0, 1600, OrdType.LIMIT), matches);
    assertLastQty(1600L, matches.get(0));
    assertLastPrice(246, matches.get(0));
    assertEquals(1, matches.size());
    matches.clear();
    
  	engine.readBids(symbol, prices, volumes);
    assertEquals(246.0, prices[0], 0.0000000001);
    assertEquals(100L, volumes[0]);
    assertEquals(0.0, prices[1], 0.0000000001);
    assertEquals(0L, volumes[1]);
  	engine.readAsks(symbol, prices, volumes);
    assertEquals(0.0, prices[0], 0.0000000001);
    assertEquals(0L, volumes[0]);
    
    matches = engine.placeOrder(createOrder(Side.SELL, 0.0, 700, OrdType.MARKET), matches);
    assertLastQty(100L, matches.get(0));
    assertLastPrice(246, matches.get(0));
    assertEquals(1, matches.size());
    matches.clear();
    
  	engine.readBids(symbol, prices, volumes);
    assertEquals(0.0, prices[0], 0.0000000001);
    assertEquals(0L, volumes[0]);
  	engine.readAsks(symbol, prices, volumes);
    assertEquals(0.0, prices[0], 0.0000000001);
    assertEquals(0L, volumes[0]);
    
    matches = engine.placeOrder(createOrder(Side.SELL, 248.0, 100, OrdType.LIMIT), matches);
    assertEquals(0, matches.size());
    
  	engine.readBids(symbol, prices, volumes);
    assertEquals(0.0, prices[0], 0.0000000001);
    assertEquals(0L, volumes[0]);
  	engine.readAsks(symbol, prices, volumes);
    assertEquals(248.0, prices[0], 0.0000000001);
    assertEquals(100L, volumes[0]);
    assertEquals(0.0, prices[1], 0.0000000001);
    assertEquals(0L, volumes[1]);
    
    matches = engine.placeOrder(createOrder(Side.SELL, 0.0, 1800, OrdType.MARKET), matches);
    assertEquals(0, matches.size());
    
  	engine.readBids(symbol, prices, volumes);
    assertEquals(0.0, prices[0], 0.0000000001);
    assertEquals(0L, volumes[0]);
  	engine.readAsks(symbol, prices, volumes);
    assertEquals(248.0, prices[0], 0.0000000001);
    assertEquals(100L, volumes[0]);
    assertEquals(0.0, prices[1], 0.0000000001);
    assertEquals(0L, volumes[1]);
    
    matches = engine.placeOrder(createOrder(Side.BUY, 0.0, 1800, OrdType.MARKET), matches);
    assertLastQty(100L, matches.get(0));
    assertLastPrice(248, matches.get(0));
    assertEquals(1, matches.size());
    matches.clear();
    
  	engine.readBids(symbol, prices, volumes);
    assertEquals(0.0, prices[0], 0.0000000001);
    assertEquals(0L, volumes[0]);
  	engine.readAsks(symbol, prices, volumes);
    assertEquals(0.0, prices[0], 0.0000000001);
    assertEquals(0L, volumes[0]);
    
    matches = engine.placeOrder(createOrder(Side.SELL, 246.0, 900, OrdType.LIMIT), matches);
    assertEquals(0, matches.size());
    
  	engine.readBids(symbol, prices, volumes);
    assertEquals(0.0, prices[0], 0.0000000001);
    assertEquals(0L, volumes[0]);
  	engine.readAsks(symbol, prices, volumes);
    assertEquals(246.0, prices[0], 0.0000000001);
    assertEquals(900L, volumes[0]);
    assertEquals(0.0, prices[1], 0.0000000001);
    assertEquals(0L, volumes[1]);
    
    matches = engine.placeOrder(createOrder(Side.BUY, 246.0, 1300, OrdType.LIMIT), matches);
    assertLastQty(900L, matches.get(0));
    assertLastPrice(246, matches.get(0));
    matches.clear();
    
  	engine.readBids(symbol, prices, volumes);
    assertEquals(246.0, prices[0], 0.0000000001);
    assertEquals(400L, volumes[0]);
    assertEquals(0.0, prices[1], 0.0000000001);
    assertEquals(0L, volumes[1]);
  	engine.readAsks(symbol, prices, volumes);
    assertEquals(0.0, prices[0], 0.0000000001);
    assertEquals(0L, volumes[0]);
    
    matches = engine.placeOrder(createOrder(Side.SELL, 250.0, 1300, OrdType.LIMIT), matches);
    assertEquals(0, matches.size());
    
  	engine.readBids(symbol, prices, volumes);
    assertEquals(246.0, prices[0], 0.0000000001);
    assertEquals(400L, volumes[0]);
    assertEquals(0.0, prices[1], 0.0000000001);
    assertEquals(0L, volumes[1]);
  	engine.readAsks(symbol, prices, volumes);
    assertEquals(250.0, prices[0], 0.0000000001);
    assertEquals(1300L, volumes[0]);
    assertEquals(0.0, prices[1], 0.0000000001);
    assertEquals(0L, volumes[1]);
    
    matches = engine.placeOrder(createOrder(Side.SELL, 250.0, 1300, OrdType.LIMIT), matches);
    assertEquals(0, matches.size());
    
  	engine.readBids(symbol, prices, volumes);
    assertEquals(246.0, prices[0], 0.0000000001);
    assertEquals(400L, volumes[0]);
    assertEquals(0.0, prices[1], 0.0000000001);
    assertEquals(0L, volumes[1]);
  	engine.readAsks(symbol, prices, volumes);
    assertEquals(250.0, prices[0], 0.0000000001);
    assertEquals(2600L, volumes[0]);
    assertEquals(0.0, prices[1], 0.0000000001);
    assertEquals(0L, volumes[1]);
    
    Order _order = createOrder(Side.SELL, 251.0, 1300, OrdType.LIMIT);
    matches = engine.placeOrder(_order, matches);
    assertEquals(0, matches.size());
    
  	engine.readBids(symbol, prices, volumes);
    assertEquals(246.0, prices[0], 0.0000000001);
    assertEquals(400L, volumes[0]);
    assertEquals(0.0, prices[1], 0.0000000001);
    assertEquals(0L, volumes[1]);
  	engine.readAsks(symbol, prices, volumes);
    assertEquals(250.0, prices[0], 0.0000000001);
    assertEquals(2600L, volumes[0]);
    assertEquals(251.0, prices[1], 0.0000000001);
    assertEquals(1300L, volumes[1]);
    assertEquals(0.0, prices[2], 0.0000000001);
    assertEquals(0L, volumes[2]);
    
    //test modify
    String error = engine.modifyOrder(_order.getSymbol(), _order.getSide(), _order.getOrderId(), _order.getOrderId()+"_New", _order.getQty()/2, _order.getPrice()*1.1);
    assertEquals(null, error);
    
    error = engine.modifyOrder(_order.getSymbol(), _order.getSide(), "not found", _order.getOrderId(), _order.getQty(), _order.getPrice());
    assertEquals("Cannot find the order", error);

    _order = engine.getOrder(_order.getSymbol(), _order.getSide(), _order.getOrderId());
    assertNotEquals(null, _order);
    
    error = engine.modifyOrder(_order.getSymbol(), _order.getSide(), _order.getOrderId(), _order.getOrderId(), 0L, _order.getPrice());
    assertEquals(null, error);
    
    _order = engine.getOrder(_order.getSymbol(), _order.getSide(), _order.getOrderId());
    assertEquals(null, _order);
    
    _order = createOrder(Side.SELL, 251.0, 1300, OrdType.LIMIT);
    matches = engine.placeOrder(_order, matches);
    assertEquals(0, matches.size());
    
    error = engine.modifyOrder(_order.getSymbol(), _order.getSide(), _order.getOrderId(), _order.getOrderId(), _order.getQty(), _order.getPrice()*1.1);
    assertEquals(null, error);
    
    error = engine.modifyOrder(_order.getSymbol(), _order.getSide(), _order.getOrderId(), _order.getOrderId(), _order.getQty()*2, _order.getPrice());
    assertEquals(null, error);
    
    Order cancelledOrder = engine.cancelOrder(_order.getSymbol(), _order.getSide(), _order.getOrderId());
    assertEquals(cancelledOrder.getOrderId(), _order.getOrderId());
    
    _order = createOrder(Side.BUY, 221.0, 1300, OrdType.LIMIT);
    
    matches = engine.placeOrder(_order, matches);
    assertEquals(0, matches.size());
    
    matches = engine.placeOrder(createOrder(Side.BUY, 221.0, 1300, OrdType.LIMIT), matches);
    assertEquals(0, matches.size());
    
    error = engine.modifyOrder(_order.getSymbol(), _order.getSide(), _order.getOrderId(), _order.getOrderId(), _order.getQty()*2, _order.getPrice());
    assertEquals(null, error);
    
    matches = engine.placeOrder(createOrder(Side.BUY, 0d, 13000, OrdType.MARKET), matches);
    assertEquals(2, matches.size());
    
    System.out.println(engine.toString());
  }
  
  @Test
  public void testPlaceOrder_CustomLinkedList_JDKTree_Volatile_Preallocate() {
    final OrderMatchingEngine engine = new OrderMatchingEngine(verbose, OrderListType.CustomLinked,
    		MarketDataStoreType.Volatile, MatchListType.Preallocate);
    testPlaceOrder(engine);
  }
  
  @Test
  public void testPlaceOrder_ArrayList_JDKTree_Volatile_AllocateEveryTime() {
    final OrderMatchingEngine engine = new OrderMatchingEngine(verbose, OrderListType.Array100,
    		MarketDataStoreType.Volatile, MatchListType.AllocateEveryTime);
    testPlaceOrder(engine);
  }
  
  @Test
  public void testPlaceOrder_LinkedList_JDKTree_Volatile_AllocateEveryTime() {
    final OrderMatchingEngine engine = new OrderMatchingEngine(verbose, OrderListType.Linked,
    		MarketDataStoreType.Volatile, MatchListType.AllocateEveryTime);
    testPlaceOrder(engine);
  }
  
  @Test
  public void testPlaceOrder_CustomLinkedList_JDKTree_Atomic_Preallocate() {
    final OrderMatchingEngine engine = new OrderMatchingEngine(verbose, OrderListType.CustomLinked,
            MarketDataStoreType.CompareAndSwap, MatchListType.Preallocate);
    testPlaceOrder(engine);
  }
  
  @Test
  public void testPlaceOrder_ArrayList_JDKTree_Atomic_AllocateEveryTime() {
    final OrderMatchingEngine engine = new OrderMatchingEngine(verbose, OrderListType.Array100,
            MarketDataStoreType.CompareAndSwap, MatchListType.AllocateEveryTime);
    testPlaceOrder(engine);
  }
  
  @Test
  public void testPlaceOrder_LinkedList_JDKTree_Atomic_AllocateEveryTime() {
    final OrderMatchingEngine engine = new OrderMatchingEngine(verbose, OrderListType.Linked,
            MarketDataStoreType.CompareAndSwap, MatchListType.AllocateEveryTime);
    testPlaceOrder(engine);
  }
  
  @Test
  public void testPlaceOrder_CustomLinkedList_JDKTree_ReadWriteLock_Preallocate() {
    final OrderMatchingEngine engine = new OrderMatchingEngine(verbose, OrderListType.CustomLinked,
            MarketDataStoreType.ReadWriteLock, MatchListType.Preallocate);
    testPlaceOrder(engine);
  }
  
  @Test
  public void testPlaceOrder_ArrayDeque_JDKTree_ReadWriteLock_AllocateEveryTime() {
    final OrderMatchingEngine engine = new OrderMatchingEngine(verbose, OrderListType.ArrayDeque,
            MarketDataStoreType.CompareAndSwap, MatchListType.AllocateEveryTime);
    testPlaceOrder(engine);
  }
  
  @Test
  public void testPlaceOrder_LinkedList_JDKTree_ReadWriteLock_AllocateEveryTime() {
    final OrderMatchingEngine engine = new OrderMatchingEngine(verbose, OrderListType.Linked,
            MarketDataStoreType.ReadWriteLock, MatchListType.AllocateEveryTime);
    testPlaceOrder(engine);
  }
  
  @Test
  public void testPlaceOrder_CustomLinkedList_JDKTree_ReentrantLock_Preallocate() {
    final OrderMatchingEngine engine = new OrderMatchingEngine(verbose, OrderListType.CustomLinked,
            MarketDataStoreType.ReentrantLock, MatchListType.Preallocate);
    testPlaceOrder(engine);
  }
  
  @Test
  public void testPlaceOrder_ArrayList_JDKTree_ReentrantLock_AllocateEveryTime() {
    final OrderMatchingEngine engine = new OrderMatchingEngine(verbose, OrderListType.Array100,
            MarketDataStoreType.ReentrantLock, MatchListType.AllocateEveryTime);
    testPlaceOrder(engine);
  }
  
  @Test
  public void testPlaceOrder_LinkedList_JDKTree_ReentrantLock_AllocateEveryTime() {
    final OrderMatchingEngine engine = new OrderMatchingEngine(verbose, OrderListType.Linked,
            MarketDataStoreType.ReentrantLock, MatchListType.AllocateEveryTime);
    testPlaceOrder(engine);
  }
}
