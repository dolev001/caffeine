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

import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.MoreObjects;
import com.google.common.base.Stopwatch;

/**
 * Statistics gathered by a policy execution.
 *
 * @author ben.manes@gmail.com (Ben Manes)
 * 
 * dolev adas - changed it to be concurrent
 */
public final class PolicyStats {
  private final String name;
  private final Stopwatch stopwatch;

//  private long hitCount;
//  private long missCount;
//  private long evictionCount;
//  private long admittedCount;
//  private long rejectedCount;
//  private long operationCount;
  
private AtomicLong hitCount;
private AtomicLong missCount;
private AtomicLong evictionCount;
private AtomicLong admittedCount;
private AtomicLong rejectedCount;
private AtomicLong operationCount;
  

  public PolicyStats(String name) {
    this.name = requireNonNull(name);
    this.stopwatch = Stopwatch.createUnstarted();
    
    this.hitCount       = new AtomicLong (0);
    this.missCount      = new AtomicLong (0);
    this.evictionCount  = new AtomicLong (0);
    this.admittedCount  = new AtomicLong (0);
    this.rejectedCount  = new AtomicLong (0);
    this.operationCount = new AtomicLong (0);
    
    
  }

  public Stopwatch stopwatch() {
    return stopwatch;
  }

  public String name() {
    return name;
  }

  public void recordOperation() {
    //operationCount++;
	  operationCount.incrementAndGet();
	  
  }

  public long operationCount() {  
    return operationCount.get();
  }

  public void addOperations(long operations) {
    operationCount.getAndAdd(operations);
  }

  public void recordHit() {
    hitCount.incrementAndGet();
  }

  public long hitCount() {
    return hitCount.get();
  }

  public void addHits(long hits) {
    hitCount .getAndAdd(hits);
  }

  public void recordMiss() {
    missCount.incrementAndGet();
  }

  public long missCount() {
    return missCount.get();
  }

  public void addMisses(long misses) {
    missCount.getAndAdd(misses);
  }

  public long evictionCount() {
    return evictionCount.get();
  }

  public void recordEviction() {
    evictionCount.incrementAndGet();
  }

  public void addEvictions(long evictions) {
    evictionCount .getAndAdd(evictions);
  }

  public long requestCount() {
    return hitCount.get() + missCount.get();
  }

  public long admissionCount() {
    return admittedCount.get();
  }

  public void recordAdmission() {
    admittedCount.incrementAndGet();
  }

  public long rejectionCount() {
    return rejectedCount.get();
  }

  public void recordRejection() {
    rejectedCount.incrementAndGet();
  }

  public double hitRate() {
    long requestCount = requestCount();
    return (requestCount == 0) ? 1.0 : (double) hitCount.get() / requestCount;
  }

  public double missRate() {
    long requestCount = requestCount();
    return (requestCount == 0) ? 0.0 : (double) missCount.get() / requestCount;
  }

  public double admissionRate() {
    double candidateCount = admittedCount.get() + rejectedCount.get();
    return (candidateCount == 0) ? 1.0 : admittedCount.get() / candidateCount;
  }

  public double complexity() {
    long requestCount = requestCount();
    return (requestCount == 0) ? 0.0 : (double) operationCount.get() / requestCount;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).addValue(name).toString();
  }
}
