package jfixgateway.benchmark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import application.FIXMessageConverter;
import jfixgateway.FIXConst;
import jfixgateway.FIXParser;
import jfixgateway.util.TestFileReadUtility;
import jfixgateway.FIXConst.FIXMessageType;
import jfixgateway.FIXConst.FIXOrderStatus;
import jfixgateway.message.FIXExecutionReportMessage;
import jfixgateway.message.FIXMessage;
import jordermatching.core.Order;
import jordermatching.core.Order.OrdType;
import jordermatching.util.TestUtility;

public class BenchmarkQuickFIXJAll {
  private static final StringBuilder tag = new StringBuilder();
  private static final StringBuilder value = new StringBuilder();
  private static quickfix.DefaultMessageFactory factory = new quickfix.DefaultMessageFactory();
  
  protected static int index;
  public static ArrayList<String> testOrders = new ArrayList<String>(4000000);
  static {
    TestFileReadUtility.getMessagesFromFile("testcase"+System.getProperty("file.separator")+"fix_4000000_1549673202901.txt", testOrders);
  }
  
  @State(Scope.Thread)
  public static class MyState {
    public StringBuilder sb = new StringBuilder();
    public String s;
    public byte[] b;
    public Order currentOrder;
    
    @Setup(Level.Invocation)
    public void setup() throws IOException {
      if (index >= testOrders.size()) {
        index = 0;
      }
      String s = testOrders.get(index++);
      b = s.getBytes();
    }
  }

  @Benchmark
  public void testJFIXGatewayParseNewOrderSingle(MyState state) {
    FIXMessage message = FIXParser.parseFromBuffer(state.b, state.b.length, tag, value);
    Order order = new Order();
    FIXMessageConverter.convertNewOrder(message, order);
    state.currentOrder = order;
  }

  public static void convertNewOrder(String s, Order order) throws quickfix.FieldNotFound, quickfix.InvalidMessage {
    final quickfix.Message message = quickfix.MessageUtils.parse(factory, null, s);
    final String sender = "";//message.getString(SenderCompID.FIELD);
    final String target = "";//message.getString(TargetCompID.FIELD);
    final String symbol = message.getString(quickfix.field.Symbol.FIELD);
    final String clientOrderId = message.getString(quickfix.field.ClOrdID.FIELD);
    final char fixOrderType = message.getChar(quickfix.field.OrdType.FIELD);
    final OrdType type = fixOrderType == quickfix.field.OrdType.LIMIT ? OrdType.LIMIT : OrdType.MARKET;
    final jordermatching.core.Order.Side side = message.getChar(quickfix.field.Side.FIELD) == quickfix.field.Side.BUY ? jordermatching.core.Order.Side.BUY : jordermatching.core.Order.Side.SELL;
    final long quantity = (long)message.getDouble(quickfix.field.OrderQty.FIELD);
    final double price = message.isSetField(quickfix.field.Price.FIELD)
            ? message.getDouble(quickfix.field.Price.FIELD)
            : 0d;

    order.set(clientOrderId, symbol, sender, target, side, type, price, quantity);
}
  
  @Benchmark
  public void testQuickFIXJParseNewOrderSingle(MyState state) throws quickfix.FieldNotFound, quickfix.InvalidMessage {
    Order order = new Order();
    convertNewOrder(new String(state.b), order);
    state.currentOrder = order;
  }

  @Benchmark
  public void testJFIXGatewayGenerateExecutionReport(MyState state) {
    FIXExecutionReportMessage message = new FIXExecutionReportMessage(null);
    message.setFixVersion(FIXConst.FIXVersion.FIX_4_4);
    message.setSenderCompId("TS");
    message.setTargetCompId("CLIENT1");
    message.setOrderId("O123456789");
    message.setExecId("1111");
    message.setExecType('F');
    message.setOrderStatus(FIXOrderStatus.PARTIALLY_FILLED.value);
    message.setOrderSide(FIXConst.ORDER_SIDE_BUY);
    message.setLeavesQty(100L);
    message.setCumQty(1900L);
    message.setAvgPx(143.40);

    message.setClOrdId("C056");
    message.setOrderQty(2000L);
    message.setSymbol("AAPL");
    message.setLastQty(1000L);
    message.setLastPx(143.42);
    state.s = message.toString(state.sb);
  }

  @Benchmark
  public void testQuickFIXJGenerateExecutionReport(MyState state) {
    quickfix.Message message = new quickfix.Message();
    quickfix.Message.Header header = message.getHeader();
	header.setField(new quickfix.StringField(0, "FIX.4.4"));
    header.setField(new quickfix.field.SenderCompID("TS"));
    header.setField(new quickfix.field.TargetCompID("CLIENT1"));
    header.setField(new quickfix.field.MsgType(quickfix.field.MsgType.EXECUTION_REPORT));
    message.setField(new quickfix.field.OrderID("O123456789"));
    message.setField(new quickfix.field.ClOrdID("C056"));
    message.setField(new quickfix.field.Symbol("AAPL"));
    message.setField(new quickfix.field.Side(quickfix.field.Side.BUY));
    message.setField(new quickfix.field.ExecID("1111"));
    message.setField(new quickfix.field.CumQty(1900d)); 
    message.setField(new quickfix.field.LastQty(1000d)); 
    message.setField(new quickfix.field.LeavesQty(100d));
    message.setField(new quickfix.field.AvgPx(143.40));
    message.setField(new quickfix.field.OrdStatus('1'));  
    message.setField(new quickfix.field.ExecType('F'));
    state.s = message.toString();
  }
  
  public static void main(String[] args)
  {
      try {
        final String className = new Object() {}.getClass().getEnclosingClass().getSimpleName();
        final String fileName = TestUtility.getBenchmarkFileName(className);
        
        final Options opt = new OptionsBuilder()
                .include(".*" + className + ".*")
                .forks(1)
                .mode(Mode.SampleTime)
                .timeUnit(TimeUnit.MICROSECONDS)
                .output(fileName+".txt")
                .result(fileName+".json")
                .resultFormat(ResultFormatType.JSON)
                .jvmArgs("-server", "-Xms1024m", "-Xmx2048m")
                //.addProfiler(StackProfiler.class)
                //.addProfiler(DTraceAsmProfiler.class)
                .addProfiler(GCProfiler.class)
                .build();
          new Runner(opt).run();
      } catch( Exception ex) {
          System.out.printf("error: %s", ex.getMessage());
      }
  }
}
