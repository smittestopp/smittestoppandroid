# IoTHub Message Format

Each IoTHub message sent from the app to the cloud includes the following fields:

- `appVersion`: `String` version of the app
- `model`: `String` on the form `manufacturer device` 
- `events`: `[EventData]` a list of either GPS or Bluetooth events, but never a mix of both. See below for more info.
- `platform`: `String` the value will always be `android` for the Android app.
- `osVersion`: `String` Android version running on the device.

`EventData` is either GPS data or BLE data as described below.

Which type of message is sent is defined by the property `eventType`. Possible values are `["gps", "bluetooth"]`

#### GPS Data
A GPS event has the following fields

- `timeFrom`: `String` ISO 8601 formatted date. The event details are valid from this timepoint.
- `timeTo`: `String` ISO 8601 formatted date. The event details are valid until this timepoint.
- `latitude`: `Double` the location latitude.
- `longitude`: `Double` the location longitude.
- `accuracy`: `Double` the radius of uncertainty for the location, measured in meters.
- `speed`: `Double` the instantaneous speed of the device, measured in meters per second.
- `speedAccuracy`: `Double` accuracy of the speed measurement, measured in meters per second. Only available on devices running Android 8.0 or newer. Older devices are currently sending `-1.0`
- `altitude`: `Double` the location altitude. The unit is meters.
- `altitudeAccuracy`: `Double` the accuracy of the altitude value, measured in meters. Only available on devices running Android 8.0 or newer. Older devices are currently sending `-1.0`

> GPS accuracies defines an interval with 68% confidence. For speed and altitude, the true measurement will be in the interval `[measurement - accuracy, measurement + accuracy]` with 68% probability. 


#### BLE Data

A Bluetooth event has the following fields

- `time`: `String` ISO 8601 formatted date. The time when the RSSI reading was acquired.
- `deviceId`: `String` the device identifier given by the other device.
- `rssi`: `Int` the RSSI value.
- `txPower`: `Int` the txPower advertised by the other device. Only available on Android 8.0 or newer. Older devices currently sends `0`.


## GPS event (Example)

```json
{
  "appVersion": "1.1.0",
  "model": "Sony I4312",
  "events": [
    {
      "timeFrom": "2020-04-30T12:38:30Z",
      "timeTo": "2020-04-30T12:38:30Z",
      "latitude": 61.93372532454498,
      "longitude": 10.728583389659596,
      "accuracy": 65.0,
      "speed": 69.41046905517578,
      "speedAccuracy": 5.65098190,
      "altitude": 71.1960678100586,
      "altitudeAccuracy": 10.0
    }
  ],
  "platform": "android",
  "osVersion": "8.1.0",
}
```

## BLE event (Example)

```json
{
  "appVersion": "1.1.0",
  "model": "Sony I4312",
  "events": [
    {
      "deviceId": "123456789abcd123456789abcd123456",
      "rssi": -90,
      "txPower": 12,
      "time": "2020-04-30T12:38:30Z"
    }
  ],
  "platform": "android",
  "osVersion": "8.1.0",
}
```
