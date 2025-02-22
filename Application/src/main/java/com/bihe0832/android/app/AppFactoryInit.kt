package com.bihe0832.android.app

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import com.bihe0832.android.app.message.AAFMessageManager
import com.bihe0832.android.app.router.RouterHelper
import com.bihe0832.android.common.permission.AAFPermissionManager
import com.bihe0832.android.framework.ZixieContext
import com.bihe0832.android.framework.ZixieCoreInit
import com.bihe0832.android.framework.privacy.AgreementPrivacy
import com.bihe0832.android.lib.download.wrapper.DownloadFileUtils
import com.bihe0832.android.lib.log.ZLog
import com.bihe0832.android.lib.thread.ThreadManager
import com.bihe0832.android.lib.utils.os.BuildUtils
import com.bihe0832.android.lib.utils.os.ManufacturerUtil

/**
 *
 * @author zixie code@bihe0832.com
 * Created on 2019-07-09.
 * Description: 加速器相关的初始化
 *
 */

object AppFactoryInit {
    // 全局变量的初始化
    var hasInit = false

    // 目前仅仅主进程和web进程需要初始化
    @Synchronized
    private fun initCore(application: android.app.Application, processName: String) {
        val ctx = application.applicationContext
        if (!hasInit) {
            hasInit = true
            ZixieCoreInit.initAfterAgreePrivacy(application)
            Log.e(ZixieCoreInit.TAG, "———————————————————————— 设备信息 ————————————————————————")
            Log.e(ZixieCoreInit.TAG, "设备ID: ${ZixieContext.deviceId}")
            Log.e(
                ZixieCoreInit.TAG,
                "厂商型号: ${ManufacturerUtil.MANUFACTURER}, ${ManufacturerUtil.MODEL}, ${ManufacturerUtil.BRAND}",
            )
            Log.e(
                ZixieCoreInit.TAG,
                "系统版本: Android ${BuildUtils.RELEASE}, API  ${BuildUtils.SDK_INT}" + if (ManufacturerUtil.isHarmonyOs()) {
                    ", Harmony(${ManufacturerUtil.getHarmonyVersion()})"
                } else {
                    ""
                },
            )
            Log.e(ZixieCoreInit.TAG, "———————————————————————— 设备信息 ————————————————————————")

            RouterHelper.initRouter()
            AAFPermissionManager.initPermission()
            ThreadManager.getInstance().start {
                DownloadFileUtils.init(ctx, 10, ZixieContext.isDebug())
            }
            AAFMessageManager.initModule(application)
            ZLog.d("Application process $processName initCore ManufacturerUtil:" + ManufacturerUtil.MODEL)
        }
    }



    fun initAll(application: android.app.Application) {
        if (AgreementPrivacy.hasAgreedPrivacy()) {
            val am = application.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningApps = am.runningAppProcesses
            for (it in runningApps) {
                if (it.pid == android.os.Process.myPid() && it.processName != null && it.processName.contains(
                        application.getPackageName(),
                    )
                ) {
                    ZLog.e("Application initCore process: name:" + it.processName + " and id:" + it.pid)
                    val processName = it.processName
                    initCore(application, processName)
                }
            }
        }
    }
}
