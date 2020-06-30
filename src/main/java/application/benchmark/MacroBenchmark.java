package application.benchmark;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
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

import application.FIXMessageConverter;
import application.FIXTradeApplication;
import application.FIXTradeApplication.ExecIDGenerator;
import jfixgateway.FIXConst;
import jfixgateway.FIXParser;
import jfixgateway.FIXConst.FIXOrderStatus;
import jfixgateway.benchmark.BenchmarkQuickFIXJAll;
import jfixgateway.message.FIXMessage;
import jordermatching.core.Order;
import jordermatching.core.OrderBook;
import jordermatching.core.OrderMatchingEngine;
import jordermatching.core.ImplementSelector.MarketDataStoreType;
import jordermatching.core.ImplementSelector.MatchListType;
import jordermatching.core.ImplementSelector.OrderListType;
import jordermatching.core.Order.OrdType;
import jordermatching.core.Order.Side;
import jordermatching.util.ParseJMHResult;
import jordermatching.util.TestFileReadUtility;
import jordermatching.util.TestUtility;
import quickfix.InvalidMessage;
/*
 * Usage: /opt/jdk-11.0.2/bin/java  -jar target/benchmarks.jar .*MacroBenchmark.* -f 1 -o benchmark/thrpt_macrobench_ZGC_i10wi10w10r120_20190325_4.txt -rf JSON -rff benchmark/thrpt_macrobench_ZGC_i10wi10w10r120_20190325_4.json -bm thrpt -tu s -i 10 -wi 10 -w 10 -r 120 -jvmArgs "-server -Xms3936m -Xmx3936m -XX:+UnlockExperimentalVMOptions -XX:+UseZGC" -prof gc
 * 
 */
public class MacroBenchmark {
  public static ArrayList<Order> baseOrders = new ArrayList<Order>(10000);
  static {
	    TestFileReadUtility.getOrdersFromFile("testcase"+System.getProperty("file.separator")+"base_10000_1553478099118.txt", baseOrders);
  }
  public static ArrayList<String> testOrders = new ArrayList<String>(5000);
  static {
    
    jfixgateway.util.TestFileReadUtility.getMessagesFromFile("testcase"+System.getProperty("file.separator")+"fix_5000_1553478409832.txt", testOrders);
    //jfixgateway.util.TestFileReadUtility.getMessagesFromFile("testcase"+System.getProperty("file.separator")+"fix_4000000_1549673202901.txt", testOrders);
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
	protected volatile OrderMatchingEngine engine;
	protected int index;
	protected int orderCnt;
    protected int executionReportCnt;
    public String newOrderSingleRequest;
    public Order currentOrder;
    public MarketDataReader[] readers;
    public Thread[] threads;
    private ExecIDGenerator generator = ExecIDGenerator.instance();
    private ArrayList<String> executionReportList = new ArrayList<String>();

    public void initialize(OrderMatchingEngine engine) {
      System.out.println("initialize");
    	this.engine = engine;
	index = 0;
	orderCnt = 0;
	executionReportCnt = 0;
    	
    	for (int i = 0; i < baseOrders.size(); i++) {
    		matches.clear();
    		engine.placeOrder(baseOrders.get(i), matches);
    	}

    	if (readers == null) {
          readers = new MarketDataReader[1];
          threads = new Thread[readers.length];
          startReaderThreads(engine, readers, threads);
    	}
        //System.out.println(engine.toString());
    }
    
    @Setup(Level.Invocation)
    public void invocationSetup() {
    	matches.clear();
    	if (index >= testOrders.size()) {
    	  index = 0;
    	}

        orderCnt++;
    	if (currentOrder != null) {
    	  OrderBook ob = engine.getOrderBook(currentOrder.getSymbol());
    	  if (currentOrder.getSide() == Order.Side.BUY && ob.getBidSize() > baseOrders.size()
    			  || currentOrder.getSide() == Order.Side.SELL && ob.getAskSize() > baseOrders.size()) {
        	  engine.cancelOrder(currentOrder.getSymbol(), currentOrder.getSide(), currentOrder.getOrderId());
    	  }
    	}
    	newOrderSingleRequest = testOrders.get(index++);
    }
    
    @TearDown(Level.Iteration)
    public void tearDown() throws InterruptedException {
        stopReaderThreads(readers, threads);
        readers = null;
        System.out.println("orderCnt: "+orderCnt+" executionReportCnt: "+executionReportCnt);
        System.out.println("");
    }
    
    @Benchmark
    @Warmup(iterations = 10, time = 10000, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 10, time = 10000, timeUnit = TimeUnit.MILLISECONDS)
    public void bench() throws Exception {
      Order order = new Order();
      convertNewOrder(newOrderSingleRequest, order);
	  engine.placeOrder(order, matches);
	  
	  executionReportList.clear();
      long leavesQty = order.getQty();
      long cumQty = 0L;
      double avgExecPx = 0d;
      for (Order _order: matches) {
        final String execID = generator.genExecutionID();
        final long lastShares = _order.getLastExecutedQty();
        final double lastPx = _order.getLastExecutedPrice();
        avgExecPx = Order.calcAvgPx(lastShares, lastPx, cumQty, avgExecPx);
        leavesQty -= lastShares;
        cumQty += lastShares;
        executionReportList.add(createExecutionReport(_order, _order.getOpenQty() == 0L ? FIXOrderStatus.FILLED : FIXOrderStatus.PARTIALLY_FILLED,
            execID, _order.getOpenQty(), _order.getExecutedQty(), _order.getAvgExecutedPrice(), _order.getLastExecutedQty(),
            _order.getLastExecutedPrice()).toString());
        executionReportList.add(createExecutionReport(order, leavesQty == 0L ? FIXOrderStatus.FILLED : FIXOrderStatus.PARTIALLY_FILLED,
            execID, leavesQty, cumQty, avgExecPx, lastShares, lastPx).toString());

        executionReportCnt = executionReportCnt + 2;
      }
      if (order.getOpenQty() > 0 && order.getType() == OrdType.MARKET) {// No opposite limit orders in the book
        executionReportList.add(createExecutionReport(order, generator.genExecutionID(), FIXOrderStatus.CANCELLED).toString());
        executionReportCnt++;
      }
      currentOrder = order;
    }
    
    public abstract void convertNewOrder(String newOrderRequest, Order order) throws Exception;
    
    public abstract String createExecutionReport(Order order, String execID, FIXOrderStatus ordStatus);

    public abstract String createExecutionReport(Order order, FIXOrderStatus ordStatus,
        String execID, long leavesQty, long cumQty, double avgPx, long lastShares, double lastPx);
  }

