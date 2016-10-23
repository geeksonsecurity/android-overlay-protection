package com.geeksonsecurity.android.overlayprotector.service;

import com.geeksonsecurity.android.overlayprotector.domain.OverlayState;

public interface IOverlayNotifyService {
    void processOverlayState(OverlayState state);

    void updateNotificationCount(long lastMinuteEventCount);
}
