package jordermatching.benchmark;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import jordermatching.core.Order;
import jordermatching.core.OrderBook;
import jordermatching.core.OrderMatchingEngine;
import jordermatching.core.ImplementSelector.MarketDataStoreType;
import jordermatching.core.ImplementSelector.MatchListType;
import jordermatching.core.ImplementSelector.OrderListType;
import jordermatching.util.ParseJMHResult;
import jordermatching.util.TestFileReadUtility;
import jordermatching.util.TestUtility;

public class BenchmarkOrderMatchingEngine {
  public static ArrayList<Order> baseOrders = new ArrayList<Order>(1000000);
  public static ArrayList<Order> testOrders = new ArrayList<Order>(4000000);
  static {
	    TestFileReadUtility.getOrdersFromFile("testcase"+System.getProperty("file.separator")+"1000000_1548403416822.txt", baseOrders);
	    TestFileReadUtility.getOrdersFromFile("testcase"+System.getProperty("file.separator")+"4000000_1548307577700.txt", testOrders);
  }
	


  public static class MarketDataReader implements Runnable {
    private volatile boolean stop = false;
    private OrderMatchingEngine engine;
    private double[] prices = new double[100];
    private long[] volumes = new long[100];
    StringBuilder sb = new StringBuilder();
    //private int cnt = 0;
    public MarketDataReader(OrderMatchingEngine engine) { this.engine = engine; }
    @Override public void run() {
      //cnt = 0;
        while (!stop) {
            engine.readAsks("stock1", prices, volumes);
            engine.readBids("stock1", prices, volumes);
            //cnt++;
            try {
              Thread.sleep(1);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
        }
        //System.out.println(System.currentTimeMillis()+" MarketDataReader cnt "+cnt);
    }
    public void stop() { this.stop = true; }
  }
  
  private static void startReaderThreads(OrderMatchingEngine engine, MarketDataReader[] readers, Thread[] threads) {
    for (int i = 0; i < readers.length; i++) {
        readers[i] = new MarketDataReader(engine);
    }
    for (int i = 0; i < threads.length; i++) {
        threads[i] = new Thread(readers[i]);
    }
    for (int i = 0; i < threads.length; i++) {
        threads[i].start();
    }
  }

  private static void stopReaderThreads(MarketDataReader[] readers, Thread[] threads) throws InterruptedException {
    for (int i = 0; i < readers.length; i++) {
        readers[i].stop();
    }
    for (int i = 0; i < threads.length; i++) {
        threads[i].join();
    }
  }
  
  @State(Scope.Thread)
  public static abstract class AbstractBenchmark {
	public ArrayList<Order> matches = new ArrayList<Order>();
	protected OrderMatchingEngine engine;
	protected int index;
    public Order currentOrder;
    public MarketDataReader[] readers;
    public Thread[] threads;

    public void initialize(OrderMatchingEngine engine) {
    	this.engine = engine;
    	
    	index = 0;
    	
    	//initialize 100k order base
    	for (int i = 0; i < baseOrders.size(); i++) {
    		matches.clear();
    		engine.placeOrder(baseOrders.get(i), matches);
    	}
    	//System.out.println("2 iterationSetup "+engine.getTypeDesc()+" engine.size(): "+engine.size()
    	//	+" baseOrders.size(): "+baseOrders.size()+" index:"+index);
if (readers == null) {
        readers = new MarketDataReader[1];
        threads = new Thread[readers.length];
        startReaderThreads(engine, readers, threads);
}
    }
    
    @Setup(Level.Invocation)
    public void invocationSetup() {
    	matches.clear();
    	if (index >= testOrders.size()) {
    	  index = 0;
    	}
    	if (currentOrder != null) {
    	  OrderBook ob = engine.getOrderBook(currentOrder.getSymbol());
    	  if (currentOrder.getSide() == Order.Side.BUY && ob.getBidSize() > baseOrders.size()
    			  || currentOrder.getSide() == Order.Side.SELL && ob.getAskSize() > baseOrders.size()) {
        	  engine.cancelOrder(currentOrder.getSymbol(), currentOrder.getSide(), currentOrder.getOrderId());
    	  }
    	}
    	currentOrder = new Order(testOrders.get(index++));
    	currentOrder.setClientOrderID(System.nanoTime()+currentOrder.getClientOrderId());
    }
    
    @TearDown
    public void tearDown() throws InterruptedException {
    	//System.out.println("tearDown ENGINE SIZE: "+engine.size());
        //stopReaderThreads(readers, threads);
    }
    
