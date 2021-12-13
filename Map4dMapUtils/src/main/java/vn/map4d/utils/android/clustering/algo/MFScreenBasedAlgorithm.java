package vn.map4d.utils.android.clustering.algo;

import vn.map4d.map.camera.MFCameraPosition;
import vn.map4d.utils.android.clustering.MFClusterItem;

/**
 * This algorithm uses map position for clustering, and should be reclustered on map movement
 *
 * @param <T>
 */

public interface MFScreenBasedAlgorithm<T extends MFClusterItem> extends MFAlgorithm<T> {

  boolean shouldReclusterOnMapMovement();

  void onCameraChange(MFCameraPosition position);
}
