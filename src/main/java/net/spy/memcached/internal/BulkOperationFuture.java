package net.spy.memcached.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.spy.memcached.MemcachedConnection;
import net.spy.memcached.OperationTimeoutException;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationState;

public class BulkOperationFuture<T> implements Future<Map<String, T>> {
  protected final Map<String, T> failedResult = new HashMap<>();
  protected final Collection<Operation> ops = new ArrayList<>();
  protected final long timeout;
  protected final CountDownLatch latch;

  public BulkOperationFuture(CountDownLatch l, long timeout) {
    this.latch = l;
    this.timeout = timeout;
  }

  @Override
  public boolean cancel(boolean ign) {
    boolean rv = false;
    for (Operation op : ops) {
      rv |= op.cancel("by application.");
    }
    return rv;
  }

  @Override
  public boolean isCancelled() {
    for (Operation op : ops) {
      if (op.isCancelled()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isDone() {
    for (Operation op : ops) {
      if (!(op.getState() == OperationState.COMPLETE || op.isCancelled())) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Map<String, T> get() throws InterruptedException, ExecutionException {
    try {
      return get(timeout, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      throw new OperationTimeoutException(e);
    }
  }

  @Override
  public Map<String, T> get(long duration,
                            TimeUnit unit) throws InterruptedException,
          TimeoutException, ExecutionException {

    long beforeAwait = System.currentTimeMillis();
    if (!latch.await(duration, unit)) {
      Collection<Operation> timedOutOps = new ArrayList<>();
      for (Operation op : ops) {
        if (op.getState() != OperationState.COMPLETE) {
          timedOutOps.add(op);
        } else {
          MemcachedConnection.opSucceeded(op);
        }
      }
      if (!timedOutOps.isEmpty()) {
        MemcachedConnection.opsTimedOut(timedOutOps);

        long elapsed = System.currentTimeMillis() - beforeAwait;
        throw new CheckedOperationTimeoutException(duration, unit, elapsed, timedOutOps);
      }
    } else {
      // continuous timeout counter will be reset
      MemcachedConnection.opsSucceeded(ops);
    }

    List<Exception> exceptions = new ArrayList<>();
    for (Operation op : ops) {
      if (op != null && op.hasErrored()) {
        exceptions.add(op.getException());
      }

      if (op != null && op.isCancelled()) {
        exceptions.add(new RuntimeException(op.getCancelCause()));
      }
    }

    if (!exceptions.isEmpty()) {
      throw new CompositeException(exceptions);
    }

    return failedResult;
  }

  public void addFailedResult(String key, T value) {
    failedResult.put(key, value);
  }

  public void addOperations(Collection<Operation> ops) {
    this.ops.addAll(ops);
  }

  public void addOperation(Operation op) {
    ops.add(op);
  }
}
