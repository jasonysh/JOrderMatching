package jfixgateway;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import jfixgateway.FIXConst.FIXMessageType;
import jfixgateway.FIXConst.FIXVersion;
import jfixgateway.message.FIXMessage;

public class FIXGatewayTest {

  private final StringBuilder tag = new StringBuilder();
  private final StringBuilder value = new StringBuilder();
  
  private static class Holder {
    public static FIXMessage sendMsg = null;
    public static String errMsg = null;
    public static ArrayDeque<Byte> inputStreamBytes;
  }
  
  private ArrayDeque<Byte> convertByteArrayToArrayDeque(byte[] bytes) {
    ArrayDeque<Byte> a = new ArrayDeque<Byte>();
    for (byte b : bytes) {
      a.add(b);
    }
    return a;
  }
  
  @Before
  public void beforeEach() throws IOException {
    session = createSession(0, 1024);
    server = new AbstractFIXServer(config, false) {
      @Override public void onFixLogon(FIXSession session) {}
      @Override public void onFixLogoff(FIXSession session) {}
      @Override public void onFixMessage(FIXMessage incomingMessage, FIXSession socket) {}
      @Override public void onFixError(String fixErrorMessage, FIXSession session) {
        Holder.errMsg = fixErrorMessage;
      }
    };
    
    AbstractFIXServer.verbose = true;
    Holder.sendMsg = null;
    Holder.errMsg = null;
    Holder.inputStreamBytes = null;
    
    FIXMessage logon = FIXParser.parseFromString("8=FIX.4.49=7035=A34=149=CLIENT152=20181229-13:59:42.63056=TS98=0108=30141=Y10=203", tag, value);
    server.processMessage(logon, session);
    assertEquals(FIXMessageType.LOGON, Holder.sendMsg.getMessageType());
    assertEquals(null, Holder.errMsg);
  }

  private FIXServerConfiguration config = new FIXServerConfiguration();
  private FIXSession session;
  
  private FIXSession createSession(int receiveCacheSize, int bufferSize) {
    return new FIXSession(receiveCacheSize, false, "TS", bufferSize) {
      @Override
      public void send(FIXMessage message) throws IOException {
        super.send(message);
        Holder.sendMsg = message;
      }
      
      @Override
      public boolean isInputStreamAvailable() throws IOException {
        return Holder.inputStreamBytes != null;
      }
      
      @Override
      public int readInputStream(byte b[], int off, int len) throws IOException {
        int size = 0;
        int idx = off;
        while (Holder.inputStreamBytes.size() > 0) {
          if (idx >= b.length) {
            break;
          }
          if (size >= len) {
            break;
          }
          b[idx] =  Holder.inputStreamBytes.poll();
          idx++;
          size++;
        }
        return size;
      }
    };
  }
  
  private AbstractFIXServer server;
    
    @Test
    public void testParseFromString() {
        FIXMessage message = FIXParser.parseFromString("8=FIX.4.49=12735=D34=249=CLIENT152=20181210-14:16:44.19956=TS11=154445140411021=138=540=154=155=AAPL59=060=20181210-22:16:44.18910=190", tag, value);
        assertEquals(FIXMessageType.NEW_ORDER, message.getMessageType());
        assertEquals("AAPL", message.getValue(FIXConst.TAG_SYMBOL));
        assertEquals("CLIENT1", message.getSenderCompId());
        assertEquals("TS", message.getTargetCompId());
        assertEquals(FIXVersion.FIX_4_4, message.getFixVersion());
        assertEquals("20181210-14:16:44.199", message.getSendingTime());
        assertEquals(2, message.getSequenceNumber());
        
        FIXMessage _message = FIXParser.parseFromString(message.toString(), tag, value);
        assertEquals(_message.getMessageType(), message.getMessageType());
        assertEquals(_message.getValue(FIXConst.TAG_SYMBOL), message.getValue(FIXConst.TAG_SYMBOL));
        assertEquals(_message.getSenderCompId(), message.getSenderCompId());
        assertEquals(_message.getTargetCompId(), message.getTargetCompId());
        assertEquals(_message.getFixVersion(), message.getFixVersion());
        assertEquals(_message.getSendingTime(), message.getSendingTime());
        assertEquals(_message.getSequenceNumber(), message.getSequenceNumber());
    }
    
