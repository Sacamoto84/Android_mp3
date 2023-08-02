package com.example.mp3

import android.R
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.audiofx.Visualizer
import android.os.Bundle
import android.provider.AlarmClock.EXTRA_MESSAGE
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import com.example.mp3.ui.theme.Mp3Theme
import javazoom.jl.decoder.Bitstream
import javazoom.jl.decoder.Decoder
import javazoom.jl.decoder.Header
import javazoom.jl.decoder.SampleBuffer
import java.io.FileInputStream
import kotlin.math.sqrt


var refresh by mutableStateOf(0)
private const val CAPTURE_SIZE = 1024
var _waveform: IntArray = IntArray(CAPTURE_SIZE)
var _fft: IntArray = IntArray(CAPTURE_SIZE)

var RMSList = ArrayDeque<Int>()

var measurement: Visualizer.MeasurementPeakRms = Visualizer.MeasurementPeakRms()


fun sss() {

    "креведко" {
        println("креведко")
    }


}

private operator fun String.invoke(value: () -> Unit) {
}


class MainActivity : ComponentActivity(), Visualizer.OnDataCaptureListener {


    var visualiser: Visualizer? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sss()

        //val intent = Intent(this, MainActivity2::class.java)
        //this.startActivity(intent)



        val mDecoder = Decoder() //? = null

        val mAudioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(48000)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(
                1024 * 1024
//                AudioTrack.getMinBufferSize(
//                    48000,
//                    AudioFormat.CHANNEL_OUT_MONO,
//                    AudioFormat.ENCODING_PCM_16BIT
//                )
            )
            .build()


        val thread = Thread {
            try {


//                val `in1` = URL(
//                    //"http://icecast.omroep.nl:80/radio1-sb-mp3"
//                    "http://chanson.hostingradio.ru:8041/chanson-romantic128.mp3"
//
//                )
//                    .openConnection()
//                    .getInputStream()

                val assetFileDescriptor: AssetFileDescriptor =
                    applicationContext.assets.openFd("me.mp3")
                val fileDescriptor = assetFileDescriptor.fileDescriptor
                val `in` = FileInputStream(fileDescriptor)

                val bitstream = Bitstream(`in`)
                val READ_THRESHOLD = 2147483647
                var framesReaded = 0
                var header: Header? = null

                
                while (framesReaded++ <= READ_THRESHOLD && bitstream.readFrame()
                        .also { header = it } != null
                ) {
                    val sampleBuffer = mDecoder.decodeFrame(header, bitstream) as SampleBuffer
                    val buffer = sampleBuffer.buffer
                    mAudioTrack.write(buffer, 0, buffer.size)

                    //calcRMS(buffer, buffer.size)
//                    RMSList.addFirst(mRMS/128)
//                    while (RMSList.size > 512) {
//                        RMSList.removeLast()
//                    }


                    println(buffer.size)

                    bitstream.closeFrame()


                }
            } catch (e: Exception) {
                print(e.printStackTrace())
            }
        }
        thread.start()
        mAudioTrack.play()

        visualiser = Visualizer(0)
        visualiser!!.setDataCaptureListener(this, Visualizer.getMaxCaptureRate(), true, true)
        visualiser!!.captureSize = CAPTURE_SIZE


        visualiser!!.measurementMode = Visualizer.MEASUREMENT_MODE_PEAK_RMS;
        //visualiser!!.scalingMode     = Visualizer.SCALING_MODE_NORMALIZED;

        visualiser!!.enabled = true

//        val assetFileDescriptor: AssetFileDescriptor = applicationContext.assets.openFd("me.mp3")
//        val fileDescriptor = assetFileDescriptor.fileDescriptor
//        val stream = FileInputStream(fileDescriptor)
//
//
//        GlobalScope.launch(Dispatchers.Main) {
//            val mPlayer = MediaPlayer.create(
//                applicationContext,
//                Uri.parse("http://chanson.hostingradio.ru:8041/chanson-romantic128.mp3")
//            )
//            mPlayer.isLooping = true
//            mPlayer.start()
//        }


        setContent {
            Mp3Theme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                }
            }
        }
    }


    override fun onWaveFormDataCapture(
        visualizer: Visualizer?,
        waveform: ByteArray?,
        samplingRate: Int
    ) {
        if (waveform != null) {
            for (i in 0 until CAPTURE_SIZE) {
                _waveform[i] =
                    if (waveform[i].toInt() > 0) waveform[i].toInt() - 128 else waveform[i].toInt() + 128
            }
            refresh++
            visualiser!!.getMeasurementPeakRms(measurement)
        }
    }

    override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {
        if (fft != null) {
            for (i in 0 until CAPTURE_SIZE) {
                _fft[i] = if (fft[i].toInt() > 0) fft[i].toInt() * -1 else fft[i].toInt()
            }
            refresh++
        }
    }

}

var mPeak = 0
var mRMS by mutableStateOf(0)

fun calcRMS(buffer: ShortArray, length: Int) {
    var accumAbs = 0.0
    mPeak = 0
    for (i in 0 until length) {
        val v: Int = kotlin.math.abs(buffer[i].toInt())
        if (mPeak < v) {
            mPeak = v
        }
        val `val` = buffer[i].toDouble()
        accumAbs += `val` * `val`
    }
    mRMS = sqrt(accumAbs / length.toDouble()).toInt()
}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    )
    {

        Text(
            text = "Ебучее онлайн радио $refresh", color = Color.LightGray
        )



        Column {

            Button(onClick = {



            }) {


            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                refresh
                val path = Path()
                path.moveTo(0f, size.height / 2f)
                _waveform.forEachIndexed { index, byte ->
                    path.lineTo(index.toFloat() * 2, size.height / 2 + byte * 3f)
                }
                drawPath(
                    path = path,
                    color = Color.Green,
                    style = Stroke(width = 2f)
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(0.1f)
            ) {
                refresh
                val path = Path()
                path.moveTo(0f, size.height / 1.5f)
                _fft.forEachIndexed { index, byte ->
                    path.lineTo(index.toFloat() * 2, size.height / 1.5f + byte * 3f)
                }
                drawPath(
                    path = path,
                    color = Color.Green,
                    style = Stroke(width = 2f)
                )
            }

        }


    }


}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Mp3Theme {
        Greeting("Android")
    }
}