package jfixgateway;

import java.util.List;

import jfixgateway.message.FIXMessage;

public class FIXReceivableCache {
    private int size = 0;
    private byte[] buffer;
    
    private boolean verbose;
    
    public FIXReceivableCache(int capacity) {
        size = 0;
        buffer = new byte[capacity];
    }

    public int getSize() { return size; }
    public byte[] getBuffer() { return buffer; }
    public long getCapacity() { return buffer.length; }
    public int getRemainingBufferSize() {  return buffer.length - size; }
    public void incrementSize(long _size) { size += _size; }

    public void parse(List<FIXMessage> messages) {
        FIXUtility.print(buffer);
        final int ret = FIXParser.parseMultipleMessagesFromBuffer(buffer, size, messages);
        if (messages.size() > 0) {
            final int remainBytesStartIdx = ret;
            final int remainBytesEndIdx = size-1;
            
            if (remainBytesEndIdx > remainBytesStartIdx) {
                shiftBufferToStart(remainBytesStartIdx, remainBytesEndIdx);
            } else {
                size = 0;
            }
            
            for (FIXMessage msg: messages) {
              System.out.println("message: "+msg);
            }
        }
    }
    
    private void shiftBufferToStart(int startIndex, int endIndex) {
        int j = 0;
        for (int i = startIndex; i <= endIndex; i++) {
            buffer[j++] = buffer[i];
        }
        size = endIndex - startIndex + 1;
    }
}
