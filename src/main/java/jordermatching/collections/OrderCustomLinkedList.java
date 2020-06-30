package jordermatching.collections;

import java.util.Iterator;

import jordermatching.core.Order;
import jordermatching.core.Order.Side;


//Not thread-safe, only allow single thread access, sequential access
//Refer to https://github.com/DrAshBooth/JavaLOB/blob/master/src/lob/OrderList.java
public class OrderCustomLinkedList extends AbstractOrderList {
  private Order head, tail, last;
  private int size = 0;
  private long volume = 0;
  
  public OrderCustomLinkedList(Side side, String symbol, double price) {
	  super(side, symbol, price);
  }
  
  @Override public Order head() { return head; }
  @Override public Order tail() { return tail; }
  @Override public int size() { return size; }
  @Override public long getVolume() { return volume; }
  @Override public void setVolume(final long volume) { this.volume = volume; }
  
  @Override 
  public void append(final Order order) {
    if (head == null) {
      head = order;
    }
    if (tail != null) {
      tail.setNextOrder(order);
    }
    tail = order;
    size++;
    volume += order.getOpenQty();
  }
  
  @Override 
  public void moveToTail(Order order) {
    final Order prevOrder = order.getPrevOrder();
    final Order nextOrder = order.getNextOrder();
    if (prevOrder == null) {
      head = nextOrder;
      nextOrder.setPrevOrder(null);
    } else {
      prevOrder.setNextOrder(nextOrder);
      nextOrder.setPrevOrder(prevOrder);
    }
    order.setPrevOrder(tail);
    order.setNextOrder(null);
    tail.setNextOrder(order);
    tail = order;
  }
  
  @Override 
  public void remove(final Order order) {
    final Order prevOrder = order.getPrevOrder();
    final Order nextOrder = order.getNextOrder();
    if (prevOrder == null) {
      head = nextOrder;
    } else {
      prevOrder.setNextOrder(nextOrder);
      order.setPrevOrder(null);
    }
    
    if (nextOrder == null) {
      tail = prevOrder;
    } else {
      nextOrder.setPrevOrder(prevOrder);
      order.setNextOrder(null);
    }
    
    volume -= order.getOpenQty();
    size--;
  }
/*
  @Override
  public boolean hasNext() {
    if (this.last==null){
      return false;
    }  
    return true;
  }

  @Override
  public Order next() {
    Order returnVal = this.last;
    this.last = this.last.getNextOrder();
    return returnVal;
  }

  @Override
  public Iterator<Order> iterator() {//Only Quote thread should use this method
    this.last = head;
    return this;
  }*/
}
