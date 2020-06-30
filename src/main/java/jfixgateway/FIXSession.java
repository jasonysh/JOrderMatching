package jfixgateway;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import jfixgateway.FIXConst.FIXVersion;
import jfixgateway.message.FIXExecutionReportMessage;
import jfixgateway.message.FIXMessage;
import jfixgateway.FIXConst.FIXMessageType;

public class FIXSession {
  private final StringBuilder tag = new StringBuilder();
  private final StringBuilder value = new StringBuilder();
  
  private final byte[] buffer;
  private final ReentrantLock sessionLock = new ReentrantLock();
  private final ReentrantLock sessionSendLock = new ReentrantLock();
  private final FIXReceivableCache receiveCache;

  private FIXVersion fixVersion;
  private int outgoingSeqNum;
  private int incomingSeqNum;

  private int heartBeatInterval;
  private int encryptionMethod;

  private String compId;
  private String targetCompId;

  private long lastReceivedTime;
  private long lastSentTime;

  private Socket socket;
  private DataInputStream inputStream;
  private DataOutputStream outputStream;

  private boolean verbose;

  public void setIncomingSeqNum(int number) {
    incomingSeqNum = number;
  }

  public void setOutgoingSequenceNumber(int number) {
    outgoingSeqNum = number;
  }

  public String getCompId() {
    return compId;
  }

  public String getTargetCompId() {
    return targetCompId;
  }

  public FIXSession(int receiveCacheSize, boolean verbose, String compID, int bufferSize) {
    buffer = new byte[bufferSize];
    fixVersion = FIXVersion.FIX_VERSION_NONE;
    outgoingSeqNum = 1;
    incomingSeqNum = 1;
    heartBeatInterval = FIXConst.DEFAULT_HEARTBEAT_INTERVAL;
    encryptionMethod = FIXConst.ENCRYPTION_NONE;
    receiveCache = receiveCacheSize > 0 ? new FIXReceivableCache(receiveCacheSize) : null;

    this.verbose = verbose;
    this.compId = compID;
  }

  public void initialise(Socket socket) throws IOException {
    inputStream = new DataInputStream(socket.getInputStream());
    outputStream = new DataOutputStream(socket.getOutputStream());
    this.socket = socket;
  }

  public void close() throws IOException {
    if (socket != null) {
      socket.close();
    }
  }

  private void updateLastReceivedTime() {
    lastReceivedTime = System.currentTimeMillis();
  }

  public boolean connected() {
    return socket != null;
  }

  public void lock() {
    sessionLock.lock();
  }

  public void unlock() {
    sessionLock.unlock();
  }

  public void saveSequenceNumbersToStore() throws FileNotFoundException {
    FIXSequenceStore.updateSequenceStore(compId, targetCompId, outgoingSeqNum, incomingSeqNum);
  }

  private FIXMessage getBaseMessage(FIXMessageType messageType) {
    FIXMessage message = new FIXMessage();
    message.setFixVersion(fixVersion);
    message.setMessageType(messageType);
    message.setSenderCompId(compId);
    message.setTargetCompId(targetCompId);
    message.setSession(this);
    return message;
  }

  public FIXMessage getLogonMessage(int m_heartBeatInterval, int m_encryptionMethod) {
    FIXMessage message = getBaseMessage(FIXConst.FIXMessageType.LOGON);
    message.setTag(FIXConst.TAG_HEARBEAT_INTERVAL, m_heartBeatInterval);
    message.setTag(FIXConst.TAG_ENCRYPT_METHOD, m_encryptionMethod);
    return message;
  }

  public FIXMessage getAdminRejectionMessage() {
    FIXMessage message = getBaseMessage(FIXConst.FIXMessageType.ADMIN_REJECT);
    message.setTag(FIXConst.TAG_REF_SEQ_NUM, message.getSequenceNumber());
    return message;
  }

  public int getIncomingSequenceNumber() {
    return incomingSeqNum;
  }

  public FIXMessage getLogoffMessage() {
    return getBaseMessage(FIXMessageType.LOGOFF);
  }

