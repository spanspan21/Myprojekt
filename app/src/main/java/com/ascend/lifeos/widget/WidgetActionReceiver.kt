package com.ascend.lifeos.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.wellbeing.JarvisGuardService
import com.ascend.lifeos.wellbeing.WellbeingStore

/** Home-screen quick actions: log without opening the app. */
class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        runCatching { Repo.initIfNeeded(ctx) }
        when (intent.getStringExtra("action")) {
            "water" -> Repo.addWater(1)
            "focus" -> {
                if (!WellbeingStore.inFocus(ctx)) {
                    WellbeingStore.startFocus(ctx, 25)
                    WellbeingStore.setEnabled(ctx, true)
                    runCatching { JarvisGuardService.start(ctx) }
                }
            }
        }
        // the process may die right after onReceive — the debounced save would
        // silently drop the tap, so force the write now
        runCatching { Repo.flush() }
        AscendWidget.refresh(ctx)
    }
}
