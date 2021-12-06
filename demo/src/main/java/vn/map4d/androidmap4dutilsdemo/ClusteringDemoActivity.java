package vn.map4d.androidmap4dutilsdemo;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.InputStream;
import java.util.List;

import vn.map4d.androidmap4dutilsdemo.model.MyItem;
import vn.map4d.map.annotations.MFMarker;
import vn.map4d.map.camera.MFCameraUpdateFactory;
import vn.map4d.map.core.Map4D;
import vn.map4d.types.MFLocationCoordinate;
import vn.map4d.utils.android.clustering.MFClusterManager;

/**
 * Simple activity demonstrating ClusterManager.
 */
public class ClusteringDemoActivity extends BaseDemoActivity {
  private MFClusterManager<MyItem> mClusterManager;

  @Override
  protected void startDemo(boolean isRestore) {
    if (!isRestore) {
      getMap().moveCamera(MFCameraUpdateFactory.newLatLngZoom(new MFLocationCoordinate(16.039713, 108.212329), 10));
    }

    mClusterManager = new MFClusterManager<>(this, getMap());
    getMap().setOnCameraIdleListener(mClusterManager);

    // Add a custom InfoWindowAdapter by setting it to the MarkerManager.Collection object from
    // ClusterManager rather than from Map4D.setInfoWindowAdapter
    mClusterManager.getMarkerCollection().setInfoWindowAdapter(new Map4D.InfoWindowAdapter() {
      @Override
      public View getInfoWindow(MFMarker marker) {
        final LayoutInflater inflater = LayoutInflater.from(ClusteringDemoActivity.this);
        final View view = inflater.inflate(R.layout.custom_info_window, null);
        final TextView textView = view.findViewById(R.id.textViewTitle);
        String text = (marker.getTitle() != null && !marker.getTitle().isEmpty()) ? marker.getTitle() : "Cluster Item";
        textView.setText(text);
        return view;
      }

      @Override
      public View getInfoContents(MFMarker marker) {
        return null;
      }
    });
    mClusterManager.getMarkerCollection().setOnInfoWindowClickListener(marker ->
      Toast.makeText(ClusteringDemoActivity.this,
        "Info window clicked.",
        Toast.LENGTH_SHORT).show());

    try {
      readItems();
    } catch (JSONException e) {
      Toast.makeText(this, "Problem reading list of markers.", Toast.LENGTH_LONG).show();
    }
  }

  private void readItems() throws JSONException {
    InputStream inputStream = getResources().openRawResource(R.raw.radar_search);
    List<MyItem> items = new MyItemReader().read(inputStream);
    mClusterManager.addItems(items);
  }
}
