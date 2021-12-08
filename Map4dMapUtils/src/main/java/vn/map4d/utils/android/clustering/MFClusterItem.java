package vn.map4d.utils.android.clustering;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vn.map4d.types.MFLocationCoordinate;

/**
 * ClusterItem represents a marker on the map.
 */
public interface MFClusterItem {

  /**
   * The position of this marker. This must always return the same value.
   */
  @NonNull
  MFLocationCoordinate getPosition();

  /**
   * The title of this marker.
   */
  @Nullable
  String getTitle();

  /**
   * The description of this marker.
   */
  @Nullable
  String getSnippet();
}
