package jfixgateway.message;

import java.io.IOException;

import jfixgateway.FIXConst;
import jfixgateway.FIXConst.FIXMessageType;
import jfixgateway.FIXConst.FIXVersion;
import jfixgateway.FIXSession;

public class FIXExecutionReportMessage implements IFIXMessage {
  private FIXVersion fixVersion = FIXVersion.FIX_VERSION_NONE;
  private int seqNum = 0;
  private final FIXMessageType msgType = FIXConst.FIXMessageType.EXECUTION_REPORT;
  private String senderCompId = "";
  private String targetCompId = "";
  private String sendingTime = "";

  public FIXVersion getFixVersion() { return fixVersion; }
  public FIXMessageType getMessageType() { return msgType; }
  public int getSequenceNumber() { return seqNum; }
  public final String getSenderCompId() { return senderCompId; }
  public final String getTargetCompId() { return targetCompId; }
  public final String getSendingTime() { return sendingTime; }
  
  public void setFixVersion(FIXVersion version) { fixVersion = version; }
  public void setSequenceNumber(int sequenceNumber) { seqNum = sequenceNumber; }
  public void setSenderCompId(String senderCompId) { this.senderCompId = senderCompId; }
  public void setTargetCompId(String targetCompId) { this.targetCompId = targetCompId; }
  public void setSendingTime(String sendingTime) { this.sendingTime = sendingTime; }

  private String orderId = "";
  public String execId = "";
  public String clOrdId = "";
  public String symbol = "";
  
  public String avgPx = "";
  public String lastPx = "";
  
  public char execType = Character.MIN_VALUE;
  public char orderStatus = Character.MIN_VALUE;
  public char orderSide = Character.MIN_VALUE;
  
  public long leavesQty = Long.MIN_VALUE;
  public long cumQty = Long.MIN_VALUE;
  public long orderQty = Long.MIN_VALUE;
  public long lastQty = Long.MIN_VALUE;
  
  private FIXSession session;
  
