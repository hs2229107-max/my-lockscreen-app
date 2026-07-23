package com.hawahi.locker;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

public class AdminReceiver extends DeviceAdminReceiver {
    @Override
    public CharSequence onDisableRequested(Context c, Intent i) {
        return "لا يمكن إلغاء صلاحية المشرف";
    }
}