package jfixgateway.message;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jfixgateway.FIXConst.FIXVersion;
import jfixgateway.FIXConst;
import jfixgateway.FIXGroup;
import jfixgateway.FIXSession;
import jfixgateway.FIXConst.FIXMessageType;

public class FIXMessage implements IFIXMessage {
  private FIXSession session;
  public void setSession(FIXSession session) {
    this.session = session;
  }
  public void send() throws IOException {
    session.send(this);
  }
  
  private FIXVersion fixVersion;
  private int seqNum;
  private FIXMessageType msgType;
  private String senderCompId;
  private String targetCompId;
  private String sendingTime;
  private HashMap<String, String> bodyTagValueMap = new HashMap<String, String>();
  
  private HashMap<String, FIXGroup> tagGroupMap = null;
  
  public void addGroup(String tag, FIXGroup group) {
    if (tagGroupMap == null) {
      tagGroupMap = new HashMap<String, FIXGroup>();
    }
    tagGroupMap.put(tag, group);
  }
  
  public FIXGroup getGroup(String tag) {
    return tagGroupMap == null? null: tagGroupMap.get(tag);
  }
  
  public FIXMessage() {
    reset();
  }

  public void reset() {
    fixVersion = FIXVersion.FIX_VERSION_NONE;
    msgType = FIXConst.FIXMessageType.MESSAGE_TYPE_NONE;
    seqNum = 0;
    senderCompId = "";
    targetCompId = "";
    sendingTime = "";
    bodyTagValueMap.clear();
  }

  public FIXVersion getFixVersion() {
    if (fixVersion == FIXVersion.FIX_VERSION_NONE && hasTag(FIXConst.TAG_VERSION)) {
      fixVersion = FIXVersion.get(getValue(FIXConst.TAG_VERSION));
      removeTag(FIXConst.TAG_VERSION);
    }
    return fixVersion;
  }
  public FIXMessageType getMessageType() {
    if (msgType == FIXConst.FIXMessageType.MESSAGE_TYPE_NONE && hasTag(FIXConst.TAG_MESSAGE_TYPE)) {
      msgType = FIXMessageType.get(getValue(FIXConst.TAG_MESSAGE_TYPE));
      removeTag(FIXConst.TAG_MESSAGE_TYPE);
    }
    return msgType;
  }
  public int getSequenceNumber() {
    if (seqNum == 0 && hasTag(FIXConst.TAG_SEQUENCE_NUMBER)) {
      seqNum = getValueAsInt(FIXConst.TAG_SEQUENCE_NUMBER);
      removeTag(FIXConst.TAG_SEQUENCE_NUMBER);
    }
    return seqNum;
  }
  public final String getSenderCompId() {
    if (senderCompId.length() == 0 && hasTag(FIXConst.TAG_SENDER_COMPID)) {
      senderCompId = getValue(FIXConst.TAG_SENDER_COMPID);
      removeTag(FIXConst.TAG_SENDER_COMPID);
    }
    return senderCompId;
  }
  public final String getTargetCompId() {
    if (targetCompId.length() == 0 && hasTag(FIXConst.TAG_TARGET_COMPID)) {
      targetCompId = getValue(FIXConst.TAG_TARGET_COMPID);
      removeTag(FIXConst.TAG_TARGET_COMPID);
    }
    return targetCompId;
  }
  public final String getSendingTime() {
    if (sendingTime.length() == 0 && hasTag(FIXConst.TAG_SENDING_TIME)) {
      sendingTime = getValue(FIXConst.TAG_SENDING_TIME);
      removeTag(FIXConst.TAG_SENDING_TIME);
    }
    return sendingTime; 
  }

  public void setFixVersion(FIXVersion version) { fixVersion = version; }
  public void setMessageType(FIXMessageType messageType) { msgType = messageType; }
  public void setSequenceNumber(int sequenceNumber) { seqNum = sequenceNumber; }
  public void setSenderCompId(String senderCompId) { this.senderCompId = senderCompId; }
  public void setTargetCompId(String targetCompId) { this.targetCompId = targetCompId; }
  public void setSendingTime(String sendingTime) { this.sendingTime = sendingTime; }

  public final boolean hasTag(String tag) { return bodyTagValueMap.containsKey(tag); }
  
  private void removeTag(String tag) { bodyTagValueMap.remove(tag); }
  
