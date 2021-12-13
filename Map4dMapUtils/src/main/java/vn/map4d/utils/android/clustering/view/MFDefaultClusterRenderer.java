package vn.map4d.utils.android.clustering.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.util.SparseArray;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import vn.map4d.map.annotations.MFBitmapDescriptor;
import vn.map4d.map.annotations.MFBitmapDescriptorFactory;
import vn.map4d.map.annotations.MFMarker;
import vn.map4d.map.annotations.MFMarkerOptions;
import vn.map4d.map.core.MFCoordinateBounds;
import vn.map4d.map.core.MFProjection;
import vn.map4d.map.core.Map4D;
import vn.map4d.types.MFLocationCoordinate;
import vn.map4d.utils.android.R;
import vn.map4d.utils.android.clustering.MFCluster;
import vn.map4d.utils.android.clustering.MFClusterItem;
import vn.map4d.utils.android.clustering.MFClusterManager;
import vn.map4d.utils.android.collections.MFMarkerManager;
import vn.map4d.utils.android.geometry.Point;
import vn.map4d.utils.android.projection.MFSphericalMercatorProjection;
import vn.map4d.utils.android.ui.MFIconGenerator;
import vn.map4d.utils.android.ui.MFSquareTextView;

/**
 * The default view for a ClusterManager. Markers are animated in and out of clusters.
 */
public class MFDefaultClusterRenderer<T extends MFClusterItem> implements MFClusterRenderer<T> {
  private static final int[] BUCKETS = {10, 20, 50, 100, 200, 500, 1000};
  private static final TimeInterpolator ANIMATION_INTERP = new DecelerateInterpolator();
  private final Map4D mMap;
  private final MFIconGenerator mIconGenerator;
  private final MFClusterManager<T> mClusterManager;
  private final float mDensity;
  private final Executor mExecutor = Executors.newSingleThreadExecutor();
  private final ViewModifier mViewModifier = new ViewModifier();
  private boolean mAnimate;
  private ShapeDrawable mColoredCircleBackground;
  /**
   * Markers that are currently on the map.
   */
  private Set<MarkerWithPosition> mMarkers = Collections.newSetFromMap(
    new ConcurrentHashMap<MarkerWithPosition, Boolean>());
  /**
   * Icons for each bucket.
   */
  private SparseArray<MFBitmapDescriptor> mIcons = new SparseArray<>();
  /**
   * Markers for single ClusterItems.
   */
  private MarkerCache<T> mMarkerCache = new MarkerCache<>();
  /**
   * If cluster size is less than this size, display individual markers.
   */
  private int mMinClusterSize = 4;
  /**
   * The currently displayed set of clusters.
   */
  private Set<? extends MFCluster<T>> mClusters;
  /**
   * Markers for Clusters.
   */
  private MarkerCache<MFCluster<T>> mClusterMarkerCache = new MarkerCache<>();
  /**
   * The target zoom level for the current set of clusters.
   */
  private double mZoom;
  private MFClusterManager.OnClusterClickListener<T> mClickListener;
  private MFClusterManager.OnClusterInfoWindowClickListener<T> mInfoWindowClickListener;
  private MFClusterManager.OnClusterInfoWindowLongClickListener<T> mInfoWindowLongClickListener;
  private MFClusterManager.OnClusterItemClickListener<T> mItemClickListener;
  private MFClusterManager.OnClusterItemInfoWindowClickListener<T> mItemInfoWindowClickListener;
  private MFClusterManager.OnClusterItemInfoWindowLongClickListener<T> mItemInfoWindowLongClickListener;

  public MFDefaultClusterRenderer(Context context, Map4D map, MFClusterManager<T> clusterManager) {
    mMap = map;
    mAnimate = true;
    mDensity = context.getResources().getDisplayMetrics().density;
    mIconGenerator = new MFIconGenerator(context);
    mIconGenerator.setContentView(makeSquareTextView(context));
    mIconGenerator.setTextAppearance(R.style.amu_ClusterIcon_TextAppearance);
    mIconGenerator.setBackground(makeClusterBackground());
    mClusterManager = clusterManager;
  }

