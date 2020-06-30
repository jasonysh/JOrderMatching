package jfixgateway;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class FIXSequenceStore {
    public static String getSequenceStoreFileName(String senderCompId, String targetCompid) {
        return new StringBuilder().append(senderCompId).append("_").append(targetCompid).append("_sequence.txt").toString();
    }

    public static String loadFromSequenceStore(String senderCompId, String targetCompid)
            throws FileNotFoundException, IOException {
        final File file = new File(getSequenceStoreFileName(senderCompId, targetCompid));
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String fileContent = br.readLine();
                return fileContent == null ? "1,0" : fileContent;
            }
        }
        return "1,0";
    }

    public static void updateSequenceStore(String senderCompId, String targetCompid, int outgoingSeqNum,
            int incomingSeqNum) throws FileNotFoundException {
        final String fileName = getSequenceStoreFileName(senderCompId, targetCompid);

        try (PrintWriter pw = new PrintWriter(new FileOutputStream(fileName, false))) {
            pw.println(outgoingSeqNum + "," + incomingSeqNum);
        }
    }
}
