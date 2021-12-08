package vn.map4d.utils.android.clustering.view;

import java.util.Set;

import vn.map4d.utils.android.clustering.MFCluster;
import vn.map4d.utils.android.clustering.MFClusterItem;
import vn.map4d.utils.android.clustering.MFClusterManager;

/**
 * Renders clusters.
 */
public interface MFClusterRenderer<T extends MFClusterItem> {

  /**
   * Called when the view needs to be updated because new clusters need to be displayed.
   *
   * @param clusters the clusters to be displayed.
   */
  void onClustersChanged(Set<? extends MFCluster<T>> clusters);

  void setOnClusterClickListener(MFClusterManager.OnClusterClickListener<T> listener);

  void setOnClusterInfoWindowClickListener(MFClusterManager.OnClusterInfoWindowClickListener<T> listener);

  void setOnClusterInfoWindowLongClickListener(MFClusterManager.OnClusterInfoWindowLongClickListener<T> listener);

  void setOnClusterItemClickListener(MFClusterManager.OnClusterItemClickListener<T> listener);

  void setOnClusterItemInfoWindowClickListener(MFClusterManager.OnClusterItemInfoWindowClickListener<T> listener);

  void setOnClusterItemInfoWindowLongClickListener(MFClusterManager.OnClusterItemInfoWindowLongClickListener<T> listener);

  /**
   * Called to set animation on or off
   */
  void setAnimation(boolean animate);

  /**
   * Called when the view is added.
   */
  void onAdd();

  /**
   * Called when the view is removed.
   */
  void onRemove();
}
