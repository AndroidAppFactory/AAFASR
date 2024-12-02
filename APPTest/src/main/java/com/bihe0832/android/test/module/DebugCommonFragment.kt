package com.bihe0832.android.test.module

import android.view.View
import com.bihe0832.android.common.debug.item.DebugItemData
import com.bihe0832.android.common.debug.item.DebugTipsData
import com.bihe0832.android.common.debug.log.DebugLogActivity
import com.bihe0832.android.common.debug.module.DebugCommonFragment
import com.bihe0832.android.common.debug.module.DebugDeviceFragment
import com.bihe0832.android.common.debug.module.getMobileInfo
import com.bihe0832.android.framework.ZixieContext
import com.bihe0832.android.lib.adapter.CardBaseModule

open class DebugCommonFragment : DebugCommonFragment() {

    override fun getDataList(): ArrayList<CardBaseModule> {
        return ArrayList<CardBaseModule>().apply {
            add(DebugTipsData("APPFactory的通用组件和工具"))
            add(DebugItemData("查看应用版本及环境", View.OnClickListener { showAPPInfo() }))
            add(DebugItemData("查看使用情况", View.OnClickListener { showUsedInfo() }))
            add(DebugItemData("查看设备概要信息", View.OnClickListener { showInfoWithHTML("设备概要信息", getMobileInfo(context)) }))
            add(getDebugFragmentItemData("查看设备详细信息", DebugDeviceFragment::class.java))
            add(DebugItemData("日志管理", View.OnClickListener { startActivityWithException(DebugLogActivity::class.java) }))
            add(DebugTipsData("AAF 支持的应用"))
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