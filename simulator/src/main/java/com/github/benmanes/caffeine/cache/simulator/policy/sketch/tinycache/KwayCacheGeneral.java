package com.github.benmanes.caffeine.cache.simulator.policy.sketch.tinycache;

import static java.util.stream.Collectors.toSet;

/*
 * K way cache several eviction polices 
 */
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.github.benmanes.caffeine.cache.simulator.admission.Admission;
import com.github.benmanes.caffeine.cache.simulator.admission.Admittor;
import com.github.benmanes.caffeine.cache.simulator.admission.tinycache.HashFunctionParser;
import com.github.benmanes.caffeine.cache.simulator.policy.PolicyStats;
import com.google.common.base.MoreObjects;
import com.google.common.base.Ticker;
import com.github.benmanes.caffeine.cache.simulator.BasicSettings;
import com.github.benmanes.caffeine.cache.simulator.policy.Policy;
import com.typesafe.config.Config;

/**
 * A K way cache with K way cache several eviction policies
 * 
 * @author dolevfel@gmail.com (Dolev Adas)
 */
@SuppressWarnings("PMD.AvoidDollarSigns")
public final class KwayCacheGeneral implements Policy {
	private final HashFunctionParser hashFunc;
	private final int ways; // k  --> 
	private final int numberOfSets; //
	private final Random rnd;

	private final Ticker ticker;
	private final PolicyStats policyStats;
	private final Admittor admittor;
	private final EvictionPolicy policy;
	// private final Sample sampleStrategy;
	private final Node[] cache;

	public KwayCacheGeneral(Admission admission, EvictionPolicy policy, Config config) {
		this.policyStats = new PolicyStats(admission.format("kway." + policy.label()));
		this.admittor = admission.from(config, policyStats);
		KwaySettings settings = new KwaySettings(config);
		this.policy = policy;
		this.ticker = new CountTicker();
		this.ways = settings.kwayWays();
		this.numberOfSets = (int) Math.ceil(settings.maximumSize() / ways);
		hashFunc = new HashFunctionParser(numberOfSets);
		rnd =new Random(settings.randomSeed());
		cache = new Node[numberOfSets * ways];

	}

	/**
	 * Returns all variations of this policy based on the configuration
	 * parameters.
	 */
	public static Set<Policy> policies(Config config, EvictionPolicy policy) {
		BasicSettings settings = new BasicSettings(config);
		return settings.admission().stream().map(admission -> new KwayCacheGeneral(admission, policy, config)).collect(toSet());
	}

	@Override
	public PolicyStats stats() {
		return policyStats;
	}

	private boolean contains(long item) {
		hashFunc.createHash(item);
		long now = ticker.read();
		int offset = this.ways * hashFunc.fpaux.set;
		for (int i = 0; i < ways; i++) {
			if (cache[offset + i] != null)
			{
				// change Key!!!
				//if (cache[offset + i].key == hashFunc.fpaux.value) 
				if (cache[offset + i].key == item) 
				{
					cache[offset + i].accessTime = now;
					cache[offset + i].frequency++;
					return true;
				}
			}
		}

		return false;

	}

	// return false if we add an item without throwing someone, true otherwise
	private boolean addItem(long item) {
		hashFunc.createHash(item);
		int offset = this.ways * hashFunc.fpaux.set;
		long now = ticker.read();
		for (int i = 0; i < ways; i++) {
			if (cache[offset + i] == null) {
				// change Key!!!
				//Node node = new Node(hashFunc.fpaux.value, i, now);
				Node node = new Node(item, i, now);

				cache[offset + i] = node;
				return false;
			}
		}
		// replace an old item with the new one

		Node victim = policy.select(cache, offset, ways, rnd);
		//if (admittor.admit(hashFunc.fpaux.value, victim.key)) {
		if (admittor.admit(item, victim.key)) {

			// if the candidate should be added and the victim removed due to eviction
			// add new ,remove old
			
			// change Key!!!
			//Node node = new Node(hashFunc.fpaux.value, victim.index, now);
			Node node = new Node(item, victim.index, now);

			cache[offset + victim.index] = node;
			
		}
		// else do nothing - keep old
		return true;
	}
	@Override
	public void record(long key) {
		  admittor.record(key);
	      policyStats.recordOperation();
		if (contains(key)) {
			policyStats.recordHit();
		} else {
			boolean evicted = addItem(key);
			policyStats.recordMiss();
			if (evicted) {
				policyStats.recordEviction();
			}
		}
	}

