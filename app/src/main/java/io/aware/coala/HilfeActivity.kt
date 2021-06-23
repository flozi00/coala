/*
 * Copyright 2020 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.aware.coala

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.*
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.HandlerCompat
import androidx.lifecycle.lifecycleScope
import io.aware.coala.databinding.AcitivityHilfeBinding
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import util.AudioSampler
import java.io.File
import java.io.InputStream
import java.util.*


class HilfeActivity : AppCompatActivity() {
    private val probabilitiesAdapter by lazy { ProbabilitiesAdapter() }

    private var audioClassifier: AudioClassifier? = null
    private var audioRecord: AudioRecord? = null
    private var classificationInterval = 500L // how often should classification run in milli-secs
    private lateinit var handler: Handler // background thread handler to run classification
    public var firstTrigger = 0.0.toLong()
    public var triggerCount = 0

    private lateinit var api: RemoteApi //remote api Instance
    private lateinit var mAudioSampler: AudioSampler


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = AcitivityHilfeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            recyclerView.apply {
                setHasFixedSize(false)
                adapter = probabilitiesAdapter
            }

        }
        //initialize api
        api = RemoteApi.invoke()
        mAudioSampler = AudioSampler.Builder()
            .sampleRate(4410)
            .channel(AudioFormat.CHANNEL_IN_MONO)
            .format(AudioFormat.ENCODING_PCM_16BIT) //It might be a float
            .create()


        val btn_click_me = findViewById(R.id.button2) as Button
        btn_click_me.setOnClickListener {
            val intent = Intent(applicationContext, assistant::class.java)
            intent.putExtra("forward", true)
            startActivity(intent)
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Create a handler to run classification in a background thread
        val handlerThread = HandlerThread("backgroundThread")
        handlerThread.start()
        handler = HandlerCompat.createAsync(handlerThread.looper)

        // Request microphone permission and start running classification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestMicrophonePermission()
        } else {
            startAudioClassification()
        }

    }

    private fun startAudioClassification() {
        // If the audio classifier is initialized and running, do nothing.
        if (audioClassifier != null) return;

        // Initialize the audio classifier
        val classifier = AudioClassifier.createFromFile(this, MODEL_FILE)
        val audioTensor = classifier.createInputTensorAudio()

        // Initialize the audio recorder
        val record = classifier.createAudioRecord()
        record.startRecording()
        firstTrigger = System.currentTimeMillis()

        // Define the classification runnable
        val run = object : Runnable {
            override fun run() {
                val startTime = System.currentTimeMillis()

                // Load the latest audio sample
                audioTensor.load(record)
                val output = classifier.classify(audioTensor)

                // Filter out results above a certain threshold, and sort them descendingly
                val filteredModelOutput = output[0].categories.filter {
                    it.score > MINIMUM_DISPLAY_THRESHOLD
                }.sortedBy {
                    -it.score
                }

                val finishTime = System.currentTimeMillis()

                Log.d(TAG, "Latency = ${finishTime - startTime}ms")

                // Updating the UI
                runOnUiThread {
                    probabilitiesAdapter.categoryList = filteredModelOutput
                    probabilitiesAdapter.notifyDataSetChanged()
                    Log.i("inference", filteredModelOutput.toString())
                    for (category in filteredModelOutput) {
                        if (category.label == "hilfe") {
                            val difference = System.currentTimeMillis() - firstTrigger
                            if (difference <= 10000) {
                                if (difference >= 1000) {
                                    triggerCount = triggerCount + 1
                                    if (triggerCount >= 2) {
                                        //Load all audio samples available in the AudioRecord without blocking.
                                        //audioTensor.load(record)
                                        // Get the `TensorBuffer` for inference and filter it
                                        filterTensorBuffer(audioTensor.tensorBuffer)

                                        val intent =
                                            Intent(applicationContext, assistant::class.java)
                                        intent.putExtra("forward", true)
                                        startActivity(intent)
                                    }
                                }
                            } else {
                                triggerCount = 1
                            }
                            Toast.makeText(
                                applicationContext,
                                "audio trigger recognized",
                                Toast.LENGTH_SHORT
                            ).show()

                            firstTrigger = System.currentTimeMillis()


                        }
                    }

                }

                // Rerun the classification after a certain interval
                handler.postDelayed(this, classificationInterval)
            }
        }

        // Start the classification process
        handler.post(run)

        // Save the instances we just created for use later
        audioClassifier = classifier
        audioRecord = record
    }

    private fun filterTensorBuffer(buffer: TensorBuffer?) {
        val fileName = UUID.randomUUID().toString()
        val filePath =
            getExternalFilesDir(Environment.DIRECTORY_MUSIC).toString() + "/$fileName.pcm"
        val file = File(filePath)

        if (file.createNewFile())
            buffer?.buffer?.array()?.let { file.writeBytes(it) }

        encodeWAV(filePath, fileName)

    }

    private fun encodeWAV(pcmFilePath: String, fileName: String) {
        val wavFilePath =
            getExternalFilesDir(Environment.DIRECTORY_MUSIC).toString() + "/$fileName.wav"

        val pcmFile = File(pcmFilePath)
        val wavFile = File(wavFilePath)
        if (pcmFile.exists()) {
            mAudioSampler.pcm2Wav(pcmFile, wavFile)
            //now upload the file to server in background
            lifecycleScope.launch {
                val isHelp = uploadAudioToServer(wavFile.inputStream())

                if (isHelp){
                    Toast.makeText(this@HilfeActivity, "isHelp = $isHelp", Toast.LENGTH_SHORT)
                        .show()
                    //todo : do the task is result is true
                }
            }
        } else {
            Toast.makeText(this@HilfeActivity, "PCM Not Found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopAudioClassification() {
        handler.removeCallbacksAndMessages(null)
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        audioClassifier = null
    }

    override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
        // Handles "top" resumed event on multi-window environment
        if (isTopResumedActivity) {
            startAudioClassification()
        } else {
            stopAudioClassification()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startAudioClassification()
        } else {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
        }
    }

    private suspend fun uploadAudioToServer(stream: InputStream): Boolean {
        val part = MultipartBody.Part.createFormData(
            "audio",
            UUID.randomUUID().toString() + ".wav",
            stream.readBytes()
                .toRequestBody(
                    "audio/wav".toMediaTypeOrNull(),
                    0, stream.readBytes().size
                )
        )

        val response = api.uploadAudio(part)
        return if (response.isSuccessful && response.body() != null)
            response.body()!!.is_help
        else {
            Log.d(TAG, "Upload Error: Code = ${response.code()} Message: ${response.message()}")
            false
        }

    }

    companion object {
        const val REQUEST_RECORD_AUDIO = 1337
        private const val TAG = "HilifeActivity-AudioDemo"
        private const val MODEL_FILE = "model_meta.tflite"
        private const val MINIMUM_DISPLAY_THRESHOLD: Float = 0.7f
    }
}
