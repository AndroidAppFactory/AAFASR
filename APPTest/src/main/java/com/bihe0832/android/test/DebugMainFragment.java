package com.bihe0832.android.test;

import androidx.fragment.app.Fragment;
import com.bihe0832.android.test.module.audio.DebugAudioFragment;
import com.bihe0832.android.test.module.audio.DebugRecordAndASRFragment;


/**
 * Created by zixie on 16/6/30.
 */
public class DebugMainFragment extends com.bihe0832.android.common.debug.DebugMainFragment {

    public static final String TAB_AUDIO_PLAY = "音频播放";
    public static final String TAB_ASR = "识别调试";

    public DebugMainFragment() {
        super(new String[]{TAB_ASR, TAB_AUDIO_PLAY});
    }

    protected Fragment getFragmentByIndex(String title) {
        if (title.equals(TAB_AUDIO_PLAY)) {
            return new DebugAudioFragment();
        } else {
            return new DebugRecordAndASRFragment();
        }
    }
}