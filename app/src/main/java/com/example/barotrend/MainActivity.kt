package com.example.barotrend

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.getAltitude
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.barotrend.databinding.ActivityMainBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import java.math.RoundingMode


class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sensorManager: SensorManager
    private var pressureSensor: Sensor? = null
    lateinit var lineChart: LineChart
    private var db: DBHelper? = DBHelper(this, null)
    lateinit var pgsPressure: ProgressBar
    lateinit var pgsAltitude: ProgressBar
    lateinit var textViewPressure: TextView
    lateinit var textViewAltitude: TextView
    lateinit var txtPressureChange3: TextView
    lateinit var txtPressureChange6: TextView
    lateinit var mAdView: AdView
    lateinit var animation: Animation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        MobileAds.initialize(this) {}

        mAdView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

        if (pressureSensor == null) {
            SweetAlertDialog(this@MainActivity, SweetAlertDialog.ERROR_TYPE).setTitleText("Oops...")
                .setContentText("Barometer sensor not found!")
                .setConfirmClickListener { this.finishAffinity() }.show()
        } else {
            startService(Intent(applicationContext, MyService::class.java))

            pgsPressure = findViewById(R.id.progressCirclePressure)
            pgsAltitude = findViewById(R.id.progressCircleAltitude)
            textViewPressure = findViewById(R.id.textViewPressure)
            textViewAltitude = findViewById(R.id.textViewAltitude)

            animation = AnimationUtils.loadAnimation(
                applicationContext,
                android.R.anim.fade_in
            )
            animation.duration = 2000

            txtPressureChange3 = findViewById(R.id.txtPressureChanges3)
            txtPressureChange6 = findViewById(R.id.txtPressureChanges6)
            lineChart = findViewById(R.id.lineChart)
            lineChart.setBackgroundColor(Color.BLACK)
            lineChart.setDrawGridBackground(false)
            lineChart.description.isEnabled = false
            lineChart.setTouchEnabled(false)
            lineChart.isDragEnabled = false
            lineChart.axisLeft.setDrawGridLines(false)
            lineChart.axisRight.setDrawGridLines(false)
            lineChart.xAxis.setDrawGridLines(false)
            lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
            lineChart.axisLeft.isEnabled = false
            lineChart.axisRight.isEnabled = false
            lineChart.legend.isEnabled = false
            lineChart.xAxis.textColor = Color.WHITE

            animate()
            refreshChart()
            checkBattery(false)
        }
    }

    private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pwrm =
            context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val name = context.applicationContext.packageName
        return pwrm.isIgnoringBatteryOptimizations(name)
    }

    private fun checkBattery(alert: Boolean): Boolean {
        if (!isIgnoringBatteryOptimizations(this)) {
            SweetAlertDialog(
                this, SweetAlertDialog.WARNING_TYPE
            ).setTitleText("Battery Optimization")
                .setContentText("Battery optimization is active. Close it?").setCancelText("No")
                .setConfirmText("Yes").showCancelButton(true).setConfirmClickListener {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    startActivity(intent)
                }.setCancelClickListener { sDialog -> sDialog.cancel() }.show()
        } else {
            if (alert) {
                SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE).setTitleText("Succes!")
                    .setContentText("Battery optimization is inactive!").show()
            }
        }
        return true
    }

    @SuppressLint("Range", "SetTextI18n")
    private fun getEntries(): ArrayList<BaroEntity> {
        val cursor = db?.getValue()
        val list: ArrayList<BaroEntity> = ArrayList()
        val values: ArrayList<Float> = ArrayList()
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    if (cursor.getString(cursor.getColumnIndex(DBHelper.VALUE_COl))
                            .toFloat() > 0F
                    ) {
                        values.add(
                            cursor.getString(cursor.getColumnIndex(DBHelper.VALUE_COl))
                                .toBigDecimal().setScale(2, RoundingMode.HALF_UP).toFloat()
                        )
                        list.add(
                            BaroEntity(
                                cursor.getString(cursor.getColumnIndex(DBHelper.DATETIME_COL))
                                    .substring(5, 16),
                                cursor.getString(cursor.getColumnIndex(DBHelper.VALUE_COl))
                                    .toBigDecimal().setScale(2, RoundingMode.HALF_UP).toFloat()
                            )
                        )
                    }
                } while (cursor.moveToNext())
            }

            val change3 = (((values[0] - values[2]) / values[2]) * 100).toBigDecimal()
                .setScale(3, RoundingMode.HALF_UP).toFloat()
            txtPressureChange3.text = "$change3 %"
            if (change3 < 0) {
                txtPressureChange3.setTextColor(Color.RED)
            } else {
                txtPressureChange3.setTextColor(Color.GREEN)
            }
            val change6 = (((values[0] - values[5]) / values[5]) * 100).toBigDecimal()
                .setScale(3, RoundingMode.HALF_UP).toFloat()
            txtPressureChange6.text = "$change6 %"
            if (change6 < 0) {
                txtPressureChange6.setTextColor(Color.RED)
            } else {
                txtPressureChange6.setTextColor(Color.GREEN)
            }
        }
        cursor?.close()
        return list
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.refresh -> {
                refreshChart()
            }
            R.id.batteryOptimization -> {
                checkBattery(true)
            }
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
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PRESSURE) {
            MyService.pressure = event.values[0]
            pgsPressure.progress = event.values[0].toInt()
            textViewPressure.text = String.format("%.1f", event.values[0])
            pgsAltitude.progress =
                getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, event.values[0]).toInt()
            textViewAltitude.text = String.format(
                "%.1f", getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, event.values[0])
            )
        }
    }

    private fun refreshChart(): Boolean {
        val list: ArrayList<Entry> = ArrayList()
        val timeStamps: ArrayList<String> = ArrayList()
        val entities: ArrayList<BaroEntity> = getEntries()

        for (i in entities.size downTo 1) {
            list.add(
                Entry(
                    entities.size - i.toFloat(), entities[i - 1].value
                )
            )
            timeStamps.add(entities[i - 1].timestamp)
        }

        val dataSet = LineDataSet(list, "Line Data")
        dataSet.color = Color.GREEN
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.GREEN
        dataSet.valueTextColor = Color.WHITE
        dataSet.color = Color.GREEN
        dataSet.highLightColor = Color.WHITE
        dataSet.setCircleColor(Color.WHITE)
        dataSet.lineWidth = 1f

        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.invalidate()

        val formatter: ValueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase): String {
                return timeStamps[value.toInt()]
            }
        }
        val xAxis: XAxis = lineChart.xAxis
        xAxis.granularity = 1f
        xAxis.valueFormatter = formatter
        xAxis.textSize = 1f

        animate()
        return true
    }

    private fun animate() {
        lineChart.animateX(2000, Easing.EaseInCubic)
        pgsPressure.startAnimation(animation)
        pgsAltitude.startAnimation(animation)
        txtPressureChange3.startAnimation(animation)
        txtPressureChange6.startAnimation(animation)
    }
}