  public FIXExecutionReportMessage(FIXSession session) {
    this.session = session;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }
  public void setExecId(String execId) {
    this.execId = execId;
  }
  public void setExecType(char execType) {
    this.execType = execType;
  }
  public void setOrderStatus(char orderStatus) {
    this.orderStatus = orderStatus;
  }
  public void setOrderSide(char orderSide) {
    this.orderSide = orderSide;
  }
  public void setLeavesQty(long leavesQty) {
    this.leavesQty = leavesQty;
  }
  public void setCumQty(long cumQty) {
    this.cumQty = cumQty;
  }
  public void setAvgPx(double avgPx) {
    this.avgPx = String.valueOf(avgPx);
  }
  public void setClOrdId(String clOrdId) {
    this.clOrdId = clOrdId;
  }
  public void setOrderQty(long orderQty) {
    this.orderQty = orderQty;
  }
  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }
  public void setLastQty(long lastQty) {
    this.lastQty = lastQty;
  }
  public void setLastPx(double lastPx) {
    this.lastPx = String.valueOf(lastPx);
  }
  
  private int calculateHeaderLength() {
    int headerLength = 20;
    // 20 = 4 * 5
    // 4 = delimiter + equals sign + tag length for all header tags
    // 5 = 35 34 49 52 56

    headerLength += msgType.value.length();// FIX Message Type 35
    headerLength += String.valueOf(seqNum).length();// FIX Sequence Number 34
    headerLength += senderCompId.length();// FIX Sender CompID 49
    headerLength += sendingTime.length();// FIX Sending Time 52
    headerLength += targetCompId.length();// FIX Target CompID 56
    return headerLength;
  }
  
  static int stringSize(long x) {
    return (int)(Math.log10(x)+1);
}
  
  private int calculateBodyLength() {
    int bodyLength = calculateHeaderLength();
    final int delimiterAndEqualsLength = 2; //length of = and delimiter

    if (orderId.length() > 0)
      bodyLength += 2 + delimiterAndEqualsLength + orderId.length();
    if (execId.length() > 0)
      bodyLength += 2 + delimiterAndEqualsLength + execId.length();
    if (symbol.length() > 0)
      bodyLength += 2 + delimiterAndEqualsLength + symbol.length();
    if (clOrdId.length() > 0)
      bodyLength += 2 + delimiterAndEqualsLength + clOrdId.length();
    
    if (avgPx.length() > 0)
      bodyLength += 1 + delimiterAndEqualsLength + avgPx.length();
    if (lastPx.length() > 0)
      bodyLength += 2 + delimiterAndEqualsLength + lastPx.length();
    
    if (execType != Character.MIN_VALUE)
      bodyLength += 3 + delimiterAndEqualsLength + 1;
    if (orderStatus != Character.MIN_VALUE)
      bodyLength += 2 + delimiterAndEqualsLength + 1;
    if (orderSide != Character.MIN_VALUE)
      bodyLength += 2 + delimiterAndEqualsLength + 1;
    
    if (leavesQty != Long.MIN_VALUE)
      bodyLength += 3 + delimiterAndEqualsLength + stringSize(leavesQty);
    if (cumQty != Long.MIN_VALUE)
      bodyLength += 2 + delimiterAndEqualsLength + stringSize(cumQty);
    if (orderQty != Long.MIN_VALUE)
      bodyLength += 2 + delimiterAndEqualsLength + stringSize(orderQty);
    if (lastQty != Long.MIN_VALUE)
      bodyLength += 2 + delimiterAndEqualsLength + stringSize(lastQty);
    
    return bodyLength;
  }
  
  public void appendTagValue(StringBuilder sb, String tag, long value) {
    sb.append(tag).append(FIXConst.FIX_EQUALS).append(value).append(FIXConst.FIX_DELIMITER);
  }
  
  public void appendTagValue(StringBuilder sb, String tag, double value) {
    sb.append(tag).append(FIXConst.FIX_EQUALS).append(value).append(FIXConst.FIX_DELIMITER);
  }
  
  public void appendTagValue(StringBuilder sb, String tag, char value) {
    sb.append(tag).append(FIXConst.FIX_EQUALS).append(value).append(FIXConst.FIX_DELIMITER);
  }
  
  public void appendTagValue(StringBuilder sb, String tag, String value) {
    sb.append(tag).append(FIXConst.FIX_EQUALS).append(value).append(FIXConst.FIX_DELIMITER);
  }
  
  public String toString(StringBuilder sb) {
    sb.setLength(0);
    // HEADER TAGS
    appendTagValue(sb, FIXConst.TAG_VERSION, fixVersion.value);
    appendTagValue(sb, FIXConst.TAG_BODY_LENGTH, calculateBodyLength());
    appendTagValue(sb, FIXConst.TAG_MESSAGE_TYPE, msgType.value);
    appendTagValue(sb, FIXConst.TAG_SEQUENCE_NUMBER, String.valueOf(seqNum));
    appendTagValue(sb, FIXConst.TAG_SENDER_COMPID, senderCompId);
    appendTagValue(sb, FIXConst.TAG_SENDING_TIME, sendingTime);
    appendTagValue(sb, FIXConst.TAG_TARGET_COMPID, targetCompId);

    // BODY TAGS
    if (orderId.length() > 0)
      appendTagValue(sb, FIXConst.TAG_ORDER_ID, orderId);
    if (execId.length() > 0)
      appendTagValue(sb, FIXConst.TAG_EXEC_ID, execId);
    if (symbol.length() > 0)
      appendTagValue(sb, FIXConst.TAG_SYMBOL, symbol);
    if (clOrdId.length() > 0)
      appendTagValue(sb, FIXConst.TAG_CLIENT_ORDER_ID, clOrdId);

    if (avgPx.length() > 0)
      appendTagValue(sb, FIXConst.TAG_AVERAGE_PRICE, avgPx);
    if (lastPx.length() > 0)
      appendTagValue(sb, FIXConst.TAG_LAST_PRICE, lastPx);
    
    if (execType != Character.MIN_VALUE)
      appendTagValue(sb, FIXConst.TAG_EXEC_TYPE, execType);
    if (orderStatus != Character.MIN_VALUE)
      appendTagValue(sb, FIXConst.TAG_ORDER_STATUS, orderStatus);
    if (orderSide != Character.MIN_VALUE)
      appendTagValue(sb, FIXConst.TAG_ORDER_SIDE, orderSide);
    
    if (leavesQty != Long.MIN_VALUE)
      appendTagValue(sb, FIXConst.TAG_LEAVES_QTY, leavesQty);
    if (cumQty != Long.MIN_VALUE)
      appendTagValue(sb, FIXConst.TAG_CUMULATIVE_QUANTITY, cumQty);
    if (orderQty != Long.MIN_VALUE)
      appendTagValue(sb, FIXConst.TAG_ORDER_QUANTITY, orderQty);
    if (lastQty != Long.MIN_VALUE)
      appendTagValue(sb, FIXConst.TAG_LAST_QUANTITY, lastQty);

    // CHECKSUM
    appendTagValue(sb, FIXConst.TAG_BODY_CHECKSUM, calculateCheckSum(sb));
    return sb.toString();
  }

  private String calculateCheckSum(final StringBuilder sb) {
    int sum = 0;
    final int len = sb.length();
    for (int i = 0; i < len; i++) {
      sum += (int) sb.charAt(i);
    }
    return String.format("%03d", sum % 256);
  }
  
  @Override
  public void send() throws IOException {
    session.send(this);
  }
}
