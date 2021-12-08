package vn.map4d.utils.android.clustering.algo;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import vn.map4d.utils.android.clustering.MFClusterItem;

/**
 * Base Algorithm class that implements lock/unlock functionality.
 */
public abstract class AbstractAlgorithm<T extends MFClusterItem> implements Algorithm<T> {

  private final ReadWriteLock mLock = new ReentrantReadWriteLock();

  @Override
  public void lock() {
    mLock.writeLock().lock();
  }

  @Override
  public void unlock() {
    mLock.writeLock().unlock();
  }
}
