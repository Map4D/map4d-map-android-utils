package vn.map4d.utils.android.projection;

import vn.map4d.types.MFLocationCoordinate;
import vn.map4d.utils.android.geometry.Point;

public class SphericalMercatorProjection {
  final double mWorldWidth;

  public SphericalMercatorProjection(final double worldWidth) {
    mWorldWidth = worldWidth;
  }

  @SuppressWarnings("deprecation")
  public Point toPoint(final MFLocationCoordinate latLng) {
    final double x = latLng.getLongitude() / 360 + .5;
    final double siny = Math.sin(Math.toRadians(latLng.getLatitude()));
    final double y = 0.5 * Math.log((1 + siny) / (1 - siny)) / -(2 * Math.PI) + .5;

    return new Point(x * mWorldWidth, y * mWorldWidth);
  }

  public MFLocationCoordinate toLocationCoordinate(vn.map4d.utils.android.geometry.Point point) {
    final double x = point.x / mWorldWidth - 0.5;
    final double lng = x * 360;

    double y = .5 - (point.y / mWorldWidth);
    final double lat = 90 - Math.toDegrees(Math.atan(Math.exp(-y * 2 * Math.PI)) * 2);

    return new MFLocationCoordinate(lat, lng);
  }
}
