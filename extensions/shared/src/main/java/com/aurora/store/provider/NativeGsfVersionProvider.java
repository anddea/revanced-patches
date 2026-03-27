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

import static com.dragons.aurora.playstoreapiv2.PackageNameUtil.getGmsCorePackageName;

import android.content.pm.PackageInfo;

import app.morphe.extension.shared.utils.PackageUtils;

public class NativeGsfVersionProvider {

    static private final int GOOGLE_SERVICES_VERSION_CODE = 250434004;

    private int gsfVersionCode = 0;

    public NativeGsfVersionProvider() {
        PackageInfo packageInfo = PackageUtils.getPackageInfo(getGmsCorePackageName());
        if (packageInfo != null) {
            gsfVersionCode = packageInfo.versionCode;
        }
    }

    public int getGsfVersionCode(boolean defaultIfNotFound) {
        return defaultIfNotFound && gsfVersionCode < GOOGLE_SERVICES_VERSION_CODE
                ? GOOGLE_SERVICES_VERSION_CODE
                : gsfVersionCode;
    }
}
