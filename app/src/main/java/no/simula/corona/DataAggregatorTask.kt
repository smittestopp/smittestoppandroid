package no.simula.corona

import android.content.Context
import android.location.Location.distanceBetween
import android.os.AsyncTask
import android.os.Build
import com.microsoft.appcenter.crashes.Crashes
import no.simula.corona.data.DatabaseProvider
import no.simula.corona.data.model.Measurement
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

class JsonBuilder {
    // based on max message size on IoT Hub backend
    private val maxMessageSize = 500

    private val chunks = mutableListOf(JSONArray())
    private val chunkSources = mutableListOf(mutableListOf<Long>())

    fun append(measurement: JSONObject) {
        chunks[currentChunk()].put(measurement)
    }

    fun addSource(id: Long?) {
        if (chunkSources.lastIndex < 0)
            return

        if (id != null)
            chunkSources[chunkSources.lastIndex].add(id)
    }

    private fun toJson(chunkNumber: Int): JSONObject {
        val v = JSONObject()

        v.put("appVersion", BuildConfig.VERSION_NAME)
        v.put("platform", "android")
        v.put("osVersion", Build.VERSION.RELEASE)
        v.put("model", "${Build.MANUFACTURER} ${Build.MODEL}")
        v.put("events", chunks[chunkNumber])

        return v
    }

    fun getChunks(): JsonChunk {
        val out = ArrayList<Pair<MutableList<Long>, JSONObject>>()

        for (chunk in chunks.indices) {
            val chunkJson = toJson(chunk)
            val p = Pair(chunkSources[chunk], chunkJson)
            out.add(p)
        }

        return JsonChunk(out)
    }

    fun isEmpty(): Boolean {
        return chunks.lastIndex < 0 || chunks[0].length() == 0
    }

    private fun currentChunk(): Int {
        if (chunks[chunks.lastIndex].length() >= maxMessageSize) {
            chunks.add(JSONArray())
            chunkSources.add(mutableListOf())
        }

        return chunks.lastIndex
    }
}

class JsonChunk(private val items: List<Pair<MutableList<Long>, JSONObject>>) {
    fun size(): Int {
        return items.size
    }

    fun get(i: Int): Pair<MutableList<Long>, JSONObject> {
        return items[i]
    }
}

data class JsonChunks(val gps: JsonChunk, val bluetooth: JsonChunk)

interface DataAggregatorListener {
    fun onJSonDataPrepared(chunks: JsonChunks?)
}

class DataAggregatorTask(
    var context: Context,
    private var callbackListener: DataAggregatorListener
) : AsyncTask<Any?, Any?, JsonChunks?>() {

    private val mDatabase = DatabaseProvider.open(context)

    override fun doInBackground(vararg params: Any?): JsonChunks? {
        var chunks: JsonChunks? = null

        try {
            chunks = prepareJson()
        } catch (ex: Exception) {
            Crashes.trackError(
                ex,
                mutableMapOf<String, String>().apply {
                    set(
                        "where",
                        "DataAggregator::doInBackground"
                    )
                },
                null
            )
        }

        return chunks
    }

    override fun onPostExecute(chunks: JsonChunks?) {
        super.onPostExecute(chunks)

        try {
            callbackListener.onJSonDataPrepared(chunks)
        } catch (ex: Exception) {
            Crashes.trackError(
                ex,
                mutableMapOf<String, String>().apply {
                    set(
                        "where",
                        "DataAggregator::postExecute"
                    )
                },
                null
            )
        } finally {
            mDatabase.close()
        }
    }

    /**
     * @return cleaned json to be uploaded
     */
    private fun prepareJson(): JsonChunks {
        // clear previously uploaded data
        mDatabase.deleteUploadedGPS()
        mDatabase.deleteUploadedBluetooth()

        // load and clean new data
        val gpsChunks = prepareGpsMeasurements()
        val bluetoothChunks = prepareBluetoothMeasurements()

        return JsonChunks(gpsChunks, bluetoothChunks)
    }

    /**
     * load new samples from the database
     * clean the samples, remove redundant samples, etc
     *
     * @return list of database IDs to all samples to be marked as isUploaded
     */
    private fun prepareGpsMeasurements(): JsonChunk {
        val builder = JsonBuilder()
        val locations = mDatabase.notUploadedGPS()
        Timber.i("Got ${locations.size} measurements to upload")

        if (locations.isEmpty()) {
            return builder.getChunks()
        }

        // keep only measurements at different locations and update timestamps
        val N = locations.size - 1
        var current = locations[0]
        builder.addSource(current.id)
        var numUploads = 0

        for (i in 1 until N) {
            current.timeTo = locations[i].timestamp
            if (!compareMeasurementLocations(locations[i], current)) {
                builder.append(current.toJson())
                numUploads += 1
                current = locations[i]
            }

            builder.addSource(locations[i].id)
        }
        // upload currentMeasurement
        builder.append(current.toJson())
        // no need to add source, was done in last loop iter
        numUploads += 1

        // upload last Measurement
        builder.append(locations[N].toJson())
        builder.addSource(locations[N].id)
        numUploads += 1

        Timber.i("After cleaning there are $numUploads measurements to upload")

        return builder.getChunks()
    }

    private fun prepareBluetoothMeasurements(): JsonChunk {
        val builder = JsonBuilder()
        val contacts = mDatabase.notUploadedBluetooth()

        if (contacts.isEmpty())
            return builder.getChunks()

        contacts.forEach { i ->
            builder.append(i.toJson())
            builder.addSource(i.id)
        }

        return builder.getChunks()
    }

    private fun compareMeasurementLocations(m1: Measurement, m2: Measurement): Boolean {
        // if any of the values is null keep both measurements
        if (m1.longitude == null || m1.latitude == null || m1.altitude == null ||
            m2.longitude == null || m2.latitude == null || m2.altitude == null
        ) {
            return false
        } else {
            // compare latitude and longitude
            val gpsAccuracy: Double = maxOf(
                m1.latLongAccuracy!!,
                m2.latLongAccuracy!!
            )
            val result = FloatArray(1)
            distanceBetween(
                m1.latitude, m1.longitude, m2.latitude, m2.longitude, result
            )
            if (result[0] > gpsAccuracy) {
                return false
            }
        }

        return true
    }

    fun isRunningOrPending(): Boolean {
        return status == AsyncTask.Status.RUNNING || status == AsyncTask.Status.PENDING
    }
}
