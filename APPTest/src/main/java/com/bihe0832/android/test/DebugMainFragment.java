package com.bihe0832.android.test;

import androidx.fragment.app.Fragment;

import com.bihe0832.android.common.debug.audio.DebugWAVListFragment;
import com.bihe0832.android.test.module.DebugCommonFragment;
import com.bihe0832.android.test.module.DebugRouterFragment;
import com.bihe0832.android.test.module.audio.DebugAudioFragment;
import com.bihe0832.android.test.module.audio.asr.DebugRecordAndASRFragRecment;


/**
 * Created by zixie on 16/6/30.
 */
public class DebugMainFragment extends com.bihe0832.android.common.debug.DebugMainFragment {

    private static final String TAB_AUDIO_PLAY = "音频播放";
    private static final String TAB_ASR = "语音识别";

    public DebugMainFragment() {
        super(new String[]{
                TAB_AUDIO_PLAY, TAB_ASR
        });
    }

    private final boolean isDev = true;

    protected Fragment getFragmentByIndex(String title) {
      if (title.equals(TAB_AUDIO_PLAY)) {
            return new DebugAudioFragment();
        } else if (title.equals(TAB_ASR)) {
            return new DebugRecordAndASRFragRecment();
        } else {
            return new DebugWAVListFragment();
        }
    }

    @Override
    protected int getDefaultTabIndex() {
        if (isDev) {
            return mTabString.length - 1;
        } else {
            return 0;
        }
    }
}
