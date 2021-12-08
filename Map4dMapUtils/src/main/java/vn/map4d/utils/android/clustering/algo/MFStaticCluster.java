package vn.map4d.utils.android.clustering.algo;

import java.util.Collection;
import java.util.LinkedHashSet;

import vn.map4d.types.MFLocationCoordinate;
import vn.map4d.utils.android.clustering.MFCluster;
import vn.map4d.utils.android.clustering.MFClusterItem;

/**
 * A cluster whose center is determined upon creation.
 */
public class MFStaticCluster<T extends MFClusterItem> implements MFCluster<T> {
  private final MFLocationCoordinate mCenter;
  private final Collection<T> mItems = new LinkedHashSet<>();

  public MFStaticCluster(MFLocationCoordinate center) {
    mCenter = center;
  }

  public boolean add(T t) {
    return mItems.add(t);
  }

  @Override
  public MFLocationCoordinate getPosition() {
    return mCenter;
  }

  public boolean remove(T t) {
    return mItems.remove(t);
  }

  @Override
  public Collection<T> getItems() {
    return mItems;
  }

  @Override
  public int getSize() {
    return mItems.size();
  }

  @Override
  public String toString() {
    return "StaticCluster{" +
      "mCenter=" + mCenter +
      ", mItems.size=" + mItems.size() +
      '}';
  }

  @Override
  public int hashCode() {
    return mCenter.hashCode() + mItems.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof MFStaticCluster<?>)) {
      return false;
    }

    return ((MFStaticCluster<?>) other).mCenter.equals(mCenter)
      && ((MFStaticCluster<?>) other).mItems.equals(mItems);
  }
}