  public FIXMessage getHeartBeatMessage() {
    return getBaseMessage(FIXMessageType.HEARTBEAT);
  }

  public FIXMessage getExecutionReportMessage() {
    return getBaseMessage(FIXMessageType.EXECUTION_REPORT);
  }

  public FIXMessage getUserLogonResponseMessage() {
    return getBaseMessage(FIXMessageType.USER_RESPONSE);
  }

  public FIXMessage getOrderCancelRejectMessage() {
    return getBaseMessage(FIXMessageType.ORDER_CANCEL_REJECT);
  }

  public FIXMessage getMarketDataSnapshotFullRefreshMessage() {
    return getBaseMessage(FIXMessageType.MARKET_DATA_SNAPSHOT_FULL_REFRESH);
  }

  public void handleLogonMessage(FIXMessage message) throws FileNotFoundException, IOException {
    targetCompId = message.getSenderCompId();

    final boolean resetOnLogonFlag = message.hasTag(FIXConst.TAG_RESET_SEQ_NUM_FLAG)
        ? message.getValueAsChar(FIXConst.TAG_RESET_SEQ_NUM_FLAG) == 'Y'
        : false;
    if (resetOnLogonFlag) {
      incomingSeqNum = 0;
      outgoingSeqNum = 1;
    } else {
      String[] numbers = FIXSequenceStore.loadFromSequenceStore(compId, targetCompId).split(",");
      outgoingSeqNum = Integer.parseInt(numbers[0]);
      incomingSeqNum = Integer.parseInt(numbers[1]);
    }

    heartBeatInterval = message.getValueAsInt(FIXConst.TAG_HEARBEAT_INTERVAL);
    fixVersion = message.getFixVersion();
    encryptionMethod = message.getValueAsInt(FIXConst.TAG_ENCRYPT_METHOD);

    send(getLogonMessage(heartBeatInterval, encryptionMethod));
  }

  public void handleAdminMessage(FIXMessage message) throws IOException {
    final FIXMessageType messageType = message.getMessageType();
    if (messageType == FIXConst.FIXMessageType.LOGOFF) {
      send(getLogoffMessage());
    } else if (messageType == FIXConst.FIXMessageType.HEARTBEAT) {
      send(getHeartBeatMessage());
    } else if (messageType == FIXConst.FIXMessageType.TEST_REQUEST) {
      final FIXMessage replyMsg = getHeartBeatMessage();
      replyMsg.setTag(FIXConst.TAG_TEST_REQUEST_ID, message.getValue(FIXConst.TAG_TEST_REQUEST_ID));
      send(replyMsg);
    }
  }

  public void receiveWithCaching(List<FIXMessage> messages) throws IOException {
    if (!isInputStreamAvailable()) {
      return;
    }
    final int receivedSize = readInputStream(receiveCache.getBuffer(), receiveCache.getSize(),
        receiveCache.getRemainingBufferSize());

    if (receivedSize > 0) {
      receiveCache.incrementSize(receivedSize);
      updateLastReceivedTime();
      receiveCache.parse(messages);
    }
  }
  
  public boolean isInputStreamAvailable() throws IOException {
    return inputStream.available() > 0;
  }
  
  public int readInputStream(byte b[], int off, int len) throws IOException {
    return inputStream.read(b, off, len);
  }
  
  public int readInputStream(byte b[]) throws IOException {
    return readInputStream(b, 0, b.length);
  }

