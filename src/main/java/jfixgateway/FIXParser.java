package jfixgateway;

import java.util.List;

import jfixgateway.message.FIXMessage;

public class FIXParser {
    public static FIXMessage parseFromString(String input, StringBuilder tag, StringBuilder value) {
        /*final FIXMessage message = new FIXMessage();
        for (final String pair : input.split(String.valueOf(FIXConst.FIX_DELIMITER))) {
            if (pair.length() == 0) {
                continue;
            }
            final String[] tokens = pair.split(String.valueOf(FIXConst.FIX_EQUALS));
            message.setTag(Integer.parseInt(tokens[0]), tokens[1]);
        }
        message.initialiseHeader();
        return message;*/
        byte[] b = input.getBytes();
        return parseFromBuffer(b, b.length, tag, value);
    }

    public static FIXMessage parseFromBuffer(byte[] input, int length, StringBuilder tag, StringBuilder value) {
      tag.setLength(0);
      value.setLength(0);
        StringBuilder target = tag;

        final FIXMessage message = new FIXMessage();
        for (int i = 0; i < length; i++) {
            final char iter = (char)input[i];

            if (iter == FIXConst.FIX_EQUALS) {
                target = value;
            } else if (iter == FIXConst.FIX_DELIMITER) {
                target = tag;
                message.setTag(tag.toString(), value.toString());
                tag.setLength(0);
                value.setLength(0);
            } else {
                target.append(iter);
            }
        }
        return message;
    }

    public static FIXMessage parseFromMultipleBuffers(byte[] buffer1, int length1, byte[] buffer2, int length2) {
        final StringBuilder tag = new StringBuilder();
        final StringBuilder value = new StringBuilder();
        
        StringBuilder target = tag;
        byte[] buffer = buffer1;
        int length = length1;
        
        FIXMessage message = new FIXMessage();
        for (int i = 0; i < length; i++) {
            final char c = (char) buffer[i];

            if (c == FIXConst.FIX_EQUALS) {
                target = value;
            } else if (c == FIXConst.FIX_DELIMITER) {
                target = tag;
                message.setTag(tag.toString(), value.toString());
                tag.setLength(0);
                value.setLength(0);
            } else {
                target.append(c);
            }

            if (i == length - 1) {
                if (buffer == buffer2) {// Switched to reading other buffer already
                    break;
                } else {
                    i = -1;
                    length = length2;
                    buffer = buffer2;
                }
            }
        }
        return message;
    }

    public static int parseMultipleMessagesFromBuffer(byte[] buffer, int bufferSize, List<FIXMessage> messages) {
        final StringBuilder tag = new StringBuilder();
        final StringBuilder value = new StringBuilder();
        FIXMessage message = null;
        
        StringBuilder target = tag;
        boolean fixMessageStarted = false;
        boolean lastTagOfMessage = false;

        int remainingBytesStartIndex = 0;
        for (int i = 0; i < bufferSize; i++) {
            final char c = (char)buffer[i];
            if (!fixMessageStarted) {
                if ((i == 0 || buffer[i-1] == FIXConst.FIX_DELIMITER) && c == '8'/*FIX Version tag*/ && buffer[i + 1] == FIXConst.FIX_EQUALS) {
                    target.append(c);
                    target = value;
                    fixMessageStarted = true;
                    ++i;
                } else {
                    continue;
                }
            } else {
                if (c == FIXConst.FIX_EQUALS) {
                    target = value;
                    if (tag.length() == 2 && tag.toString().equals("10"/*CheckSum tag*/)) {
                        lastTagOfMessage = true;
                    }
                } else if (c == FIXConst.FIX_DELIMITER) {
                    target = tag;
                    if (message == null) {
                        message = new FIXMessage();
                    }
                    message.setTag(tag.toString(), value.toString());
                    tag.setLength(0);
                    value.setLength(0);
    
                    if (lastTagOfMessage) {
                        messages.add(message);
                        remainingBytesStartIndex = i + 1;
                        message = null;
                        lastTagOfMessage = false;
                        fixMessageStarted = false;
                    }
                } else {
                    target.append(c);
                }
            }
        }
        return remainingBytesStartIndex;
    }
}
