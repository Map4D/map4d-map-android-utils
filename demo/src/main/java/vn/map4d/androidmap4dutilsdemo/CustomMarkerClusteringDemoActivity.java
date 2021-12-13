package vn.map4d.androidmap4dutilsdemo;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import vn.map4d.androidmap4dutilsdemo.model.Person;
import vn.map4d.map.annotations.MFBitmapDescriptor;
import vn.map4d.map.annotations.MFBitmapDescriptorFactory;
import vn.map4d.map.annotations.MFMarker;
import vn.map4d.map.annotations.MFMarkerOptions;
import vn.map4d.map.camera.MFCameraUpdateFactory;
import vn.map4d.map.core.MFCoordinateBounds;
import vn.map4d.types.MFLocationCoordinate;
import vn.map4d.utils.android.clustering.MFCluster;
import vn.map4d.utils.android.clustering.MFClusterItem;
import vn.map4d.utils.android.clustering.MFClusterManager;
import vn.map4d.utils.android.clustering.view.MFDefaultClusterRenderer;
import vn.map4d.utils.android.ui.MFIconGenerator;

/**
 * Demonstrates heavy customisation of the look of rendered clusters.
 */
public class CustomMarkerClusteringDemoActivity extends BaseDemoActivity implements MFClusterManager.OnClusterClickListener<Person>, MFClusterManager.OnClusterInfoWindowClickListener<Person>, MFClusterManager.OnClusterItemClickListener<Person>, MFClusterManager.OnClusterItemInfoWindowClickListener<Person> {
  private MFClusterManager<Person> mClusterManager;
  private Random mRandom = new Random(1984);

  @Override
  public boolean onClusterClick(MFCluster<Person> cluster) {
    // Show a toast with some info when the cluster is clicked.
    String firstName = cluster.getItems().iterator().next().name;
    Toast.makeText(this, cluster.getSize() + " (including " + firstName + ")", Toast.LENGTH_SHORT).show();

    // Zoom in the cluster. Need to create LatLngBounds and including all the cluster items
    // inside of bounds, then animate to center of the bounds.

    // Create the builder to collect all essential cluster items for the bounds.
    MFCoordinateBounds.Builder builder = MFCoordinateBounds.builder();
    for (MFClusterItem item : cluster.getItems()) {
      builder.include(item.getPosition());
    }
    // Get the LatLngBounds
    final MFCoordinateBounds bounds = builder.build();

    // Animate camera to the bounds
    try {
      getMap().animateCamera(MFCameraUpdateFactory.newCoordinateBounds(bounds, 100));
    } catch (Exception e) {
      e.printStackTrace();
    }

    return true;
  }

  @Override
  public void onClusterInfoWindowClick(MFCluster<Person> cluster) {
    // Does nothing, but you could go to a list of the users.
  }

  @Override
  public boolean onClusterItemClick(Person item) {
    // Does nothing, but you could go into the user's profile page, for example.
    return false;
  }

  @Override
  public void onClusterItemInfoWindowClick(Person item) {
    // Does nothing, but you could go into the user's profile page, for example.
  }

  @Override
  protected void startDemo(boolean isRestore) {
    if (!isRestore) {
      getMap().moveCamera(MFCameraUpdateFactory.newCoordinateZoom(new MFLocationCoordinate(16.067615, 108.214951), 15f));
    }

    mClusterManager = new MFClusterManager<>(this, getMap());
    mClusterManager.setRenderer(new PersonRenderer());
    getMap().setOnCameraIdleListener(mClusterManager);
    getMap().setOnMarkerClickListener(mClusterManager);
    getMap().setOnInfoWindowClickListener(mClusterManager);
    mClusterManager.setOnClusterClickListener(this);
    mClusterManager.setOnClusterInfoWindowClickListener(this);
    mClusterManager.setOnClusterItemClickListener(this);
    mClusterManager.setOnClusterItemInfoWindowClickListener(this);

    addItems();
    mClusterManager.cluster();
  }

  private void addItems() {
    // http://www.flickr.com/photos/sdasmarchives/5036248203/
    mClusterManager.addItem(new Person(position(), "Walter", R.drawable.walter));

    // http://www.flickr.com/photos/usnationalarchives/4726917149/
    mClusterManager.addItem(new Person(position(), "Gran", R.drawable.gran));

    // http://www.flickr.com/photos/nypl/3111525394/
    mClusterManager.addItem(new Person(position(), "Ruth", R.drawable.ruth));

    // http://www.flickr.com/photos/smithsonian/2887433330/
    mClusterManager.addItem(new Person(position(), "Stefan", R.drawable.stefan));

    // http://www.flickr.com/photos/library_of_congress/2179915182/
    mClusterManager.addItem(new Person(position(), "Mechanic", R.drawable.mechanic));

    // http://www.flickr.com/photos/nationalmediamuseum/7893552556/
    mClusterManager.addItem(new Person(position(), "Yeats", R.drawable.yeats));

    // http://www.flickr.com/photos/sdasmarchives/5036231225/
    mClusterManager.addItem(new Person(position(), "John", R.drawable.john));

    // http://www.flickr.com/photos/anmm_thecommons/7694202096/
    mClusterManager.addItem(new Person(position(), "Trevor the Turtle", R.drawable.turtle));

    // http://www.flickr.com/photos/usnationalarchives/4726892651/
    mClusterManager.addItem(new Person(position(), "Teach", R.drawable.teacher));
  }