  public FIXMessage receive() throws IOException {
    if (!isInputStreamAvailable()) {
      return null;
    }
    final int initialBufferSize = 20;
    final int receivedSize = readInputStream(buffer, 0, 20);// Length of 8=FIX.4.2@9=7000@35= so we always get 35=
    if (receivedSize <= 0) {
      return null;
    }
    if (AbstractFIXServer.verbose) {
      FIXUtility.print(buffer);
    }
    
    int fixBodyLength = 0;
    int receivedBodyStartIndex = 0;

    for (int i = 0; i < receivedSize; ++i) {
      // Find out body length
      if (i != 0 && buffer[i - 1] == FIXConst.FIX_DELIMITER && buffer[i] == '9'/* BODY_LENGTH */
          && buffer[i + 1] == FIXConst.FIX_EQUALS) {
        final StringBuilder sb = new StringBuilder();
        for (int j = i + 2; j < receivedSize; j++) {
          final char c = (char) buffer[j];
          if (c == FIXConst.FIX_DELIMITER) {
            fixBodyLength = Integer.parseInt(sb.toString());
            i = j;
            break;
          } else {
            sb.append(c);
          }
        }
      }

      if (buffer[i] == '3' && buffer[i + 1] == '5') {
        receivedBodyStartIndex = i;// Find out received body start index
        break;
      }
    }

    final int remainingBytesSize = fixBodyLength - (receivedSize - receivedBodyStartIndex) + 7;// 7 is length of Check
                                                                                               // Sum 10=081@
    int totalLength = receivedSize;

    FIXMessage message = null;
    if (remainingBytesSize + initialBufferSize > buffer.length) {// Not enough size, create a new byte array
      if (AbstractFIXServer.verbose) {
        System.out.println("Not enough size, create a new byte array");
      }
      final byte[] remainBytes = new byte[remainingBytesSize];
      final int receivedSize2 = readInputStream(remainBytes);
      if (receivedSize2 <= 0) {
        return null;
      }

      totalLength += receivedSize2;
      message = FIXParser.parseFromMultipleBuffers(buffer, receivedSize, remainBytes, receivedSize2);
    } else {
      final int receivedSize2 = readInputStream(buffer, receivedSize, remainingBytesSize);
      if (receivedSize2 <= 0) {
        return null;
      }
      totalLength += receivedSize2;
      message = FIXParser.parseFromBuffer(buffer, totalLength, tag, value);
    }

    updateLastReceivedTime();

    if (AbstractFIXServer.verbose) {
      System.out.println("INCOMING: " + message.toString());
    }

    return message;
  }

  public boolean validateSequenceNumber(int incomingSequenceNumber) {
    return getIncomingSequenceNumber() + 1 == incomingSequenceNumber;
  }

  public boolean validateTargetCompid(final FIXMessage message) {
    return message.getTargetCompId().equals(compId);
  }

  public static boolean verifyTag(FIXMessage message, String tag) {
    return message.hasTag(tag);
  }

