/*
 * *
 *  * Created by zixie <code@bihe0832.com> on 2022/7/8 下午10:09
 *  * Copyright (c) 2022 . All rights reserved.
 *  * Last modified 2022/7/8 下午10:05
 *
 */

package com.bihe0832.android.test.module.audio.asr

import android.util.Log
import android.view.View
import com.bihe0832.android.common.debug.audio.DebugWAVListFragment
import com.bihe0832.android.common.debug.audio.card.AudioData
import com.bihe0832.android.lib.audio.AudioRecordConfig
import com.bihe0832.android.lib.audio.record.AudioRecordManager
import com.bihe0832.android.lib.file.FileUtils
import com.bihe0832.android.lib.log.ZLog
import com.bihe0832.android.lib.speech.DEFAULT_ENDPOINT_MODEL_DIR
import com.bihe0832.android.lib.speech.getDefaultOnlineRecognizerConfig
import com.bihe0832.android.lib.speech.recognition.ASROfflineManager
import com.bihe0832.android.lib.speech.recognition.ASROnlineManager
import com.bihe0832.android.lib.thread.ThreadManager
import com.bihe0832.android.lib.timer.BaseTask
import com.bihe0832.android.lib.timer.TaskManager
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import com.k2fsa.sherpa.onnx.SherpaAudioConvertTools
import java.io.File

class DebugWAVWithASRListFragment : DebugWAVListFragment() {
    private var hasInitSuccess = false
    private val mParaformerASROfflineManager by lazy { ASROfflineManager() }
    private val mSmallParaformerASROfflineManager by lazy { ASROfflineManager() }

    //    private val mWhisperTinyASROfflineManager by lazy { ASROfflineManager() }
//    private val mWhisperBaseASROfflineManager by lazy { ASROfflineManager() }
    private val mZipformerZH14ASROnlineManager by lazy { ASROnlineManager() }
    private val mZipformerBilingualZHENASROnlineManager by lazy { ASROnlineManager() }
//    private val mKeywordSpotterManager by lazy { KeywordSpotterManager() }

    override fun initView(view: View) {
        super.initView(view)
        TaskManager.getInstance().addTask(object : BaseTask() {
            val name = "fsfdsf"
            override fun getMyInterval(): Int {
                return 1
            }

            override fun getNextEarlyRunTime(): Int {
                return 0
            }

            override fun run() {
                ZLog.d(
                    AudioRecordManager.TAG,
                    "mASROfflineManager: ${mParaformerASROfflineManager.isReady()}"
                )
                ZLog.d(
                    AudioRecordManager.TAG,
                    "mASROfflineManager: ${mSmallParaformerASROfflineManager.isReady()}"
                )

                ZLog.d(
                    AudioRecordManager.TAG,
                    "mASROfflineManager: ${mZipformerZH14ASROnlineManager.isReady()}"
                )
                ZLog.d(
                    AudioRecordManager.TAG,
                    "mASROfflineManager: ${mZipformerBilingualZHENASROnlineManager.isReady()}"
                )


                if (mParaformerASROfflineManager.isReady() && mSmallParaformerASROfflineManager.isReady() && mZipformerZH14ASROnlineManager.isReady() && mZipformerBilingualZHENASROnlineManager.isReady()) {
                    TaskManager.getInstance().removeTask(name)
                }
            }

            override fun getTaskName(): String {
                return name
            }

        })
        ThreadManager.getInstance().start {
            mZipformerZH14ASROnlineManager.initRecognizer(
                context!!, getDefaultOnlineRecognizerConfig(
                    AudioRecordConfig.DEFAULT_SAMPLE_RATE_IN_HZ, DEFAULT_ENDPOINT_MODEL_DIR
                )
            )
            val modelDir = "sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20"
            mZipformerBilingualZHENASROnlineManager.initRecognizer(
                context!!, OnlineRecognizerConfig(
                    featConfig = FeatureConfig(
                        sampleRate = AudioRecordConfig.DEFAULT_SAMPLE_RATE_IN_HZ, featureDim = 80
                    ), modelConfig = OnlineModelConfig(
                        transducer = OnlineTransducerModelConfig(
                            encoder = "$modelDir/encoder-epoch-99-avg-1.int8.onnx",
                            decoder = "$modelDir/decoder-epoch-99-avg-1.int8.onnx",
                            joiner = "$modelDir/joiner-epoch-99-avg-1.int8.onnx",
                        ),
                        tokens = "$modelDir/tokens.txt",
                        modelType = "zipformer",
                    ), hotwordsFile = "$modelDir/hotword.txt"
                )
            )

            mSmallParaformerASROfflineManager.initRecognizer(
                getASROfflineRecognizerConfig_paraformer_small(
                    context!!
                )
            )
            mParaformerASROfflineManager.initRecognizer(
                getASROfflineRecognizerConfig_paraformer(
                    context!!
                )
            )
//            mWhisperBaseASROfflineManager.initRecognizer(
//                getASROfflineRecognizerConfig_whisper_base(
//                    context!!
//                )
//            )
//            mWhisperTinyASROfflineManager.initRecognizer(
//                getASROfflineRecognizerConfig_whisper_tiny(
//                    context!!
//                )
//            )
//
//            mKeywordSpotterManager.initRecognizer(
//                context!!, getDefaultKeywordSpotterConfig(
//                    AudioRecordConfig.DEFAULT_SAMPLE_RATE_IN_HZ, DEFAULT_KWS_MODEL_DIR
//                )
//            )
//            mKeywordSpotterManager.start("")
            showResult("")
            hasInitSuccess = true
        }
    }

