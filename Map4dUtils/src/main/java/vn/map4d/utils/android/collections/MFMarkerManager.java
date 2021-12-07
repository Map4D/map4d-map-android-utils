package vn.map4d.utils.android.collections;

import android.view.View;

import vn.map4d.map.annotations.MFMarker;
import vn.map4d.map.annotations.MFMarkerOptions;
import vn.map4d.map.core.Map4D;

/**
 * Keeps track of collections of markers on the map. Delegates all Marker-related events to each
 * collection's individually managed listeners.
 * <p/>
 * All marker operations (adds and removes) should occur via its collection class. That is, don't
 * add a marker via a collection, then remove it via Marker.remove()
 */
public class MFMarkerManager extends MFMapObjectManager<MFMarker, MFMarkerManager.Collection> implements
  Map4D.OnInfoWindowClickListener,
  Map4D.OnMarkerClickListener,
  Map4D.OnMarkerDragListener,
  Map4D.InfoWindowAdapter {

  public MFMarkerManager(Map4D map) {
    super(map);
  }

  @Override
  void setListenersOnUiThread() {
    if (mMap != null) {
      mMap.setOnInfoWindowClickListener(this);
      //mMap.setOnInfoWindowLongClickListener(this);
      mMap.setOnMarkerClickListener(this);
      mMap.setOnMarkerDragListener(this);
      mMap.setInfoWindowAdapter(this);
    }
  }

  public Collection newCollection() {
    return new Collection();
  }

  @Override
  public View getInfoWindow(MFMarker marker) {
    Collection collection = mAllObjects.get(marker);
    if (collection != null && collection.mInfoWindowAdapter != null) {
      return collection.mInfoWindowAdapter.getInfoWindow(marker);
    }
    return null;
  }

  @Override
  public View getInfoContents(MFMarker marker) {
    Collection collection = mAllObjects.get(marker);
    if (collection != null && collection.mInfoWindowAdapter != null) {
      return collection.mInfoWindowAdapter.getInfoContents(marker);
    }
    return null;
  }

  @Override
  public void onInfoWindowClick(MFMarker marker) {
    Collection collection = mAllObjects.get(marker);
    if (collection != null && collection.mInfoWindowClickListener != null) {
      collection.mInfoWindowClickListener.onInfoWindowClick(marker);
    }
  }

    /*@Override
    public void onInfoWindowLongClick(MFMarker marker) {
        Collection collection = mAllObjects.get(marker);
        if (collection != null && collection.mInfoWindowLongClickListener != null) {
            collection.mInfoWindowLongClickListener.onInfoWindowLongClick(marker);
        }
    }*/

  @Override
  public boolean onMarkerClick(MFMarker marker) {
    Collection collection = mAllObjects.get(marker);
    if (collection != null && collection.mMarkerClickListener != null) {
      return collection.mMarkerClickListener.onMarkerClick(marker);
    }
    return false;
  }

  @Override
  public void onMarkerDragStart(MFMarker marker) {
    Collection collection = mAllObjects.get(marker);
    if (collection != null && collection.mMarkerDragListener != null) {
      collection.mMarkerDragListener.onMarkerDragStart(marker);
    }
  }

  @Override
  public void onMarkerDrag(MFMarker marker) {
    Collection collection = mAllObjects.get(marker);
    if (collection != null && collection.mMarkerDragListener != null) {
      collection.mMarkerDragListener.onMarkerDrag(marker);
    }
  }

  @Override
  public void onMarkerDragEnd(MFMarker marker) {
    Collection collection = mAllObjects.get(marker);
    if (collection != null && collection.mMarkerDragListener != null) {
      collection.mMarkerDragListener.onMarkerDragEnd(marker);
    }
  }

  @Override
  protected void removeObjectFromMap(MFMarker object) {
    object.remove();
  }

  public class Collection extends MFMapObjectManager.Collection {
    private Map4D.OnInfoWindowClickListener mInfoWindowClickListener;
    //private Map4D.OnInfoWindowLongClickListener mInfoWindowLongClickListener;
    private Map4D.OnMarkerClickListener mMarkerClickListener;
    private Map4D.OnMarkerDragListener mMarkerDragListener;
    private Map4D.InfoWindowAdapter mInfoWindowAdapter;

    public Collection() {
    }

    public MFMarker addMarker(MFMarkerOptions opts) {
      MFMarker marker = mMap.addMarker(opts);
      super.add(marker);
      return marker;
    }

    public void addAll(java.util.Collection<MFMarkerOptions> opts) {
      for (MFMarkerOptions opt : opts) {
        addMarker(opt);
      }
    }

    public void addAll(java.util.Collection<MFMarkerOptions> opts, boolean defaultVisible) {
      for (MFMarkerOptions opt : opts) {
        addMarker(opt).setVisible(defaultVisible);
      }
    }

    public void showAll() {
      for (MFMarker marker : getMarkers()) {
        marker.setVisible(true);
      }
    }

    public void hideAll() {
      for (MFMarker marker : getMarkers()) {
        marker.setVisible(false);
      }
    }

    public boolean remove(MFMarker marker) {
      return super.remove(marker);
    }

    public java.util.Collection<MFMarker> getMarkers() {
      return getObjects();
    }

    public void setOnInfoWindowClickListener(Map4D.OnInfoWindowClickListener infoWindowClickListener) {
      mInfoWindowClickListener = infoWindowClickListener;
    }

        /*public void setOnInfoWindowLongClickListener(Map4D.OnInfoWindowLongClickListener infoWindowLongClickListener) {
            mInfoWindowLongClickListener = infoWindowLongClickListener;
        }*/

    public void setOnMarkerClickListener(Map4D.OnMarkerClickListener markerClickListener) {
      mMarkerClickListener = markerClickListener;
    }

    public void setOnMarkerDragListener(Map4D.OnMarkerDragListener markerDragListener) {
      mMarkerDragListener = markerDragListener;
    }

    public void setInfoWindowAdapter(Map4D.InfoWindowAdapter infoWindowAdapter) {
      mInfoWindowAdapter = infoWindowAdapter;
    }
  }
}
