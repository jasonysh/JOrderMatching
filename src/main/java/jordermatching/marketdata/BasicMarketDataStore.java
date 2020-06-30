package jordermatching.marketdata;

public class BasicMarketDataStore implements IMarketDataStore {
	private double price0, price1, price2, price3, price4, price5, price6, price7, price8, price9;
	private long volume0, volume1, volume2, volume3, volume4, volume5, volume6, volume7, volume8, volume9;

	@Override
	public int set(double price0, long volume0, double price1, long volume1, double price2, long volume2, double price3,
			long volume3, double price4, long volume4, double price5, long volume5, double price6, long volume6,
			double price7, long volume7, double price8, long volume8, double price9, long volume9) {
		this.price0 = price0;
		this.price1 = price1;
		this.price2 = price2;
		this.price3 = price3;
		this.price4 = price4;
		this.price5 = price5;
		this.price6 = price6;
		this.price7 = price7;
		this.price8 = price8;
		this.price9 = price9;
		this.volume0 = volume0;
		this.volume1 = volume1;
		this.volume2 = volume2;
		this.volume3 = volume3;
		this.volume4 = volume4;
		this.volume5 = volume5;
		this.volume6 = volume6;
		this.volume7 = volume7;
		this.volume8 = volume8;
		this.volume9 = volume9;
		return 1;
	}

	@Override
	public void read(double[] _prices, long[] _volumes) {
		_prices[0] = price0;
		_prices[1] = price1;
		_prices[2] = price2;
		_prices[3] = price3;
		_prices[4] = price4;
		_prices[5] = price5;
		_prices[6] = price6;
		_prices[7] = price7;
		_prices[8] = price8;
		_prices[9] = price9;
		_volumes[0] = volume0;
		_volumes[1] = volume1;
		_volumes[2] = volume2;
		_volumes[3] = volume3;
		_volumes[4] = volume4;
		_volumes[5] = volume5;
		_volumes[6] = volume6;
		_volumes[7] = volume7;
		_volumes[8] = volume8;
		_volumes[9] = volume9;
	}
}
