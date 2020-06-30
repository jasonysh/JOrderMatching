package jordermatching.collections;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import jordermatching.core.Order;
import jordermatching.core.Order.Side;

//Not thread-safe, only allow single thread access
public class OrderLinkedList extends AbstractOrderList {
  private LinkedList<Order> list = new LinkedList<Order>();
  //private int last;
  private long volume;
  
  public OrderLinkedList(Side side, String symbol, double price) {
	  super(side, symbol, price);
  }
  
  @Override public Order head() { return list.getFirst(); }
  @Override public Order tail() { return list.getLast(); }
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
    if (this.last >= list.size()) {
        throw new NoSuchElementException();
    }
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
