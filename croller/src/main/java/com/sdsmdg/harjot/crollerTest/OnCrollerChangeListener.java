package com.sdsmdg.harjot.crollerTest;

public interface OnCrollerChangeListener {
    void onProgressChanged(Croller croller, int progress);

    void onStartTrackingTouch(Croller croller, int progress);

    void onStopTrackingTouch(Croller croller, int progress);
}
