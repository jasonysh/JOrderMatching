package jfixgateway;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class FIXGroup {

  private LinkedHashMap<String, String> bodyTagValueMap = new LinkedHashMap<String, String>();
  
  private ArrayList<FIXGroup> groupList = null;
  
  public void AddGroup(FIXGroup group) {
    if (groupList == null) {
      groupList = new ArrayList<FIXGroup>();
    }
    groupList.add(group);
  }
  
  public int getGroupCount() {
    return groupList == null? 0: groupList.size();
  }
  
  
  public FIXGroup getGroup(int index) {
    return groupList == null? null: groupList.get(index);
  }
  
  public int calculateBodyLength() {
    int bodyLength = 0;
    final int delimiterAndEqualsLength = 2; //length of = and delimiter
    for (Map.Entry<String, String> tagValue : bodyTagValueMap.entrySet()) {
      bodyLength += String.valueOf(tagValue.getKey()).length() + delimiterAndEqualsLength
          + tagValue.getValue().length();
    }
    if (groupList != null) {
      for (FIXGroup group : groupList) {
        bodyLength += group.calculateBodyLength();
      }  
    }
    return bodyLength;
  }

  public final boolean hasTag(String tag) { return bodyTagValueMap.containsKey(tag); }
  
  public void setTag(String tag, String value) { bodyTagValueMap.put(tag, value); }
  public void setTag(String tag, int value) { setTag(tag, String.valueOf(value)); }
  public void setTag(String tag, char value) { setTag(tag, String.valueOf(value)); }
  public void setTag(String tag, double value) { setTag(tag, String.valueOf(value)); }
  public void setTag(String tag, long value) { setTag(tag, String.valueOf(value)); }

  public String getValue(String tag) { return bodyTagValueMap.get(tag); }
  public char getValueAsChar(String tag) { return getValue(tag).charAt(0); }
  public int getValueAsInt(String tag) { return Integer.parseInt(getValue(tag)); }
  public long getValueAsLong(String tag) { return Long.parseLong(getValue(tag)); }
  public final double getValueAsDouble(String tag) { return Double.parseDouble(getValue(tag)); }
  
  public void appendTagValue(StringBuilder sb, String tag, String value) {
    sb.append(tag).append(FIXConst.FIX_EQUALS).append(value).append(FIXConst.FIX_DELIMITER);
  }
  
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    for (String tagValueKey : bodyTagValueMap.keySet()) {
      appendTagValue(sb, tagValueKey, getValue(tagValueKey));
    }
    if (groupList != null) {
      for (FIXGroup group : groupList) {
        sb.append(group.toString());
      }
    }
    return sb.toString();
  }
}
