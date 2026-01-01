/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
 *
 * Aurora Store is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Aurora Store is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package com.aurora.store.provider;

import android.os.Build;

import com.dragons.aurora.playstoreapiv2.DeviceInfoProvider;

public class NativeDeviceInfoProvider implements DeviceInfoProvider {

    private NativeGsfVersionProvider gsfVersionProvider;

    public void setGsfVersionProvider() {
        gsfVersionProvider = new NativeGsfVersionProvider();
    }

    public int getSdkVersion() {
        return Build.VERSION.SDK_INT;
    }

    public int getPlayServicesVersion() {
        return gsfVersionProvider.getGsfVersionCode(true);
    }

    public String getAuthUserAgentString() {
        return "GoogleAuth/1.4 (" + Build.DEVICE + " " + Build.ID + ")";
    }
}