	/** A node form Ben to save the meta data */
	final class Node {
		final long key;
		private final long insertionTime;
		long accessTime;
		int frequency;
		int index;

		/** Creates a new node. */
		public Node(long key, int index, long tick) {
			this.insertionTime = tick;
			this.accessTime = tick;
			this.index = index;
			this.key = key;
			this.frequency=1;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this).add("key", key).add("index", index).toString();
		}
	}

	/**
	 * An incrementing time source. This is used because calling
	 * {@link System#nanoTime()} and {@link System#currentTimeMillis()} are
	 * expensive operations. For sampling purposes the access time relative to
	 * other entries is needed, which a counter serves equally well as a true
	 * time source.
	 */
	final class CountTicker extends Ticker {
		private long tick;

		@Override
		public long read() {
			return ++tick;
		}
	}


	 static final class KwaySettings extends BasicSettings {
		    public KwaySettings(Config config) {
		      super(config);
		    }
		    public int kwayWays() {
		      return config().getInt("kway.ways");
		    }  
		  }
	 

	/** The replacement policy. form Ben is sampling policy */
	public enum EvictionPolicy {
		/** Evicts entries based on insertion order. */
		FIFO {
			@Override
			Node select(Node[] set, int start, int end, Random random) {
				Node min = set[start];
				for (int i = 0; i < end; i++) {
					if (set[start + i].insertionTime < min.insertionTime) {
						min = set[start + i];
					}

				}
				return min;

			}
		},

		/**
		 * Evicts entries based on how recently they are used, with the least
		 * recent evicted first.
		 */
		LRU {
			@Override
			Node select(Node[] set, int start, int end, Random random) {
				Node min = set[start];
				for (int i = 0; i < end; i++) {
					if (set[start + i].accessTime < min.accessTime) {
						min = set[start + i];
					}

				}
				return min;
			}

		},

		/**
		 * Evicts entries based on how recently they are used, with the least
		 * recent evicted first.
		 */
		MRU {
			@Override
			Node select(Node[] set, int start, int end, Random random) {
				Node max = set[start];
				for (int i = 0; i > end; i++) {
					if (set[start + i].accessTime > max.accessTime) {
						max = set[start + i];
					}

				}
				return max;
			}

		},

		/**
		 * Evicts entries based on how frequently they are used, with the least
		 * frequent evicted first.
		 */
		LFU {

			@Override
			Node select(Node[] set, int start, int end, Random random) {
				Node min = set[start];
				for (int i = 0; i < end; i++) {
					if (set[start + i].frequency < min.frequency) {
						min = set[start + i];
					}

				}
				return min;
			}

		},

		/**
		 * Evicts entries based on how frequently they are used, with the most
		 * frequent evicted first.
		 */
		MFU {
			@Override
			Node select(Node[] set, int start, int end, Random random) {
				Node max = set[start];
				for (int i = 0; i < end; i++) {
					if (set[start + i].frequency > max.frequency) {
						max = set[start + i];
					}

				}
				return max;
			}

		},

		/** Evicts a random entry. */
		RANDOM {
			@Override
			Node select(Node[] set, int start, int end, Random random) {

				int victimOffset = random.nextInt(end);
				return set[start + victimOffset];

			}
		};

		public String label() {
			return StringUtils.capitalize(name().toLowerCase());
		}

		/** Determines which node to evict. */
		abstract Node select(Node[] set, int start, int end, Random random);
	}

}
