/*
 * Copyright 2015 Ben Manes. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.benmanes.caffeine.cache.simulator.policy;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.simulator.BasicSettings;
import com.github.benmanes.caffeine.cache.simulator.Simulator.Message;
import com.typesafe.config.Config;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.dispatch.BoundedMessageQueueSemantics;
import akka.dispatch.RequiresMessageQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;

/**
 * An actor that proxies to the page replacement policy.
 *
 * @author ben.manes@gmail.com (Ben Manes)
 */
public final class PolicyActor extends UntypedActor
    implements RequiresMessageQueue<BoundedMessageQueueSemantics> {
  private final Policy policy;
  // new for concurrent
  private ExecutorService executor;
  
  // number of threads 
  private  Integer numberOfTeads;
  
  public PolicyActor(Policy policy) {
    this.policy = requireNonNull(policy);
    
   
    Config config=  getContext().system().settings().config().getConfig("caffeine.simulator");
  ConcurrentSettings settings = new ConcurrentSettings(config);
  numberOfTeads = settings.threads();
  System.out.println( numberOfTeads);
   executor =  Executors.newFixedThreadPool(numberOfTeads);
    
  }

  @Override
  public void onReceive(Object msg) {
    if (msg instanceof LongArrayList) {
      LongArrayList events = (LongArrayList) msg;
      process(events);
    } else if (msg == Message.FINISH) {
      policy.finished();
      getSender().tell(policy.stats(), ActorRef.noSender());
      getContext().stop(getSelf());
    } else if (msg == Message.ERROR) {
      getContext().stop(getSelf());
    } else {
      context().system().log().error("Invalid message: " + msg);
    }
  }

  private void process(LongArrayList events) {
	  
	
	  
    policy.stats().stopwatch().start();
    try {
    	
      for ( int i = 0; i < events.size(); i++) {
    	  
    	 
    	  final int j=i;
    	  // this is where the element get inserted to the cache 
    	  
    	  
    	   Runnable task = ()-> {
    		   policy.record(events.getLong(j));
    	   };
  			executor.execute(task);
  			
  			
       
      }
   // policy.record(events.getLong(i)); // old
          
    } catch (Exception e) {
      context().system().log().error(e, "");
      getSender().tell(Message.ERROR, ActorRef.noSender());
    } finally {
      policy.stats().stopwatch().stop();
     // stop(executor);
    }
  }
  
// severe performance penalty for using finalizers
  /*  @Override
 protected void finalize() throws Throwable {
	   System.out.println("In finalize block");
	  stop(executor); 
      super.finalize();
  }
  */
  
 
  
 
  static final class ConcurrentSettings extends BasicSettings {
	    public ConcurrentSettings(Config config) {
	      super(config);
	    }
	    public int threads() {
	      return config().getInt("concurrent.threads");
	    }  
	  } 
  
  
  private void stop(ExecutorService executor) {
      try {
          System.out.println("attempt to shutdown executor");
          executor.shutdown();
          executor.awaitTermination(5, TimeUnit.SECONDS);
      }
      catch (InterruptedException e) {
    		 context().system().log().error("tasks interrupted");
          System.err.println("termination interrupted");
      }
      finally {
          if (!executor.isTerminated()) {
              System.err.println("killing non-finished tasks");
          }
          executor.shutdownNow();
          System.out.println("shutdown finished");
      }
  }
  
 
  
  
}
