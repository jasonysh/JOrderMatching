package jordermatching.core;

public class ImplementSelector {

	  public static enum OrderListType {CustomLinked, Linked, Array, Array10, Array100, ArrayDeque}
	  
	  public static enum OrderListTreeMapType {/*CustomPrimitive,*/ JDK}
	  
	  public static enum MarketDataStoreType {CompareAndSwap, ReadWriteLock, ReentrantLock, Volatile}
	  
	  public static enum MatchListType { Preallocate, AllocateEveryTime }
}
