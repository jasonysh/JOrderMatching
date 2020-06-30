package jordermatching.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Setup;

import jordermatching.core.Order;
import jordermatching.core.OrderMatchingEngine;
import jordermatching.core.ImplementSelector.MarketDataStoreType;
import jordermatching.core.ImplementSelector.MatchListType;
import jordermatching.core.ImplementSelector.OrderListType;
import jordermatching.core.Order.OrdType;
import jordermatching.core.Order.Side;

public class TestUtility {
  public static String getBenchmarkFileName(String className) {
	String s = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(new Date());
    return "benchmark/"+s+"_"+className;
  }
	
  private static long orderId = 0;
  
  public static Order createOrder(Side side, Double price, long qty, OrdType ordType) {
      String clientOrderId = String.valueOf(orderId++);
      String symbol = "AAPL";
      String sender = "client1";
      String target = "TS";
      return new Order(clientOrderId, symbol, sender, target, side, ordType, price, qty);
  }
  
  public static void main(String[] args) {
    /*    public static enum OrderListType {CustomLinked, Linked, Array100, Array1000, Array5000}
      
      public static enum MarketDataStoreType {AllocateNew, Atomic, ReadWriteLock, ReentrantLock}
      
      public static enum MatchListType { Preallocate, AllocateEveryTime }*/
    ArrayList<String> orderListTypes = new ArrayList<String>();
    orderListTypes.add("CustomLinked");
    orderListTypes.add("Linked");
    orderListTypes.add("Array100");
    orderListTypes.add("Array1000");
    
    ArrayList<String> marketDataStoreTypes = new ArrayList<String>();
    marketDataStoreTypes.add("AllocateNew");
    marketDataStoreTypes.add("Atomic");
    marketDataStoreTypes.add("ReadWriteLock");
    marketDataStoreTypes.add("ReentrantLock");
    
    ArrayList<String> matchListTypes = new ArrayList<String>();
    matchListTypes.add("Preallocate");
    matchListTypes.add("AllocateEveryTime");
    
    for (String orderListType: orderListTypes) {
      for (String marketDataStoreType: marketDataStoreTypes) {
        for (String matchListType: matchListTypes) {
          System.out.println("          public static class Benchmark_OrderList"+orderListType+"_MarketDataStore"+marketDataStoreType+"_MatchList"+matchListType+" extends AbstractBenchmark {");
          System.out.println("            @Setup(Level.Iteration)");
          System.out.println("            public void setup() {");
          System.out.println("                initialize(new OrderMatchingEngine(false, OrderListType."+orderListType+", MarketDataStoreType."+marketDataStoreType+", MatchListType."+matchListType+"));");
          System.out.println("            }");
          System.out.println("          }");
        }
      }
    }
  }
}
