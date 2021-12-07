package vn.map4d.androidmap4dutilsdemo;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProviders;

import org.json.JSONException;

import vn.map4d.androidmap4dutilsdemo.model.MyItem;
import vn.map4d.map.camera.MFCameraUpdateFactory;
import vn.map4d.types.MFLocationCoordinate;
import vn.map4d.utils.android.clustering.MFClusterManager;

public class ClusteringViewModelDemoActivity extends BaseDemoActivity {
  private MFClusterManager<MyItem> mClusterManager;
  private ClusteringViewModel mViewModel;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mViewModel = ViewModelProviders.of(this).get(ClusteringViewModel.class);
    if (savedInstanceState == null) {
      try {
        mViewModel.readItems(getResources());
      } catch (JSONException e) {
        Toast.makeText(this, "Problem reading list of markers.", Toast.LENGTH_LONG).show();
      }
    }
  }

  @Override
  protected void startDemo(boolean isRestore) {
    if (!isRestore) {
      getMap().moveCamera(MFCameraUpdateFactory.newCoordinateZoom(new MFLocationCoordinate(16.039713, 108.212329), 10));
    }

    DisplayMetrics metrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(metrics);

    int widthDp = (int) (metrics.widthPixels / metrics.density);
    int heightDp = (int) (metrics.heightPixels / metrics.density);

    mViewModel.getAlgorithm().updateViewSize(widthDp, heightDp);

    mClusterManager = new MFClusterManager<>(this, getMap());
    mClusterManager.setAlgorithm(mViewModel.getAlgorithm());

    getMap().setOnCameraIdleListener(mClusterManager);
  }
}
