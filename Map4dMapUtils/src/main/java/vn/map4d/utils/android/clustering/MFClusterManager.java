package vn.map4d.utils.android.clustering;

import android.content.Context;
import android.os.AsyncTask;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import vn.map4d.map.annotations.MFMarker;
import vn.map4d.map.camera.MFCameraPosition;
import vn.map4d.map.core.Map4D;
import vn.map4d.utils.android.clustering.algo.Algorithm;
import vn.map4d.utils.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm;
import vn.map4d.utils.android.clustering.algo.PreCachingAlgorithmDecorator;
import vn.map4d.utils.android.clustering.algo.ScreenBasedAlgorithm;
import vn.map4d.utils.android.clustering.algo.ScreenBasedAlgorithmAdapter;
import vn.map4d.utils.android.clustering.view.MFClusterRenderer;
import vn.map4d.utils.android.clustering.view.MFDefaultClusterRenderer;
import vn.map4d.utils.android.collections.MFMarkerManager;

/**
 * Groups many items on a map based on zoom level.
 * <p/>
 * ClusterManager should be added to the map as an: <ul> <li>{@link Map4D.OnCameraIdleListener}</li>
 * <li>{@link Map4D.OnMarkerClickListener}</li> </ul>
 */
public class MFClusterManager<T extends MFClusterItem> implements
  Map4D.OnCameraIdleListener,
  Map4D.OnMarkerClickListener,
  Map4D.OnInfoWindowClickListener {

  private final MFMarkerManager mMarkerManager;
  private final MFMarkerManager.Collection mMarkers;
  private final MFMarkerManager.Collection mClusterMarkers;
  private final ReadWriteLock mClusterTaskLock = new ReentrantReadWriteLock();
  private ScreenBasedAlgorithm<T> mAlgorithm;
  private MFClusterRenderer<T> mRenderer;
  private Map4D mMap;
  private MFCameraPosition mPreviousCameraPosition;
  private ClusterTask mClusterTask;
  private OnClusterItemClickListener<T> mOnClusterItemClickListener;
  private OnClusterInfoWindowClickListener<T> mOnClusterInfoWindowClickListener;
  private OnClusterInfoWindowLongClickListener<T> mOnClusterInfoWindowLongClickListener;
  private OnClusterItemInfoWindowClickListener<T> mOnClusterItemInfoWindowClickListener;
  private OnClusterItemInfoWindowLongClickListener<T> mOnClusterItemInfoWindowLongClickListener;
  private OnClusterClickListener<T> mOnClusterClickListener;

  public MFClusterManager(Context context, Map4D map) {
    this(context, map, new MFMarkerManager(map));
  }

  public MFClusterManager(Context context, Map4D map, MFMarkerManager markerManager) {
    mMap = map;
    mMarkerManager = markerManager;
    mClusterMarkers = markerManager.newCollection();
    mMarkers = markerManager.newCollection();
    mRenderer = new MFDefaultClusterRenderer<>(context, map, this);
    mAlgorithm = new ScreenBasedAlgorithmAdapter<>(new PreCachingAlgorithmDecorator<>(
      new NonHierarchicalDistanceBasedAlgorithm<T>()));

    mClusterTask = new ClusterTask();
    mRenderer.onAdd();
  }

  public MFMarkerManager.Collection getMarkerCollection() {
    return mMarkers;
  }

  public MFMarkerManager.Collection getClusterMarkerCollection() {
    return mClusterMarkers;
  }

  public MFMarkerManager getMarkerManager() {
    return mMarkerManager;
  }

  public void setAnimation(boolean animate) {
    mRenderer.setAnimation(animate);
  }

  public MFClusterRenderer<T> getRenderer() {
    return mRenderer;
  }

  public void setRenderer(MFClusterRenderer<T> renderer) {
    mRenderer.setOnClusterClickListener(null);
    mRenderer.setOnClusterItemClickListener(null);
    mClusterMarkers.clear();
    mMarkers.clear();
    mRenderer.onRemove();
    mRenderer = renderer;
    mRenderer.onAdd();
    mRenderer.setOnClusterClickListener(mOnClusterClickListener);
    mRenderer.setOnClusterInfoWindowClickListener(mOnClusterInfoWindowClickListener);
    mRenderer.setOnClusterInfoWindowLongClickListener(mOnClusterInfoWindowLongClickListener);
    mRenderer.setOnClusterItemClickListener(mOnClusterItemClickListener);
    mRenderer.setOnClusterItemInfoWindowClickListener(mOnClusterItemInfoWindowClickListener);
    mRenderer.setOnClusterItemInfoWindowLongClickListener(mOnClusterItemInfoWindowLongClickListener);
    cluster();
  }

  public Algorithm<T> getAlgorithm() {
    return mAlgorithm;
  }

  public void setAlgorithm(Algorithm<T> algorithm) {
    if (algorithm instanceof ScreenBasedAlgorithm) {
      setAlgorithm((ScreenBasedAlgorithm<T>) algorithm);
    } else {
      setAlgorithm(new ScreenBasedAlgorithmAdapter<>(algorithm));
    }
  }

  public void setAlgorithm(ScreenBasedAlgorithm<T> algorithm) {
    algorithm.lock();
    try {
      final Algorithm<T> oldAlgorithm = getAlgorithm();
      mAlgorithm = algorithm;

      if (oldAlgorithm != null) {
        oldAlgorithm.lock();
        try {
          algorithm.addItems(oldAlgorithm.getItems());
        } finally {
          oldAlgorithm.unlock();
        }
      }
    } finally {
      algorithm.unlock();
    }

    if (mAlgorithm.shouldReclusterOnMapMovement()) {
      mAlgorithm.onCameraChange(mMap.getCameraPosition());
    }

    cluster();
  }

  /**
   * Removes all items from the cluster manager. After calling this method you must invoke
   * {@link #cluster()} for the map to be cleared.
   */
  public void clearItems() {
    final Algorithm<T> algorithm = getAlgorithm();
    algorithm.lock();
    try {
      algorithm.clearItems();
    } finally {
      algorithm.unlock();
    }
  }

  /**
   * Adds items to clusters. After calling this method you must invoke {@link #cluster()} for the
   * state of the clusters to be updated on the map.
   *
   * @param items items to add to clusters
   * @return true if the cluster manager contents changed as a result of the call
   */
  public boolean addItems(Collection<T> items) {
    final Algorithm<T> algorithm = getAlgorithm();
    algorithm.lock();
    try {
      return algorithm.addItems(items);
    } finally {
      algorithm.unlock();
    }
  }

  /**
   * Adds an item to a cluster. After calling this method you must invoke {@link #cluster()} for
   * the state of the clusters to be updated on the map.
   *
   * @param myItem item to add to clusters
   * @return true if the cluster manager contents changed as a result of the call
   */
  public boolean addItem(T myItem) {
    final Algorithm<T> algorithm = getAlgorithm();
    algorithm.lock();
    try {
      return algorithm.addItem(myItem);
    } finally {
      algorithm.unlock();
    }
  }

  /**
   * Removes items from clusters. After calling this method you must invoke {@link #cluster()} for
   * the state of the clusters to be updated on the map.
   *
   * @param items items to remove from clusters
   * @return true if the cluster manager contents changed as a result of the call
   */
  public boolean removeItems(Collection<T> items) {
    final Algorithm<T> algorithm = getAlgorithm();
    algorithm.lock();
    try {
      return algorithm.removeItems(items);
    } finally {
      algorithm.unlock();
    }
  }

  /**
   * Removes an item from clusters. After calling this method you must invoke {@link #cluster()}
   * for the state of the clusters to be updated on the map.
   *
   * @param item item to remove from clusters
   * @return true if the item was removed from the cluster manager as a result of this call
   */
  public boolean removeItem(T item) {
    final Algorithm<T> algorithm = getAlgorithm();
    algorithm.lock();
    try {
      return algorithm.removeItem(item);
    } finally {
      algorithm.unlock();
    }
  }

  /**
   * Updates an item in clusters. After calling this method you must invoke {@link #cluster()} for
   * the state of the clusters to be updated on the map.
   *
   * @param item item to update in clusters
   * @return true if the item was updated in the cluster manager, false if the item is not
   * contained within the cluster manager and the cluster manager contents are unchanged
   */
  public boolean updateItem(T item) {
    final Algorithm<T> algorithm = getAlgorithm();
    algorithm.lock();
    try {
      return algorithm.updateItem(item);
    } finally {
      algorithm.unlock();
    }
  }

  /**
   * Force a re-cluster on the map. You should call this after adding, removing, updating,
   * or clearing item(s).
   */
  public void cluster() {
    mClusterTaskLock.writeLock().lock();
    try {
      // Attempt to cancel the in-flight request.
      mClusterTask.cancel(true);
      mClusterTask = new ClusterTask();
      mClusterTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mMap.getCameraPosition().getZoom());
    } finally {
      mClusterTaskLock.writeLock().unlock();
    }
  }

  /**
   * Might re-cluster.
   */
  @Override
  public void onCameraIdle() {
    if (mRenderer instanceof Map4D.OnCameraIdleListener) {
      ((Map4D.OnCameraIdleListener) mRenderer).onCameraIdle();
    }

    mAlgorithm.onCameraChange(mMap.getCameraPosition());

    // delegate clustering to the algorithm
    if (mAlgorithm.shouldReclusterOnMapMovement()) {
      cluster();

      // Don't re-compute clusters if the map has just been panned/tilted/rotated.
    } else if (mPreviousCameraPosition == null || mPreviousCameraPosition.getZoom() != mMap.getCameraPosition().getZoom()) {
      mPreviousCameraPosition = mMap.getCameraPosition();
      cluster();
    }
  }

  @Override
  public boolean onMarkerClick(MFMarker marker) {
    return getMarkerManager().onMarkerClick(marker);
  }

  @Override
  public void onInfoWindowClick(MFMarker marker) {
    getMarkerManager().onInfoWindowClick(marker);
  }

  /**
   * Sets a callback that's invoked when a Cluster is tapped. Note: For this listener to function,
   * the ClusterManager must be added as a click listener to the map.
   */
  public void setOnClusterClickListener(OnClusterClickListener<T> listener) {
    mOnClusterClickListener = listener;
    mRenderer.setOnClusterClickListener(listener);
  }

  /**
   * Sets a callback that's invoked when a Cluster info window is tapped. Note: For this listener to function,
   * the ClusterManager must be added as a info window click listener to the map.
   */
  public void setOnClusterInfoWindowClickListener(OnClusterInfoWindowClickListener<T> listener) {
    mOnClusterInfoWindowClickListener = listener;
    mRenderer.setOnClusterInfoWindowClickListener(listener);
  }

  /**
   * Sets a callback that's invoked when a Cluster info window is long-pressed. Note: For this listener to function,
   * the ClusterManager must be added as a info window click listener to the map.
   */
  public void setOnClusterInfoWindowLongClickListener(OnClusterInfoWindowLongClickListener<T> listener) {
    mOnClusterInfoWindowLongClickListener = listener;
    mRenderer.setOnClusterInfoWindowLongClickListener(listener);
  }

  /**
   * Sets a callback that's invoked when an individual ClusterItem is tapped. Note: For this
   * listener to function, the ClusterManager must be added as a click listener to the map.
   */
  public void setOnClusterItemClickListener(OnClusterItemClickListener<T> listener) {
    mOnClusterItemClickListener = listener;
    mRenderer.setOnClusterItemClickListener(listener);
  }

  /**
   * Sets a callback that's invoked when an individual ClusterItem's Info Window is tapped. Note: For this
   * listener to function, the ClusterManager must be added as a info window click listener to the map.
   */
  public void setOnClusterItemInfoWindowClickListener(OnClusterItemInfoWindowClickListener<T> listener) {
    mOnClusterItemInfoWindowClickListener = listener;
    mRenderer.setOnClusterItemInfoWindowClickListener(listener);
  }

  /**
   * Sets a callback that's invoked when an individual ClusterItem's Info Window is long-pressed. Note: For this
   * listener to function, the ClusterManager must be added as a info window click listener to the map.
   */
  public void setOnClusterItemInfoWindowLongClickListener(OnClusterItemInfoWindowLongClickListener<T> listener) {
    mOnClusterItemInfoWindowLongClickListener = listener;
    mRenderer.setOnClusterItemInfoWindowLongClickListener(listener);
  }

  /**
   * Called when a Cluster is clicked.
   */
  public interface OnClusterClickListener<T extends MFClusterItem> {
    /**
     * Called when cluster is clicked.
     * Return true if click has been handled
     * Return false and the click will dispatched to the next listener
     */
    boolean onClusterClick(MFCluster<T> cluster);
  }

  /**
   * Called when a Cluster's Info Window is clicked.
   */
  public interface OnClusterInfoWindowClickListener<T extends MFClusterItem> {
    void onClusterInfoWindowClick(MFCluster<T> cluster);
  }

  /**
   * Called when a Cluster's Info Window is long clicked.
   */
  public interface OnClusterInfoWindowLongClickListener<T extends MFClusterItem> {
    void onClusterInfoWindowLongClick(MFCluster<T> cluster);
  }

  /**
   * Called when an individual ClusterItem is clicked.
   */
  public interface OnClusterItemClickListener<T extends MFClusterItem> {

    /**
     * Called when {@code item} is clicked.
     *
     * @param item the item clicked
     * @return true if the listener consumed the event (i.e. the default behavior should not
     * occur), false otherwise (i.e. the default behavior should occur).  The default behavior
     * is for the camera to move to the marker and an info window to appear.
     */
    boolean onClusterItemClick(T item);
  }

  /**
   * Called when an individual ClusterItem's Info Window is clicked.
   */
  public interface OnClusterItemInfoWindowClickListener<T extends MFClusterItem> {
    void onClusterItemInfoWindowClick(T item);
  }

  /**
   * Called when an individual ClusterItem's Info Window is long clicked.
   */
  public interface OnClusterItemInfoWindowLongClickListener<T extends MFClusterItem> {
    void onClusterItemInfoWindowLongClick(T item);
  }

  /**
   * Runs the clustering algorithm in a background thread, then re-paints when results come back.
   */
  private class ClusterTask extends AsyncTask<Double, Void, Set<? extends MFCluster<T>>> {
    @Override
    protected Set<? extends MFCluster<T>> doInBackground(Double... zoom) {
      final Algorithm<T> algorithm = getAlgorithm();
      algorithm.lock();
      try {
        return algorithm.getClusters(zoom[0]);
      } finally {
        algorithm.unlock();
      }
    }

    @Override
    protected void onPostExecute(Set<? extends MFCluster<T>> clusters) {
      mRenderer.onClustersChanged(clusters);
    }
  }
}
