package no.simula.corona.data.greendao;


import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Index;
import org.greenrobot.greendao.annotation.Transient;

import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.*;

import no.simula.corona.Utils;
import org.greenrobot.greendao.annotation.Generated;


@Entity
public class Measurement {

    @Id(autoincrement = true)
    private Long id;

    private Long timestamp;
    private Double latitude;
    private Double longitude;
    private Double latLongAccuracy;
    private Double altitude;
    private Double altitudeAccuracy;
    private Double speed;
    private Double speedAccuracy;
    private Boolean isUploaded = false;

    @Generated(hash = 1735522951)
    public Measurement(Long id, Long timestamp, Double latitude, Double longitude,
            Double latLongAccuracy, Double altitude, Double altitudeAccuracy, Double speed,
            Double speedAccuracy, Boolean isUploaded) {
        this.id = id;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.latLongAccuracy = latLongAccuracy;
        this.altitude = altitude;
        this.altitudeAccuracy = altitudeAccuracy;
        this.speed = speed;
        this.speedAccuracy = speedAccuracy;
        this.isUploaded = isUploaded;
    }

    @Generated(hash = 1439585572)
    public Measurement() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Double getLatitude() {
        return this.latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return this.longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatLongAccuracy() {
        return this.latLongAccuracy;
    }

    public void setLatLongAccuracy(Double latLongAccuracy) {
        this.latLongAccuracy = latLongAccuracy;
    }

    public Double getAltitude() {
        return this.altitude;
    }

    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }

    public Double getAltitudeAccuracy() {
        return this.altitudeAccuracy;
    }

    public void setAltitudeAccuracy(Double altitudeAccuracy) {
        this.altitudeAccuracy = altitudeAccuracy;
    }

    public Double getSpeed() {
        return this.speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public Double getSpeedAccuracy() {
        return this.speedAccuracy;
    }

    public void setSpeedAccuracy(Double speedAccuracy) {
        this.speedAccuracy = speedAccuracy;
    }

    public Boolean getIsUploaded() {
        return this.isUploaded;
    }

    public void setIsUploaded(Boolean isUploaded) {
        this.isUploaded = isUploaded;
    }

}
