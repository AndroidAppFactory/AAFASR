/*
 * *
 *  * Created by zixie <code@bihe0832.com> on 2022/7/8 下午10:09
 *  * Copyright (c) 2022 . All rights reserved.
 *  * Last modified 2022/7/8 下午10:05
 *
 */

package com.bihe0832.android.test.module.audio.asr

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.View
import com.bihe0832.android.common.debug.item.getDebugItem
import com.bihe0832.android.common.debug.module.DebugEnvFragment
import com.bihe0832.android.common.permission.PermissionResultOfAAF
import com.bihe0832.android.framework.file.AAFFileWrapper
import com.bihe0832.android.lib.aaf.tools.AAFDataCallback
import com.bihe0832.android.lib.adapter.CardBaseModule
import com.bihe0832.android.lib.audio.AudioRecordConfig
import com.bihe0832.android.lib.audio.AudioUtils
import com.bihe0832.android.lib.audio.record.AudioRecordManager
import com.bihe0832.android.lib.audio.record.wrapper.AAFAudioTools
import com.bihe0832.android.lib.audio.record.wrapper.AudioRecordFile
import com.bihe0832.android.lib.audio.wav.PcmToWav
import com.bihe0832.android.lib.audio.wav.WavHeader
import com.bihe0832.android.lib.audio.wav.WaveFileReader
import com.bihe0832.android.lib.device.cpu.CPUHelper
import com.bihe0832.android.lib.file.FileUtils
import com.bihe0832.android.lib.log.ZLog
import com.bihe0832.android.lib.speech.DEFAULT_ENDPOINT_MODEL_DIR
import com.bihe0832.android.lib.speech.endpoint.AudioRecordWithEndpoint
import com.bihe0832.android.lib.speech.getDefaultOnlineRecognizerConfig
import com.bihe0832.android.lib.speech.recognition.ASROfflineManager
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineParaformerModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import com.k2fsa.sherpa.onnx.SherpaAudioConvertTools
import com.k2fsa.sherpa.onnx.getFeatureConfig
import java.io.File
import kotlin.math.max

open class DebugRecordAndASRFragRecment : DebugEnvFragment() {
    val TAG = this.javaClass.simpleName
    private val mAudioRecordFile = mutableListOf<AudioRecordFile>()
    private val mASROfflineManager1 by lazy { ASROfflineManager() }
//    private val mKeywordSpotterManager by lazy { KeywordSpotterManager() }

    private val scene = "debugRecord"
    override fun getDataList(): ArrayList<CardBaseModule> {
        return ArrayList<CardBaseModule>().apply {

            add(getDebugItem("初始化", View.OnClickListener { init() }))
            add(
                getDebugFragmentItemData(
                    "本地 WAV 查看及识别",
                    DebugWAVWithASRListFragment::class.java
                )
            )
            add(
                getDebugItem("ARS 静音检测：结束实时识别及录制",
                    View.OnClickListener { AudioRecordWithEndpoint.stopRecord(context!!) })
            )
            add(getDebugItem("ARS 实时打断", View.OnClickListener { forceEndCurrent() }))
            add(
                getDebugItem("ARS 静音检测：开启录制在回调保存为文件",
                    View.OnClickListener { startRecord() })
            )
            add(
                getDebugItem("ARS 静音检测：开启录制在回调识别部分文区间",
                    View.OnClickListener { testSplit() })
            )
            add(
                getDebugItem("ARS 识别：开启录制识别静音检测回调数据",
                    View.OnClickListener { startReal() })
            )
            add(
                getDebugItem("ARS 识别：开启录制识别静音检测回调文件",
                    View.OnClickListener { startRealFile() })
            )

            add(
                getDebugItem("ARS 识别：基于本地文件识别",
                    View.OnClickListener { startFile(preFile()) })
            )
            add(getDebugItem("WAV 录制测试：开始录制", View.OnClickListener { startWav() }))
            add(getDebugItem("WAV 录制测试：结束录制", View.OnClickListener { stopWav() }))
            add(
                getDebugItem("WAV 录制测试：录制文件开始（可同时多个）",
                    View.OnClickListener { startWaveFile() })
            )
            add(
                getDebugItem("WAV 录制测试：录制文件结束（关闭所有）",
                    View.OnClickListener { stopWaveFile() })
            )

            add(getDebugItem(
                "清空本地音频临时缓存"
            ) { FileUtils.deleteDirectory(File(AAFFileWrapper.getMediaTempFolder())) })

        }
    }

