package jfixgateway.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import jfixgateway.benchmark.BenchmarkQuickFIXJAll;
import jordermatching.core.Order;
import quickfix.FieldNotFound;
import quickfix.InvalidMessage;

public class TestFileReadUtility {

	public static ArrayList<String> getMessagesFromFile(String fileName, ArrayList<String> list) {
		final File file = new File(System.getProperty("user.dir")+System.getProperty("file.separator")+fileName);
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String s;
			while ((s = br.readLine()) != null) {
				list.add(s);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return list;
	}

    public static void main(String[] args) throws IOException, FieldNotFound, InvalidMessage {
      ArrayList<String> testOrders = new ArrayList<String>(10);
      TestFileReadUtility.getMessagesFromFile("testcase"+System.getProperty("file.separator")+"fix_4000000_1549673202901.txt", testOrders);

      for (String s: testOrders) {
        Order order = new Order();
        BenchmarkQuickFIXJAll.convertNewOrder(s, order);
      }
    }
}
