package vn.map4d.utils.android.clustering;

import java.util.Collection;

import vn.map4d.types.MFLocationCoordinate;

/**
 * A collection of ClusterItems that are nearby each other.
 */
public interface MFCluster<T extends MFClusterItem> {
  MFLocationCoordinate getPosition();

  Collection<T> getItems();

  int getSize();
}
