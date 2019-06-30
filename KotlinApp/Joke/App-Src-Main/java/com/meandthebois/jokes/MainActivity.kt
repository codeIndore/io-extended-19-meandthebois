package com.meandthebois.jokes

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Debug
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import org.w3c.dom.Text
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.HttpURLConnection.HTTP_OK
import java.net.URL
import java.util.*
import java.util.concurrent.ExecutionException
import kotlin.coroutines.CoroutineContext
import android.R.attr.data



class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private var tts:TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        joke_view.text = "Getting a Joke...${getEmoji(0x1F603)}"

        registerOnClickListeners()

        tts = TextToSpeech(this@MainActivity, this@MainActivity)

        val getJokeTask = GetJokeTask(object : CheckResponse {
            override fun onGetFinish(response: StringBuffer) {
                joke_view.text = "$response ${getEmoji(0x1F604)} ${getEmoji(0x1F604)}"
            }
        })

        val isConnected = checkNetworkConnectivity()
        if (isConnected) getJokeTask.execute() else Toast.makeText(this, "No Network", Toast.LENGTH_SHORT).show()
    }

    override fun onInit(status: Int) {
        if (status === TextToSpeech.SUCCESS) {
            val ttsLang = tts?.setLanguage(Locale.US)

            if (ttsLang == TextToSpeech.LANG_MISSING_DATA || ttsLang == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "The Language is not supported!")
            } else {
                Log.i("TTS", "Language Supported.")
            }
            Log.i("TTS", "Initialization success.")
        } else {
            Toast.makeText(applicationContext, "TTS Initialization failed!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun registerOnClickListeners() {
        getjoke.setOnClickListener {
            getjoke.isEnabled = false

            val getJokeTask = GetJokeTask(object : CheckResponse {
                override fun onGetFinish(response: StringBuffer) {
                    joke_view.text = "$response ${getEmoji(0x1F604)} ${getEmoji(0x1F604)}"
                    getjoke.isEnabled = true
                    val speechStatus = tts?.speak(response.toString(), TextToSpeech.QUEUE_FLUSH, null)

                    if (speechStatus == TextToSpeech.ERROR) {
                        Log.e("TTS", "Error in converting Text to Speech!")
                    }
                }
            })

            val isConnected = checkNetworkConnectivity()
            if (isConnected) getJokeTask.execute() else Toast.makeText(this, "No Network", Toast.LENGTH_SHORT).show()
        }

        share.setOnClickListener {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "${this@MainActivity.getString(R.string.joke_share_message)} ${joke_view.text}")
                type = "text/plain"
            }
            startActivity(Intent.createChooser(sendIntent, getString(R.string.share_joke)))
        }
    }

    private fun getEmoji(unicode:Int):String {
        return String(Character.toChars(unicode))
    }

    private fun getJoke(): StringBuffer?{
        val urlForGetRequest = URL("https://icanhazdadjoke.com/")
        var readLine: String?
        val connection = urlForGetRequest.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "text/plain")
        connection.setRequestProperty("User-Agent", "")
        val responseCode = connection.responseCode

        if (responseCode == HTTP_OK) {
            val inBuff = BufferedReader(
                InputStreamReader(connection.inputStream)
            )
            val response = StringBuffer()
            readLine = inBuff.readLine()

            while (readLine != null) {
                response.append(readLine)
                readLine = inBuff.readLine()
            }
            inBuff.close()

            println("JSON String Result $response")
            return response
        } else {
            println("GET NOT WORKED")
            return null
        }
    }

    private fun checkNetworkConnectivity(): Boolean {
        val connectivityManager =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo

        return activeNetwork != null && activeNetwork.isConnectedOrConnecting
    }

    private interface CheckResponse {
        fun onGetFinish(response: StringBuffer)
    }

    private inner class GetJokeTask internal constructor(checkRes: CheckResponse) :
        AsyncTask<Void, Void, StringBuffer>() {

        internal var checkResponse: CheckResponse? = null

        init {
            this.checkResponse = checkRes
        }

        override fun doInBackground(vararg voids: Void): StringBuffer? {
            try {
                return GetJoke()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } catch (e: ExecutionException) {
                e.printStackTrace()
            }

            return null
        }

        override fun onPostExecute(response: StringBuffer) {
            checkResponse!!.onGetFinish(response)
        }

        @Throws(ExecutionException::class, InterruptedException::class)
        private fun GetJoke(): StringBuffer? {
            return getJoke()
        }
    }
}