  public static class BenchmarkArrayDequeCompareAndSwap extends AbstractBenchmark {
    final StringBuilder sb = new StringBuilder();
    final StringBuilder tag = new StringBuilder();
    final StringBuilder value = new StringBuilder();

    @Setup(Level.Iteration)
    public void setup() {
       initialize(new OrderMatchingEngine(false, OrderListType.ArrayDeque, MarketDataStoreType.CompareAndSwap, MatchListType.Preallocate));
    }
    
    @Override
    public String createExecutionReport(Order order, String execID, FIXOrderStatus ordStatus) {
      return createExecutionReport(order, ordStatus, execID, order.getOpenQty(), order.getExecutedQty(),
          order.getAvgExecutedPrice(), order.getLastExecutedQty(), order.getLastExecutedPrice());
    }

    @Override
    public String createExecutionReport(Order order, FIXOrderStatus ordStatus, String execID,
        long leavesQty, long cumQty, double avgPx, long lastShares, double lastPx) {
      return FIXTradeApplication.createExecutionReport(null, order, ordStatus, execID,
          leavesQty, cumQty, avgPx, lastShares, lastPx).toString(sb);
    }

    @Override
    public void convertNewOrder(String newOrderRequest, Order order) {
      FIXMessage message = FIXParser.parseFromString(newOrderRequest, tag, value);
      FIXMessageConverter.convertNewOrder(message, order);
      order.setClientOrderID(order.getClientOrderId()+String.valueOf(System.nanoTime()));
    }
  }
  
  public static class BenchmarkArray100CompareAndSwap extends AbstractBenchmark {
    final StringBuilder sb = new StringBuilder();
    final StringBuilder tag = new StringBuilder();
    final StringBuilder value = new StringBuilder();

