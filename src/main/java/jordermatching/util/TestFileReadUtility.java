package jordermatching.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import jordermatching.core.Order;

public class TestFileReadUtility {

	public static ArrayList<Order> getOrdersFromFile(String fileName, ArrayList<Order> list) {
		final File file = new File(System.getProperty("user.dir")+System.getProperty("file.separator")+fileName);
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String s;
			while ((s = br.readLine()) != null) {
				String[] keyEqualValues = s.split(";");
				HashMap<String, String> map = new HashMap<String, String>();
				for (String keyEqualValue : keyEqualValues) {
					String[] keyValue = keyEqualValue.split("=");
					map.put(keyValue[0], keyValue[1]);
				}
				list.add(new Order(map));
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return list;
	}
}