  private MFLocationCoordinate position() {
    return new MFLocationCoordinate(random(16.062204, 16.069875), random(108.206996, 108.220042));
  }

  private double random(double min, double max) {
    return mRandom.nextDouble() * (max - min) + min;
  }

  /**
   * Draws profile photos inside markers (using IconGenerator).
   * When there are multiple people in the cluster, draw multiple photos (using MultiDrawable).
   */
  private class PersonRenderer extends MFDefaultClusterRenderer<Person> {
    private final MFIconGenerator mIconGenerator = new MFIconGenerator(getApplicationContext());
    private final MFIconGenerator mClusterIconGenerator = new MFIconGenerator(getApplicationContext());
    private final ImageView mImageView;
    private final ImageView mClusterImageView;
    private final int mDimension;

    public PersonRenderer() {
      super(getApplicationContext(), getMap(), mClusterManager);

      View multiProfile = getLayoutInflater().inflate(R.layout.multi_profile, null);
      mClusterIconGenerator.setContentView(multiProfile);
      mClusterImageView = multiProfile.findViewById(R.id.image);

      mImageView = new ImageView(getApplicationContext());
      mDimension = (int) getResources().getDimension(R.dimen.custom_profile_image);
      mImageView.setLayoutParams(new ViewGroup.LayoutParams(mDimension, mDimension));
      int padding = (int) getResources().getDimension(R.dimen.custom_profile_padding);
      mImageView.setPadding(padding, padding, padding, padding);
      mIconGenerator.setContentView(mImageView);
    }

    @Override
    protected void onBeforeClusterItemRendered(@NonNull Person person, MFMarkerOptions markerOptions) {
      // Draw a single person - show their profile photo and set the info window to show their name
      markerOptions
        .icon(getItemIcon(person))
        .title(person.name);
    }

    @Override
    protected void onClusterItemUpdated(@NonNull Person person, MFMarker marker) {
      // Same implementation as onBeforeClusterItemRendered() (to update cached markers)
      marker.setIcon(getItemIcon(person));
      marker.setTitle(person.name);
    }

    /**
     * Get a descriptor for a single person (i.e., a marker outside a cluster) from their
     * profile photo to be used for a marker icon
     *
     * @param person person to return an BitmapDescriptor for
     * @return the person's profile photo as a BitmapDescriptor
     */
    private MFBitmapDescriptor getItemIcon(Person person) {
      mImageView.setImageResource(person.profilePhoto);
      Bitmap icon = mIconGenerator.makeIcon();
      return MFBitmapDescriptorFactory.fromBitmap(icon);
    }

    @Override
    protected void onBeforeClusterRendered(@NonNull MFCluster<Person> cluster, MFMarkerOptions markerOptions) {
      // Draw multiple people.
      // Note: this method runs on the UI thread. Don't spend too much time in here (like in this example).
      markerOptions.icon(getClusterIcon(cluster));
    }

    @Override
    protected void onClusterUpdated(@NonNull MFCluster<Person> cluster, MFMarker marker) {
      // Same implementation as onBeforeClusterRendered() (to update cached markers)
      marker.setIcon(getClusterIcon(cluster));
    }

    /**
     * Get a descriptor for multiple people (a cluster) to be used for a marker icon. Note: this
     * method runs on the UI thread. Don't spend too much time in here (like in this example).
     *
     * @param cluster cluster to draw a BitmapDescriptor for
     * @return a BitmapDescriptor representing a cluster
     */
    private MFBitmapDescriptor getClusterIcon(MFCluster<Person> cluster) {
      List<Drawable> profilePhotos = new ArrayList<>(Math.min(4, cluster.getSize()));
      int width = mDimension;
      int height = mDimension;

      for (Person p : cluster.getItems()) {
        // Draw 4 at most.
        if (profilePhotos.size() == 4) break;
        Drawable drawable = getResources().getDrawable(p.profilePhoto);
        drawable.setBounds(0, 0, width, height);
        profilePhotos.add(drawable);
      }
      MultiDrawable multiDrawable = new MultiDrawable(profilePhotos);
      multiDrawable.setBounds(0, 0, width, height);

      mClusterImageView.setImageDrawable(multiDrawable);
      Bitmap icon = mClusterIconGenerator.makeIcon(String.valueOf(cluster.getSize()));
      return MFBitmapDescriptorFactory.fromBitmap(icon);
    }

    @Override
    protected boolean shouldRenderAsCluster(MFCluster cluster) {
      // Always render clusters.
      return cluster.getSize() > 1;
    }
  }
}
