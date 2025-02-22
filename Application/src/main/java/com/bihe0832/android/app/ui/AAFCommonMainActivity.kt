package com.bihe0832.android.app.ui

import android.os.Bundle
import com.bihe0832.android.app.R
import com.bihe0832.android.app.message.checkMsgAndShowFace
import com.bihe0832.android.app.ui.navigation.AAFNavigationDrawerFragment
import com.bihe0832.android.app.ui.navigation.addRedDotAction
import com.bihe0832.android.app.update.UpdateManager
import com.bihe0832.android.common.main.CommonActivityWithNavigationDrawer
import com.bihe0832.android.common.navigation.drawer.NavigationDrawerFragment
import com.bihe0832.android.framework.ZixieContext

open class AAFCommonMainActivity : CommonActivityWithNavigationDrawer() {

    private val mAAFNavigationDrawerFragment = AAFNavigationDrawerFragment()

    override fun getNavigationDrawerFragment(): NavigationDrawerFragment? {
        return mAAFNavigationDrawerFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addRedDotAction(findViewById(R.id.title_icon_unread))
        checkMsgAndShowFace()
        UpdateManager.checkUpdateAndShowDialog(this, false, ZixieContext.isOfficial())
        updateTitle(titleName)
        showQrcodeScan(needSound = true, needVibrate = true, onlyQRCode = false)
    }
}