    @Benchmark
    //@Warmup(iterations = 1, batchSize = 7000)
   // @Measurement(iterations = 1, batchSize = 7000)
    @Warmup(iterations = 10, time = 10000, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 10, time = 10000, timeUnit = TimeUnit.MILLISECONDS)
    public void bench() {
		engine.placeOrder(currentOrder, matches);
    }
  }
  /*
  public static class BenchmarkArrayDequeCAS extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
       initialize(new OrderMatchingEngine(false, OrderListType.ArrayDeque, MarketDataStoreType.CompareAndSwap, MatchListType.Preallocate));
    }
  }
  
  public static class BenchmarkACustomLinkedCAS extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
       initialize(new OrderMatchingEngine(false, OrderListType.CustomLinked, MarketDataStoreType.CompareAndSwap, MatchListType.Preallocate));
    }
  }*/
  /*
  public static class BenchmarkCustomLinkedReentrantLock extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
       initialize(new OrderMatchingEngine(false, OrderListType.CustomLinked, MarketDataStoreType.ReentrantLock, MatchListType.Preallocate));
    }
  }
  
  public static class BenchmarkCustomLinkedReadWriteLock extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
       initialize(new OrderMatchingEngine(false, OrderListType.CustomLinked, MarketDataStoreType.ReadWriteLock, MatchListType.Preallocate));
    }
  }*/

  /*
  public static class BenchmarkCustomLinkedVolatile extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
       initialize(new OrderMatchingEngine(false, OrderListType.CustomLinked, MarketDataStoreType.Volatile, MatchListType.Preallocate));
    }
  }
  */
  public static class BenchmarkArray100CAS extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
       initialize(new OrderMatchingEngine(false, OrderListType.Array100, MarketDataStoreType.CompareAndSwap, MatchListType.Preallocate));
    }
  }
  public static class BenchmarkArrayDequeCAS extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
       initialize(new OrderMatchingEngine(false, OrderListType.ArrayDeque, MarketDataStoreType.CompareAndSwap, MatchListType.Preallocate));
    }
  }
  /*
  public static class BenchmarkArray100ListReadWriteLock extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
       initialize(new OrderMatchingEngine(false, OrderListType.Array100, MarketDataStoreType.ReadWriteLock, MatchListType.Preallocate));
    }
  }
  public static class BenchmarkArray100ListReentrantLock extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
       initialize(new OrderMatchingEngine(false, OrderListType.Array100, MarketDataStoreType.ReentrantLock, MatchListType.Preallocate));
    }
  }
  public static class BenchmarkArray100ListVolatile extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
       initialize(new OrderMatchingEngine(false, OrderListType.Array100, MarketDataStoreType.Volatile, MatchListType.Preallocate));
    }
  }*//*
  
  public static class BenchmarkLinkedListVolatile extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
       initialize(new OrderMatchingEngine(false, OrderListType.Linked, MarketDataStoreType.Volatile, MatchListType.Preallocate));
    }
  }
  
  public static class BenchmarkArrayDequeCompareAndSwap extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
       initialize(new OrderMatchingEngine(false, OrderListType.ArrayDeque, MarketDataStoreType.CompareAndSwap, MatchListType.Preallocate));
    }
  }
  */
  /*
  public static class BenchmarkArray50ListCAS extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
       initialize(new OrderMatchingEngine(false, OrderListType.Array10, MarketDataStoreType.CompareAndSwap, MatchListType.Preallocate));
    }
  }*/
  /*
  public static class BenchmarkCustomLinkedCASAllocateEveryTime extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
       initialize(new OrderMatchingEngine(false, OrderListType.CustomLinked, MarketDataStoreType.CompareAndSwap, MatchListType.AllocateEveryTime));
    }
  }
  */
  public static void main(String[] args)
  {
      try {
    	  final String className = new Object() {}.getClass().getEnclosingClass().getSimpleName();
    	  

    	  final String fileName = TestUtility.getBenchmarkFileName(className);
    	  
    	  final Options opt = new OptionsBuilder()
                  .include(".*" + className + ".*")
                  .forks(1)
                  .output(fileName+".txt")
                  .result(fileName+".json")
                  .resultFormat(ResultFormatType.JSON)
                  .jvmArgs("-server", "-Xms3072m", "-Xmx3072m", "-XX:+UseConcMarkSweepGC")
                  //.jvmArgs("-server", "-Xms3072m", "-Xmx3072m")
                  .addProfiler(GCProfiler.class)
                  .build();

          new Runner(opt).run();
          ParseJMHResult.convertTxtToCSV(fileName);
      } catch( Exception ex) {
          System.out.printf("error: %s", ex.getMessage());
      }
  }
}
