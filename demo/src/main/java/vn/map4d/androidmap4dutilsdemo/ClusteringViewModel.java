package vn.map4d.androidmap4dutilsdemo;

import android.content.res.Resources;

import androidx.lifecycle.ViewModel;

import org.json.JSONException;

import java.io.InputStream;
import java.util.List;

import vn.map4d.androidmap4dutilsdemo.model.MyItem;
import vn.map4d.types.MFLocationCoordinate;
import vn.map4d.utils.android.clustering.algo.MFNonHierarchicalViewBasedAlgorithm;

public class ClusteringViewModel extends ViewModel {

  private MFNonHierarchicalViewBasedAlgorithm<MyItem> mAlgorithm = new MFNonHierarchicalViewBasedAlgorithm<>(0, 0);

  MFNonHierarchicalViewBasedAlgorithm<MyItem> getAlgorithm() {
    return mAlgorithm;
  }

  void readItems(Resources resources) throws JSONException {
    InputStream inputStream = resources.openRawResource(R.raw.radar_search);
    List<MyItem> items = new MyItemReader().read(inputStream);
    mAlgorithm.lock();
    try {
      for (int i = 0; i < 100; i++) {
        double offset = i / 60d;
        for (MyItem item : items) {
          MFLocationCoordinate position = item.getPosition();
          double lat = position.getLatitude() + offset;
          double lng = position.getLongitude() + offset;
          MyItem offsetItem = new MyItem(lat, lng);
          mAlgorithm.addItem(offsetItem);
        }
      }
    } finally {
      mAlgorithm.unlock();
    }
  }
}