    @Test
    public void testFIXMarketDataFullRefresh() throws IOException {
      FIXMessage msg = session.getMarketDataSnapshotFullRefreshMessage();
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
      
      FIXGroup _groupNoMDEntries = msg.getGroup(FIXConst.TAG_NO_MD_ENTRIES);
      assertEquals(2, _groupNoMDEntries.getValueAsInt(FIXConst.TAG_NO_MD_ENTRIES));
      assertEquals(2, _groupNoMDEntries.getGroupCount());
      FIXGroup _group = _groupNoMDEntries.getGroup(0);
      assertEquals(FIXConst.MD_ENTRY_TYPE_BID, _group.getValueAsChar(FIXConst.TAG_MD_ENTRY_TYPE));
      assertEquals(156.23, _group.getValueAsDouble(FIXConst.TAG_MD_ENTRY_PX), 0.0001);
      assertEquals(1000, _group.getValueAsInt(FIXConst.TAG_MD_ENTRY_SIZE));

      _group = _groupNoMDEntries.getGroup(1);
      assertEquals(FIXConst.MD_ENTRY_TYPE_ASK, _group.getValueAsChar(FIXConst.TAG_MD_ENTRY_TYPE));
      assertEquals(156.25, _group.getValueAsDouble(FIXConst.TAG_MD_ENTRY_PX), 0.0001);
      assertEquals(2000, _group.getValueAsInt(FIXConst.TAG_MD_ENTRY_SIZE));

      msg.setSequenceNumber(0);
      FIXMessage fullRefresh = FIXParser.parseFromString(msg.toString(), tag, value);
      assertEquals(106, fullRefresh.getValueAsInt(FIXConst.TAG_BODY_LENGTH));
    }
    
    @Test
    public void testLogonMessage() throws IOException {
      FIXMessage logon = FIXParser.parseFromString("8=FIX.4.49=7035=A34=149=CLIENT152=20181229-13:59:42.63056=TS98=0108=30141=Y10=203", tag, value);
      assertEquals(1, logon.getSequenceNumber());
      assertEquals(FIXMessageType.LOGON, logon.getMessageType());
      assertEquals("CLIENT1", logon.getSenderCompId());
      assertEquals("TS", logon.getTargetCompId());
      assertEquals(FIXVersion.FIX_4_4, logon.getFixVersion());
      assertEquals("20181229-13:59:42.630", logon.getSendingTime());
      assertEquals(1, logon.getSequenceNumber());
      assertEquals("TS", server.getCompID());
      
      server.processMessage(logon, session);
      assertEquals(FIXMessageType.LOGON, Holder.sendMsg.getMessageType());
      assertEquals(null, Holder.errMsg);
      
      assertEquals("TS", session.getCompId());
      assertEquals("TS CLIENT1", session.toString());
    }
    
    private void testMissingTag(String missingTag, String msgString) throws IOException {
      Holder.inputStreamBytes = convertByteArrayToArrayDeque(msgString.getBytes());
      FIXMessage msg = session.receive();
      server.processMessage(msg, session);
      assertEquals(null, Holder.errMsg);
      assertEquals(FIXMessageType.ADMIN_REJECT, Holder.sendMsg.getMessageType());
      assertEquals("Required tag missing", Holder.sendMsg.getValue(FIXConst.TAG_FREE_TEXT));
      assertEquals(missingTag, Holder.sendMsg.getValue(FIXConst.TAG_REF_TAG));
    }

    @Test
    public void testLogon_MissingTag_35() throws IOException {
      testMissingTag(FIXConst.TAG_MESSAGE_TYPE, "8=FIX.4.49=7039=7034=249=CLIENT152=20181229-13:59:42.63056=TS98=0108=30141=Y10=203");
    }

