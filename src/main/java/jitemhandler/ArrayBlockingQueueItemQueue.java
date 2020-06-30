package jitemhandler;

import java.util.concurrent.ArrayBlockingQueue;

public class ArrayBlockingQueueItemQueue<T> implements IElementQueue<T> {
  private ArrayBlockingQueue<T> queue = new ArrayBlockingQueue<T>(1000);
  
  public void produce(T e) {
    queue.add(e);
  }
  
  public T consume() {
    return queue.poll();
  }
}
