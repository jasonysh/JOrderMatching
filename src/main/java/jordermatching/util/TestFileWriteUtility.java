package jordermatching.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

import jordermatching.core.Order;
import jordermatching.core.Order.OrdType;
import jordermatching.core.Order.Side;

public class TestFileWriteUtility {

	  private static int getRandom(int min, int max) {
	    return min + (int)(Math.random() * (max - min + 1));
	  }

	  private static double getRandom(double min, double max, int scale) {
	    return BigDecimal.valueOf(min + (Math.random() * (max - min + 1))).setScale(scale, RoundingMode.UP).doubleValue();
	  }
	  
	  public static void print(String fileName, int count) throws FileNotFoundException, UnsupportedEncodingException {
	    final PrintWriter writer = new PrintWriter(fileName,"UTF-8");
	    final String symbol = "stock1";
	    final String targetCompID = "TS";
	    for (int i = 0; i < count; i++) {
          OrdType type = /*getRandom(0,4, 0) > 2? */OrdType.LIMIT;//: OrdType.MARKET;
	      Side side = getRandom(0,4, 0) > 2? Side.BUY: Side.SELL;
	      double price = OrdType.MARKET == type? 0d:
	          Side.BUY == side? getRandom(173.0, 177, 0): getRandom(178, 182.0, 0);
	      long qty = 100L * getRandom(1, 20);
	      writer.println("clientOrderId="+(System.nanoTime()+""+i)+";symbol="+symbol+";sender="+"client"+getRandom(0,4, 0)+";target="+targetCompID
	          +";side="+side+";type="+type+";price="+price+";quantity="+qty);
	    }
	    writer.close();
	  }
	  
	  private static void generateFile(int count) throws FileNotFoundException, UnsupportedEncodingException {
	    final String fileName = "testcase/base_"+count+"_"+System.currentTimeMillis()+".txt";
	    print(fileName, count);
	    final ArrayList<Order> list = new ArrayList<Order>(count);
	    
	    TestFileReadUtility.getOrdersFromFile(fileName, list);
	    System.out.println(fileName);
	    System.out.println(list.size());
	  }

	  public static void main(String[] args) throws IOException {
		  generateFile(10000);
	  }
}
