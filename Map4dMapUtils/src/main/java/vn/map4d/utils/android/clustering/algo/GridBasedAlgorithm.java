package vn.map4d.utils.android.clustering.algo;

import androidx.collection.LongSparseArray;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import vn.map4d.utils.android.clustering.MFCluster;
import vn.map4d.utils.android.clustering.MFClusterItem;
import vn.map4d.utils.android.geometry.Point;
import vn.map4d.utils.android.projection.SphericalMercatorProjection;

/**
 * Groups markers into a grid.
 */
public class GridBasedAlgorithm<T extends MFClusterItem> extends AbstractAlgorithm<T> {
  private static final int DEFAULT_GRID_SIZE = 100;
  private final Set<T> mItems = Collections.synchronizedSet(new HashSet<T>());
  private int mGridSize = DEFAULT_GRID_SIZE;

  private static long getCoord(long numCells, double x, double y) {
    return (long) (numCells * Math.floor(x) + Math.floor(y));
  }

  /**
   * Adds an item to the algorithm
   *
   * @param item the item to be added
   * @return true if the algorithm contents changed as a result of the call
   */
  @Override
  public boolean addItem(T item) {
    return mItems.add(item);
  }

  /**
   * Adds a collection of items to the algorithm
   *
   * @param items the items to be added
   * @return true if the algorithm contents changed as a result of the call
   */
  @Override
  public boolean addItems(Collection<T> items) {
    return mItems.addAll(items);
  }

  @Override
  public void clearItems() {
    mItems.clear();
  }

  /**
   * Removes an item from the algorithm
   *
   * @param item the item to be removed
   * @return true if this algorithm contained the specified element (or equivalently, if this
   * algorithm changed as a result of the call).
   */
  @Override
  public boolean removeItem(T item) {
    return mItems.remove(item);
  }

  /**
   * Removes a collection of items from the algorithm
   *
   * @param items the items to be removed
   * @return true if this algorithm contents changed as a result of the call
   */
  @Override
  public boolean removeItems(Collection<T> items) {
    return mItems.removeAll(items);
  }

  /**
   * Updates the provided item in the algorithm
   *
   * @param item the item to be updated
   * @return true if the item existed in the algorithm and was updated, or false if the item did
   * not exist in the algorithm and the algorithm contents remain unchanged.
   */
  @Override
  public boolean updateItem(T item) {
    boolean result;
    synchronized (mItems) {
      result = removeItem(item);
      if (result) {
        // Only add the item if it was removed (to help prevent accidental duplicates on map)
        result = addItem(item);
      }
    }
    return result;
  }

  @Override
  public int getMaxDistanceBetweenClusteredItems() {
    return mGridSize;
  }

  @Override
  public void setMaxDistanceBetweenClusteredItems(int maxDistance) {
    mGridSize = maxDistance;
  }

  @Override
  public Set<? extends MFCluster<T>> getClusters(double zoom) {
    long numCells = (long) Math.ceil(256 * Math.pow(2, zoom) / mGridSize);
    SphericalMercatorProjection proj = new SphericalMercatorProjection(numCells);

    HashSet<MFCluster<T>> clusters = new HashSet<MFCluster<T>>();
    LongSparseArray<MFStaticCluster<T>> sparseArray = new LongSparseArray<MFStaticCluster<T>>();

    synchronized (mItems) {
      for (T item : mItems) {
        Point p = proj.toPoint(item.getPosition());

        long coord = getCoord(numCells, p.x, p.y);

        MFStaticCluster<T> cluster = sparseArray.get(coord);
        if (cluster == null) {
          cluster = new MFStaticCluster<T>(proj.toLocationCoordinate(new Point(Math.floor(p.x) + .5, Math.floor(p.y) + .5)));
          sparseArray.put(coord, cluster);
          clusters.add(cluster);
        }
        cluster.add(item);
      }
    }

    return clusters;
  }

  @Override
  public Collection<T> getItems() {
    return mItems;
  }
}
