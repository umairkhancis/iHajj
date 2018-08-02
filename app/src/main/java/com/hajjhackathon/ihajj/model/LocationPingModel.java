package com.hajjhackathon.ihajj.model;

/**
 * Created by umair.khan on 8/2/18.
 */

public class LocationPingModel {
  private double lat;
  private double lon;
  private long timestamp;
  private String deviceId;

  public LocationPingModel() {
  }

  public LocationPingModel(double lat, double lon, long timestamp, String deviceId) {
    this.lat = lat;
    this.lon = lon;
    this.timestamp = timestamp;
    this.deviceId = deviceId;
  }

  public double getLat() {
    return lat;
  }

  public void setLat(double lat) {
    this.lat = lat;
  }

  public double getLon() {
    return lon;
  }

  public void setLon(double lon) {
    this.lon = lon;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    LocationPingModel that = (LocationPingModel) o;

    if (Double.compare(that.lat, lat) != 0) {
      return false;
    }
    if (Double.compare(that.lon, lon) != 0) {
      return false;
    }
    if (timestamp != that.timestamp) {
      return false;
    }
    return deviceId != null ? deviceId.equals(that.deviceId) : that.deviceId == null;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = Double.doubleToLongBits(lat);
    result = (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(lon);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
    result = 31 * result + (deviceId != null ? deviceId.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "LocationPingModel{" +
            "lat=" + lat +
            ", lon=" + lon +
            ", timestamp=" + timestamp +
            ", deviceId='" + deviceId + '\'' +
            '}';
  }
}
