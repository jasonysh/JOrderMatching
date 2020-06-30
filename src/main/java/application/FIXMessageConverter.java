package application;

import jfixgateway.FIXConst;
import jfixgateway.message.FIXMessage;
import jordermatching.core.Order;
import jordermatching.core.Order.OrdType;
import jordermatching.core.Order.Side;

public class FIXMessageConverter {

    public static void convertNewOrder(FIXMessage message, Order order) {
        final String sender = message.getSenderCompId();
        final String target = message.getTargetCompId();
        final String symbol = message.getValue(FIXConst.TAG_SYMBOL);
        final String clientOrderId = message.getValue(FIXConst.TAG_CLIENT_ORDER_ID);
        final char fixOrderType = message.getValueAsChar(FIXConst.TAG_ORDER_TYPE);
        final OrdType type = fixOrderType == FIXConst.ORDER_TYPE_LIMIT ? OrdType.LIMIT : OrdType.MARKET;
        final Side side = message.getValueAsChar(FIXConst.TAG_ORDER_SIDE) == FIXConst.ORDER_SIDE_BUY ? Side.BUY : Side.SELL;
        final long quantity = message.getValueAsLong(FIXConst.TAG_ORDER_QUANTITY);
        final double price = message.hasTag(FIXConst.TAG_ORDER_PRICE)
                ? message.getTagValueAsDouble(FIXConst.TAG_ORDER_PRICE)
                : 0d;

        order.set(clientOrderId, symbol, sender, target, side, type, price, quantity);
    }
}