    @Test
    public void testLogon_MissingTag_49() throws IOException {
      testMissingTag(FIXConst.TAG_SENDER_COMPID, "8=FIX.4.49=7035=A39=7034=152=20181229-13:59:42.63056=TS98=0108=30141=Y10=203");
    }

    @Test
    public void testLogon_MissingTag_56() throws IOException {
      testMissingTag(FIXConst.TAG_TARGET_COMPID, "8=FIX.4.49=7035=A34=249=CLIENT152=20181229-13:59:42.63098=0108=30141=Y10=203");
    }

    @Test
    public void testLogon_MissingTag_98() throws IOException {
      testMissingTag(FIXConst.TAG_ENCRYPT_METHOD, "8=FIX.4.49=7035=A34=149=CLIENT152=20181229-13:59:42.63056=TS108=30141=Y10=203");
    }

    @Test
    public void testLogon_MissingTag_108() throws IOException {
      testMissingTag(FIXConst.TAG_HEARBEAT_INTERVAL, "8=FIX.4.49=7035=A34=149=CLIENT152=20181229-13:59:42.63056=TS98=0141=Y10=203");
    }

    @Test
    public void testLogon_MissingTag_10() throws IOException {
      testMissingTag(FIXConst.TAG_BODY_CHECKSUM, "8=FIX.4.49=7035=A34=149=CLIENT152=20181229-13:59:42.63056=TS98=0108=30141=Y");
    }

    @Test
    public void testNewOrderSingle_MissingTag_52() throws IOException {
      testMissingTag(FIXConst.TAG_SENDING_TIME, "8=FIX.4.49=12735=D34=249=CLIENT156=TS11=154445140411021=138=540=154=155=AAPL59=060=20181210-22:16:44.18910=190");
    }

    @Test
    public void testNewOrderSingle_MissingTag_55() throws IOException {
      testMissingTag(FIXConst.TAG_SYMBOL, "8=FIX.4.49=12735=D34=249=CLIENT152=20181210-14:16:44.19956=TS11=154445140411021=138=540=154=159=060=20181210-22:16:44.18910=190");
    }

    @Test
    public void testNewOrderSingle_MissingTag_11() throws IOException {
      testMissingTag(FIXConst.TAG_CLIENT_ORDER_ID, "8=FIX.4.49=12735=D34=249=CLIENT152=20181210-14:16:44.19956=TS21=138=540=154=155=AAPL59=060=20181210-22:16:44.18910=190");
    }

    @Test
    public void testNewOrderSingle_MissingTag_38() throws IOException {
      testMissingTag(FIXConst.TAG_ORDER_QUANTITY, "8=FIX.4.49=12735=D34=249=CLIENT152=20181210-14:16:44.19956=TS11=154445140411021=140=154=155=AAPL59=060=20181210-22:16:44.18910=190");
    }

    @Test
    public void testNewOrderSingle_MissingTag_54() throws IOException {
      testMissingTag(FIXConst.TAG_ORDER_SIDE, "8=FIX.4.49=12735=D34=249=CLIENT152=20181210-14:16:44.19956=TS11=154445140411021=138=540=155=AAPL59=060=20181210-22:16:44.18910=190");
    }

    @Test
    public void testNewOrderSingle_MissingTag_40() throws IOException {
      testMissingTag(FIXConst.TAG_ORDER_TYPE, "8=FIX.4.49=12735=D34=249=CLIENT152=20181210-14:16:44.19956=TS11=154445140411021=138=554=155=AAPL59=060=20181210-22:16:44.18910=190");
    }

    @Test
    public void testNewOrderSingle_MissingTag_44() throws IOException {
      testMissingTag(FIXConst.TAG_ORDER_PRICE, "8=FIX.4.49=12735=D34=249=CLIENT152=20181210-14:16:44.19956=TS11=154445140411021=138=540=254=155=AAPL59=060=20181210-22:16:44.18910=190");
    }

