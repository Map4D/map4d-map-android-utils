package vn.map4d.utils.android.clustering.algo;

import vn.map4d.map.camera.MFCameraPosition;
import vn.map4d.utils.android.clustering.MFClusterItem;

/**
 * This algorithm uses map position for clustering, and should be reclustered on map movement
 *
 * @param <T>
 */

public interface ScreenBasedAlgorithm<T extends MFClusterItem> extends Algorithm<T> {

  boolean shouldReclusterOnMapMovement();

  void onCameraChange(MFCameraPosition position);
}
