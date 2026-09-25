/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 *
 * Modifications and additions Copyright (C) 2026 anddea.
 * https://github.com/anddea/revanced-patches
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.morphe.extension.shared.patches;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import app.morphe.extension.shared.utils.Logger;

/**
 * Resolves the in-process PoToken service through the same dynamic lookup used by GMS clients.
 */
@SuppressWarnings("NullableProblems")
public class PoTokenServiceProvider extends ContentProvider {
    private static final String[] COLUMNS = {"version", "apkPath", "loaderPath", "apkDescStr"};

    @Override
    public boolean onCreate() {
        PoTokenMinter.warmUp();
        Logger.printInfo(() -> "PoTokenServiceProvider: ContentProvider created and initiated PoTokenMinter warmup");
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (extras != null && TextUtils.equals("serviceIntentCall", method)) {
            Context context = getContext();
            String serviceAction = extras.getString("serviceActionBundleKey");
            Logger.printInfo(() -> "PoTokenServiceProvider: serviceIntentCall for action: " + serviceAction);
            if (context != null && serviceAction != null) {
                Intent intent = new Intent(serviceAction);
                intent.setPackage(context.getPackageName());

                // Fast path: bypass PackageManager IPC for the in-process PoToken service
                if (PoTokenProviderPatch.LOCAL_PO_TOKEN_SERVICE_ACTION.equals(serviceAction)
                        || serviceAction.endsWith(".potokens.service.START")) {
                    intent.setClassName(context.getPackageName(), PoTokenService.class.getName());
                    Bundle response = new Bundle(1);
                    response.putParcelable("serviceResponseIntentKey", intent);
                    Logger.printInfo(() -> "PoTokenServiceProvider: Fast-path resolved directly to " + PoTokenService.class.getName());
                    return response;
                }

                ResolveInfo resolveInfo = context.getPackageManager().resolveService(intent, 0);
                if (resolveInfo != null) {
                    intent.setClassName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
                    Bundle response = new Bundle(1);
                    response.putParcelable("serviceResponseIntentKey", intent);
                    Logger.printInfo(() -> "PoTokenServiceProvider: PackageManager resolved to " + resolveInfo.serviceInfo.name);
                    return response;
                } else {
                    Logger.printWarn(() -> "PoTokenServiceProvider: Failed to resolve service for action: " + serviceAction);
                }
            }
        }
        return super.call(method, arg, extras);
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder
    ) {
        return new MatrixCursor(COLUMNS);
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.item/app.morphe.extension.shared.potoken";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return uri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