    @Test
    public void testNewOrderSingle_MissingTag_60() throws IOException {
      testMissingTag(FIXConst.TAG_TRANSACTION_TIME, "8=FIX.4.49=12735=D34=249=CLIENT152=20181210-14:16:44.19956=TS11=154445140411021=138=540=154=155=AAPL59=010=190");
    }
    
    @Test
    public void testOrderCancel_MissingTag_52() throws IOException {
      testMissingTag(FIXConst.TAG_SENDING_TIME, "8=FIX.4.49=13935=F34=249=CLIENT156=TS38=19=12641=154618020708210=21411=154618020857954=155=a60=20181230-22:30:08.57910=032");
    }
    
    @Test
    public void testOrderCancel_MissingTag_11() throws IOException {
      testMissingTag(FIXConst.TAG_CLIENT_ORDER_ID, "8=FIX.4.49=13935=F34=249=CLIENT152=20181230-14:30:08.57956=TS38=19=12641=154618020708210=21454=155=a60=20181230-22:30:08.57910=032");
    }
    
    @Test
    public void testOrderCancel_MissingTag_41() throws IOException {
      testMissingTag(FIXConst.TAG_ORIG_CLIENT_ORDER_ID, "8=FIX.4.49=13935=F34=249=CLIENT152=20181230-14:30:08.57956=TS38=19=12610=21411=154618020857954=155=a60=20181230-22:30:08.57910=032");
    }
    
    @Test
    public void testLogonMessage_InvalidSequenceNumber() throws IOException {
      FIXMessage logon = FIXParser.parseFromString("8=FIX.4.49=7035=A34=249=CLIENT152=20181229-13:59:42.63056=TS98=0108=30141=Y10=203", tag, value);
      server.processMessage(logon, session);
      assertEquals("Invalid sequence number received , expected : 1 , actual : 2", Holder.errMsg);
    }
    
    @Test
    public void testHeartBeatMessage() throws IOException {
      FIXMessage msg = FIXParser.parseFromString("8=FIX.4.49=5235=034=249=CLIENT152=20181230-15:06:25.68056=TS10=109", tag, value);

      assertEquals(FIXMessageType.HEARTBEAT, msg.getMessageType());
      assertEquals("TS", msg.getTargetCompId());
      assertEquals("CLIENT1", msg.getSenderCompId());
      
      server.processMessage(msg, session);
      assertEquals(FIXMessageType.HEARTBEAT, Holder.sendMsg.getMessageType());
      assertEquals("CLIENT1", Holder.sendMsg.getTargetCompId());
      assertEquals("TS", Holder.sendMsg.getSenderCompId());
      assertEquals(null, Holder.errMsg);
      assertEquals("TS", session.getCompId());
    }
    
    @Test
    public void testTestMessage() throws IOException {
      FIXMessage msg = FIXParser.parseFromString("8=FIX.4.49=5235=134=249=CLIENT152=20181230-15:06:25.68056=TS112=TEST110=109", tag, value);
      assertEquals(FIXMessageType.TEST_REQUEST, msg.getMessageType());
      assertEquals("TS", msg.getTargetCompId());
      assertEquals("CLIENT1", msg.getSenderCompId());
      
      server.processMessage(msg, session);
      assertEquals(FIXMessageType.HEARTBEAT, Holder.sendMsg.getMessageType());
      assertEquals("CLIENT1", Holder.sendMsg.getTargetCompId());
      assertEquals("TS", Holder.sendMsg.getSenderCompId());
      assertEquals(null, Holder.errMsg);
    }
    