    @SuppressLint("MissingPermission")
    fun init() {
        AAFAudioTools.addRecordScene(scene, "读取麦克风", "音频录制测试")
        AAFAudioTools.startRecordPermissionCheck(activity,
            scene,
            object : PermissionResultOfAAF(false) {
                override fun onSuccess() {
                    AAFAudioTools.init()
                    AudioRecordWithEndpoint.init(
                        context!!, getDefaultOnlineRecognizerConfig(
                            AudioRecordConfig.DEFAULT_SAMPLE_RATE_IN_HZ, DEFAULT_ENDPOINT_MODEL_DIR
                        )
                    )
                    mASROfflineManager1.initRecognizer(
                        getASROfflineRecognizerConfig_paraformer_small(
                            context!!
                        )
                    )
//                    mKeywordSpotterManager.initRecognizer(
//                        context!!, getDefaultKeywordSpotterConfig(
//                            AudioRecordConfig.DEFAULT_SAMPLE_RATE_IN_HZ, DEFAULT_KWS_MODEL_DIR
//                        )
//                    )
                }
            })
    }

    fun startWav() {
        AAFAudioTools.startRecordPermissionCheck(activity,
            scene,
            object : PermissionResultOfAAF(false) {
                override fun onSuccess() {
                    AudioRecordManager.startRecord(
                        activity, scene, "rere"
                    ) { config, audioChunk, dataLength ->
                        ZLog.d(
                            TAG, "Started recording callback:${dataLength}"
                        )
                    }
                }
            })
    }

    fun stopWav() {
        AudioRecordManager.stopRecord(context!!, scene)
    }

    fun startWaveFile() {
        val file = AAFFileWrapper.getMediaTempFolder() + System.currentTimeMillis() + ".wav"
        AAFAudioTools.startRecordPermissionCheck(activity,
            scene,
            object : PermissionResultOfAAF(false) {
                override fun onSuccess() {
                    AudioRecordFile(System.currentTimeMillis().toString(), File(file)).let {
                        mAudioRecordFile.add(it)
                        it.startRecord(activity, "特惠")
                    }
                }
            })
    }

    fun stopWaveFile() {
        mAudioRecordFile.forEach {
            it.stopRecord(context!!)
        }
    }

    fun preFile(): String {
        val file = AAFFileWrapper.getMediaTempFolder() + "temp.wav"
        FileUtils.copyAssetsFileToPath(context!!, "0.wav", file)
        return file
    }

    fun preEmpty(): String {
        val file = AAFFileWrapper.getMediaTempFolder() + "temp.wav"
        File(file).let {
            FileUtils.deleteFile(file)
            it.createNewFile()
            FileUtils.writeDataToFile(
                file, 0, WavHeader(
                    AudioRecordConfig(
                        AudioRecordConfig.DEFAULT_AUDIO_SOURCE,
                        AudioRecordConfig.DEFAULT_SAMPLE_RATE_IN_HZ,
                        AudioRecordConfig.DEFAULT_CHANNEL_CONFIG,
                        AudioRecordConfig.DEFAULT_AUDIO_FORMAT
                    ), it.length()
                ).toBytes(), false
            )
        }
        return file
    }

    fun readWavHead(filePath: String) {
        val wavFileReader = WaveFileReader(filePath)
        ZLog.d(wavFileReader.toString())
    }

    fun startRecord() {
        AAFAudioTools.startRecordPermissionCheck(activity,
            scene,
            object : PermissionResultOfAAF(false) {
                override fun onSuccess() {
                    AudioRecordWithEndpoint.startDataRecord() { audioRecordConfig, pcmData, result ->
                        ZLog.d(TAG, "Started recording callback:${pcmData?.size}")
                        val fileFolder = AAFFileWrapper.getMediaTempFolder()
                        pcmData?.let {
                            if (FileUtils.checkAndCreateFolder(fileFolder)) {
                                val file =
                                    FileUtils.getFolderPathWithSeparator(fileFolder) + System.currentTimeMillis() + ".wav"
                                ZLog.d(TAG, "record data:${file}")
                                val byteArray = AudioUtils.shortArrayToByteArray(pcmData)
                                PcmToWav(
                                    audioRecordConfig.sampleRateInHz,
                                    audioRecordConfig.channelConfig,
                                    audioRecordConfig.audioFormat
                                ).convertToFile(byteArray, file)
                                ZLog.d(TAG, "record data:${file}")
                            }
                        }
                    }
                }
            })
    }

    fun forceEndCurrent() {
        AudioRecordWithEndpoint.forceEndCurrent().let {
            ZLog.d(TAG, "forceEndCurrent:$it")
        }
    }

