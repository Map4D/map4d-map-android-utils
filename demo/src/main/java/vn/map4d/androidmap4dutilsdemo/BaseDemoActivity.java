package vn.map4d.androidmap4dutilsdemo;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import vn.map4d.map.core.MFMapView;
import vn.map4d.map.core.Map4D;
import vn.map4d.map.core.OnMapReadyCallback;

public abstract class BaseDemoActivity extends FragmentActivity implements OnMapReadyCallback {
  private Map4D mMap;
  private boolean mIsRestore;

  protected int getLayoutId() {
    return R.layout.map;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mIsRestore = savedInstanceState != null;
    setContentView(getLayoutId());
    setUpMap();
  }

  @Override
  public void onMapReady(Map4D map) {
    if (mMap != null) {
      return;
    }
    mMap = map;
    startDemo(mIsRestore);
  }

  private void setUpMap() {
    ((MFMapView) findViewById(R.id.mapView)).getMapAsync(this);
  }

  /**
   * Run the demo-specific code.
   */
  protected abstract void startDemo(boolean isRestore);

  protected Map4D getMap() {
    return mMap;
  }
}
