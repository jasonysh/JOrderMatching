package application;

import jfixgateway.AbstractFIXServer;
import jfixgateway.FIXServerConfiguration;
import jfixgateway.FIXSession;
import jfixgateway.message.FIXMessage;

public class FIXServer extends AbstractFIXServer {

  public FIXServer(FIXServerConfiguration configuration, boolean _verbose) {
    super(configuration, _verbose);
  }

  @Override public void onFixLogon(FIXSession session) {
  }

  @Override public void onFixLogoff(FIXSession session) {
  }

  @Override public void onFixMessage(FIXMessage incomingMessage, FIXSession session) {
  }

  @Override public void onFixError(String fixErrorMessage, FIXSession session) {
  }

}

