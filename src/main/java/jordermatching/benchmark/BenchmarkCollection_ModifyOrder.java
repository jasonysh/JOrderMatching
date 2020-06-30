package jordermatching.benchmark;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

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
import org.openjdk.jmh.profile.DTraceAsmProfiler;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import jordermatching.core.Order;
import jordermatching.core.OrderMatchingEngine;
import jordermatching.core.ImplementSelector.MarketDataStoreType;
import jordermatching.core.ImplementSelector.MatchListType;
import jordermatching.core.ImplementSelector.OrderListType;
import jordermatching.util.ParseJMHResult;
import jordermatching.util.TestFileReadUtility;
import jordermatching.util.TestUtility;

public class BenchmarkCollection_ModifyOrder {
  public static ArrayList<Order> baseOrders = new ArrayList<Order>(100000);
  public static ArrayList<Order> testOrders = new ArrayList<Order>(2000000);
  static {
	    TestFileReadUtility.getOrdersFromFile("testcase"+System.getProperty("file.separator")+"all_unmatch_limit_100000_1542808419144.txt", baseOrders);
	    TestFileReadUtility.getOrdersFromFile("testcase"+System.getProperty("file.separator")+"all_unmatch_limit_100000_1542808419144.txt", testOrders);
  }
	
  @BenchmarkMode(Mode.SampleTime)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  //@BenchmarkMode(Mode.AverageTime)
  //@OutputTimeUnit(TimeUnit.MICROSECONDS)
  @State(Scope.Thread)
  public static abstract class AbstractBenchmark {
	public ArrayList<Order> matches = new ArrayList<Order>();
	protected OrderMatchingEngine engine;
	protected int index;
    public Order currentOrder;
    
    public void initialize(OrderMatchingEngine _engine) {
    	engine = _engine;
    	
    	index = 0;
    	
    	for (int i = 0; i < baseOrders.size(); i++) {
    		matches.clear();
    		engine.placeOrder(new Order(baseOrders.get(i)), matches);
    	}
    	System.out.println("2 iterationSetup "+engine.getTypeDesc()+" engine.size(): "+engine.size()
    		+" baseOrders.size(): "+baseOrders.size());
    }
    
    @Setup(Level.Invocation)
    public void invocationSetup() {
      if (index==testOrders.size()) {
        index = 0;
        /*for (int i = 0; i < baseOrders.size(); i++) {
          matches.clear();
          engine.placeOrder(new Order(baseOrders.get(i)), matches);
        }*/
      }
      currentOrder = new Order(testOrders.get(index++));
    }
    
    @TearDown
    public void tearDown() {
    	System.out.println("tearDown ENGINE SIZE: "+engine.size());
        //matches.clear();
    	//engine.placeOrder(new Order(currentOrder), matches);
    }
    
