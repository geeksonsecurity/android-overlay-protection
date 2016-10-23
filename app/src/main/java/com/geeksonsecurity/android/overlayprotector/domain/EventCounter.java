package com.geeksonsecurity.android.overlayprotector.domain;

public class EventCounter {
    private long[] _minuteCount = new long[60];
    private long _lastTimestamp;

    private int _currentIdx = 0;

    public void newEvent() {
        long now = System.currentTimeMillis();
        if (_lastTimestamp == 0) {
            _lastTimestamp = now;
        }

        long deltaSec = (now - _lastTimestamp) / 1000;

        if (deltaSec >= 1) {
            for (int i = 0; i < deltaSec && i < _minuteCount.length; i++)
                _minuteCount[i] = 0;

            _currentIdx += deltaSec;
            _currentIdx = _currentIdx % _minuteCount.length;
            _minuteCount[_currentIdx] = 0;
            _lastTimestamp = now;
        }
        _minuteCount[_currentIdx] += 1;
    }

    public long getLastMinuteEventCount() {
        long total = 0;
        for (long i : _minuteCount) {
            total += i;
        }
        return total;
    }
}
