package jitemhandler;

public interface IElementQueue<T> {
  public void produce(T e);
  public T consume();
}