    override fun filter(filePath: String): Boolean {
        return File(filePath).length() > 44
    }

    override fun palyAndRecognise(data: AudioData, play: Boolean) {
        if (hasInitSuccess) {
            ThreadManager.getInstance().start {
                super.palyAndRecognise(data, play)
            }

            Log.e(
                TAG, "-------------------------------\n识别测试 ${data.filePath}"
            )
            SherpaAudioConvertTools.readWavAudioToSherpaArray(data.filePath)?.let { audioData ->
                val max = (Short.MAX_VALUE * 0.1f).toInt().toShort()
                data.amplitude =
                    "最大振幅：" + (audioData.max() * Byte.MAX_VALUE * 2.0F).toInt() + ", 基准：$max"
                Log.e(
                    TAG,
                    "record data size:${audioData.size} max:$max, audioMax: ${audioData.max() * Byte.MAX_VALUE * 2.0F}"
                )

                var msg = "未能识别数据"
                if (SherpaAudioConvertTools.isOverSilence(audioData, max)) {
                    var time = System.currentTimeMillis()
                    mZipformerZH14ASROnlineManager.start()?.let { stream ->
                        mZipformerZH14ASROnlineManager.acceptWaveform(
                            stream, AudioRecordConfig.DEFAULT_SAMPLE_RATE_IN_HZ, audioData
                        ).let { result ->
                            "流式识别1：$result 用时：（${System.currentTimeMillis() - time}）模型：streaming-zipformer-zh-14M-2023-02-23".let { log ->
                                Log.e(TAG, log)
                                msg = log
                            }

                        }
                        mZipformerZH14ASROnlineManager.stop(stream)
                    }

                    time = System.currentTimeMillis()
                    mZipformerBilingualZHENASROnlineManager.start()?.let { stream ->
                        mZipformerBilingualZHENASROnlineManager.acceptWaveform(
                            stream, AudioRecordConfig.DEFAULT_SAMPLE_RATE_IN_HZ, audioData
                        ).let { result ->
                            "流式识别2：$result 用时：（${System.currentTimeMillis() - time}）模型：streaming-zipformer-bilingual-zh-en-2023-02-20".let { log ->
                                Log.e(TAG, log)
                                msg = "$msg\n$log"
                            }
                        }
                        mZipformerZH14ASROnlineManager.stop(stream)
                    }

                    time = System.currentTimeMillis()
                    mParaformerASROfflineManager.startRecognizer(
                        AudioRecordConfig.DEFAULT_SAMPLE_RATE_IN_HZ, audioData
                    ).let { result ->
                        "离线识别1：$result 用时：（${System.currentTimeMillis() - time}）模型：paraformer-zh-2023-09-14".let { log ->
                            Log.e(TAG, log)
                            msg = "$msg\n$log"
                        }
                    }
                    time = System.currentTimeMillis()
                    mSmallParaformerASROfflineManager.startRecognizer(
                        AudioRecordConfig.DEFAULT_SAMPLE_RATE_IN_HZ, audioData
                    ).let { result ->
                        "离线识别2：$result 用时：（${System.currentTimeMillis() - time}）模型：paraformer-zh-small-2023-09-14".let { log ->
                            Log.e(TAG, log)
                            msg = "$msg\n$log"
                        }
                    }

//                    try {
//                        time = System.currentTimeMillis()
//                        mWhisperBaseASROfflineManager.startRecognizer(
//                            AudioRecordConfig.DEFAULT_SAMPLE_RATE_IN_HZ, audioData
//                        ).let { result ->
//                            "离线识别3：$result 用时：（${System.currentTimeMillis() - time}）模型：whisper-base".let { log ->
//                                Log.e(TAG, log)
//                                msg = "$msg\n$log"
//                            }
//                        }
//                    }catch (e:Exception){
//                        e.printStackTrace()
//                    }
//
//                    try {
//                        time = System.currentTimeMillis()
//                        mWhisperTinyASROfflineManager.startRecognizer(
//                            AudioRecordConfig.DEFAULT_SAMPLE_RATE_IN_HZ, audioData
//                        ).let { result ->
//                            "离线识别4：$result 用时：（${System.currentTimeMillis() - time}）模型：whisper-tiny".let { log ->
//                                Log.e(TAG, log)
//                                msg = "$msg\n$log"
//                            }
//                        }
//                    }catch (e:Exception){
//                        e.printStackTrace()
//                    }


//                    time = System.currentTimeMillis()
//                    mKeywordSpotterManager.doRecognizer(
//                        AudioRecordConfig.DEFAULT_SAMPLE_RATE_IN_HZ, audioData
//                    ).let { result ->
//                        Log.e(
//                            TAG,
//                            "关键字识别（${System.currentTimeMillis() - time}）mKeywordSpotterManager Start to recognizer:$result"
//                        )
//                        msg = "$msg\n关键字识别（${System.currentTimeMillis() - time}）：$result"
//                    }

                    data.recogniseResult = msg
                } else {
                    msg = "无效音频，无有效内容"
                    FileUtils.deleteFile(data.filePath)
                }
            }
        } else {
            showResult("初始化未完成，请稍候")
        }

    }
}