    @Test
    public void testTestMessage_MissingTag_112() throws IOException {
      FIXMessage msg = FIXParser.parseFromString("8=FIX.4.49=5235=134=249=CLIENT152=20181230-15:06:25.68056=TS10=109", tag, value);
      assertEquals(FIXMessageType.TEST_REQUEST, msg.getMessageType());
      assertEquals("TS", msg.getTargetCompId());
      assertEquals("CLIENT1", msg.getSenderCompId());
      
      server.processMessage(msg, session);
      assertEquals(FIXMessageType.ADMIN_REJECT, Holder.sendMsg.getMessageType());
      assertEquals("Required tag missing", Holder.sendMsg.getValue(FIXConst.TAG_FREE_TEXT));
    }
    
    @Test
    public void testReceive_EnoughBufferSize() throws IOException {
      Holder.inputStreamBytes = convertByteArrayToArrayDeque("8=FIX.4.49=7035=A34=149=CLIENT152=20181229-13:59:42.63056=TS98=0108=30141=Y10=203".getBytes());

      FIXMessage logon = session.receive();
      assertEquals(1, logon.getSequenceNumber());
      assertEquals(FIXMessageType.LOGON, logon.getMessageType());
      assertEquals("CLIENT1", logon.getSenderCompId());
      assertEquals("TS", logon.getTargetCompId());
      assertEquals(FIXVersion.FIX_4_4, logon.getFixVersion());
      assertEquals("20181229-13:59:42.630", logon.getSendingTime());
      assertEquals(1, logon.getSequenceNumber());
      assertEquals("TS", server.getCompID());
    }
    
    @Test
    public void testReceive_NotEnoughBufferSize() throws IOException {
      config.setFixCompId("TS");
      FIXSession session = createSession(config.getFixReceiveCacheSize(), 40);
      Holder.inputStreamBytes = convertByteArrayToArrayDeque("8=FIX.4.49=7035=A34=149=CLIENT152=20181229-13:59:42.63056=TS98=0108=30141=Y10=203".getBytes());
      
      FIXMessage logon = session.receive();
      assertEquals(1, logon.getSequenceNumber());
      assertEquals(FIXMessageType.LOGON, logon.getMessageType());
      assertEquals("CLIENT1", logon.getSenderCompId());
      assertEquals("TS", logon.getTargetCompId());
      assertEquals(FIXVersion.FIX_4_4, logon.getFixVersion());
      assertEquals("20181229-13:59:42.630", logon.getSendingTime());
      assertEquals(1, logon.getSequenceNumber());
      assertEquals("TS", server.getCompID());
    }
    
    @Test
    public void testReceiveCache() throws IOException {
      Holder.inputStreamBytes = convertByteArrayToArrayDeque(("8=FIX.4.49=5235=034=249=CLIENT152=20181230-15:06:25.68056=TS10=109"
          +"8=FIX.4.49=5235=034=349=CLIENT152=20181230-15:06:55.68056=TS10=109"
          +"8=FIX.4.49=5235=034=449=CLIENT152=20181230-15:07:25.68056=TS10=109").getBytes());

      FIXSession session = createSession(200, 1024);
      List<FIXMessage> messages = new ArrayList<FIXMessage>();
      session.receiveWithCaching(messages);
      assertEquals(2, messages.size());
      FIXMessage msg1 = messages.get(0);
      FIXMessage msg2 = messages.get(1);
      assertEquals(FIXMessageType.HEARTBEAT, msg1.getMessageType());
      assertEquals(2, msg1.getSequenceNumber());
      assertEquals(FIXMessageType.HEARTBEAT, msg2.getMessageType());
      assertEquals(3, msg2.getSequenceNumber());
      messages.clear();
      session.receiveWithCaching(messages);
      assertEquals(1, messages.size());
      FIXMessage msg3 = messages.get(0);
      assertEquals(FIXMessageType.HEARTBEAT, msg3.getMessageType());
      assertEquals(4, msg3.getSequenceNumber());
    }
    
    @Test
    public void testNewOrderSingleRequest() {
      FIXMessage msg = FIXParser.parseFromString("8=FIX.4.49=12735=D34=249=CLIENT156=TS11=154445140411021=138=540=154=155=AAPL59=060=20181210-22:16:44.18910=190", tag, value);
    }
}
