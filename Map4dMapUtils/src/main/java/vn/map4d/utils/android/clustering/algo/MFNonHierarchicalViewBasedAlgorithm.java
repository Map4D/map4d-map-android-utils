package vn.map4d.utils.android.clustering.algo;

import java.util.ArrayList;
import java.util.Collection;

import vn.map4d.map.camera.MFCameraPosition;
import vn.map4d.types.MFLocationCoordinate;
import vn.map4d.utils.android.clustering.MFClusterItem;
import vn.map4d.utils.android.geometry.Bounds;
import vn.map4d.utils.android.geometry.Point;
import vn.map4d.utils.android.projection.MFSphericalMercatorProjection;
import vn.map4d.utils.android.quadtree.PointQuadTree;

/**
 * This algorithm works the same way as {@link NonHierarchicalDistanceBasedAlgorithm} but works, only in
 * visible area. It requires to be reclustered on camera movement because clustering is done only for visible area.
 *
 * @param <T>
 */
public class MFNonHierarchicalViewBasedAlgorithm<T extends MFClusterItem>
  extends MFNonHierarchicalDistanceBasedAlgorithm<T> implements MFScreenBasedAlgorithm<T> {

  private static final MFSphericalMercatorProjection PROJECTION = new MFSphericalMercatorProjection(1);

  private int mViewWidth;
  private int mViewHeight;

  private MFLocationCoordinate mMapCenter;

  /**
   * @param screenWidth  map width in dp
   * @param screenHeight map height in dp
   */
  public MFNonHierarchicalViewBasedAlgorithm(int screenWidth, int screenHeight) {
    mViewWidth = screenWidth;
    mViewHeight = screenHeight;
  }

  @Override
  public void onCameraChange(MFCameraPosition cameraPosition) {
    mMapCenter = cameraPosition.getTarget();
  }

  @Override
  protected Collection<QuadItem<T>> getClusteringItems(PointQuadTree<QuadItem<T>> quadTree, double zoom) {
    Bounds visibleBounds = getVisibleBounds(zoom);
    Collection<QuadItem<T>> items = new ArrayList<>();

    // Handle wrapping around international date line
    if (visibleBounds.minX < 0) {
      Bounds wrappedBounds = new Bounds(visibleBounds.minX + 1, 1, visibleBounds.minY, visibleBounds.maxY);
      items.addAll(quadTree.search(wrappedBounds));
      visibleBounds = new Bounds(0, visibleBounds.maxX, visibleBounds.minY, visibleBounds.maxY);
    }
    if (visibleBounds.maxX > 1) {
      Bounds wrappedBounds = new Bounds(0, visibleBounds.maxX - 1, visibleBounds.minY, visibleBounds.maxY);
      items.addAll(quadTree.search(wrappedBounds));
      visibleBounds = new Bounds(visibleBounds.minX, 1, visibleBounds.minY, visibleBounds.maxY);
    }
    items.addAll(quadTree.search(visibleBounds));

    return items;
  }

  @Override
  public boolean shouldReclusterOnMapMovement() {
    return true;
  }

  /**
   * Update view width and height in case map size was changed.
   * You need to recluster all the clusters, to update view state after view size changes.
   *
   * @param width  map width in dp
   * @param height map height in dp
   */
  public void updateViewSize(int width, int height) {
    mViewWidth = width;
    mViewHeight = height;
  }

  private Bounds getVisibleBounds(double zoom) {
    if (mMapCenter == null) {
      return new Bounds(0, 0, 0, 0);
    }

    Point p = PROJECTION.toPoint(mMapCenter);

    final double halfWidthSpan = mViewWidth / Math.pow(2, zoom) / 256 / 2;
    final double halfHeightSpan = mViewHeight / Math.pow(2, zoom) / 256 / 2;

    return new Bounds(
      p.x - halfWidthSpan, p.x + halfWidthSpan,
      p.y - halfHeightSpan, p.y + halfHeightSpan);
  }
}