    @Setup(Level.Iteration)
    public void setup() {
       initialize(new OrderMatchingEngine(false, OrderListType.Array100, MarketDataStoreType.CompareAndSwap, MatchListType.Preallocate));
    }
    
    @Override
    public String createExecutionReport(Order order, String execID, FIXOrderStatus ordStatus) {
      return createExecutionReport(order, ordStatus, execID, order.getOpenQty(), order.getExecutedQty(),
          order.getAvgExecutedPrice(), order.getLastExecutedQty(), order.getLastExecutedPrice());
    }

    @Override
    public String createExecutionReport(Order order, FIXOrderStatus ordStatus, String execID,
        long leavesQty, long cumQty, double avgPx, long lastShares, double lastPx) {
      return FIXTradeApplication.createExecutionReport(null, order, ordStatus, execID,
          leavesQty, cumQty, avgPx, lastShares, lastPx).toString(sb);
    }

    @Override
    public void convertNewOrder(String newOrderRequest, Order order) {
      FIXMessage message = FIXParser.parseFromString(newOrderRequest, tag, value);
      FIXMessageConverter.convertNewOrder(message, order);
      order.setClientOrderID(order.getClientOrderId()+String.valueOf(System.nanoTime()));
    }
  }
  
  public static class BenchmarkLinkedReadWriteLockQuickFIXJ extends AbstractBenchmark {
    final StringBuilder tag = new StringBuilder();
    final StringBuilder value = new StringBuilder();

    @Setup(Level.Iteration)
    public void setup() {
       initialize(new OrderMatchingEngine(false, OrderListType.Linked, MarketDataStoreType.ReadWriteLock, MatchListType.Preallocate));
    }
    
    @Override
    public String createExecutionReport(Order order, String execID, FIXOrderStatus ordStatus) {
      return createExecutionReport(order, ordStatus, execID, order.getOpenQty(),
          order.getExecutedQty(), order.getAvgExecutedPrice(), order.getLastExecutedQty(), order.getLastExecutedPrice());
    }

    @Override
    public String createExecutionReport(Order order, FIXOrderStatus ordStatus, String execID,
        long leavesQty, long cumQty, double avgPx, long lastShares, double lastPx) {

      char execType = ordStatus == FIXOrderStatus.FILLED || ordStatus == FIXOrderStatus.PARTIALLY_FILLED?
          'F'/*Trade*/: ordStatus.value;

      quickfix.Message message = new quickfix.Message();
      quickfix.Message.Header header = message.getHeader();
      header.setField(new quickfix.field.MsgType(quickfix.field.MsgType.EXECUTION_REPORT));
      header.setField(new quickfix.StringField(0, "FIX.4.4"));
      header.setField(new quickfix.field.SenderCompID(order.getTarget()));
      header.setField(new quickfix.field.TargetCompID(order.getSender()));
      message.setField(new quickfix.field.OrderID(order.getOrderId()));
      message.setField(new quickfix.field.ExecID(execID));
      message.setField(new quickfix.field.ExecType(execType));
      message.setField(new quickfix.field.OrdStatus(ordStatus.value));  
      message.setField(new quickfix.field.Side(order.getSide() == Side.BUY? FIXConst.ORDER_SIDE_BUY: FIXConst.ORDER_SIDE_SELL));
      message.setField(new quickfix.field.LeavesQty(leavesQty));
      message.setField(new quickfix.field.CumQty(cumQty)); 
      message.setField(new quickfix.field.AvgPx(avgPx));
      
      message.setField(new quickfix.field.ClOrdID(order.getClientOrderId()));
      message.setField(new quickfix.field.OrderQty(order.getQty()));
      message.setField(new quickfix.field.Symbol(order.getSymbol()));
      if (execType == 'F'/*Trade*/) {
        message.setField(new quickfix.field.LastQty(lastShares)); 
        message.setField(new quickfix.field.LastPx(lastPx)); 
      }
      
      return message.toString();
    }

    @Override
    public void convertNewOrder(String newOrderRequest, Order order) throws Exception {
      BenchmarkQuickFIXJAll.convertNewOrder(newOrderRequest, order);
      order.setClientOrderID(order.getClientOrderId()+String.valueOf(System.nanoTime()));
    }
  }
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
