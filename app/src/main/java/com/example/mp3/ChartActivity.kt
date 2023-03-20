package com.example.mp3


import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.highsoft.highcharts.core.HIChartView


class ChartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chart)

        val chartView = findViewById<View>(R.id.hc) as HIChartView


    }
}