    @Benchmark
    @Warmup(iterations = 1, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 1, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
    public void bench() {
      String e = engine.modifyOrder(currentOrder.getSymbol(), currentOrder.getSide(), 
          Order.getOrderId(currentOrder.getSender(), currentOrder.getClientOrderId()),
          Order.getOrderId(currentOrder.getSender(), currentOrder.getClientOrderId()+"_NEW"),
          currentOrder.getQty()*2, currentOrder.getPrice()*0.9);
      //System.out.println(e);
      //engine.placeOrder(currentOrder, matches);
    }
  }
  /*
  public static class Benchmark_OrderListCustomLinked_MarketDataStoreVolatile_MatchListPreallocate extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.CustomLinked, MarketDataStoreType.Volatile, MatchListType.Preallocate));
    }
  }
  public static class Benchmark_OrderListCustomLinked_MarketDataStoreVolatile_MatchListAllocateEveryTime extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.CustomLinked, MarketDataStoreType.Volatile, MatchListType.AllocateEveryTime));
    }
  }
  public static class Benchmark_OrderListCustomLinked_MarketDataStoreAtomic_MatchListPreallocate extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.CustomLinked, MarketDataStoreType.Atomic, MatchListType.Preallocate));
    }
  }
  public static class Benchmark_OrderListCustomLinked_MarketDataStoreAtomic_MatchListAllocateEveryTime extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.CustomLinked, MarketDataStoreType.Atomic, MatchListType.AllocateEveryTime));
    }
  }*/
  public static class Benchmark_OrderListCustomLinked_MarketDataStoreReadWriteLock_MatchListPreallocate extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.CustomLinked, MarketDataStoreType.ReadWriteLock, MatchListType.Preallocate));
    }
  }
  /*
  public static class Benchmark_OrderListCustomLinked_MarketDataStoreReadWriteLock_MatchListAllocateEveryTime extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.CustomLinked, MarketDataStoreType.ReadWriteLock, MatchListType.AllocateEveryTime));
    }
  }
  public static class Benchmark_OrderListCustomLinked_MarketDataStoreReentrantLock_MatchListPreallocate extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.CustomLinked, MarketDataStoreType.ReentrantLock, MatchListType.Preallocate));
    }
  }
  public static class Benchmark_OrderListCustomLinked_MarketDataStoreReentrantLock_MatchListAllocateEveryTime extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.CustomLinked, MarketDataStoreType.ReentrantLock, MatchListType.AllocateEveryTime));
    }
  }
  public static class Benchmark_OrderListLinked_MarketDataStoreVolatile_MatchListPreallocate extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.Linked, MarketDataStoreType.Volatile, MatchListType.Preallocate));
    }
  }
  public static class Benchmark_OrderListLinked_MarketDataStoreVolatile_MatchListAllocateEveryTime extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.Linked, MarketDataStoreType.Volatile, MatchListType.AllocateEveryTime));
    }
  }
  public static class Benchmark_OrderListLinked_MarketDataStoreAtomic_MatchListPreallocate extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.Linked, MarketDataStoreType.Atomic, MatchListType.Preallocate));
    }
  }
  public static class Benchmark_OrderListLinked_MarketDataStoreAtomic_MatchListAllocateEveryTime extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.Linked, MarketDataStoreType.Atomic, MatchListType.AllocateEveryTime));
    }
  }
  public static class Benchmark_OrderListLinked_MarketDataStoreReadWriteLock_MatchListPreallocate extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.Linked, MarketDataStoreType.ReadWriteLock, MatchListType.Preallocate));
    }
  }
  public static class Benchmark_OrderListLinked_MarketDataStoreReadWriteLock_MatchListAllocateEveryTime extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.Linked, MarketDataStoreType.ReadWriteLock, MatchListType.AllocateEveryTime));
    }
  }
  public static class Benchmark_OrderListLinked_MarketDataStoreReentrantLock_MatchListPreallocate extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.Linked, MarketDataStoreType.ReentrantLock, MatchListType.Preallocate));
    }
  }
  public static class Benchmark_OrderListLinked_MarketDataStoreReentrantLock_MatchListAllocateEveryTime extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.Linked, MarketDataStoreType.ReentrantLock, MatchListType.AllocateEveryTime));
    }
  }
  public static class Benchmark_OrderListArray100_MarketDataStoreVolatile_MatchListPreallocate extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.Array100, MarketDataStoreType.Volatile, MatchListType.Preallocate));
    }
  }
  public static class Benchmark_OrderListArray100_MarketDataStoreVolatile_MatchListAllocateEveryTime extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.Array100, MarketDataStoreType.Volatile, MatchListType.AllocateEveryTime));
    }
  }
  public static class Benchmark_OrderListArray100_MarketDataStoreAtomic_MatchListPreallocate extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.Array100, MarketDataStoreType.Atomic, MatchListType.Preallocate));
    }
  }
  public static class Benchmark_OrderListArray100_MarketDataStoreAtomic_MatchListAllocateEveryTime extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.Array100, MarketDataStoreType.Atomic, MatchListType.AllocateEveryTime));
    }
  }
  public static class Benchmark_OrderListArray100_MarketDataStoreReadWriteLock_MatchListPreallocate extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.Array100, MarketDataStoreType.ReadWriteLock, MatchListType.Preallocate));
    }
  }
  public static class Benchmark_OrderListArray100_MarketDataStoreReadWriteLock_MatchListAllocateEveryTime extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.Array100, MarketDataStoreType.ReadWriteLock, MatchListType.AllocateEveryTime));
    }
  }
  public static class Benchmark_OrderListArray100_MarketDataStoreReentrantLock_MatchListPreallocate extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.Array100, MarketDataStoreType.ReentrantLock, MatchListType.Preallocate));
    }
  }
  public static class Benchmark_OrderListArray100_MarketDataStoreReentrantLock_MatchListAllocateEveryTime extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.Array100, MarketDataStoreType.ReentrantLock, MatchListType.AllocateEveryTime));
    }
  }
  public static class Benchmark_OrderListArray1000_MarketDataStoreVolatile_MatchListPreallocate extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.Array1000, MarketDataStoreType.Volatile, MatchListType.Preallocate));
    }
  }
  public static class Benchmark_OrderListArray1000_MarketDataStoreVolatile_MatchListAllocateEveryTime extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.Array1000, MarketDataStoreType.Volatile, MatchListType.AllocateEveryTime));
    }
  }
  public static class Benchmark_OrderListArray1000_MarketDataStoreAtomic_MatchListPreallocate extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.Array1000, MarketDataStoreType.Atomic, MatchListType.Preallocate));
    }
  }
  public static class Benchmark_OrderListArray1000_MarketDataStoreAtomic_MatchListAllocateEveryTime extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.Array1000, MarketDataStoreType.Atomic, MatchListType.AllocateEveryTime));
    }
  }
  public static class Benchmark_OrderListArray1000_MarketDataStoreReadWriteLock_MatchListPreallocate extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.Array1000, MarketDataStoreType.ReadWriteLock, MatchListType.Preallocate));
    }
  }
  public static class Benchmark_OrderListArray1000_MarketDataStoreReadWriteLock_MatchListAllocateEveryTime extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.Array1000, MarketDataStoreType.ReadWriteLock, MatchListType.AllocateEveryTime));
    }
  }
  public static class Benchmark_OrderListArray1000_MarketDataStoreReentrantLock_MatchListPreallocate extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.Array1000, MarketDataStoreType.ReentrantLock, MatchListType.Preallocate));
    }
  }
  public static class Benchmark_OrderListArray1000_MarketDataStoreReentrantLock_MatchListAllocateEveryTime extends AbstractBenchmark {
    @Setup(Level.Iteration)
    public void setup() {
        initialize(new OrderMatchingEngine(false, OrderListType.Array1000, MarketDataStoreType.ReentrantLock, MatchListType.AllocateEveryTime));
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
                  .jvmArgs("-server", "-Xms1024m", "-Xmx2048m")
                  //.addProfiler(StackProfiler.class)
                  //.addProfiler(DTraceAsmProfiler.class)
                  .addProfiler(GCProfiler.class)
                  .build();

          new Runner(opt).run();
          
          ParseJMHResult.convertTxtToCSV(fileName);
      } catch( Exception ex) {
          System.out.printf("error: %s", ex.getMessage());
      }
  }
}
