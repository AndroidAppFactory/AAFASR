package com.bihe0832.android.test

import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import com.bihe0832.android.app.router.RouterConstants
import com.bihe0832.android.app.ui.AAFCommonMainActivity
import com.bihe0832.android.lib.adapter.CardInfoHelper
import com.bihe0832.android.lib.immersion.hideBottomUIMenu
import com.bihe0832.android.lib.router.annotation.APPMain
import com.bihe0832.android.lib.router.annotation.Module
import com.bihe0832.android.lib.sqlite.impl.CommonDBManager
import com.bihe0832.android.lib.utils.os.BuildUtils

@APPMain
@Module(RouterConstants.MODULE_NAME_DEBUG)
class DebugMainActivity : AAFCommonMainActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initToolbar(R.id.common_toolbar, "DebugMainActivity", false, needBack = true, iconRes = R.drawable.ic_left_arrow)
        if (BuildUtils.SDK_INT > Build.VERSION_CODES.GINGERBREAD) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())
        }
        CardInfoHelper.getInstance().setAutoAddItem(true)
        hideBottomUIMenu()
        CommonDBManager.init(this)
    }

    override fun getRootFragmentClassName(): String {
        return DebugMainFragment::class.java.name
    }
}
