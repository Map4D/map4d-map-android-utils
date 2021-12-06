package vn.map4d.utils.android.clustering.algo;

import java.util.Collection;
import java.util.Set;

import vn.map4d.map.camera.MFCameraPosition;
import vn.map4d.utils.android.clustering.MFCluster;
import vn.map4d.utils.android.clustering.MFClusterItem;

public class ScreenBasedAlgorithmAdapter<T extends MFClusterItem> extends AbstractAlgorithm<T> implements ScreenBasedAlgorithm<T> {

  private Algorithm<T> mAlgorithm;

  public ScreenBasedAlgorithmAdapter(Algorithm<T> algorithm) {
    mAlgorithm = algorithm;
  }

  @Override
  public boolean shouldReclusterOnMapMovement() {
    return false;
  }

  @Override
  public boolean addItem(T item) {
    return mAlgorithm.addItem(item);
  }

  @Override
  public boolean addItems(Collection<T> items) {
    return mAlgorithm.addItems(items);
  }

  @Override
  public void clearItems() {
    mAlgorithm.clearItems();
  }

  @Override
  public boolean removeItem(T item) {
    return mAlgorithm.removeItem(item);
  }

  @Override
  public boolean removeItems(Collection<T> items) {
    return mAlgorithm.removeItems(items);
  }

  @Override
  public boolean updateItem(T item) {
    return mAlgorithm.updateItem(item);
  }

  @Override
  public Set<? extends MFCluster<T>> getClusters(double zoom) {
    return mAlgorithm.getClusters(zoom);
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
  }

  @Override
  public void onCameraChange(MFCameraPosition cameraPosition) {
    // stub
  }

}
