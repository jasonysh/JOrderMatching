package application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jfixgateway.FIXServerConfiguration;
import jfixgateway.AbstractFIXServer;
import jfixgateway.FIXSession;
import jitemhandler.IElementQueue;
import jfixgateway.FIXConst;
import jfixgateway.FIXConst.FIXOrderStatus;
import jfixgateway.message.FIXExecutionReportMessage;
import jfixgateway.message.FIXMessage;
import jfixgateway.message.IFIXMessage;
import jordermatching.core.Order;
import jordermatching.core.OrderMatchingEngine;
import jordermatching.core.Order.OrdType;
import jordermatching.core.Order.Side;

public class FIXTradeApplication extends AbstractFIXServer {

  public static class ExecIDGenerator {
      private long counter = 0L;
      private static ExecIDGenerator instance; 
      private ExecIDGenerator() {}
      public static ExecIDGenerator instance() {
          if (instance==null) {
              instance = new ExecIDGenerator();
          }
          return instance;
      }
      
      public String genExecutionID() {
        if (counter == Long.MAX_VALUE) {
          counter = 0L;
        }
        return Long.toString(counter++);
      }
  }
  
  private IElementQueue<FIXMessage> queue;
  private OrderMatchingEngine engine;
  private ExecIDGenerator generator = ExecIDGenerator.instance();

  public FIXTradeApplication(OrderMatchingEngine engine, FIXServerConfiguration configuration, IElementQueue<FIXMessage> queue, boolean verbose) {
    super(configuration, verbose);
    this.engine = engine;
    this.queue = queue;
  }
  
  @Override
  public void start() throws IOException {
    super.start();
    new Thread(new MessageConsumerPollingTask()).start();
  }
  
  public class MessageConsumerPollingTask implements Runnable {
    FIXTradeHandler handler = new FIXTradeHandler();
    
