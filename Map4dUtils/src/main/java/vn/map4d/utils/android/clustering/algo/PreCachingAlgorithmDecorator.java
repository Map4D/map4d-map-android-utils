package vn.map4d.utils.android.clustering.algo;

import androidx.collection.LruCache;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import vn.map4d.utils.android.clustering.MFCluster;
import vn.map4d.utils.android.clustering.MFClusterItem;

/**
 * Optimistically fetch clusters for adjacent zoom levels, caching them as necessary.
 */
public class PreCachingAlgorithmDecorator<T extends MFClusterItem> extends AbstractAlgorithm<T> {
  private final Algorithm<T> mAlgorithm;

  // TODO: evaluate maxSize parameter for LruCache.
  private final LruCache<Integer, Set<? extends MFCluster<T>>> mCache = new LruCache<Integer, Set<? extends MFCluster<T>>>(5);
  private final ReadWriteLock mCacheLock = new ReentrantReadWriteLock();
  private final Executor mExecutor = Executors.newCachedThreadPool();

  public PreCachingAlgorithmDecorator(Algorithm<T> algorithm) {
    mAlgorithm = algorithm;
  }

  @Override
  public boolean addItem(T item) {
    boolean result = mAlgorithm.addItem(item);
    if (result) {
      clearCache();
    }
    return result;
  }

  @Override
  public boolean addItems(Collection<T> items) {
    boolean result = mAlgorithm.addItems(items);
    if (result) {
      clearCache();
    }
    return result;
  }

  @Override
  public void clearItems() {
    mAlgorithm.clearItems();
    clearCache();
  }

  @Override
  public boolean removeItem(T item) {
    boolean result = mAlgorithm.removeItem(item);
    if (result) {
      clearCache();
    }
    return result;
  }

  @Override
  public boolean removeItems(Collection<T> items) {
    boolean result = mAlgorithm.removeItems(items);
    if (result) {
      clearCache();
    }
    return result;
  }

  @Override
  public boolean updateItem(T item) {
    boolean result = mAlgorithm.updateItem(item);
    if (result) {
      clearCache();
    }
    return result;
  }

  private void clearCache() {
    mCache.evictAll();
  }

  @Override
  public Set<? extends MFCluster<T>> getClusters(double zoom) {
    int discreteZoom = (int) zoom;
    Set<? extends MFCluster<T>> results = getClustersInternal(discreteZoom);
    // TODO: Check if requests are already in-flight.
    if (mCache.get(discreteZoom + 1) == null) {
      mExecutor.execute(new PrecacheRunnable(discreteZoom + 1));
    }
    if (mCache.get(discreteZoom - 1) == null) {
      mExecutor.execute(new PrecacheRunnable(discreteZoom - 1));
    }
    return results;
  }

  @Override
  public Collection<T> getItems() {
    return mAlgorithm.getItems();
  }

  @Override
  public int getMaxDistanceBetweenClusteredItems() {
    return mAlgorithm.getMaxDistanceBetweenClusteredItems();
  }

  @Override
  public void setMaxDistanceBetweenClusteredItems(int maxDistance) {
    mAlgorithm.setMaxDistanceBetweenClusteredItems(maxDistance);
    clearCache();
  }

  private Set<? extends MFCluster<T>> getClustersInternal(int discreteZoom) {
    Set<? extends MFCluster<T>> results;
    mCacheLock.readLock().lock();
    results = mCache.get(discreteZoom);
    mCacheLock.readLock().unlock();

    if (results == null) {
      mCacheLock.writeLock().lock();
      results = mCache.get(discreteZoom);
      if (results == null) {
        results = mAlgorithm.getClusters(discreteZoom);
        mCache.put(discreteZoom, results);
      }
      mCacheLock.writeLock().unlock();
    }
    return results;
  }

  private class PrecacheRunnable implements Runnable {
    private final int mZoom;

    public PrecacheRunnable(int zoom) {
      mZoom = zoom;
    }

    @Override
    public void run() {
      try {
        // Wait between 500 - 1000 ms.
        Thread.sleep((long) (Math.random() * 500 + 500));
      } catch (InterruptedException e) {
        // ignore. keep going.
      }
      getClustersInternal(mZoom);
    }
  }
}
