package jfixgateway.message;

import java.io.IOException;

public interface IFIXMessage {
  public void send() throws IOException;
}