  private static double distanceSquared(Point a, Point b) {
    return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y);
  }

  @Override
  public void onAdd() {
    mClusterManager.getMarkerCollection().setOnMarkerClickListener(new Map4D.OnMarkerClickListener() {
      @Override
      public boolean onMarkerClick(MFMarker marker) {
        return mItemClickListener != null && mItemClickListener.onClusterItemClick(mMarkerCache.get(marker));
      }
    });

    mClusterManager.getMarkerCollection().setOnInfoWindowClickListener(new Map4D.OnInfoWindowClickListener() {
      @Override
      public void onInfoWindowClick(MFMarker marker) {
        if (mItemInfoWindowClickListener != null) {
          mItemInfoWindowClickListener.onClusterItemInfoWindowClick(mMarkerCache.get(marker));
        }
      }
    });

        /*mClusterManager.getMarkerCollection().setOnInfoWindowLongClickListener(new Map4D.OnInfoWindowLongClickListener() {
            @Override
            public void onInfoWindowLongClick(Marker marker) {
                if (mItemInfoWindowLongClickListener != null) {
                    mItemInfoWindowLongClickListener.onClusterItemInfoWindowLongClick(mMarkerCache.get(marker));
                }
            }
        });*/

    mClusterManager.getClusterMarkerCollection().setOnMarkerClickListener(new Map4D.OnMarkerClickListener() {
      @Override
      public boolean onMarkerClick(MFMarker marker) {
        return mClickListener != null && mClickListener.onClusterClick(mClusterMarkerCache.get(marker));
      }
    });

    mClusterManager.getClusterMarkerCollection().setOnInfoWindowClickListener(new Map4D.OnInfoWindowClickListener() {
      @Override
      public void onInfoWindowClick(MFMarker marker) {
        if (mInfoWindowClickListener != null) {
          mInfoWindowClickListener.onClusterInfoWindowClick(mClusterMarkerCache.get(marker));
        }
      }
    });

		/*mClusterManager.getClusterMarkerCollection().setOnInfoWindowLongClickListener(new Map4D.OnInfoWindowLongClickListener() {
            @Override
            public void onInfoWindowLongClick(Marker marker) {
                if (mInfoWindowLongClickListener != null) {
                    mInfoWindowLongClickListener.onClusterInfoWindowLongClick(mClusterMarkerCache.get(marker));
                }
            }
        });*/
  }

  @Override
  public void onRemove() {
    mClusterManager.getMarkerCollection().setOnMarkerClickListener(null);
    mClusterManager.getMarkerCollection().setOnInfoWindowClickListener(null);
    //mClusterManager.getMarkerCollection().setOnInfoWindowLongClickListener(null);
    mClusterManager.getClusterMarkerCollection().setOnMarkerClickListener(null);
    mClusterManager.getClusterMarkerCollection().setOnInfoWindowClickListener(null);
    //mClusterManager.getClusterMarkerCollection().setOnInfoWindowLongClickListener(null);
  }

  private LayerDrawable makeClusterBackground() {
    mColoredCircleBackground = new ShapeDrawable(new OvalShape());
    ShapeDrawable outline = new ShapeDrawable(new OvalShape());
    outline.getPaint().setColor(0x80ffffff); // Transparent white.
    LayerDrawable background = new LayerDrawable(new Drawable[]{outline, mColoredCircleBackground});
    int strokeWidth = (int) (mDensity * 3);
    background.setLayerInset(1, strokeWidth, strokeWidth, strokeWidth, strokeWidth);
    return background;
  }

  private MFSquareTextView makeSquareTextView(Context context) {
    MFSquareTextView squareTextView = new MFSquareTextView(context);
    ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    squareTextView.setLayoutParams(layoutParams);
    squareTextView.setId(R.id.amu_text);
    int twelveDpi = (int) (12 * mDensity);
    squareTextView.setPadding(twelveDpi, twelveDpi, twelveDpi, twelveDpi);
    return squareTextView;
  }

  protected int getColor(int clusterSize) {
    final float hueRange = 220;
    final float sizeRange = 300;
    final float size = Math.min(clusterSize, sizeRange);
    final float hue = (sizeRange - size) * (sizeRange - size) / (sizeRange * sizeRange) * hueRange;
    return Color.HSVToColor(new float[]{
      hue, 1f, .6f
    });
  }

  @NonNull
  protected String getClusterText(int bucket) {
    if (bucket < BUCKETS[0]) {
      return String.valueOf(bucket);
    }
    return bucket + "+";
  }

  /**
   * Gets the "bucket" for a particular cluster. By default, uses the number of points within the
   * cluster, bucketed to some set points.
   */
  protected int getBucket(@NonNull MFCluster<T> cluster) {
    int size = cluster.getSize();
    if (size <= BUCKETS[0]) {
      return size;
    }
    for (int i = 0; i < BUCKETS.length - 1; i++) {
      if (size < BUCKETS[i + 1]) {
        return BUCKETS[i];
      }
    }
    return BUCKETS[BUCKETS.length - 1];
  }

  /**
   * Gets the minimum cluster size used to render clusters. For example, if "4" is returned,
   * then for any clusters of size 3 or less the items will be rendered as individual markers
   * instead of as a single cluster marker.
   *
   * @return the minimum cluster size used to render clusters. For example, if "4" is returned,
   * then for any clusters of size 3 or less the items will be rendered as individual markers
   * instead of as a single cluster marker.
   */
  public int getMinClusterSize() {
    return mMinClusterSize;
  }

  /**
   * Sets the minimum cluster size used to render clusters. For example, if "4" is provided,
   * then for any clusters of size 3 or less the items will be rendered as individual markers
   * instead of as a single cluster marker.
   *
   * @param minClusterSize the minimum cluster size used to render clusters. For example, if "4"
   *                       is provided, then for any clusters of size 3 or less the items will be
   *                       rendered as individual markers instead of as a single cluster marker.
   */
  public void setMinClusterSize(int minClusterSize) {
    mMinClusterSize = minClusterSize;
  }

  /**
   * Determine whether the cluster should be rendered as individual markers or a cluster.
   *
   * @param cluster cluster to examine for rendering
   * @return true if the provided cluster should be rendered as a single marker on the map, false
   * if the items within this cluster should be rendered as individual markers instead.
   */
  protected boolean shouldRenderAsCluster(@NonNull MFCluster<T> cluster) {
    return cluster.getSize() >= mMinClusterSize;
  }

  @Override
  public void onClustersChanged(Set<? extends MFCluster<T>> clusters) {
    mViewModifier.queue(clusters);
  }

  @Override
  public void setOnClusterClickListener(MFClusterManager.OnClusterClickListener<T> listener) {
    mClickListener = listener;
  }

  @Override
  public void setOnClusterInfoWindowClickListener(MFClusterManager.OnClusterInfoWindowClickListener<T> listener) {
    mInfoWindowClickListener = listener;
  }

  @Override
  public void setOnClusterInfoWindowLongClickListener(MFClusterManager.OnClusterInfoWindowLongClickListener<T> listener) {
    mInfoWindowLongClickListener = listener;
  }

  @Override
  public void setOnClusterItemClickListener(MFClusterManager.OnClusterItemClickListener<T> listener) {
    mItemClickListener = listener;
  }

  @Override
  public void setOnClusterItemInfoWindowClickListener(MFClusterManager.OnClusterItemInfoWindowClickListener<T> listener) {
    mItemInfoWindowClickListener = listener;
  }

  @Override
  public void setOnClusterItemInfoWindowLongClickListener(MFClusterManager.OnClusterItemInfoWindowLongClickListener<T> listener) {
    mItemInfoWindowLongClickListener = listener;
  }

  @Override
  public void setAnimation(boolean animate) {
    mAnimate = animate;
  }

  private Point findClosestCluster(List<Point> markers, Point point) {
    if (markers == null || markers.isEmpty()) return null;

    int maxDistance = mClusterManager.getAlgorithm().getMaxDistanceBetweenClusteredItems();
    double minDistSquared = maxDistance * maxDistance;
    Point closest = null;
    for (Point candidate : markers) {
      double dist = distanceSquared(candidate, point);
      if (dist < minDistSquared) {
        closest = candidate;
        minDistSquared = dist;
      }
    }
    return closest;
  }

  /**
   * Called before the marker for a ClusterItem is added to the map. The default implementation
   * sets the marker and snippet text based on the respective item text if they are both
   * available, otherwise it will set the title if available, and if not it will set the marker
   * title to the item snippet text if that is available.
   * <p>
   * The first time {@link MFClusterManager#cluster()} is invoked on a set of items
   * {@link #onBeforeClusterItemRendered(MFClusterItem, vn.map4d.map.annotations.MFMarkerOptions)} will be called and
   * {@link #onClusterItemUpdated(MFClusterItem, MFMarker)} will not be called.
   * If an item is removed and re-added (or updated) and {@link MFClusterManager#cluster()} is
   * invoked again, then {@link #onClusterItemUpdated(MFClusterItem, MFMarker)} will be called and
   * {@link #onBeforeClusterItemRendered(MFClusterItem, vn.map4d.map.annotations.MFMarkerOptions)} will not be called.
   *
   * @param item          item to be rendered
   * @param markerOptions the markerOptions representing the provided item
   */
  protected void onBeforeClusterItemRendered(@NonNull T item, @NonNull MFMarkerOptions markerOptions) {
    if (item.getTitle() != null && item.getSnippet() != null) {
      markerOptions.title(item.getTitle());
      markerOptions.snippet(item.getSnippet());
    } else if (item.getTitle() != null) {
      markerOptions.title(item.getTitle());
    } else if (item.getSnippet() != null) {
      markerOptions.title(item.getSnippet());
    }
  }

  /**
   * Called when a cached marker for a ClusterItem already exists on the map so the marker may
   * be updated to the latest item values. Default implementation updates the title and snippet
   * of the marker if they have changed and refreshes the info window of the marker if it is open.
   * Note that the contents of the item may not have changed since the cached marker was created -
   * implementations of this method are responsible for checking if something changed (if that
   * matters to the implementation).
   * <p>
   * The first time {@link MFClusterManager#cluster()} is invoked on a set of items
   * {@link #onBeforeClusterItemRendered(MFClusterItem, MFMarkerOptions)} will be called and
   * {@link #onClusterItemUpdated(MFClusterItem, MFMarker)} will not be called.
   * If an item is removed and re-added (or updated) and {@link MFClusterManager#cluster()} is
   * invoked again, then {@link #onClusterItemUpdated(MFClusterItem, MFMarker)} will be called and
   * {@link #onBeforeClusterItemRendered(MFClusterItem, MFMarkerOptions)} will not be called.
   *
   * @param item   item being updated
   * @param marker cached marker that contains a potentially previous state of the item.
   */
  protected void onClusterItemUpdated(@NonNull T item, @NonNull MFMarker marker) {
    boolean changed = false;
    // Update marker text if the item text changed - same logic as adding marker in CreateMarkerTask.perform()
    if (item.getTitle() != null && item.getSnippet() != null) {
      if (!item.getTitle().equals(marker.getTitle())) {
        marker.setTitle(item.getTitle());
        changed = true;
      }
      if (!item.getSnippet().equals(marker.getSnippet())) {
        marker.setSnippet(item.getSnippet());
        changed = true;
      }
    } else if (item.getSnippet() != null && !item.getSnippet().equals(marker.getTitle())) {
      marker.setTitle(item.getSnippet());
      changed = true;
    } else if (item.getTitle() != null && !item.getTitle().equals(marker.getTitle())) {
      marker.setTitle(item.getTitle());
      changed = true;
    }
    // Update marker position if the item changed position
    if (!marker.getPosition().equals(item.getPosition())) {
      marker.setPosition(item.getPosition());
      changed = true;
    }
    if (changed && marker.isInfoWindowShown()) {
      // Force a refresh of marker info window contents
      marker.showInfoWindow();
    }
  }

  /**
   * Called before the marker for a Cluster is added to the map.
   * The default implementation draws a circle with a rough count of the number of items.
   * <p>
   * The first time {@link MFClusterManager#cluster()} is invoked on a set of items
   * {@link #onBeforeClusterRendered(MFCluster, MFMarkerOptions)} will be called and
   * {@link #onClusterUpdated(MFCluster, MFMarker)} will not be called. If an item is removed and
   * re-added (or updated) and {@link MFClusterManager#cluster()} is invoked
   * again, then {@link #onClusterUpdated(MFCluster, MFMarker)} will be called and
   * {@link #onBeforeClusterRendered(MFCluster, MFMarkerOptions)} will not be called.
   *
   * @param cluster       cluster to be rendered
   * @param markerOptions markerOptions representing the provided cluster
   */
  protected void onBeforeClusterRendered(@NonNull MFCluster<T> cluster, @NonNull MFMarkerOptions markerOptions) {
    // TODO: consider adding anchor(.5, .5) (Individual markers will overlap more often)
    markerOptions.icon(getDescriptorForCluster(cluster));
  }

  /**
   * Gets a BitmapDescriptor for the given cluster that contains a rough count of the number of
   * items. Used to set the cluster marker icon in the default implementations of
   * {@link #onBeforeClusterRendered(MFCluster, MFMarkerOptions)} and
   * {@link #onClusterUpdated(MFCluster, MFMarker)}.
   *
   * @param cluster cluster to get BitmapDescriptor for
   * @return a BitmapDescriptor for the marker icon for the given cluster that contains a rough
   * count of the number of items.
   */
  @NonNull
  protected MFBitmapDescriptor getDescriptorForCluster(@NonNull MFCluster<T> cluster) {
    int bucket = getBucket(cluster);
    MFBitmapDescriptor descriptor = mIcons.get(bucket);
    if (descriptor == null) {
      mColoredCircleBackground.getPaint().setColor(getColor(bucket));
      descriptor = MFBitmapDescriptorFactory.fromBitmap(mIconGenerator.makeIcon(getClusterText(bucket)));
      mIcons.put(bucket, descriptor);
    }
    return descriptor;
  }

  /**
   * Called after the marker for a Cluster has been added to the map.
   *
   * @param cluster the cluster that was just added to the map
   * @param marker  the marker representing the cluster that was just added to the map
   */
  protected void onClusterRendered(@NonNull MFCluster<T> cluster, @NonNull MFMarker marker) {
  }

  /**
   * Called when a cached marker for a Cluster already exists on the map so the marker may
   * be updated to the latest cluster values. Default implementation updated the icon with a
   * circle with a rough count of the number of items. Note that the contents of the cluster may
   * not have changed since the cached marker was created - implementations of this method are
   * responsible for checking if something changed (if that matters to the implementation).
   * <p>
   * The first time {@link MFClusterManager#cluster()} is invoked on a set of items
   * {@link #onBeforeClusterRendered(MFCluster, MFMarkerOptions)} will be called and
   * {@link #onClusterUpdated(MFCluster, MFMarker)} will not be called. If an item is removed and
   * re-added (or updated) and {@link MFClusterManager#cluster()} is invoked
   * again, then {@link #onClusterUpdated(MFCluster, MFMarker)} will be called and
   * {@link #onBeforeClusterRendered(MFCluster, MFMarkerOptions)} will not be called.
   *
   * @param cluster cluster being updated
   * @param marker  cached marker that contains a potentially previous state of the cluster
   */
  protected void onClusterUpdated(@NonNull MFCluster<T> cluster, @NonNull MFMarker marker) {
    // TODO: consider adding anchor(.5, .5) (Individual markers will overlap more often)
    marker.setIcon(getDescriptorForCluster(cluster));
  }

  /**
   * Called after the marker for a ClusterItem has been added to the map.
   *
   * @param clusterItem the item that was just added to the map
   * @param marker      the marker representing the item that was just added to the map
   */
  protected void onClusterItemRendered(@NonNull T clusterItem, @NonNull MFMarker marker) {
  }

  /**
   * Get the marker from a ClusterItem
   *
   * @param clusterItem ClusterItem which you will obtain its marker
   * @return a marker from a ClusterItem or null if it does not exists
   */
  public MFMarker getMarker(T clusterItem) {
    return mMarkerCache.get(clusterItem);
  }

  /**
   * Get the ClusterItem from a marker
   *
   * @param marker which you will obtain its ClusterItem
   * @return a ClusterItem from a marker or null if it does not exists
   */
  public T getClusterItem(MFMarker marker) {
    return mMarkerCache.get(marker);
  }

  /**
   * Get the marker from a Cluster
   *
   * @param cluster which you will obtain its marker
   * @return a marker from a cluster or null if it does not exists
   */
  public MFMarker getMarker(MFCluster<T> cluster) {
    return mClusterMarkerCache.get(cluster);
  }

  /**
   * Get the Cluster from a marker
   *
   * @param marker which you will obtain its Cluster
   * @return a Cluster from a marker or null if it does not exists
   */
  public MFCluster<T> getCluster(MFMarker marker) {
    return mClusterMarkerCache.get(marker);
  }

  /**
   * A cache of markers representing individual ClusterItems.
   */
  private static class MarkerCache<T> {
    private Map<T, MFMarker> mCache = new HashMap<>();
    private Map<MFMarker, T> mCacheReverse = new HashMap<>();

    public MFMarker get(T item) {
      return mCache.get(item);
    }

    public T get(MFMarker m) {
      return mCacheReverse.get(m);
    }

    public void put(T item, MFMarker m) {
      mCache.put(item, m);
      mCacheReverse.put(m, item);
    }

    public void remove(MFMarker m) {
      T item = mCacheReverse.get(m);
      mCacheReverse.remove(m);
      mCache.remove(item);
    }
  }

  /**
   * A Marker and its position. {@link MFMarker#getPosition()} must be called from the UI thread, so this
   * object allows lookup from other threads.
   */
  private static class MarkerWithPosition {
    private final MFMarker marker;
    private MFLocationCoordinate position;

    private MarkerWithPosition(MFMarker marker) {
      this.marker = marker;
      position = marker.getPosition();
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof MarkerWithPosition) {
        return marker.equals(((MarkerWithPosition) other).marker);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return marker.hashCode();
    }
  }

  /**
   * ViewModifier ensures only one re-rendering of the view occurs at a time, and schedules
   * re-rendering, which is performed by the RenderTask.
   */
  @SuppressLint("HandlerLeak")
  private class ViewModifier extends Handler {
    private static final int RUN_TASK = 0;
    private static final int TASK_FINISHED = 1;
    private boolean mViewModificationInProgress = false;
    private RenderTask mNextClusters = null;

    @Override
    public void handleMessage(Message msg) {
      if (msg.what == TASK_FINISHED) {
        mViewModificationInProgress = false;
        if (mNextClusters != null) {
          // Run the task that was queued up.
          sendEmptyMessage(RUN_TASK);
        }
        return;
      }
      removeMessages(RUN_TASK);

      if (mViewModificationInProgress) {
        // Busy - wait for the callback.
        return;
      }

      if (mNextClusters == null) {
        // Nothing to do.
        return;
      }
      MFProjection projection = mMap.getProjection();

      RenderTask renderTask;
      synchronized (this) {
        renderTask = mNextClusters;
        mNextClusters = null;
        mViewModificationInProgress = true;
      }

      renderTask.setCallback(new Runnable() {
        @Override
        public void run() {
          sendEmptyMessage(TASK_FINISHED);
        }
      });
      renderTask.setProjection(projection);
      renderTask.setMapZoom(mMap.getCameraPosition().getZoom());
      mExecutor.execute(renderTask);
    }

    public void queue(Set<? extends MFCluster<T>> clusters) {
      synchronized (this) {
        // Overwrite any pending cluster tasks - we don't care about intermediate states.
        mNextClusters = new RenderTask(clusters);
      }
      sendEmptyMessage(RUN_TASK);
    }
  }

  /**
   * Transforms the current view (represented by DefaultClusterRenderer.mClusters and DefaultClusterRenderer.mZoom) to a
   * new zoom level and set of clusters.
   * <p/>
   * This must be run off the UI thread. Work is coordinated in the RenderTask, then queued up to
   * be executed by a MarkerModifier.
   * <p/>
   * There are three stages for the render:
   * <p/>
   * 1. Markers are added to the map
   * <p/>
   * 2. Markers are animated to their final position
   * <p/>
   * 3. Any old markers are removed from the map
   * <p/>
   * When zooming in, markers are animated out from the nearest existing cluster. When zooming
   * out, existing clusters are animated to the nearest new cluster.
   */
  private class RenderTask implements Runnable {
    final Set<? extends MFCluster<T>> clusters;
    private Runnable mCallback;
    private MFProjection mProjection;
    private MFSphericalMercatorProjection mSphericalMercatorProjection;
    private double mMapZoom;

    private RenderTask(Set<? extends MFCluster<T>> clusters) {
      this.clusters = clusters;
    }

    /**
     * A callback to be run when all work has been completed.
     *
     * @param callback
     */
    public void setCallback(Runnable callback) {
      mCallback = callback;
    }

    public void setProjection(MFProjection projection) {
      this.mProjection = projection;
    }

    public void setMapZoom(double zoom) {
      this.mMapZoom = zoom;
      this.mSphericalMercatorProjection = new MFSphericalMercatorProjection(256 * Math.pow(2, Math.min(zoom, mZoom)));
    }

    @SuppressLint("NewApi")
    public void run() {
      if (clusters.equals(MFDefaultClusterRenderer.this.mClusters)) {
        mCallback.run();
        return;
      }

      final MarkerModifier markerModifier = new MarkerModifier();

      final double zoom = mMapZoom;
      final boolean zoomingIn = zoom > mZoom;
      final double zoomDelta = zoom - mZoom;

      final Set<MarkerWithPosition> markersToRemove = mMarkers;

      MFCoordinateBounds visibleBounds;
      try {
        visibleBounds = mMap.getBounds();
      } catch (Exception e) {
        e.printStackTrace();
        visibleBounds = MFCoordinateBounds.builder()
          .include(new MFLocationCoordinate(0, 0))
          .build();
      }
      // TODO: Add some padding, so that markers can animate in from off-screen.

      // Find all of the existing clusters that are on-screen. These are candidates for
      // markers to animate from.
      List<Point> existingClustersOnScreen = null;
      if (MFDefaultClusterRenderer.this.mClusters != null && mAnimate) {
        existingClustersOnScreen = new ArrayList<>();
        for (MFCluster<T> c : MFDefaultClusterRenderer.this.mClusters) {
          if (shouldRenderAsCluster(c) && visibleBounds.contains(c.getPosition())) {
            Point point = mSphericalMercatorProjection.toPoint(c.getPosition());
            existingClustersOnScreen.add(point);
          }
        }
      }

      // Create the new markers and animate them to their new positions.
      final Set<MarkerWithPosition> newMarkers = Collections.newSetFromMap(
        new ConcurrentHashMap<MarkerWithPosition, Boolean>());
      for (MFCluster<T> c : clusters) {
        boolean onScreen = visibleBounds.contains(c.getPosition());
        if (zoomingIn && onScreen && mAnimate) {
          Point point = mSphericalMercatorProjection.toPoint(c.getPosition());
          Point closest = findClosestCluster(existingClustersOnScreen, point);
          if (closest != null) {
            MFLocationCoordinate animateTo = mSphericalMercatorProjection.toLocationCoordinate(closest);
            markerModifier.add(true, new CreateMarkerTask(c, newMarkers, animateTo));
          } else {
            markerModifier.add(true, new CreateMarkerTask(c, newMarkers, null));
          }
        } else {
          markerModifier.add(onScreen, new CreateMarkerTask(c, newMarkers, null));
        }
      }

      // Wait for all markers to be added.
      markerModifier.waitUntilFree();

      // Don't remove any markers that were just added. This is basically anything that had
      // a hit in the MarkerCache.
      markersToRemove.removeAll(newMarkers);

      // Find all of the new clusters that were added on-screen. These are candidates for
      // markers to animate from.
      List<Point> newClustersOnScreen = null;
      if (mAnimate) {
        newClustersOnScreen = new ArrayList<>();
        for (MFCluster<T> c : clusters) {
          if (shouldRenderAsCluster(c) && visibleBounds.contains(c.getPosition())) {
            Point p = mSphericalMercatorProjection.toPoint(c.getPosition());
            newClustersOnScreen.add(p);
          }
        }
      }

      // Remove the old markers, animating them into clusters if zooming out.
      for (final MarkerWithPosition marker : markersToRemove) {
        boolean onScreen = visibleBounds.contains(marker.position);
        // Don't animate when zooming out more than 3 zoom levels.
        // TODO: drop animation based on speed of device & number of markers to animate.
        if (!zoomingIn && zoomDelta > -3 && onScreen && mAnimate) {
          final Point point = mSphericalMercatorProjection.toPoint(marker.position);
          final Point closest = findClosestCluster(newClustersOnScreen, point);
          if (closest != null) {
            MFLocationCoordinate animateTo = mSphericalMercatorProjection.toLocationCoordinate(closest);
            markerModifier.animateThenRemove(marker, marker.position, animateTo);
          } else {
            markerModifier.remove(true, marker.marker);
          }
        } else {
          markerModifier.remove(onScreen, marker.marker);
        }
      }

      markerModifier.waitUntilFree();

      mMarkers = newMarkers;
      MFDefaultClusterRenderer.this.mClusters = clusters;
      mZoom = zoom;

      mCallback.run();
    }
  }

  /**
   * Handles all markerWithPosition manipulations on the map. Work (such as adding, removing, or
   * animating a markerWithPosition) is performed while trying not to block the rest of the app's
   * UI.
   */
  @SuppressLint("HandlerLeak")
  private class MarkerModifier extends Handler implements MessageQueue.IdleHandler {
    private static final int BLANK = 0;

    private final Lock lock = new ReentrantLock();
    private final Condition busyCondition = lock.newCondition();

    private Queue<CreateMarkerTask> mCreateMarkerTasks = new LinkedList<>();
    private Queue<CreateMarkerTask> mOnScreenCreateMarkerTasks = new LinkedList<>();
    private Queue<MFMarker> mRemoveMarkerTasks = new LinkedList<>();
    private Queue<MFMarker> mOnScreenRemoveMarkerTasks = new LinkedList<>();
    private Queue<AnimationTask> mAnimationTasks = new LinkedList<>();

    /**
     * Whether the idle listener has been added to the UI thread's MessageQueue.
     */
    private boolean mListenerAdded;

    private MarkerModifier() {
      super(Looper.getMainLooper());
    }

    /**
     * Creates markers for a cluster some time in the future.
     *
     * @param priority whether this operation should have priority.
     */
    public void add(boolean priority, CreateMarkerTask c) {
      lock.lock();
      sendEmptyMessage(BLANK);
      if (priority) {
        mOnScreenCreateMarkerTasks.add(c);
      } else {
        mCreateMarkerTasks.add(c);
      }
      lock.unlock();
    }

    /**
     * Removes a markerWithPosition some time in the future.
     *
     * @param priority whether this operation should have priority.
     * @param m        the markerWithPosition to remove.
     */
    public void remove(boolean priority, MFMarker m) {
      lock.lock();
      sendEmptyMessage(BLANK);
      if (priority) {
        mOnScreenRemoveMarkerTasks.add(m);
      } else {
        mRemoveMarkerTasks.add(m);
      }
      lock.unlock();
    }

    /**
     * Animates a markerWithPosition some time in the future.
     *
     * @param marker the markerWithPosition to animate.
     * @param from   the position to animate from.
     * @param to     the position to animate to.
     */
    public void animate(MarkerWithPosition marker, MFLocationCoordinate from, MFLocationCoordinate to) {
      lock.lock();
      mAnimationTasks.add(new AnimationTask(marker, from, to));
      lock.unlock();
    }

    /**
     * Animates a markerWithPosition some time in the future, and removes it when the animation
     * is complete.
     *
     * @param marker the markerWithPosition to animate.
     * @param from   the position to animate from.
     * @param to     the position to animate to.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void animateThenRemove(MarkerWithPosition marker, MFLocationCoordinate from, MFLocationCoordinate to) {
      lock.lock();
      AnimationTask animationTask = new AnimationTask(marker, from, to);
      animationTask.removeOnAnimationComplete(mClusterManager.getMarkerManager());
      mAnimationTasks.add(animationTask);
      lock.unlock();
    }

    @Override
    public void handleMessage(Message msg) {
      if (!mListenerAdded) {
        Looper.myQueue().addIdleHandler(this);
        mListenerAdded = true;
      }
      removeMessages(BLANK);

      lock.lock();
      try {

        // Perform up to 10 tasks at once.
        // Consider only performing 10 remove tasks, not adds and animations.
        // Removes are relatively slow and are much better when batched.
        for (int i = 0; i < 10; i++) {
          performNextTask();
        }

        if (!isBusy()) {
          mListenerAdded = false;
          Looper.myQueue().removeIdleHandler(this);
          // Signal any other threads that are waiting.
          busyCondition.signalAll();
        } else {
          // Sometimes the idle queue may not be called - schedule up some work regardless
          // of whether the UI thread is busy or not.
          // TODO: try to remove this.
          sendEmptyMessageDelayed(BLANK, 10);
        }
      } finally {
        lock.unlock();
      }
    }

    /**
     * Perform the next task. Prioritise any on-screen work.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void performNextTask() {
      if (!mOnScreenRemoveMarkerTasks.isEmpty()) {
        removeMarker(mOnScreenRemoveMarkerTasks.poll());
      } else if (!mAnimationTasks.isEmpty()) {
        mAnimationTasks.poll().perform();
      } else if (!mOnScreenCreateMarkerTasks.isEmpty()) {
        mOnScreenCreateMarkerTasks.poll().perform(this);
      } else if (!mCreateMarkerTasks.isEmpty()) {
        mCreateMarkerTasks.poll().perform(this);
      } else if (!mRemoveMarkerTasks.isEmpty()) {
        removeMarker(mRemoveMarkerTasks.poll());
      }
    }

    private void removeMarker(MFMarker m) {
      mMarkerCache.remove(m);
      mClusterMarkerCache.remove(m);
      mClusterManager.getMarkerManager().remove(m);
    }

    /**
     * @return true if there is still work to be processed.
     */
    public boolean isBusy() {
      try {
        lock.lock();
        return !(mCreateMarkerTasks.isEmpty() && mOnScreenCreateMarkerTasks.isEmpty() &&
          mOnScreenRemoveMarkerTasks.isEmpty() && mRemoveMarkerTasks.isEmpty() &&
          mAnimationTasks.isEmpty()
        );
      } finally {
        lock.unlock();
      }
    }

    /**
     * Blocks the calling thread until all work has been processed.
     */
    public void waitUntilFree() {
      while (isBusy()) {
        // Sometimes the idle queue may not be called - schedule up some work regardless
        // of whether the UI thread is busy or not.
        // TODO: try to remove this.
        sendEmptyMessage(BLANK);
        lock.lock();
        try {
          if (isBusy()) {
            busyCondition.await();
          }
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        } finally {
          lock.unlock();
        }
      }
    }

    @Override
    public boolean queueIdle() {
      // When the UI is not busy, schedule some work.
      sendEmptyMessage(BLANK);
      return true;
    }
  }

  /**
   * Creates markerWithPosition(s) for a particular cluster, animating it if necessary.
   */
  private class CreateMarkerTask {
    private final MFCluster<T> cluster;
    private final Set<MarkerWithPosition> newMarkers;
    private final MFLocationCoordinate animateFrom;

    /**
     * @param c            the cluster to render.
     * @param markersAdded a collection of markers to append any created markers.
     * @param animateFrom  the location to animate the markerWithPosition from, or null if no
     *                     animation is required.
     */
    public CreateMarkerTask(MFCluster<T> c, Set<MarkerWithPosition> markersAdded, MFLocationCoordinate animateFrom) {
      this.cluster = c;
      this.newMarkers = markersAdded;
      this.animateFrom = animateFrom;
    }

    private void perform(MarkerModifier markerModifier) {
      // Don't show small clusters. Render the markers inside, instead.
      if (!shouldRenderAsCluster(cluster)) {
        for (T item : cluster.getItems()) {
          MFMarker marker = mMarkerCache.get(item);
          MarkerWithPosition markerWithPosition;
          if (marker == null) {
            MFMarkerOptions markerOptions = new MFMarkerOptions();
            if (animateFrom != null) {
              markerOptions.position(animateFrom);
            } else {
              markerOptions.position(item.getPosition());
            }
            onBeforeClusterItemRendered(item, markerOptions);
            marker = mClusterManager.getMarkerCollection().addMarker(markerOptions);
            markerWithPosition = new MarkerWithPosition(marker);
            mMarkerCache.put(item, marker);
            if (animateFrom != null) {
              markerModifier.animate(markerWithPosition, animateFrom, item.getPosition());
            }
          } else {
            markerWithPosition = new MarkerWithPosition(marker);
            onClusterItemUpdated(item, marker);
          }
          onClusterItemRendered(item, marker);
          newMarkers.add(markerWithPosition);
        }
        return;
      }

      MFMarker marker = mClusterMarkerCache.get(cluster);
      MarkerWithPosition markerWithPosition;
      if (marker == null) {
        MFMarkerOptions markerOptions = new MFMarkerOptions().
          position(animateFrom == null ? cluster.getPosition() : animateFrom);
        onBeforeClusterRendered(cluster, markerOptions);
        marker = mClusterManager.getClusterMarkerCollection().addMarker(markerOptions);
        mClusterMarkerCache.put(cluster, marker);
        markerWithPosition = new MarkerWithPosition(marker);
        if (animateFrom != null) {
          markerModifier.animate(markerWithPosition, animateFrom, cluster.getPosition());
        }
      } else {
        markerWithPosition = new MarkerWithPosition(marker);
        onClusterUpdated(cluster, marker);
      }
      onClusterRendered(cluster, marker);
      newMarkers.add(markerWithPosition);
    }
  }

  /**
   * Animates a markerWithPosition from one position to another. TODO: improve performance for
   * slow devices (e.g. Nexus S).
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
  private class AnimationTask extends AnimatorListenerAdapter implements ValueAnimator.AnimatorUpdateListener {
    private final MarkerWithPosition markerWithPosition;
    private final MFMarker marker;
    private final MFLocationCoordinate from;
    private final MFLocationCoordinate to;
    private boolean mRemoveOnComplete;
    private MFMarkerManager mMarkerManager;

    private AnimationTask(MarkerWithPosition markerWithPosition, MFLocationCoordinate from, MFLocationCoordinate to) {
      this.markerWithPosition = markerWithPosition;
      this.marker = markerWithPosition.marker;
      this.from = from;
      this.to = to;
    }

    public void perform() {
      ValueAnimator valueAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
      valueAnimator.setInterpolator(ANIMATION_INTERP);
      valueAnimator.addUpdateListener(this);
      valueAnimator.addListener(this);
      valueAnimator.start();
    }

    @Override
    public void onAnimationEnd(Animator animation) {
      if (mRemoveOnComplete) {
        mMarkerCache.remove(marker);
        mClusterMarkerCache.remove(marker);
        mMarkerManager.remove(marker);
      }
      markerWithPosition.position = to;
    }

    public void removeOnAnimationComplete(MFMarkerManager markerManager) {
      mMarkerManager = markerManager;
      mRemoveOnComplete = true;
    }

    @Override
    public void onAnimationUpdate(ValueAnimator valueAnimator) {
      float fraction = valueAnimator.getAnimatedFraction();
      double lat = (to.getLatitude() - from.getLatitude()) * fraction + from.getLatitude();
      double lngDelta = to.getLongitude() - from.getLongitude();

      // Take the shortest path across the 180th meridian.
      if (Math.abs(lngDelta) > 180) {
        lngDelta -= Math.signum(lngDelta) * 360;
      }
      double lng = lngDelta * fraction + from.getLongitude();
      MFLocationCoordinate position = new MFLocationCoordinate(lat, lng);
      marker.setPosition(position);
    }
  }
}
