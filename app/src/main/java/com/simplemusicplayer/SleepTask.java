package com.simplemusicplayer;


import android.content.Intent;
import android.support.annotation.NonNull;

import androidx.work.Worker;

/**
 * Class used with work manager to stop playback
 */
public class SleepTask extends Worker {

    @NonNull
    @Override
    public Result doWork() {
        getApplicationContext().sendBroadcast(new Intent("STOP"));
        return Result.SUCCESS;
    }

}
