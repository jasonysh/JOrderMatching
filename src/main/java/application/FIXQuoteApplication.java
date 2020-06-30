package application;

import jfixgateway.FIXServerConfiguration;

import java.io.IOException;

import jfixgateway.AbstractFIXServer;
import jfixgateway.FIXConst;
import jfixgateway.FIXGroup;
import jfixgateway.FIXSession;
import jfixgateway.message.FIXMessage;
import jordermatching.core.OrderMatchingEngine;

public class FIXQuoteApplication extends AbstractFIXServer {
  private OrderMatchingEngine engine;
  private static ThreadLocal<double[]> pricesLocal = new ThreadLocal<double[]>() {
    protected double[] initialValue() {
      return new double[10];
    }
  };
  private static ThreadLocal<long[]> volumesLocal = new ThreadLocal<long[]>() {
    protected long[] initialValue() {
      return new long[10];
    }
  };

  public FIXQuoteApplication(OrderMatchingEngine engine, FIXServerConfiguration configuration, boolean verbose) {
    super(configuration, verbose);
    this.engine = engine;
  }
  
  @Override
  public void onFixLogon(FIXSession session) {
    if (verbose) {
      System.out.println(this.getClass().getSimpleName()+"::onFixLogon " + session);
    }
  }

  @Override
  public void onFixLogoff(FIXSession session) {
    if (verbose) {
      System.out.println(this.getClass().getSimpleName()+"::onFixLogoff " + session);
    }
  }
/*
  @Override
  public void onTraderLogon(FIXSession session, String username) {
    if (verbose) {
      System.out.println(this.getClass().getSimpleName()+"::onTraderLogon " + session + " " + username);
    }
  }
*/
  @Override
  public void onFixMessage(FIXMessage incomingMsg, FIXSession session) {
    if (verbose) {
      System.out.println(this.getClass().getSimpleName()+"::onFixMessage " + incomingMsg + " " + session);
    }

    try {
      switch (incomingMsg.getMessageType()) {
        case MARKET_DATA_REQUEST:
          handleMarketDataRequest(incomingMsg, session);
          break;
        default:
          break;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  private void handleMarketDataRequest(FIXMessage incomingMsg, FIXSession session) throws IOException {
    final String mdReqID = incomingMsg.getValue(FIXConst.TAG_MD_REQ_ID);
    final String symbol = incomingMsg.getValue(FIXConst.TAG_SYMBOL);
    
    FIXMessage msg = session.getMarketDataSnapshotFullRefreshMessage();
    msg.setTag(FIXConst.TAG_MD_REQ_ID, mdReqID);
    msg.setTag(FIXConst.TAG_SYMBOL, symbol);
    
    FIXGroup groupNoMDEntries = new FIXGroup();
    groupNoMDEntries.setTag(FIXConst.TAG_NO_MD_ENTRIES, 2);
    
    FIXGroup groupBid = new FIXGroup();
    groupBid.setTag(FIXConst.TAG_MD_ENTRY_TYPE, FIXConst.MD_ENTRY_TYPE_BID);
    double[] prices = pricesLocal.get();
    long[] volumes = volumesLocal.get();
    engine.readBids(symbol, prices, volumes);
    if (verbose) {
      for (double p: prices) {
        System.out.println("p: "+p);
      }
      for (double v: volumes) {
        System.out.println("v: "+v);
      }
    }
    groupBid.setTag(FIXConst.TAG_MD_ENTRY_PX, prices[0]);
    groupBid.setTag(FIXConst.TAG_MD_ENTRY_SIZE, volumes[0]);
    groupNoMDEntries.AddGroup(groupBid);
    
    FIXGroup groupAsk = new FIXGroup();
    groupAsk.setTag(FIXConst.TAG_MD_ENTRY_TYPE, FIXConst.MD_ENTRY_TYPE_ASK);
    prices = pricesLocal.get();
    volumes = volumesLocal.get();
    engine.readAsks(symbol, prices, volumes);
    if (verbose) {
      for (double p: prices) {
        System.out.println("p: "+p);
      }
      for (double v: volumes) {
        System.out.println("v: "+v);
      }
    }
    groupAsk.setTag(FIXConst.TAG_MD_ENTRY_PX, prices[0]);
    groupAsk.setTag(FIXConst.TAG_MD_ENTRY_SIZE, volumes[0]);
    groupNoMDEntries.AddGroup(groupAsk);
    
    msg.addGroup(FIXConst.TAG_NO_MD_ENTRIES, groupNoMDEntries);
    
    session.send(msg);
  }

  @Override
  public void onFixError(String fixErrorMessage, FIXSession session) {
    if (verbose) {
      System.out.println(this.getClass().getSimpleName()+"::onFixError " + fixErrorMessage + " " + session);
    }
  }
}
