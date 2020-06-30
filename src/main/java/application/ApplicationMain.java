package application;

import java.io.IOException;

import jfixgateway.FIXServerConfiguration;
import jfixgateway.message.FIXMessage;
import jitemhandler.ArrayBlockingQueueItemQueue;
import jitemhandler.IElementQueue;
import jordermatching.core.OrderMatchingEngine;

public class ApplicationMain {
    public static void main(String[] args) throws IOException {
        FIXServerConfiguration config = new FIXServerConfiguration();
        config.setFixCompId("TS");
        config.setFixReceiveCacheSize(1024);
        config.setFixPort(8323);
        IElementQueue<FIXMessage> handler = new ArrayBlockingQueueItemQueue<FIXMessage>();
        OrderMatchingEngine engine = new OrderMatchingEngine();
        FIXTradeApplication tradeServer = new FIXTradeApplication(engine, config, handler, true);
        tradeServer.start();

        config.setFixPort(8324);
        FIXQuoteApplication quoteServer = new FIXQuoteApplication(engine, config, true);
        quoteServer.start();
    }
}