    fun testSplit() {
        AudioRecordWithEndpoint.startDataRecord { audioRecordConfig, pcmData, result ->
            ZLog.d(TAG, "Started recording callback:${pcmData?.size}")
            val fileFolder = AAFFileWrapper.getMediaTempFolder()
            pcmData?.let {
                if (FileUtils.checkAndCreateFolder(fileFolder)) {
                    val file =
                        FileUtils.getFolderPathWithSeparator(fileFolder) + System.currentTimeMillis() + ".wav"
                    ZLog.d(TAG, "record data:${file}")
                    val byteArray = AudioUtils.shortArrayToByteArray(pcmData)
                    recognise(
                        audioRecordConfig.sampleRateInHz,
                        SherpaAudioConvertTools.byteArrayToSherpaArray(byteArray)
                    )
                    PcmToWav(
                        audioRecordConfig.sampleRateInHz,
                        audioRecordConfig.channelConfig,
                        audioRecordConfig.audioFormat
                    ).convertToFile(byteArray, file)

                    val length = it.size * 1.0f / audioRecordConfig.sampleRateInHz
                    if (length > 3.2) {
                        val shortArray =
                            ShortArray((audioRecordConfig.sampleRateInHz * 3.2).toInt() + audioRecordConfig.sampleRateInHz)
                        System.arraycopy(
                            it,
                            it.size - (audioRecordConfig.sampleRateInHz * 3.2).toInt(),
                            shortArray,
                            audioRecordConfig.sampleRateInHz,
                            (audioRecordConfig.sampleRateInHz * 3.2).toInt()
                        )
                        val file2 =
                            FileUtils.getFolderPathWithSeparator(fileFolder) + System.currentTimeMillis() + ".wav"
                        val byteArray2 = AudioUtils.shortArrayToByteArray(shortArray)
                        recognise(
                            audioRecordConfig.sampleRateInHz,
                            SherpaAudioConvertTools.byteArrayToSherpaArray(byteArray2)
                        )
                        PcmToWav(
                            audioRecordConfig.sampleRateInHz,
                            audioRecordConfig.channelConfig,
                            audioRecordConfig.audioFormat
                        ).convertToFile(byteArray2, file2)
                        ZLog.d(TAG, "record data:${file2}")
                    }

                }
            }
        }
    }


    fun startReal() {
        AudioRecordWithEndpoint.startDataRecord { audioRecordConfig, pcmData, result ->
            pcmData?.let { data ->
                Log.i(TAG, "====================")
                val max = (Short.MAX_VALUE * 0.1f).toInt().toShort()
                Log.i(
                    TAG, "record data size:${pcmData.size} max:$max, audioMax: ${pcmData.max()}"
                )
                if (SherpaAudioConvertTools.isOverSilence(pcmData, max)) {
                    val samples = SherpaAudioConvertTools.shortArrayToSherpaArray(data, data.size)
                    recognise(audioRecordConfig.sampleRateInHz, samples)
//                    AudioUtils.shortArrayToByteArray(data).let {
//                        SherpaAudioConvertTools.byteArrayToSherpaArray(it).let { float ->
//                            recognise(audioRecordConfig.sampleRateInHz, float)
//                        }
//
//                        AudioUtils.byteArrayToShortArray(it, it.size).let { short ->
//                            val erer = SherpaAudioConvertTools.shortArrayToSherpaArray(short, short.size)
//                            recognise(audioRecordConfig.sampleRateInHz, erer)
//                        }
//                    }
                } else {
                    Log.i(TAG, "无效音频，无有效内容")
                }
                Log.i(TAG, "====================")
            }
        }
    }

    fun startRealFile() {
        val max = (Short.MAX_VALUE * 0.1f).toInt().toShort()
        AudioRecordWithEndpoint.startFileRecord(activity,
            "File",
            AAFFileWrapper.getMediaTempFolder(),
            max,
            object : AAFDataCallback<String>() {
                override fun onSuccess(result: String?) {
                    ZLog.d(TAG, "record data:${result}")
                    result?.let {
                        readWavHead(it)
                        recognise(
                            WaveFileReader(result).sampleRate.toInt(),
                            SherpaAudioConvertTools.readWavAudioToSherpaArray(result)
                        )
                    }
                }
            })
    }

    fun concatenate(first: FloatArray?, second: FloatArray?): FloatArray {
        val result = FloatArray((first?.size ?: 0) + (second?.size ?: 0))
        first?.copyInto(result, 0)
        second?.copyInto(result, first?.size ?: 0)
        return result
    }