  public void setTag(String tag, String value) { bodyTagValueMap.put(tag, value); }
  public void setTag(String tag, int value) { setTag(tag, String.valueOf(value)); }
  public void setTag(String tag, char value) { setTag(tag, String.valueOf(value)); }
  public void setTag(String tag, double value) { setTag(tag, String.valueOf(value)); }
  public void setTag(String tag, long value) { setTag(tag, String.valueOf(value)); }

  public String getValue(String tag) { return bodyTagValueMap.get(tag); }
  public char getValueAsChar(String tag) { return getValue(tag).charAt(0); }
  public int getValueAsInt(String tag) { return Integer.parseInt(getValue(tag)); }
  public long getValueAsLong(String tag) { return Long.parseLong(getValue(tag)); }
  public final double getTagValueAsDouble(String tag) { return Double.parseDouble(getValue(tag)); }

  public boolean isAdminMessage() {
    return msgType == FIXMessageType.LOGOFF || msgType == FIXMessageType.LOGON || msgType == FIXMessageType.HEARTBEAT
        || msgType == FIXMessageType.TEST_REQUEST || msgType == FIXMessageType.USER_LOGON
        || msgType == FIXMessageType.ADMIN_REJECT;
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

  private int calculateBodyLength() {
    int bodyLength = calculateHeaderLength();
    final int delimiterAndEqualsLength = 2; //length of = and delimiter
    for (Map.Entry<String, String> tagValue : bodyTagValueMap.entrySet()) {
      String key = tagValue.getKey();
      if (FIXConst.TAG_VERSION.equals(key) || FIXConst.TAG_MESSAGE_TYPE.equals(key)
          || FIXConst.TAG_SEQUENCE_NUMBER.equals(key) || FIXConst.TAG_TARGET_COMPID.equals(key)
          || FIXConst.TAG_SENDING_TIME.equals(key)) {
        continue;
      }
      bodyLength += key.length() + delimiterAndEqualsLength
          + tagValue.getValue().length();
    }
    if (tagGroupMap != null) {
      for (FIXGroup group : tagGroupMap.values()) {
        bodyLength += group.calculateBodyLength();
      }  
    }
    return bodyLength;
  }
/*
  public static void loadFromFile(String filename, List<FIXMessage> messages) throws IOException {
    final File file = new File(filename);
    if (!file.exists()) {
      throw new RuntimeException(String.format("File %s could not be opened", filename));
    }

    final BufferedReader br = new BufferedReader(new FileReader(file));
    String line;
    while ((line = br.readLine()) != null) {
      if (!line.startsWith("#")) {
        FIXMessage message = FIXParser.parseFromString(line);
        messages.add(message);
      }
    }
    br.close();
  }*/
  
  public void appendTagValue(StringBuilder sb, String tag, String value) {
    sb.append(tag).append(FIXConst.FIX_EQUALS).append(value).append(FIXConst.FIX_DELIMITER);
  }

  public String toString() {
    final StringBuilder sb = new StringBuilder();
    // HEADER TAGS
    appendTagValue(sb, FIXConst.TAG_VERSION, this.getFixVersion().value);
    appendTagValue(sb, FIXConst.TAG_BODY_LENGTH, String.valueOf(calculateBodyLength()));
    appendTagValue(sb, FIXConst.TAG_MESSAGE_TYPE, this.getMessageType().value);
    appendTagValue(sb, FIXConst.TAG_SEQUENCE_NUMBER, String.valueOf(this.getSequenceNumber()));
    appendTagValue(sb, FIXConst.TAG_SENDER_COMPID, this.getSenderCompId());
    appendTagValue(sb, FIXConst.TAG_SENDING_TIME, this.getSendingTime());
    appendTagValue(sb, FIXConst.TAG_TARGET_COMPID, this.getTargetCompId());

    // BODY TAGS
    for (Map.Entry<String, String> entry : bodyTagValueMap.entrySet()) {
      appendTagValue(sb, entry.getKey(), entry.getValue());
    }
    
    if (tagGroupMap != null) {
      for (FIXGroup group : tagGroupMap.values()) {
        sb.append(group.toString());
      }
    }

    // CHECKSUM
    appendTagValue(sb, FIXConst.TAG_BODY_CHECKSUM, calculateCheckSum(sb));
    return sb.toString();
  }

  private String calculateCheckSum(final StringBuilder sb) {
    int sum = 0;
    for (int i = 0; i < sb.length(); i++) {
      sum += (int) sb.charAt(i);
    }
    return String.format("%03d", sum % 256);
  }
}
