package vn.map4d.androidmap4dutilsdemo;

import android.widget.Toast;

import org.json.JSONException;

import java.io.InputStream;
import java.util.List;

import vn.map4d.androidmap4dutilsdemo.model.MyItem;
import vn.map4d.map.camera.MFCameraUpdateFactory;
import vn.map4d.types.MFLocationCoordinate;
import vn.map4d.utils.android.clustering.MFClusterManager;

public class BigClusteringDemoActivity extends BaseDemoActivity {
  private MFClusterManager<MyItem> mClusterManager;

  @Override
  protected void startDemo(boolean isRestore) {
    if (!isRestore) {
      getMap().moveCamera(MFCameraUpdateFactory.newCoordinateZoom(new MFLocationCoordinate(16.039713, 108.212329), 10));
    }

    mClusterManager = new MFClusterManager<>(this, getMap());

    getMap().setOnCameraIdleListener(mClusterManager);
    try {
      readItems();
    } catch (JSONException e) {
      Toast.makeText(this, "Problem reading list of markers.", Toast.LENGTH_LONG).show();
    }
  }

  private void readItems() throws JSONException {
    InputStream inputStream = getResources().openRawResource(R.raw.radar_search);
    List<MyItem> items = new MyItemReader().read(inputStream);
    for (int i = 0; i < 10; i++) {
      double offset = i / 60d;
      for (MyItem item : items) {
        MFLocationCoordinate position = item.getPosition();
        double lat = position.getLatitude() + offset;
        double lng = position.getLongitude() + offset;
        MyItem offsetItem = new MyItem(lat, lng);
        mClusterManager.addItem(offsetItem);
      }
    }
  }
}