    fun recognise(sampleRateInHz: Int, data: FloatArray?) {
        data?.let { samples ->
            Log.i(TAG, "record data:${samples.size}")
//            mKeywordSpotterManager.doRecognizer(sampleRateInHz, samples).let {
//                Log.i(TAG, "mKeywordSpotterManager Start to recognizer:$it")
//            }
            val time = System.currentTimeMillis()
            mASROfflineManager1.startRecognizer(sampleRateInHz, samples).let {
                Log.i(
                    TAG,
                    "mRecognizerManager cost ${System.currentTimeMillis() - time} to recognizer:$it"
                )

                showResult(it)
            }
        }
    }

    fun startFile(file: String) {
        readWavHead(file)
        SherpaAudioConvertTools.readWavAudioToSherpaArray(file)?.let {
            recognise(WaveFileReader(file).sampleRate.toInt(), it)
        }
    }
}

fun getASROfflineRecognizerConfig_paraformer(context: Context): OfflineRecognizerConfig {
    val modelDir = "sherpa-onnx-paraformer-zh-2023-09-14"
    FileUtils.copyAssetsFolderToFolder(context, modelDir, AAFFileWrapper.getTempFolder() + modelDir)
    return OfflineRecognizerConfig(
        featConfig = getFeatureConfig(AudioRecordConfig.DEFAULT_SAMPLE_RATE_IN_HZ, 80),
        modelConfig = OfflineModelConfig(
            paraformer = OfflineParaformerModelConfig(
                model = AAFFileWrapper.getTempFolder() + "$modelDir/model.int8.onnx",
            ),
            numThreads = max(CPUHelper.getNumberOfCores()/2, 1),
            debug = true,
            tokens = AAFFileWrapper.getTempFolder() + "$modelDir/tokens.txt",
            modelType = "paraformer",
        )
    )
}

fun getASROfflineRecognizerConfig_paraformer_small(context: Context): OfflineRecognizerConfig {
    val modelDir = "sherpa-onnx-paraformer-zh-small-2024-03-09"
    FileUtils.copyAssetsFolderToFolder(context, modelDir, AAFFileWrapper.getTempFolder() + modelDir)
    return OfflineRecognizerConfig(
        featConfig = getFeatureConfig(AudioRecordConfig.DEFAULT_SAMPLE_RATE_IN_HZ, 80),
        modelConfig = OfflineModelConfig(
            paraformer = OfflineParaformerModelConfig(
                model = AAFFileWrapper.getTempFolder() + "$modelDir/model.int8.onnx",
            ),
            numThreads = max(CPUHelper.getNumberOfCores()/2, 1),
            debug = true,
            tokens = AAFFileWrapper.getTempFolder() + "$modelDir/tokens.txt",
            modelType = "paraformer",
        )
    )
}

fun getASROfflineRecognizerConfig_whisper_tiny(context: Context): OfflineRecognizerConfig {
    val modelDir = "sherpa-onnx-whisper-tiny"
    FileUtils.copyAssetsFolderToFolder(context, modelDir, AAFFileWrapper.getTempFolder() + modelDir)
    return OfflineRecognizerConfig(
        featConfig = getFeatureConfig(AudioRecordConfig.DEFAULT_SAMPLE_RATE_IN_HZ, 80),
        modelConfig = OfflineModelConfig(
            whisper = OfflineWhisperModelConfig(
                encoder = AAFFileWrapper.getTempFolder() + "$modelDir/tiny-encoder.int8.onnx",
                decoder = AAFFileWrapper.getTempFolder() + "$modelDir/tiny-decoder.int8.onnx",
                language = "zh"
            ),
            tokens = AAFFileWrapper.getTempFolder() + "$modelDir/tiny-tokens.txt",
            modelType = "whisper",
        )
    )
}

fun getASROfflineRecognizerConfig_whisper_base(context: Context): OfflineRecognizerConfig {
    val modelDir = "sherpa-onnx-whisper-base"
    FileUtils.copyAssetsFolderToFolder(context, modelDir, AAFFileWrapper.getTempFolder() + modelDir)
    return OfflineRecognizerConfig(
        featConfig = getFeatureConfig(AudioRecordConfig.DEFAULT_SAMPLE_RATE_IN_HZ, 80),
        modelConfig = OfflineModelConfig(
            whisper = OfflineWhisperModelConfig(
                encoder = AAFFileWrapper.getTempFolder() + "$modelDir/base-encoder.int8.onnx",
                decoder = AAFFileWrapper.getTempFolder() + "$modelDir/base-decoder.int8.onnx",
                language = "zh"
            ),
            tokens = AAFFileWrapper.getTempFolder() + "$modelDir/base-tokens.txt",
            modelType = "whisper",
        )
    )
}
