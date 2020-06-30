package jordermatching.marketdata;

import java.util.concurrent.atomic.AtomicReference;

//Multiple reader, one writer scenario
public class AtomicMarketDataStore implements IMarketDataStore {
	private final AtomicReference<BasicMarketDataStore> md = new AtomicReference<>(new BasicMarketDataStore());
	private BasicMarketDataStore alternateStore = new BasicMarketDataStore();

	@Override
	public int set(double price0, long volume0, double price1, long volume1, double price2, long volume2,
			double price3, long volume3, double price4, long volume4, double price5, long volume5, double price6,
			long volume6, double price7, long volume7, double price8, long volume8, double price9, long volume9) {

        int tries = 0;
        BasicMarketDataStore currentMD = null;

        alternateStore.set(price0, volume0, price1, volume1, price2, volume2,
        		price3, volume3, price4, volume4, price5, volume5, price6, volume6, price7, volume7,
        		price8, volume8, price9, volume9);
        do
        {
            ++tries;
            currentMD = md.get();
        }
        while (!md.compareAndSet(currentMD, alternateStore));
        alternateStore = currentMD;

        return tries;
	}
	
	@Override
	public void read(double[] _prices, long[] _volumes) {
        final BasicMarketDataStore currentMD = md.get();
        currentMD.read(_prices, _volumes);
	}
}
