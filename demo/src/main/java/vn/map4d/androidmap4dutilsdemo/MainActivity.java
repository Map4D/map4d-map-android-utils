package vn.map4d.androidmap4dutilsdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
  private ViewGroup mListView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mListView = findViewById(R.id.list);

    addDemo("Clustering", ClusteringDemoActivity.class);
    addDemo("Clustering: Custom Look", CustomMarkerClusteringDemoActivity.class);
    addDemo("Clustering: 2K markers", BigClusteringDemoActivity.class);
    addDemo("Clustering: 20K only visible markers", VisibleClusteringDemoActivity.class);
    addDemo("Clustering: ViewModel", ClusteringViewModelDemoActivity.class);
  }

  private void addDemo(String demoName, Class<? extends Activity> activityClass) {
    Button b = new Button(this);
    ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    b.setLayoutParams(layoutParams);
    b.setText(demoName);
    b.setTag(activityClass);
    b.setOnClickListener(this);
    mListView.addView(b);
  }

  @Override
  public void onClick(View view) {
    Class activityClass = (Class) view.getTag();
    startActivity(new Intent(this, activityClass));
  }
}