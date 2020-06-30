package jfixgateway;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import jfixgateway.FIXConst.FIXMessageType;
import jfixgateway.message.FIXMessage;

public abstract class AbstractFIXServer {
  
  public static boolean verbose;

  private ServerSocket serverSocket;
  private boolean stop;

  private CopyOnWriteArrayList<FIXSession> sessions = new CopyOnWriteArrayList<FIXSession>();
  private ConcurrentHashMap<String/*SenderCompID*/, FIXSession> compIDSessionMap = new ConcurrentHashMap<String/*SenderCompID*/, FIXSession>();

  private final int receiveCacheSize;
  private final boolean isSeqNumValidationEnable;
  private final int port;
  private final int backlog;
  private final int bufferSize;
  private final String address;
  private final String serverCompID;
  public String getCompID() {
    return serverCompID;
  }

  public AbstractFIXServer(FIXServerConfiguration configuration, boolean _verbose) {
    // FIX options
    address = configuration.getFixAddress();
    port = configuration.getFixPort();
    backlog = configuration.getBacklog();
    bufferSize = configuration.getBufferSize();
    serverCompID = configuration.getFixCompId();
    receiveCacheSize = configuration.getFixReceiveCacheSize();
    isSeqNumValidationEnable = configuration.isFixSeqNumValidation();
    verbose = _verbose;
  }
  
  public void start() throws IOException {
    System.out.println("start - port:"+port+", backlog:"+backlog);
    serverSocket = new ServerSocket(port, backlog);
    Thread threadListenNewSocket = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          while (!stop) {
            final FIXSession session = new FIXSession(receiveCacheSize, verbose, serverCompID, bufferSize);
            session.initialise(serverSocket.accept());
            sessions.add(session);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
    threadListenNewSocket.start();

    Thread threadListenSocket = new Thread(new Runnable() {
      @Override
      public void run() {
        while (!stop) {
          for (FIXSession session : sessions) {
            try {
              handleClient(session);
            } catch (IOException e) {
              e.printStackTrace();
              closeSession(session);
            }
          }
          try {
            Thread.sleep(1);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    });
    threadListenSocket.start();
  }
  
  public FIXSession getSession(String compID) {
    return compIDSessionMap.get(compID);
  }

  public void closeSession(FIXSession session) {
    if (session.connected()) {
      try {
        session.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (session.getTargetCompId() != null) {
      compIDSessionMap.remove(session.getTargetCompId());
    }
    sessions.remove(session);
  }

  public void stop() {
    stop = true;
    for (FIXSession session : sessions) {
      if (session.connected()) {
        try {
          session.saveSequenceNumbersToStore();
        } catch (Exception e) {
          e.printStackTrace();
        }
        try {
          session.close();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

    }
  }

  //public abstract void onUnhandledSocketError(int errorCode, int eventResult);

  public abstract void onFixLogon(FIXSession session);

  public abstract void onFixLogoff(FIXSession session);

  //public abstract void onTraderLogon(FIXSession session, String username);

  public abstract void onFixMessage(FIXMessage incomingMessage, FIXSession socket);

  public abstract void onFixError(String fixErrorMessage, FIXSession session);

  public void onClientDisconnected(FIXSession session) throws FileNotFoundException {
    onFixLogoff(session);

    session.lock();
    session.saveSequenceNumbersToStore();
    closeSession(session);
    session.unlock();
  }

  public void processMessage(FIXMessage incomingMsg, FIXSession session) throws IOException {
    final String missingTag = FIXSession.validateRequiredTags(incomingMsg);
    // Validations done in fix server level for all sessions
    if (missingTag != null) {
      // ADMIN LEVEL REJECTION FOR MISSING TAGS
      final FIXMessage message = session.getAdminRejectionMessage();
      message.setTag(FIXConst.TAG_FREE_TEXT, "Required tag missing");
      message.setTag(FIXConst.TAG_REF_TAG, missingTag);
      message.setTag(FIXConst.TAG_REF_MSG_TYPE, incomingMsg.getMessageType().value);
      message.setTag(FIXConst.TAG_REJECT_REASON, 1);
      session.send(message);
      return;
    }
    
    if (incomingMsg.getMessageType() == FIXConst.FIXMessageType.LOGON) {
      // Handle Logon messages before sequence number validation
      if (!session.validateTargetCompid(incomingMsg)) {
        onFixError(String.format("Invalid target comp id , expected : %s , actual : %s", serverCompID,
            incomingMsg.getTargetCompId()), session);
        session.close();
        onClientDisconnected(session);
        return;
      }
      session.handleLogonMessage(incomingMsg);
      compIDSessionMap.put(session.getTargetCompId(), session);
      onFixLogon(session);
    }

    final int actualIncomingSeqNum = incomingMsg.getSequenceNumber();
    // Sequence number validation
    if (isSeqNumValidationEnable) {
      if (!session.validateSequenceNumber(actualIncomingSeqNum)) {
        // Currently sequence number resetting / gap filling not supported
        onFixError(String.format("Invalid sequence number received , expected : %d , actual : %d",
            session.getIncomingSequenceNumber() + 1, actualIncomingSeqNum), session);
        session.close();
        onClientDisconnected(session);
        return;
      }
    }
    session.setIncomingSeqNum(actualIncomingSeqNum);

    if (incomingMsg.isAdminMessage()) {
      session.handleAdminMessage(incomingMsg);

      final FIXMessageType _messageType = incomingMsg.getMessageType();

      if (_messageType == FIXConst.FIXMessageType.LOGOFF) {
        onClientDisconnected(session);
      }
    } else {
      onFixMessage(incomingMsg, session);
    }
  }

  public void handleClient(FIXSession session) throws IOException {
    if (receiveCacheSize == 0) {
      final FIXMessage incomingMsg = session.receive();// Receive message one by one
      if (incomingMsg == null) {
        // checkErrors(receivedSize, session);
        return;
      }
      processMessage(incomingMsg, session);
    } else {
      final List<FIXMessage> messages = new ArrayList<FIXMessage>();// Receive multiple messages at once
      session.receiveWithCaching(messages);
      if (messages.size() == 0) {
        return;
      }

      for (int i = 0; i < messages.size(); ++i) {
        processMessage(messages.get(i), session);
      }
    }
    return;
  }

}
