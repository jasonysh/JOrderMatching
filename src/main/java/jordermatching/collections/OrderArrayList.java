package jordermatching.collections;

import java.util.ArrayList;
import java.util.Iterator;

import jordermatching.core.Order;
import jordermatching.core.Order.Side;

//Not thread-safe, only allow single thread access, sequential access
public class OrderArrayList extends AbstractOrderList {
  private ArrayList<Order> list;
  //private int last;
  private long volume;
  
  public OrderArrayList(Side side, String symbol, double price, int initSize) {
	  super(side, symbol, price);
	  list = new ArrayList<Order>(initSize);
  }
  
  @Override public Order head() { return list.get(0); }
  @Override public Order tail() { return list.size() > 0? list.get(list.size()-1): null; }
  @Override public int size() { return list.size(); }
  @Override public long getVolume() { return volume; }
  @Override
  public void setVolume(final long volume) {
	this.volume = volume;
  }
  
  @Override 
  public void append(final Order order) {
    list.add(order);
    volume += order.getOpenQty();
  }
  
  @Override 
  public void moveToTail(Order order) {
    for (Order _order: list) {
      if (_order == order) {
        list.remove(_order);
        break;
      }
    }
    list.add(order);
  }
  
  @Override 
  public void remove(final Order order) {
    for (Order _order: list) {
      if (_order == order) {
        list.remove(_order);
        break;
      }
    }
    volume -= order.getOpenQty();
  }
/*
  @Override
  public boolean hasNext() {
    if (this.last >= list.size()){
      return false;
    }  
    return true;
  }

  @Override
  public Order next() {
    int _last = last;
    last++;
    return list.get(_last);
  }

  @Override
  public Iterator<Order> iterator() {//Only Quote thread should use this method
    this.last = 0;
    return this;
  }*/
}
