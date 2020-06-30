package jordermatching.collections;

import java.util.Iterator;

import jordermatching.core.Order;
import jordermatching.core.Order.Side;

public abstract class AbstractOrderList/* implements Iterable<Order>, Iterator<Order>*/ {
  
  protected final Side side;
  protected final String symbol;
  protected final double price;
  public AbstractOrderList(Side side, String symbol, double price) {
	  this.side = side;
	  this.price = price;
	  this.symbol = symbol;
  }
  
  public double getPrice() {
	  return price;
  }
  
  public abstract Order head();
  public abstract Order tail();
  public abstract int size();
  public abstract long getVolume();
  
  public abstract void setVolume(final long volume);
  public abstract void append(final Order order);
  public abstract void moveToTail(Order order);
  public abstract void remove(final Order order);
  /*
  public abstract boolean hasNext();
  public abstract Order next();
  public abstract Iterator<Order> iterator();
  
  private void debug() {
    //valid volume
    long _v = 0L;
    int _s = 0;
    for (Order o : this) {
      _v += o.getOpenQty();
      _s++;
      if (o.isClosed()) {
        throw new RuntimeException("order should be removed from the order book");
      }
    }
    if (getVolume() != _v) {
      throw new RuntimeException("Wrong volume, cached:"+getVolume()+", real:"+_v);
    }

    if (size() != _s) {
      throw new RuntimeException("Wrong order number, cached:"+size()+", real:"+_s);
    }
  }*/
  
  public String toString() {
    //debug();
    StringBuilder outString = new StringBuilder().append(String.valueOf(getVolume()))
        .append("\t#").append(size()).append("\t$").append(price);
    /*for (Order o : this) {
        outString.append("| ").append(o.toString());
    }*/
    return outString.toString();
  }
}
