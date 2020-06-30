package jordermatching.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import jordermatching.core.Order;

public class ParseJMHResult {
  public static class JMHResult {
    String benchmark = "XXXXXXXXXXXx";
    double result;
    double p000;
    double p050;
    double p090;
    double p095;
    double p099;
    double p0999;
    double p09999;
    double p100;
    double gcallocrate;
    double gcallocratenorm;
    double gcchurnPS_Eden_Space;
    double gcchurnPS_Eden_Spacenorm;
    double gcchurnPS_Survivor_Space;
    double gcchurnPS_Survivor_Spacenorm;
    double gccount;
    double gctime;
  }
  
  private static HashMap<String, JMHResult> getMap(String filename) {
    HashMap<String, JMHResult> map = new HashMap<String, JMHResult>();
    final File file = new File(System.getProperty("user.dir")+System.getProperty("file.separator")+filename);
    try {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String s;
        JMHResult result = new JMHResult();
        boolean start = false;
        while ((s = br.readLine()) != null) {
          if (start) {
            while (s.contains("  ")) {
              s = s.replace("  ", " ");
            }
            if (!s.contains("·")) {
            }
            String[] ss = s.split(" ");
            if (ss.length > 3 && !s.contains("saved")) {
              if (!s.contains(result.benchmark)) {
                result = new JMHResult();
                result.benchmark = ss[0];
                
                map.put(result.benchmark, result);
              }
              int idx = ss.length==4?2:3;
              if (s.contains("gc.alloc.rate.norm")) {
                result.gcallocratenorm = Double.parseDouble(ss[idx]);
              } else if (s.contains("gc.alloc.rate")) {
                result.gcallocrate = Double.parseDouble(ss[idx]);
              } else if (s.contains("gc.churn.PS_Eden_Space.norm")) {
                result.gcchurnPS_Eden_Spacenorm = Double.parseDouble(ss[idx]);
              } else if (s.contains("gc.churn.PS_Eden_Space")) {
                result.gcchurnPS_Eden_Space = Double.parseDouble(ss[idx]);
              } else if (s.contains("gc.churn.PS_Survivor_Space.norm")) {
                result.gcchurnPS_Survivor_Spacenorm = Double.parseDouble(ss[idx]);
              } else if (s.contains("gc.churn.PS_Survivor_Space")) {
                result.gcchurnPS_Survivor_Space = Double.parseDouble(ss[idx]);
              } else if (s.contains("gc.count")) {
                result.gccount = Double.parseDouble(ss[idx]);
              } else if (s.contains("gc.time")) {
                result.gctime = Double.parseDouble(ss[idx]);
              } else if (s.contains("p0.00")) {
                result.p000 = Double.parseDouble(ss[idx]);
              } else if (s.contains("p0.50")) {
                result.p050 = Double.parseDouble(ss[idx]);
              } else if (s.contains("p0.9999")) {
                result.p09999 = Double.parseDouble(ss[idx]);
              } else if (s.contains("p0.999")) {
                result.p0999 = Double.parseDouble(ss[idx]);
              } else if (s.contains("p0.99")) {
                result.p099 = Double.parseDouble(ss[idx]);
              } else if (s.contains("p0.90")) {
                result.p090 = Double.parseDouble(ss[idx]);
              } else if (s.contains("p0.95")) {
                result.p095 = Double.parseDouble(ss[idx]);
              } else if (s.contains("p100")) {
                result.p100 = Double.parseDouble(ss[idx]);
              } else {
                result.result = Double.parseDouble(ss[idx]);
              }
//              System.out.println(s);
//              System.out.println(ss[0]+","+ss[1]+","+ss[idx]);
            }
            
          }
          if (s.contains("Benchmark           ")) {
            start = true;
          }
        }
        br.close();
    } catch (IOException e) {
        e.printStackTrace();
    }
    return map;
  }
  
  public static void convertTxtToCSV(String filename) throws FileNotFoundException {
    PrintWriter pw = new PrintWriter(new FileOutputStream(filename+".csv", false));
    HashMap<String, JMHResult> map = getMap(filename+".txt");
    pw.println("benchmark,0,50,90,95,99,99.9,99.99,100,result,gc.alloc.rate.norm,gc.time");
    String dl = ",";
    for (JMHResult e: map.values()) {
      pw.println(e.benchmark.replace("Benchmark", "")
          +dl+e.p000+dl+e.p050+dl+e.p090+dl+e.p095+dl+e.p099+dl+e.p0999+dl+e.p09999
          +dl+e.result+dl+e.p100+dl+e.gcallocratenorm+dl+e.gctime);
    }
    pw.flush();
    pw.close();
  }
  
  public static void main(String[] args) throws FileNotFoundException {
    //2019-01-10 22-45-44_BenchmarkCollection_ModifyOrder
    //2019-01-10 00-18-06_BenchmarkFIXGateway
    //2019-01-10 23-02-15_BenchmarkCollection_ModifyOrder
    //2019-01-10 23-16-21_BenchmarkCollection_ModifyOrder
    //2019-01-10 23-29-28_BenchmarkCollection_CancelOrder
    //2019-01-10 23-50-39_BenchmarkCollection_CancelOrder
    //2019-01-11 00-39-58_BenchmarkCollection_ModifyOrder
    //2019-01-11 20-01-52_BenchmarkCollection_ModifyOrder
    //2019-01-11 20-55-25_BenchmarkCollection_OrderListType
    String filename = "benchmark\\2019-01-12 09-30-28_BenchmarkCollection_MatchListType";
    convertTxtToCSV(filename);
  }
}
