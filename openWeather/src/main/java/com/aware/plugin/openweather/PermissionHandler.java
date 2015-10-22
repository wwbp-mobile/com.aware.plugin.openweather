package com.aware.plugin.openweather;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

/**
 * Created by denzil on 22/10/15.
 */
public class PermissionHandler extends Activity {

    private final int PERMISSIONS_RESULT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCompat.requestPermissions(PermissionHandler.this, Plugin.REQUIRED_PERMISSIONS, PERMISSIONS_RESULT);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        finish();
    }
}