  // Return 0 = true, Return > 0 = missing tag
  public static String validateRequiredTags(final FIXMessage message) {
    if (message.getFixVersion() == FIXConst.FIXVersion.FIX_VERSION_NONE)
      return FIXConst.TAG_VERSION;
    if (!verifyTag(message, FIXConst.TAG_BODY_LENGTH))
      return FIXConst.TAG_BODY_LENGTH;
    if (message.getMessageType() == FIXConst.FIXMessageType.MESSAGE_TYPE_NONE)
      return FIXConst.TAG_MESSAGE_TYPE;
    if (!verifyTag(message, FIXConst.TAG_BODY_CHECKSUM))
      return FIXConst.TAG_BODY_CHECKSUM;
    if (message.getSequenceNumber() == 0)
      return FIXConst.TAG_SEQUENCE_NUMBER;
    if (message.getSenderCompId().length() == 0)
      return FIXConst.TAG_SENDER_COMPID;
    if (message.getTargetCompId().length() == 0)
      return FIXConst.TAG_TARGET_COMPID;
    if (message.getSendingTime().length() == 0)
      return FIXConst.TAG_SENDING_TIME;

    switch (message.getMessageType()) {
    case LOGON:
      if (!verifyTag(message, FIXConst.TAG_HEARBEAT_INTERVAL))
        return FIXConst.TAG_HEARBEAT_INTERVAL;
      if (!verifyTag(message, FIXConst.TAG_ENCRYPT_METHOD))
        return FIXConst.TAG_ENCRYPT_METHOD;
      break;

    case NEW_ORDER:
      if (!verifyTag(message, FIXConst.TAG_SYMBOL))
        return FIXConst.TAG_SYMBOL;
      if (!verifyTag(message, FIXConst.TAG_CLIENT_ORDER_ID))
        return FIXConst.TAG_CLIENT_ORDER_ID;
      if (!verifyTag(message, FIXConst.TAG_ORDER_QUANTITY))
        return FIXConst.TAG_ORDER_QUANTITY;
      if (!verifyTag(message, FIXConst.TAG_ORDER_SIDE))
        return FIXConst.TAG_ORDER_SIDE;
      if (!verifyTag(message, FIXConst.TAG_ORDER_TYPE))
        return FIXConst.TAG_ORDER_TYPE;
      if (message.getValueAsChar(FIXConst.TAG_ORDER_TYPE) == FIXConst.ORDER_TYPE_LIMIT
          && !verifyTag(message, FIXConst.TAG_ORDER_PRICE))
        return FIXConst.TAG_ORDER_PRICE;
      if (!verifyTag(message, FIXConst.TAG_TRANSACTION_TIME))
        return FIXConst.TAG_TRANSACTION_TIME;
      break;

    case ORDER_CANCEL:
      if (!verifyTag(message, FIXConst.TAG_CLIENT_ORDER_ID))
        return FIXConst.TAG_CLIENT_ORDER_ID;
      if (!verifyTag(message, FIXConst.TAG_ORIG_CLIENT_ORDER_ID))
        return FIXConst.TAG_ORIG_CLIENT_ORDER_ID;
      break;

    case ORDER_CANCEL_REPLACE:
      if (!verifyTag(message, FIXConst.TAG_CLIENT_ORDER_ID))
        return FIXConst.TAG_CLIENT_ORDER_ID;
      if (!verifyTag(message, FIXConst.TAG_ORIG_CLIENT_ORDER_ID))
        return FIXConst.TAG_ORIG_CLIENT_ORDER_ID;
      break;
      
    case TEST_REQUEST:
      if (!verifyTag(message, FIXConst.TAG_TEST_REQUEST_ID))
        return FIXConst.TAG_TEST_REQUEST_ID;
      break;
    default:
      break;
    }
    return null;
  }
  
  public void send(FIXExecutionReportMessage message) throws IOException {
    sessionSendLock.lock();

    // FixVersion, MessageType, SenderCompId,TargetCompId are added by
    // getBaseMessage
    final String currentUTCDateTime = FIXUtility.getUtcDatetime();
    message.setSequenceNumber(outgoingSeqNum);
    message.setSendingTime(currentUTCDateTime);
    
    if (outputStream != null) {
      if (verbose) {
        System.out.println("OUTGOING: " + message.toString());
      }
      outputStream.writeUTF(message.toString());
      outputStream.flush();
    } else {
      if (verbose) {
        System.out.println("NOT SEND: " + message.toString());
      }
    }
    
    outgoingSeqNum++;
    lastSentTime = System.currentTimeMillis();

    sessionSendLock.unlock();
    return;
  }

  public void send(FIXMessage message) throws IOException {
    sessionSendLock.lock();

    // FixVersion, MessageType, SenderCompId,TargetCompId are added by
    // getBaseMessage
    final String currentUTCDateTime = FIXUtility.getUtcDatetime();
    message.setSequenceNumber(outgoingSeqNum);
    message.setSendingTime(currentUTCDateTime);
    if (!message.isAdminMessage() && message.getMessageType() != FIXMessageType.MARKET_DATA_SNAPSHOT_FULL_REFRESH) {
      message.setTag(FIXConst.TAG_TRANSACTION_TIME, currentUTCDateTime);
    }
    
    if (outputStream != null) {
      if (verbose) {
        System.out.println("OUTGOING: " + message.toString());
      }
      outputStream.writeUTF(message.toString());
      outputStream.flush();
    } else {
      if (verbose) {
        System.out.println("NOT SEND: " + message.toString());
      }
    }
    
    outgoingSeqNum++;
    lastSentTime = System.currentTimeMillis();

    sessionSendLock.unlock();
    return;
  }

  @Override
  public String toString() {
    return compId + " " + targetCompId;
  }
}
