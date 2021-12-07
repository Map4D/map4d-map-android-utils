package vn.map4d.androidmap4dutilsdemo.model;

import vn.map4d.types.MFLocationCoordinate;
import vn.map4d.utils.android.clustering.MFClusterItem;

public class MyItem implements MFClusterItem {
  private final MFLocationCoordinate mPosition;
  private String mTitle;
  private String mSnippet;

  public MyItem(double lat, double lng) {
    mPosition = new MFLocationCoordinate(lat, lng);
    mTitle = null;
    mSnippet = null;
  }

  public MyItem(double lat, double lng, String title, String snippet) {
    mPosition = new MFLocationCoordinate(lat, lng);
    mTitle = title;
    mSnippet = snippet;
  }

  @Override
  public MFLocationCoordinate getPosition() {
    return mPosition;
  }

  @Override
  public String getTitle() {
    return mTitle;
  }

  /**
   * Set the title of the marker
   *
   * @param title string to be set as title
   */
  public void setTitle(String title) {
    mTitle = title;
  }

  @Override
  public String getSnippet() {
    return mSnippet;
  }

  /**
   * Set the description of the marker
   *
   * @param snippet string to be set as snippet
   */
  public void setSnippet(String snippet) {
    mSnippet = snippet;
  }
}
