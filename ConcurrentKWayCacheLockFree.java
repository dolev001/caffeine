package com.github.benmanes.caffeine.cache.simulator.policy.sketch.tinycache;
import static java.util.stream.Collectors.toSet;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.apache.commons.lang3.StringUtils;

import com.github.benmanes.caffeine.cache.simulator.admission.Admission;
import com.github.benmanes.caffeine.cache.simulator.admission.tinycache.HashFunctionParser;
import com.github.benmanes.caffeine.cache.simulator.policy.PolicyStats;

import com.google.common.base.MoreObjects;

import com.github.benmanes.caffeine.cache.simulator.BasicSettings;
import com.github.benmanes.caffeine.cache.simulator.policy.Policy;
import com.typesafe.config.Config;




/**
 * Concurrent K Way Cache Lock Free -  several eviction policies
 * 
 * @author dolevfel@gmail.com (Dolev Adas)
 */

@SuppressWarnings("PMD.AvoidDollarSigns")
public class ConcurrentKWayCacheLockFree implements Policy { 

	
private final HashFunctionParser hashFunc;
private final int ways; // k length
private final int numberOfSets; // width	
private final Random rnd;

//private final Ticker ticker;
private final PolicyStats policyStats;

private final EvictionPolicy policy;


private  SetNode[] cache ;


public ConcurrentKWayCacheLockFree(Admission admission, EvictionPolicy policy, Config config) {
	this.policyStats = new PolicyStats(admission.format("concorentkway." + policy.label()));
	
	KwaySettings settings = new KwaySettings(config);
	this.policy = policy;
	this.ways = settings.kwayWays();
	this.numberOfSets = (int) Math.ceil(settings.maximumSize() / ways);
	hashFunc = new HashFunctionParser(numberOfSets);
	rnd =new Random(settings.randomSeed());
	
	this.cache = new SetNode[numberOfSets];
	for ( int i=0 ; i<numberOfSets; ++i)
	{
		cache[i]= new SetNode(i,ways);
	}
	 

}

/**
 * Returns all variations of this policy based on the configuration
 * parameters.
 */
public static Set<Policy> policies(Config config, EvictionPolicy policy) {
	BasicSettings settings = new BasicSettings(config);
	return settings.admission().stream().map(admission -> new ConcurrentKWayCacheLockFree(admission, policy, config)).collect(toSet());
}

@Override
public PolicyStats stats() {
	return policyStats;
}

@Override
public void record(long key) {
    policyStats.recordOperation();
	if (contains(key)) {
		policyStats.recordHit();
	} else {
		boolean evicted = add(key);
		policyStats.recordMiss();
		if (evicted) {
			policyStats.recordEviction();
		}
	}	
	
}

//return true if we add the item false otherwise
	private boolean add(long key ) { 
		hashFunc.createHash(key);
		int set =  hashFunc.fpaux.set;
		
		for (int i = 0; i < ways; i++) {
			if ( cache[set].setArray.get(i)==null)	
			{
				Node update = new Node( key ,i, cache[set].readTime());
				 return   cache[set].setArray.compareAndSet( i, null,update) ;
				
			}
		}
		Node victim = policy.select(cache[set].setArray,  rnd);
		// TODO need to implement concurrent tiny lfu
		
	Node update = new Node( key ,victim.index, cache[set].readTime());
	 return   cache[set].setArray.compareAndSet(victim.index , victim,update) ;
		
	}
	
private boolean contains(long key) {	
	hashFunc.createHash(key);
	int set =  hashFunc.fpaux.set;
	for (int i = 0; i < ways; i++) {
		if ( cache[set].setArray.get(i)!=null)	
		{
			//Node n = (Node) cache[set].setArray.get(i);
			Node n =  cache[set].setArray.get(i);
			if ( n.key == key)
			{
				Node update = new Node((Node) cache[set].setArray.get(i));
				update.frequency++;
				update.accessTime= cache[set].readTime();
				// TODO i am not sure if this is correct because i am using n which is a reference to the node , so the cas will always succeed
				// even if n got change in the middle
				// but on the other hand if n got changed so the address will change too so the cas will not succeed i think .. 
				
				// TODO i can do it faster for FIFO and RAND , because they do not need to modify the node. i need to do it in another function 
				if(  cache[set].setArray.compareAndSet( i, n,update))  
				{
					return true;
				}
				else{
					return false;
				}
				
			}
		
		}
	}
	return  false;
	
}



static final class KwaySettings extends BasicSettings {
    public KwaySettings(Config config) {
      super(config);
    }
    public int kwayWays() {
      return config().getInt("kway.ways");
    }  
  }


/** The replacement policy. form Ben in sampling policy */
public enum EvictionPolicy {
	/** Evicts entries based on insertion order. */
	FIFO {
		@Override
		public Node select(AtomicReferenceArray<Node> setArray, Random random) {
			Node min = setArray.get(0);
			for (int i = 0; i < setArray.length(); i++) {
				if(setArray.get(i).insertionTime < min.insertionTime)
				{
					min = setArray.get(i);
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
		public Node select(AtomicReferenceArray<Node> setArray, Random random) {
			Node min = setArray.get(0);
			for (int i = 0; i < setArray.length(); i++) {
				if(setArray.get(i).accessTime < min.accessTime)
				{
					min = setArray.get(i);
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
		public Node select(AtomicReferenceArray<Node> setArray, Random random) {
			Node max = setArray.get(0);
			for (int i = 0; i < setArray.length(); i++) {
				if(setArray.get(i).accessTime > max.accessTime)
				{
					max = setArray.get(i);
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
		public Node select(AtomicReferenceArray<Node> setArray, Random random) {
			Node min = setArray.get(0);
			for (int i = 0; i < setArray.length(); i++) {
				if(setArray.get(i).frequency < min.frequency)
				{
					min = setArray.get(i);
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
		public Node select(AtomicReferenceArray<Node> setArray, Random random)  {
			Node max = setArray.get(0);
			for (int i = 0; i < setArray.length(); i++) {
				if(setArray.get(i).frequency > max.frequency)
				{
					max = setArray.get(i);
				}

			}
			return max;
		}

	},

	/** Evicts a random entry. */
	RANDOM {
		@Override
		public Node select(AtomicReferenceArray<Node> setArray, Random random) {

			int victimOffset = random.nextInt(setArray.length());
			return 	  (Node) setArray.get( victimOffset);

		}
	};
	/** Determines which node to evict. */
	 abstract  Node select(AtomicReferenceArray<Node> setArray, Random random);
	 
	 
	 public String label() {
			return StringUtils.capitalize(name().toLowerCase());
		}
}

 static final class Node {
	private final long key;
	private final long insertionTime;
	private long accessTime;
	private int frequency;
	private int index;

	/** Creates a new node. */
	public Node(long key, int index, long tick) {
		this.insertionTime = tick;
		this.accessTime = tick;
		this.index = index;
		this.key = key;			
	}
	
	public Node( Node n)
	{
		this.key = n.key;
		this.insertionTime=n.insertionTime;
		this.accessTime=n.accessTime;
		this.frequency=n.frequency;
		this.index= n.index;	
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("key", key).add("index", index).toString();
	}

}
 
 // this is the  set - array of nodes with the length of K . 
 static final class SetNode {
	 int setIndex;
	 private AtomicLong time;
	 private AtomicReferenceArray<Node> setArray;
	 
	 public SetNode( int setIndex , int k )
	 {
		 this.setIndex=setIndex;
		 this.time=new AtomicLong (0);
		 setArray = new AtomicReferenceArray<Node>(k);
	 }
	 public long readTime()
	 {
		 return 	time.incrementAndGet();	 
	 }
	 
 }
 

		
	
	
	
	
	
	
	

}
