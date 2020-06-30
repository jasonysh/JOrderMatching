package jfixgateway.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

import jfixgateway.FIXConst;
import jfixgateway.FIXConst.FIXMessageType;
import jfixgateway.message.FIXMessage;
import jordermatching.core.Order;
import jordermatching.core.Order.OrdType;
import jordermatching.core.Order.Side;
import jordermatching.util.TestFileReadUtility;

public class TestFileWriteUtility {

  private static int getRandom(int min, int max) {
    return min + (int)(Math.random() * (max - min + 1));
  }

  private static double getRandom(double min, double max, int scale) {
    return BigDecimal.valueOf(min + (Math.random() * (max - min + 1))).setScale(scale, RoundingMode.UP).doubleValue();
  }
	  
	  public static void print(String fileName) throws FileNotFoundException, UnsupportedEncodingException {
	    //ArrayList<Order> testOrders = new ArrayList<Order>(10000);
	    //TestFileReadUtility.getOrdersFromFile("testcase"+System.getProperty("file.separator")+"10000_1548307577700.txt", testOrders);
	    
	    final PrintWriter writer = new PrintWriter(fileName,"UTF-8");
	    for (int i = 0; i < 5000; i++) {
	      //final Order order = testOrders.get(i);

          OrdType type = getRandom(0,4, 0) > 2? OrdType.LIMIT: OrdType.MARKET;
          Side side = getRandom(0,4, 0) > 2? Side.BUY: Side.SELL;
          double price = OrdType.MARKET == type? 0d: getRandom(173, 182.0, 0);
          long qty = 100L * getRandom(1, 20);
	      final FIXMessage message = new FIXMessage();
	      message.setFixVersion(FIXConst.FIXVersion.FIX_4_4);
	      message.setMessageType(FIXMessageType.NEW_ORDER);
	      message.setSenderCompId("Trader"+getRandom(1, 20));
	      message.setTargetCompId("TS");
	      message.setTag(FIXConst.TAG_CLIENT_ORDER_ID, System.nanoTime()+""+i);
	      message.setTag(FIXConst.TAG_SYMBOL, "stock1");
	      message.setTag(FIXConst.TAG_ORDER_SIDE, side == jordermatching.core.Order.Side.BUY? FIXConst.ORDER_SIDE_BUY: FIXConst.ORDER_SIDE_SELL);
	      message.setTag(FIXConst.TAG_ORDER_QUANTITY, qty); 
	      final boolean isLimit = type == OrdType.LIMIT;
	      message.setTag(FIXConst.TAG_ORDER_TYPE, isLimit? FIXConst.ORDER_TYPE_LIMIT: FIXConst.ORDER_TYPE_MARKET); 
	      if (isLimit) {
	        message.setTag(FIXConst.TAG_ORDER_PRICE, price);
	      }
	      writer.println(message.toString());
	    }
	    writer.close();
	  }
	  
	  private static void generateFile() throws FileNotFoundException, UnsupportedEncodingException {
	    final String fileName = "testcase/fix_5000_"+System.currentTimeMillis()+".txt";
	    print(fileName);
	  }

	  public static void main(String[] args) throws IOException {
		  generateFile();
	  }
}
