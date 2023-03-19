package com.example.mp3

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.mp3.ui.theme.Mp3Theme
import javazoom.jl.decoder.Bitstream
import javazoom.jl.decoder.Decoder
import javazoom.jl.decoder.Header
import javazoom.jl.decoder.SampleBuffer
import java.net.URL


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // JLayer decoder
        //val decoder = Decoder()
        val mDecoder = Decoder() //? = null
        //var mAudioTrack: AudioTrack? = null
        //val sampleRate = 44100

//        val minBufferSize = AudioTrack.getMinBufferSize(
//            sampleRate,
//            AudioFormat.CHANNEL_OUT_STEREO,
//            AudioFormat.ENCODING_PCM_16BIT
//        )
//
//        val mAudioTrack = AudioTrack(
//            AudioManager.STREAM_MUSIC,
//            sampleRate,
//            AudioFormat.CHANNEL_OUT_STEREO,
//            AudioFormat.ENCODING_PCM_16BIT,
//            minBufferSize,
//            AudioTrack.MODE_STREAM
//        )

        val mAudioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
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
                AudioTrack.getMinBufferSize(
                    48000,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
            )
            .build()


        val thread = Thread {
            try {
                val `in` = URL("http://icecast.omroep.nl:80/radio1-sb-mp3")
                    .openConnection()
                    .getInputStream()
                val bitstream = Bitstream(`in`)
                val READ_THRESHOLD = 2147483647
                var framesReaded = 0
                var header : Header? = null

                while (framesReaded++ <= READ_THRESHOLD && bitstream.readFrame()
                        .also { header = it } != null
                ) {
                    val sampleBuffer = mDecoder.decodeFrame(header, bitstream) as SampleBuffer
                    val buffer = sampleBuffer.buffer
                    mAudioTrack.write(buffer, 0, buffer.size)
                    bitstream.closeFrame()
                }
            } catch (e: Exception) {
               print( e.printStackTrace() )
            }
        }
        thread.start()
        mAudioTrack.play()

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
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )


}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Mp3Theme {
        Greeting("Android")
    }
}