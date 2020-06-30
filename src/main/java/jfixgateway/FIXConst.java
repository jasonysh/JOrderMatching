package jfixgateway;

import java.util.HashMap;

public class FIXConst {

    public enum FIXVersion {
        FIX_VERSION_NONE("NONE"), FIX_4_0("FIX.4.0"), FIX_4_1("FIX.4.1"), FIX_4_2("FIX.4.2"), FIX_4_3("FIX.4.3"),
        FIX_4_4("FIX.4.4"), FIX_5_0("FIX.5.0"), FIX_5_0SP1("FIX.5.0SP1"), FIX_5_0SP2("FIX.5.0SP2");

        public final String value;
        private FIXVersion(String value) { this.value = value; }
        
        private static HashMap<String, FIXVersion> map = new HashMap<String, FIXVersion>();
        static {
          for (FIXVersion e: FIXVersion.values()) {
            map.put(e.value, e);
          }
        }
        public static FIXVersion get(String key) {
          return map.get(key);
        }
    }

    public enum FIXMessageType {
        MESSAGE_TYPE_NONE("NONE"), LOGON("A"), LOGOFF("5"), TEST_REQUEST("1"), RESEND_REQUEST("2"), ADMIN_REJECT("3"),
        SEQUENCE_RESET("4"), HEARTBEAT("0"), USER_LOGON("BE"), USER_RESPONSE("BF"), NEW_ORDER("D"), ORDER_CANCEL("F"),
        EXECUTION_REPORT("8"), BUSINESS_REJECT("j"), ORDER_CANCEL_REJECT("9"), ORDER_CANCEL_REPLACE("G"),
        MARKET_DATA_SNAPSHOT_FULL_REFRESH("W"), MARKET_DATA_REQUEST("V");

        public final String value;
        private FIXMessageType(String value) { this.value = value; }
        
        private static HashMap<String, FIXMessageType> map = new HashMap<String, FIXMessageType>();
        static {
          for (FIXMessageType e: FIXMessageType.values()) {
            map.put(e.value, e);
          }
        }
        public static FIXMessageType get(String key) {
          FIXMessageType msgType = map.get(key);
          return msgType == null? FIXMessageType.MESSAGE_TYPE_NONE: msgType;
        }
    }
    
    public enum FIXOrderStatus {
        NEW('0'), PARTIALLY_FILLED('1'), FILLED('2'), CANCELLED('4'), REPLACED('5'),
        PENDING_CANCEL('6'), REJECTED('8'), PENDING_REPLACE('E');

        public final char value;

        FIXOrderStatus(char value) {
            this.value = value;
        }
    }
    
    public static final int DEFAULT_HEARTBEAT_INTERVAL = 30;

    // GENERAL
    public static final char FIX_EQUALS = '=';
    public static final char FIX_DELIMITER = 1;

    // TAGS
    public static final String TAG_AVERAGE_PRICE = "6";
    public static final String TAG_VERSION = "8";
    public static final String TAG_BODY_LENGTH = "9";
    public static final String TAG_BODY_CHECKSUM = "10";
    public static final String TAG_CLIENT_ORDER_ID = "11";
    public static final String TAG_CUMULATIVE_QUANTITY = "14";
    public static final String TAG_EXEC_ID = "17";
    public static final String TAG_EXEC_INST = "18";
    public static final String TAG_EXEC_TRANSTYPE = "20";
    public static final String TAG_HAND_INST = "21";
    public static final String TAG_LAST_PRICE = "31";
    public static final String TAG_LAST_QUANTITY = "32";
    public static final String TAG_SEQUENCE_NUMBER = "34";
    public static final String TAG_MESSAGE_TYPE = "35";
    public static final String TAG_ORDER_ID = "37";
    public static final String TAG_ORDER_QUANTITY = "38";
    public static final String TAG_ORDER_STATUS = "39";
    public static final String TAG_ORDER_TYPE = "40";
    public static final String TAG_ORIG_CLIENT_ORDER_ID = "41";
    public static final String TAG_ORDER_PRICE = "44";
    public static final String TAG_REF_SEQ_NUM = "45";
    public static final String TAG_SECURITY_ID = "48";
    public static final String TAG_SENDER_COMPID = "49";
    public static final String TAG_SENDER_SUBID = "50";
    public static final String TAG_SENDING_TIME = "52";
    public static final String TAG_ORDER_SIDE = "54";
    public static final String TAG_SYMBOL = "55";
    public static final String TAG_TARGET_COMPID = "56";
    public static final String TAG_TARGET_SUBID = "57";
    public static final String TAG_FREE_TEXT = "58";
    public static final String TAG_TIME_IN_FORCE = "59";
    public static final String TAG_TRANSACTION_TIME = "60";
    public static final String TAG_ENCRYPT_METHOD = "98";
    public static final String TAG_HEARBEAT_INTERVAL = "108";
    public static final String TAG_TEST_REQUEST_ID = "112";
    public static final String TAG_RESET_SEQ_NUM_FLAG = "141";
    public static final String TAG_NO_RELATED_SYM = "146";
    public static final String TAG_EXEC_TYPE = "150";
    public static final String TAG_LEAVES_QTY = "151";
    public static final String TAG_MD_REQ_ID = "262";
    public static final String TAG_SUBSCRIPTION_REQUEST_TYPE = "263";
    public static final String TAG_NO_MD_ENTRY_TYPES = "267";
    public static final String TAG_NO_MD_ENTRIES  = "268";
    public static final String TAG_MD_ENTRY_TYPE = "269";
    public static final String TAG_MD_ENTRY_PX = "270";
    public static final String TAG_MD_ENTRY_SIZE = "271";
    public static final String TAG_USERNAME = "553";
    public static final String TAG_PASSWORD = "554";
    public static final String TAG_REF_TAG = "371";
    public static final String TAG_REF_MSG_TYPE = "372";
    public static final String TAG_REJECT_REASON = "373";
    public static final String TAG_CXL_REJ_RESPONSE_TO = "434";
    public static final String TAG_USER_REQUEST_ID = "923";
    public static final String TAG_USER_PASSWORD = "924";
    
    // ORDER TYPE
    public static final char ORDER_TYPE_MARKET = '1';
    public static final char ORDER_TYPE_LIMIT = '2';
    // SIDE
    public static final char ORDER_SIDE_BUY = '1';
    public static final char ORDER_SIDE_SELL = '2';
    // TIME IN FORCE
    public static final int ORDER_TIF_DAY = 0;
    // ENCRYPTION METHODS
    public static final int ENCRYPTION_NONE = 0;
    // CXL REJ RESPONSE TO
    public static final int CXL_REJ_RESPONSE_TO_ORDER_CANCEL_REQUEST = 1;
    public static final int CXL_REJ_RESPONSE_TO_ORDER_CANCEL_REPLACE_REQUEST = 2;
    
    public static final char MD_ENTRY_TYPE_BID = '0';
    public static final char MD_ENTRY_TYPE_ASK = '1';
}