    @Override
    public void run() {
      while (true) {
        try {
          FIXMessage msg = queue.consume();
          if (msg == null) {
            continue;
          }
          if (verbose) {
            System.out.println("handle: "+msg);
          }
          switch (msg.getMessageType()) {
            case NEW_ORDER:
              Order order = new Order();
              FIXMessageConverter.convertNewOrder(msg, order);
              FIXSession session = getSession(msg.getSenderCompId());
              if (order.getOpenQty() != 0) {
                IFIXMessage _msg = createExecutionReport(session, order, generator.genExecutionID(), FIXOrderStatus.NEW);
                _msg.send();
                List<IFIXMessage> list = handler.handleNewOrderSingle(session, order);
                for (IFIXMessage tmpMsg: list) {
                  tmpMsg.send();
                }
              } else {
                IFIXMessage _msg = createExecutionReport(session, order, generator.genExecutionID(), FIXOrderStatus.REJECTED);
                _msg.send();
              }
              break;
            case ORDER_CANCEL:
              IFIXMessage _msg = handler.handleOrderCancelRequest(getSession(msg.getSenderCompId()), msg);
              _msg.send();
              break;
            case ORDER_CANCEL_REPLACE:
              IFIXMessage _msg2 = handler.handleOrderCancelReplaceRequest(getSession(msg.getSenderCompId()), msg);
              _msg2.send();
              break;
            default:
              break;
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }
  
  @Override
  public void onFixLogon(FIXSession session) {
    if (verbose) {
      System.out.println("onFixLogon " + session);
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
  private void addToMessageHandlingQueue(FIXMessage incomingMsg, FIXSession session) {
    if (verbose) {
      System.out.println("addToMessageHandlingQueue: "+incomingMsg);
    }
    queue.produce(incomingMsg);
  }

  @Override
  public void onFixMessage(FIXMessage incomingMsg, FIXSession session) {
    if (verbose) {
      System.out.println(this.getClass().getSimpleName()+"::onFixMessage: "+incomingMsg);
    }
    switch (incomingMsg.getMessageType()) {
      case NEW_ORDER:
        addToMessageHandlingQueue(incomingMsg, session);
        break;
      case ORDER_CANCEL:
        createExecutionReport(session, getOrderFromCancelReplaceRequest(incomingMsg), generator.genExecutionID(), FIXOrderStatus.PENDING_CANCEL);
        addToMessageHandlingQueue(incomingMsg, session);
        break;
      case ORDER_CANCEL_REPLACE:
        createExecutionReport(session, getOrderFromCancelReplaceRequest(incomingMsg), generator.genExecutionID(), FIXOrderStatus.PENDING_REPLACE);
        addToMessageHandlingQueue(incomingMsg, session);
        break;
      default:
        break;
    }
  }

  @Override
  public void onFixError(String fixErrorMessage, FIXSession session) {
    if (verbose) {
      System.out.println(this.getClass().getSimpleName()+"::onFixError " + fixErrorMessage + " " + session);
    }
  }
  
  public class FIXTradeHandler {

    private List<Order> matchedList = new ArrayList<Order>();
    private List<IFIXMessage> replyList = new ArrayList<IFIXMessage>(20);
    
    public List<IFIXMessage> handleNewOrderSingle(FIXSession session, Order order) {
      try {
          matchedList.clear();
          replyList.clear();
          matchedList = engine.placeOrder(order, matchedList);

          // change below to other class
          long leavesQty = order.getQty();
          long cumQty = 0L;
          double avgExecPx = 0d;
          if (verbose) {
            System.out.println("matches.size(): "+matchedList.size());
          }
          for (Order _order: matchedList) {
            final String execID = generator.genExecutionID();
            final long lastShares = _order.getLastExecutedQty();
            final double lastPx = _order.getLastExecutedPrice();
            avgExecPx = Order.calcAvgPx(lastShares, lastPx, cumQty, avgExecPx);
            leavesQty -= lastShares;
            cumQty += lastShares;
            replyList.add(createExecutionReport(getSession(_order.getSender()), _order, _order.getOpenQty() == 0L ? FIXOrderStatus.FILLED : FIXOrderStatus.PARTIALLY_FILLED,
                execID, _order.getOpenQty(), _order.getExecutedQty(), _order.getAvgExecutedPrice(), _order.getLastExecutedQty(),
                _order.getLastExecutedPrice()));
            replyList.add(createExecutionReport(session, order, leavesQty == 0L ? FIXOrderStatus.FILLED : FIXOrderStatus.PARTIALLY_FILLED,
                execID, leavesQty, cumQty, avgExecPx, lastShares, lastPx));
          }
          if (order.getOpenQty() > 0 && order.getType() == OrdType.MARKET) {// No opposite limit orders in the book
            replyList.add(createExecutionReport(session, order, generator.genExecutionID(), FIXOrderStatus.CANCELLED));
          }
          if (verbose) {
            System.out.println(engine.toString());
          }
      } catch (Exception e) {
        e.printStackTrace();
        // rejectOrder(targetCompId, senderCompId, clOrdId, symbol, side,
        // e.getMessage());
      }
      return replyList;
    }
    
    public IFIXMessage handleOrderCancelReplaceRequest(FIXSession session, FIXMessage message) {
      final String sender = message.getSenderCompId();
      final String clOrdID = message.getValue(FIXConst.TAG_CLIENT_ORDER_ID);
      final String origClOrdID = message.getValue(FIXConst.TAG_ORIG_CLIENT_ORDER_ID);
      final String internalOrderID = Order.getOrderId(sender, origClOrdID);
      final String newInternalOrderID = Order.getOrderId(sender, clOrdID);
      final String symbol = message.getValue(FIXConst.TAG_SYMBOL);
      final Side side = message.getValueAsChar(FIXConst.TAG_ORDER_SIDE) == FIXConst.ORDER_SIDE_BUY ? Side.BUY : Side.SELL;
      
      String error = engine.modifyOrder(symbol, side, internalOrderID, newInternalOrderID,
          message.getValueAsLong(FIXConst.TAG_ORDER_QUANTITY),
          message.getTagValueAsDouble(FIXConst.TAG_ORDER_PRICE));
      if (error == null) {
        Order order = engine.getOrder(symbol, side, newInternalOrderID);
        return createExecutionReport(session, order, generator.genExecutionID(), FIXOrderStatus.REPLACED);
      } else {
        return createOrderCancelRejectMessage(session, clOrdID, origClOrdID, error);
      }
    }
    
    public IFIXMessage handleOrderCancelRequest(FIXSession session, FIXMessage msg) {
      final String symbol = msg.getValue(FIXConst.TAG_SYMBOL);
      final Side side = msg.getValueAsChar(FIXConst.TAG_ORDER_SIDE) == FIXConst.ORDER_SIDE_BUY ? Side.BUY : Side.SELL;

      final String origClOrdID = msg.getValue(FIXConst.TAG_ORIG_CLIENT_ORDER_ID);
      final Order order = engine.cancelOrder(symbol, side, Order.getOrderId(msg.getSenderCompId(), origClOrdID));
      if (order != null) {
        return createExecutionReport(session, order, generator.genExecutionID(), FIXOrderStatus.CANCELLED);
      }
      final String clOrdID = msg.getValue(FIXConst.TAG_CLIENT_ORDER_ID);
      return createOrderCancelRejectMessage(session, clOrdID, origClOrdID, "Order not found");
    }
  }
  
  

  public static IFIXMessage createExecutionReport(FIXSession session, Order order, final String execID, FIXOrderStatus ordStatus) {
    return createExecutionReport(session, order, ordStatus, execID, order.getOpenQty(),
        order.getExecutedQty(), order.getAvgExecutedPrice(), order.getLastExecutedQty(), order.getLastExecutedPrice());
  }

  public static FIXExecutionReportMessage createExecutionReport(final FIXSession session, final Order order, final FIXOrderStatus ordStatus,
      final String execID, final long leavesQty, final long cumQty, final double avgPx, final long lastShares,
      final double lastPx) {
    
    char execType = ordStatus == FIXOrderStatus.FILLED || ordStatus == FIXOrderStatus.PARTIALLY_FILLED?
        'F'/*Trade*/: ordStatus.value;
    FIXExecutionReportMessage message = new FIXExecutionReportMessage(session);
    message.setFixVersion(FIXConst.FIXVersion.FIX_4_4);
    message.setSenderCompId(order.getTarget());
    message.setTargetCompId(order.getSender());
    message.setOrderId(order.getOrderId());
    message.setExecId(execID);
    message.setExecType(execType);
    message.setOrderStatus(ordStatus.value);
    message.setOrderSide(order.getSide() == Side.BUY? FIXConst.ORDER_SIDE_BUY: FIXConst.ORDER_SIDE_SELL);
    message.setLeavesQty(leavesQty);
    message.setCumQty(cumQty);
    message.setAvgPx(avgPx);

    message.setClOrdId(order.getClientOrderId());
    message.setOrderQty(order.getQty());
    message.setSymbol(order.getSymbol());
    if (execType == 'F'/*Trade*/) {
      message.setLastQty(lastShares);
      message.setLastPx(lastPx);
    }
    return message;
/*
    try {
      session.send(message);
    } catch (IOException e) {
      e.printStackTrace();
    }*/
  }
  
  private FIXMessage createOrderCancelRejectMessage(FIXSession session, String clOrdID, String origClOrdID, String text) {

    final FIXMessage ret = session.getOrderCancelRejectMessage();
    ret.setTag(FIXConst.TAG_ORDER_ID, "NONE");
    ret.setTag(FIXConst.TAG_CLIENT_ORDER_ID, clOrdID);
    ret.setTag(FIXConst.TAG_ORIG_CLIENT_ORDER_ID, origClOrdID);
    ret.setTag(FIXConst.TAG_ORDER_STATUS, FIXOrderStatus.REJECTED.value);
    ret.setTag(FIXConst.TAG_CXL_REJ_RESPONSE_TO, FIXConst.CXL_REJ_RESPONSE_TO_ORDER_CANCEL_REQUEST);
    if (text != null && text.length() > 0) {
      ret.setTag(FIXConst.TAG_FREE_TEXT, text);
    }
    return ret;
  }
  
  private Order getOrderFromCancelReplaceRequest(FIXMessage message) {
    final String symbol = message.getValue(FIXConst.TAG_SYMBOL);
    final Side side = message.getValueAsChar(FIXConst.TAG_ORDER_SIDE) == FIXConst.ORDER_SIDE_BUY? Side.BUY: Side.SELL;
    final String sender = message.getSenderCompId();
    
    final String origClOrdID = message.getValue(FIXConst.TAG_ORIG_CLIENT_ORDER_ID);
    if (verbose) {
      System.out.println(engine.toString());
    }
    Order order = engine.getOrder(symbol, side, Order.getOrderId(sender, origClOrdID));
    return order;
  }
}
