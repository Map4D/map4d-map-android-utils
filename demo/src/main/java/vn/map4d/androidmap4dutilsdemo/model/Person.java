package vn.map4d.androidmap4dutilsdemo.model;

import vn.map4d.types.MFLocationCoordinate;
import vn.map4d.utils.android.clustering.MFClusterItem;

public class Person implements MFClusterItem {
  public final String name;
  public final int profilePhoto;
  private final MFLocationCoordinate mPosition;

  public Person(MFLocationCoordinate position, String name, int pictureResource) {
    this.name = name;
    profilePhoto = pictureResource;
    mPosition = position;
  }

  @Override
  public MFLocationCoordinate getPosition() {
    return mPosition;
  }

  @Override
  public String getTitle() {
    return null;
  }

  @Override
  public String getSnippet() {
    return null;
  }
}
