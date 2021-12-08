package vn.map4d.utils.android.clustering.algo;

import java.util.Collection;
import java.util.Set;

import vn.map4d.utils.android.clustering.MFCluster;
import vn.map4d.utils.android.clustering.MFClusterItem;

/**
 * Logic for computing clusters
 */
public interface Algorithm<T extends MFClusterItem> {

  /**
   * Adds an item to the algorithm
   *
   * @param item the item to be added
   * @return true if the algorithm contents changed as a result of the call
   */
  boolean addItem(T item);

  /**
   * Adds a collection of items to the algorithm
   *
   * @param items the items to be added
   * @return true if the algorithm contents changed as a result of the call
   */
  boolean addItems(Collection<T> items);

  void clearItems();

  /**
   * Removes an item from the algorithm
   *
   * @param item the item to be removed
   * @return true if this algorithm contained the specified element (or equivalently, if this
   * algorithm changed as a result of the call).
   */
  boolean removeItem(T item);

  /**
   * Updates the provided item in the algorithm
   *
   * @param item the item to be updated
   * @return true if the item existed in the algorithm and was updated, or false if the item did
   * not exist in the algorithm and the algorithm contents remain unchanged.
   */
  boolean updateItem(T item);

  /**
   * Removes a collection of items from the algorithm
   *
   * @param items the items to be removed
   * @return true if this algorithm contents changed as a result of the call
   */
  boolean removeItems(Collection<T> items);

  Set<? extends MFCluster<T>> getClusters(double zoom);

  Collection<T> getItems();

  int getMaxDistanceBetweenClusteredItems();

  void setMaxDistanceBetweenClusteredItems(int maxDistance);

  void lock();

  void unlock();
}
