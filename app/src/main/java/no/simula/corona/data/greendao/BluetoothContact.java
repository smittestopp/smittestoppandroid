package no.simula.corona.data.greendao;


import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import no.simula.corona.Utils;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class BluetoothContact {

    @Id(autoincrement = true)
    private Long id;

    private Long timestamp;
    private String deviceId;
    private int rssi;
    private int txPower;
    private Boolean isUploaded = false;


    @Generated(hash = 1507368312)
    public BluetoothContact(Long id, Long timestamp, String deviceId, int rssi, int txPower,
            Boolean isUploaded) {
        this.id = id;
        this.timestamp = timestamp;
        this.deviceId = deviceId;
        this.rssi = rssi;
        this.txPower = txPower;
        this.isUploaded = isUploaded;
    }


    @Generated(hash = 352376836)
    public BluetoothContact() {
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


    public String getDeviceId() {
        return this.deviceId;
    }


    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }


    public int getRssi() {
        return this.rssi;
    }


    public void setRssi(int rssi) {
        this.rssi = rssi;
    }


    public int getTxPower() {
        return this.txPower;
    }


    public void setTxPower(int txPower) {
        this.txPower = txPower;
    }


    public Boolean getIsUploaded() {
        return this.isUploaded;
    }


    public void setIsUploaded(Boolean isUploaded) {
        this.isUploaded = isUploaded;
    }

}
