package com.stage.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.stage.play.StagePlay
import com.stage.sample.ui.theme.StageSampleTheme

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StageSampleTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: Lazy<MainActivityViewModel> = viewModels()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .clickable {

                                StagePlay
                                    .Prepare(this, PlayerActivity::class.java)
                                    .setMediaId("10")
                                    .enableHeartbeats()
                                    .setUrl("https://storage.googleapis.com/wvmedia/cenc/vp9/tears/tears.mpd")
                                    .setDrmScheme("widevine")
                                    .setDrmLicenseUrl("https://proxy.uat.widevine.com/proxy?video_id=2015_tears&provider=widevine_test")
                                    .startPlayer()


                            }
                            .background(color = Color.Yellow),
                        contentAlignment = Alignment.Center

                        /*Modifier
                            .contentAli(Alignment.Center)
                            .clickable {

                            val intent = Intent(this, PlayerActivity::class.java)
                            *//* intent.putExtra(
                             IntentUtil.PREFER_EXTENSION_DECODERS_EXTRA,
                             SampleChooserActivity.isNonNullAndChecked(preferExtensionDecodersMenuItem)
                         )*//*
                        val mediaItem =
                            MediaItem.fromUri(Uri.parse("https://storage.googleapis.com/wvmedia/clear/h264/tears/tears_uhd.mpd"))
                        val mediaList: ArrayList<MediaItem> = ArrayList()
                        mediaList.add(mediaItem)
                        IntentUtil.addToIntent(mediaList, intent)
                        startActivity(intent)

                    }*/
                    ) {
                        Greeting("Click me")
                    }


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
    StageSampleTheme {
        Greeting("Android")
    }
}