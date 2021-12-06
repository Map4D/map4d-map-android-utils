package vn.map4d.utils.android.collections;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import vn.map4d.map.core.Map4D;

/**
 * Abstract base implementation for map object collection manager classes.
 * <p/>
 * Keeps track of collections of objects on the map. Delegates all object-related events to each
 * collection's individually managed listeners.
 * <p/>
 * All object operations (adds and removes) should occur via its collection class. That is, don't
 * add an object via a collection, then remove it via Object.remove()
 */
abstract class MFMapObjectManager<O, C extends MFMapObjectManager.Collection> {
  protected final Map4D mMap;
  protected final Map<O, C> mAllObjects = new HashMap<>();
  private final Map<String, C> mNamedCollections = new HashMap<>();

  public MFMapObjectManager(@NonNull Map4D map) {
    mMap = map;
    new Handler(Looper.getMainLooper()).post(new Runnable() {
      @Override
      public void run() {
        setListenersOnUiThread();
      }
    });
  }

  abstract void setListenersOnUiThread();

  public abstract C newCollection();

  /**
   * Create a new named collection, which can later be looked up by {@link #getCollection(String)}
   *
   * @param id a unique id for this collection.
   */
  public C newCollection(String id) {
    if (mNamedCollections.get(id) != null) {
      throw new IllegalArgumentException("collection id is not unique: " + id);
    }
    C collection = newCollection();
    mNamedCollections.put(id, collection);
    return collection;
  }

  /**
   * Gets a named collection that was created by {@link #newCollection(String)}
   *
   * @param id the unique id for this collection.
   */
  public C getCollection(String id) {
    return mNamedCollections.get(id);
  }

  /**
   * Removes an object from its collection.
   *
   * @param object the object to remove.
   * @return true if the object was removed.
   */
  public boolean remove(O object) {
    C collection = mAllObjects.get(object);
    return collection != null && collection.remove(object);
  }

  protected abstract void removeObjectFromMap(O object);

  public class Collection {
    private final Set<O> mObjects = new LinkedHashSet<>();

    public Collection() {
    }

    protected void add(O object) {
      mObjects.add(object);
      mAllObjects.put(object, (C) this);
    }

    protected boolean remove(O object) {
      if (mObjects.remove(object)) {
        mAllObjects.remove(object);
        removeObjectFromMap(object);
        return true;
      }
      return false;
    }

    public void clear() {
      for (O object : mObjects) {
        removeObjectFromMap(object);
        mAllObjects.remove(object);
      }
      mObjects.clear();
    }

    protected java.util.Collection<O> getObjects() {
      return Collections.unmodifiableCollection(mObjects);
    }
  }
}
