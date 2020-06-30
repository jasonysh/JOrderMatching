package jfixgateway.benchmark;


import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import jfixgateway.AbstractFIXServer;
import jfixgateway.FIXConst;
import jfixgateway.FIXGroup;
import jfixgateway.FIXParser;
import jfixgateway.FIXServerConfiguration;
import jfixgateway.FIXSession;
import jordermatching.util.TestUtility;
import jfixgateway.FIXConst.FIXMessageType;
import jfixgateway.FIXConst.FIXOrderStatus;
import jfixgateway.FIXConst.FIXVersion;
import jfixgateway.message.FIXMessage;

public class BenchmarkFIXGateway {
  
  @State(Scope.Thread)
  public static class MyState {
    private final StringBuilder tag = new StringBuilder();
    private final StringBuilder value = new StringBuilder();
    
    public FIXSession session;
    public AbstractFIXServer server;
    
    public final byte[] logonBytes = "8=FIX.4.49=7035=A34=149=CLIENT152=20181229-13:59:42.63056=TS98=0108=30141=Y10=203".getBytes();
    public final byte[] heartBeatBytes = "8=FIX.4.49=5235=034=249=CLIENT152=20181230-15:06:25.68056=TS10=109".getBytes();
    public final byte[] testRequestBytes = "8=FIX.4.49=5235=134=249=CLIENT152=20181230-15:06:25.68056=TS112=TEST110=109".getBytes();
    public final byte[] newOrderSingleBytes = "8=FIX.4.49=12735=D34=249=CLIENT156=TS11=154445140411021=138=540=154=155=AAPL59=060=20181210-22:16:44.18910=190".getBytes();
    
    public FIXMessage msg;
    public String s;
    
    @Setup(Level.Invocation)
    public void setup() throws IOException {
      FIXServerConfiguration config = new FIXServerConfiguration();
      session = new FIXSession(0, false, "TS", 128);
      server = new AbstractFIXServer(config, false) {
        @Override public void onFixLogon(FIXSession session) {}
        @Override public void onFixLogoff(FIXSession session) {}
        @Override public void onFixMessage(FIXMessage incomingMessage, FIXSession socket) {}
        @Override public void onFixError(String fixErrorMessage, FIXSession session) {}
      };
      
      FIXMessage logon = FIXParser.parseFromString("8=FIX.4.49=7035=A34=149=CLIENT152=20181229-13:59:42.63056=TS98=0108=30141=Y10=203", tag, value);
      server.processMessage(logon, session);
    }
  }

  @Benchmark
  public void testParseLogon(MyState state) {
    byte[] b = state.logonBytes;
    state.msg = FIXParser.parseFromBuffer(b, b.length, state.tag, state.value);
  }

  @Benchmark
  public void testParseHeartBeat(MyState state) {
    byte[] b = state.heartBeatBytes;
    state.msg = FIXParser.parseFromBuffer(b, b.length, state.tag, state.value);
  }

  @Benchmark
  public void testParseTestRequest(MyState state) {
    byte[] b = state.testRequestBytes;
    state.msg = FIXParser.parseFromBuffer(b, b.length, state.tag, state.value);
  }
  
  @Benchmark
  public void testParseNewOrderSingle(MyState state) {
    byte[] b = state.newOrderSingleBytes;
    state.msg = FIXParser.parseFromBuffer(b, b.length, state.tag, state.value);
  }
  

  @Benchmark
  public void testGenerateExecutionReport(MyState state) {

    final FIXMessage message = state.session.getExecutionReportMessage();
    message.setTag(FIXConst.TAG_ORDER_ID, "O123456789");
    message.setTag(FIXConst.TAG_EXEC_ID, "1111");
    message.setTag(FIXConst.TAG_EXEC_TYPE, 'F');
    message.setTag(FIXConst.TAG_ORDER_STATUS, FIXOrderStatus.PARTIALLY_FILLED.value);
    message.setTag(FIXConst.TAG_ORDER_SIDE, FIXConst.ORDER_SIDE_BUY);
    message.setTag(FIXConst.TAG_LEAVES_QTY, 100L);
    message.setTag(FIXConst.TAG_CUMULATIVE_QUANTITY, 1900L);
    message.setTag(FIXConst.TAG_AVERAGE_PRICE, 143.40);

    message.setTag(FIXConst.TAG_CLIENT_ORDER_ID, "C056");
    message.setTag(FIXConst.TAG_ORDER_QUANTITY, 2000L);
    message.setTag(FIXConst.TAG_SYMBOL, "AAPL");
    message.setTag(FIXConst.TAG_LAST_QUANTITY, 1000L);
    message.setTag(FIXConst.TAG_LAST_PRICE, 143.42);
    state.s = message.toString();
  }

  @Benchmark
  public void testGenerateMarketDataSnapshotFullRefresh(MyState state) {

    FIXMessage msg = state.session.getMarketDataSnapshotFullRefreshMessage();
    msg.setTag(FIXConst.TAG_MD_REQ_ID, "MDR1");
    msg.setTag(FIXConst.TAG_SYMBOL, "AAPL");
    
    FIXGroup groupNoMDEntries = new FIXGroup();
    groupNoMDEntries.setTag(FIXConst.TAG_NO_MD_ENTRIES, 2);
    
    FIXGroup groupBid = new FIXGroup();
    groupBid.setTag(FIXConst.TAG_MD_ENTRY_TYPE, FIXConst.MD_ENTRY_TYPE_BID);
    groupBid.setTag(FIXConst.TAG_MD_ENTRY_PX, 156.23);
    groupBid.setTag(FIXConst.TAG_MD_ENTRY_SIZE, 1000);
    groupNoMDEntries.AddGroup(groupBid);
    
    FIXGroup groupAsk = new FIXGroup();
    groupAsk.setTag(FIXConst.TAG_MD_ENTRY_TYPE, FIXConst.MD_ENTRY_TYPE_ASK);
    groupAsk.setTag(FIXConst.TAG_MD_ENTRY_PX, 156.25);
    groupAsk.setTag(FIXConst.TAG_MD_ENTRY_SIZE, 2000);
    groupNoMDEntries.AddGroup(groupAsk);
    
    msg.addGroup(FIXConst.TAG_NO_MD_ENTRIES, groupNoMDEntries);
    
    state.s = msg.toString();
  }
  
  public static void main(String[] args)
  {
      try {
        final String className = new Object() {}.getClass().getEnclosingClass().getSimpleName();
        final String fileName = TestUtility.getBenchmarkFileName(className);
        
        final Options opt = new OptionsBuilder()
                .include(".*" + className + ".*")
                .forks(1)
                .mode(Mode.AverageTime)
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
