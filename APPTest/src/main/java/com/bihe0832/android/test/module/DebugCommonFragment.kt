package com.bihe0832.android.test.module

import android.view.View
import com.bihe0832.android.common.debug.device.DebugDeviceFragment
import com.bihe0832.android.common.debug.device.getMobileInfo
import com.bihe0832.android.common.debug.item.getDebugItem
import com.bihe0832.android.common.debug.item.getTipsItem
import com.bihe0832.android.common.debug.log.DebugLogActivity
import com.bihe0832.android.common.debug.module.DebugCommonFragment
import com.bihe0832.android.framework.ZixieContext
import com.bihe0832.android.lib.adapter.CardBaseModule

open class DebugCommonFragment : DebugCommonFragment() {

    override fun getDataList(): ArrayList<CardBaseModule> {
        return ArrayList<CardBaseModule>().apply {
            add(getTipsItem("APPFactory的通用组件和工具"))
            add(getDebugItem("查看应用版本及环境", View.OnClickListener { showAPPInfo() }))
            add(getDebugItem("查看使用情况", View.OnClickListener { showUsedInfo() }))
            add(getDebugItem("查看设备概要信息", View.OnClickListener { showInfoWithHTML("设备概要信息", getMobileInfo(context)) }))
            add(getDebugFragmentItemData("查看设备详细信息", DebugDeviceFragment::class.java))
            add(getDebugItem("日志管理", View.OnClickListener { startActivityWithException(DebugLogActivity::class.java) }))
            add(getTipsItem("AAF 支持的应用"))
        }
    }

    fun openNew(activityName: String) {
        try {
            startActivityWithException(activityName)
        } catch (e: Exception) {
            e.printStackTrace()
            ZixieContext.showDebug("APP模块未集成")
        }
    }
}