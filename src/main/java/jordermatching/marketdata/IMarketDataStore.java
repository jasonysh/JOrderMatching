package jordermatching.marketdata;

//Multiple reader, one writer scenario
public interface IMarketDataStore {
  
	public int set(double price0, long volume0,
			double price1, long volume1,
			double price2, long volume2,
			double price3, long volume3,
			double price4, long volume4,
			double price5, long volume5,
			double price6, long volume6,
			double price7, long volume7,
			double price8, long volume8,
			double price9, long volume9);

	public void read(double[] prices, long[] volumes);
}
