package com.example.barotrend

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.barotrend.databinding.ActivityMainBinding
import com.github.anastr.speedviewlib.SpeedView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import java.math.RoundingMode


class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sensorManager: SensorManager
    private var pressureSensor: Sensor? = null
    lateinit var lineChart: LineChart
    private var db: DBHelper? = DBHelper(this, null)
    private lateinit var speedometer: SpeedView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        startService(Intent(applicationContext, MyService::class.java))

        speedometer = findViewById(R.id.speedView)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        if (pressureSensor == null) {
            Toast.makeText(this, "Barometer sensor not found", Toast.LENGTH_SHORT).show()
        }

        lineChart = findViewById(R.id.lineChart)
        lineChart.setDrawGridBackground(false)
        lineChart.description.isEnabled = false
        lineChart.setTouchEnabled(false)
        lineChart.isDragEnabled = false
        lineChart.axisLeft.setDrawGridLines(false);
        lineChart.axisRight.setDrawGridLines(false);
        lineChart.xAxis.setDrawGridLines(false);

        val dataSet = LineDataSet(getEntries(), "Line Data")
        dataSet.color = Color.GREEN
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.GREEN
        dataSet.valueTextColor = Color.WHITE

        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.invalidate()
    }

    @SuppressLint("Range")
    private fun getEntries(): List<Entry> {
        val cursor = db?.getValue()
        val list: ArrayList<Entry> = ArrayList()
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                var i: Float = 0F
                do {
                    if (i < 7F && cursor.getString(cursor.getColumnIndex(DBHelper.VALUE_COl))
                            .toFloat() > 0F
                    ) {
                        list.add(
                            Entry(
                                i,
                                cursor.getString(cursor.getColumnIndex(DBHelper.VALUE_COl))
                                    .toBigDecimal().setScale(2, RoundingMode.HALF_UP).toFloat()
                            )
                        )
                        i += 1
                    }
                    println(
                        cursor.getString(cursor.getColumnIndex(DBHelper.ID_COL))
                            .toString()+" : "+cursor.getString(cursor.getColumnIndex(DBHelper.VALUE_COl))
                            .toString()
                    )
                } while (cursor.moveToNext())
            }
        }
        cursor?.close()
        return list
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        registerBarometerListener()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    private fun registerBarometerListener() {
        sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Do nothing
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PRESSURE) {
            MyService.pressure = event.values[0]
            speedometer.speedTo(event.values[0])
        }
    }
}