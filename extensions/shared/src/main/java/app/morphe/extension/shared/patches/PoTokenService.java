/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version
 *    control systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

package app.morphe.extension.shared.patches;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import app.morphe.extension.shared.utils.Logger;

/**
 * In-process implementation of the GMS PoToken service used by supported YouTube builds.
 *
 * <p>The public contract is an AIDL/Binder contract, so importing GMS model classes would risk
 * colliding with classes already present in the host app. This class writes
 * the small safe-parcel payloads directly and accepts both the legacy and current transaction
 * numbers used by the supported clients.</p>
 */
public class PoTokenService extends Service {
    private static final String BROKER_DESCRIPTOR = "com.google.android.gms.common.internal.IGmsServiceBroker";
    private static final String CALLBACK_DESCRIPTOR = "com.google.android.gms.common.internal.IGmsCallbacks";
    private static final String TOKEN_SERVICE_DESCRIPTOR = "com.google.android.gms.potokens.internal.IPoTokensService";
    private static final String TOKEN_CALLBACK_DESCRIPTOR = "com.google.android.gms.potokens.internal.ITokenCallbacks";
    private static final String STATUS_CALLBACK_DESCRIPTOR = "com.google.android.gms.common.api.internal.IStatusCallback";

    private static final int LEGACY_BROKER_TRANSACTION = 45;
    private static final int CURRENT_BROKER_TRANSACTION = 46;
    private static final int LEGACY_TOKEN_TRANSACTION = 2;
    private static final int CURRENT_TOKEN_TRANSACTION = 3;
    private static final int LEGACY_TOKEN_CALLBACK_TRANSACTION = 1;
    private static final int CURRENT_TOKEN_CALLBACK_TRANSACTION = 2;
    private static final int API_METADATA_MAGIC = -204102970;

    private final IBinder tokenService = new TokenServiceBinder();
    private final IBinder broker = new BrokerBinder();

    @Override
    public IBinder onBind(Intent intent) {
        Logger.printInfo(() -> "PoTokenService: onBind: " + intent);
        return broker;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Logger.printInfo(() -> "PoTokenService: onUnbind: " + intent);
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        Logger.printInfo(() -> "PoTokenService: onRebind: " + intent);
        super.onRebind(intent);
    }

    private final class BrokerBinder extends InterfaceBinder {
        private BrokerBinder() {
            super(BROKER_DESCRIPTOR);
        }

        @Override
        public boolean onTransact(int code, @NonNull Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code != LEGACY_BROKER_TRANSACTION && code != CURRENT_BROKER_TRANSACTION) {
                Logger.printInfo(() -> "PoTokenService: Unknown broker transaction code: " + code);
                return super.onTransact(code, data, reply, flags);
            }

            Logger.printInfo(() -> "PoTokenService: IGmsServiceBroker transaction received (code=" + code + ")");
            enforceInterface(data);
            IBinder callback = data.readStrongBinder();
            sendConnectionInfo(callback);
            writeNoException(reply);
            return true;
        }

        private void sendConnectionInfo(IBinder callback) throws RemoteException {
            if (callback == null) {
                return;
            }

            Parcel request = Parcel.obtain();
            Parcel response = Parcel.obtain();
            try {
                request.writeInterfaceToken(CALLBACK_DESCRIPTOR);
                request.writeInt(0);
                request.writeStrongBinder(tokenService);
                request.writeInt(1);
                writeConnectionInfo(request);
                callback.transact(3, request, response, 0);
            } finally {
                request.recycle();
                response.recycle();
            }
        }
    }

    private final class TokenServiceBinder extends InterfaceBinder {
        private TokenServiceBinder() {
            super(TOKEN_SERVICE_DESCRIPTOR);
        }

        @Override
        public boolean onTransact(int code, @NonNull Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code != 1 && code != LEGACY_TOKEN_TRANSACTION && code != CURRENT_TOKEN_TRANSACTION) {
                Logger.printInfo(() -> "PoTokenService: Unknown token transaction code: " + code);
                return super.onTransact(code, data, reply, flags);
            }

            enforceInterface(data);
            IBinder callback = data.readStrongBinder();
            data.readInt(); // Request code, currently unused by the PoToken client.

            if (code == 1) {
                Logger.printInfo(() -> "PoTokenService: Status check requested (code=1)");
                sendStatus(callback);
            } else {
                long startTime = System.currentTimeMillis();
                byte[] input = data.createByteArray();
                int inputLength = input != null ? input.length : 0;
                boolean currentProtocol = code == CURRENT_TOKEN_TRANSACTION;
                boolean callbackIncludesApiMetadata = currentProtocol && hasApiMetadataField(data);

                Logger.printInfo(() -> "PoTokenService: Token requested (code=" + code
                        + ", inputLength=" + inputLength
                        + ", currentProtocol=" + currentProtocol
                        + ", hasApiMetadata=" + callbackIncludesApiMetadata + ")");

                sendToken(callback, currentProtocol, callbackIncludesApiMetadata, input);

                long duration = System.currentTimeMillis() - startTime;
                Logger.printInfo(() -> "PoTokenService: Token transaction completed in " + duration + "ms");
            }

            writeNoException(reply);
            return true;
        }

        private void sendStatus(IBinder callback) throws RemoteException {
            if (callback == null) {
                return;
            }

            Parcel request = Parcel.obtain();
            Parcel response = Parcel.obtain();
            try {
                request.writeInterfaceToken(STATUS_CALLBACK_DESCRIPTOR);
                request.writeInt(1);
                writeEmptySafeParcel(request);
                callback.transact(1, request, response, 0);
            } finally {
                request.recycle();
                response.recycle();
            }
        }

        private void sendToken(
                IBinder callback,
                boolean currentProtocol,
                boolean callbackIncludesApiMetadata,
                byte[] input
        )
                throws RemoteException {
            if (callback == null) {
                Logger.printWarn(() -> "PoTokenService: Token callback binder is null!");
                return;
            }

            byte[] tokenResult = PoTokenMinter.buildPoTokenResult(input);
            if (tokenResult.length == 0) {
                Logger.printWarn(() -> "PoTokenService: Minted PoToken result is empty!");
            } else {
                Logger.printInfo(() -> "PoTokenService: PoToken minted successfully (" + tokenResult.length + " bytes)");
            }

            Parcel request = Parcel.obtain();
            Parcel response = Parcel.obtain();
            try {
                request.writeInterfaceToken(TOKEN_CALLBACK_DESCRIPTOR);
                request.writeInt(1);
                writeEmptySafeParcel(request); // Status.SUCCESS.
                request.writeInt(1);
                writePoToken(request, tokenResult);

                // Newer clients added ApiMetadata to this callback. Older clients reject any
                // trailing data, so only append the null marker when the request carries the
                // newer versioned ApiMetadata shape.
                if (callbackIncludesApiMetadata) {
                    request.writeInt(0);
                }

                int transaction = currentProtocol
                        ? CURRENT_TOKEN_CALLBACK_TRANSACTION
                        : LEGACY_TOKEN_CALLBACK_TRANSACTION;
                callback.transact(transaction, request, response, 0);
            } finally {
                request.recycle();
                response.recycle();
            }
        }
    }

    /**
     * Detects the ApiMetadata shape used by newer GMS clients without loading their model classes.
     * The newer parcel contains a presence marker, ApiMetadata's version sentinel, and a
     * safe-parcel object with field 2. The older parcel contains no trailing value.
     */
    private static boolean hasApiMetadataField(Parcel parcel) {
        int originalPosition = parcel.dataPosition();
        try {
            if (parcel.dataAvail() < 4 || parcel.readInt() == 0) {
                return false;
            }

            // After the presence marker, the metadata contains its sentinel and the safe-parcel
            // object header plus size: 4 + 4 + 4 bytes.
            if (parcel.dataAvail() < 12 || parcel.readInt() != API_METADATA_MAGIC) {
                return false;
            }

            int objectHeader = parcel.readInt();
            if ((objectHeader & 0xFFFF0000) != 0xFFFF0000 || (objectHeader & 0xFFFF) != 20293) {
                return false;
            }

            int objectSize = parcel.readInt();
            int objectEnd = parcel.dataPosition() + objectSize;
            if (objectSize < 0 || objectEnd < parcel.dataPosition() || objectEnd > parcel.dataSize()) {
                return false;
            }

            while (parcel.dataPosition() < objectEnd) {
                if (objectEnd - parcel.dataPosition() < 4) {
                    return false;
                }

                int fieldHeader = parcel.readInt();
                int fieldSize = (fieldHeader & 0xFFFF0000) == 0xFFFF0000
                        ? parcel.readInt()
                        : fieldHeader >>> 16;
                int fieldEnd = parcel.dataPosition() + fieldSize;
                if (fieldSize < 0 || fieldEnd < parcel.dataPosition() || fieldEnd > objectEnd) {
                    return false;
                }
                if ((fieldHeader & 0xFFFF) == 2) {
                    return true;
                }
                parcel.setDataPosition(fieldEnd);
            }
            return false;
        } finally {
            parcel.setDataPosition(originalPosition);
        }
    }

    private abstract static class InterfaceBinder extends Binder implements IInterface {
        private final String descriptor;

        private InterfaceBinder(String descriptor) {
            this.descriptor = descriptor;
            attachInterface(this, descriptor);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, @NonNull Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == INTERFACE_TRANSACTION) {
                if (reply != null) {
                    reply.writeString(descriptor);
                }
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }

        protected final void enforceInterface(Parcel data) {
            data.enforceInterface(descriptor);
        }
    }

    private static void writeNoException(Parcel reply) {
        if (reply != null) {
            reply.writeNoException();
        }
    }

    /**
     * Writes the safe-parcel representation expected by ConnectionInfo, including PO_TOKENS
     * feature version 1. These helpers intentionally avoid declaring duplicate GMS classes.
     */
    private static void writeConnectionInfo(Parcel parcel) {
        int objectStart = beginSafeParcelObject(parcel);

        int featuresStart = beginVariableField(parcel, 2);
        parcel.writeInt(1); // Feature array length.
        parcel.writeInt(1); // Non-null Feature.
        writeFeature(parcel);
        finishVariableField(parcel, featuresStart);

        writeIntField(parcel, 3, 0);
        finishSafeParcelObject(parcel, objectStart);
    }

    private static void writeFeature(Parcel parcel) {
        int objectStart = beginSafeParcelObject(parcel);
        writeStringField(parcel, 1, "PO_TOKENS");
        writeIntField(parcel, 2, -1);
        writeLongField(parcel, 3, 1L);
        writeIntField(parcel, 4, 0); // fullyRolledOut = false for the supported client protocol.
        finishSafeParcelObject(parcel, objectStart);
    }

    private static void writePoToken(Parcel parcel, byte[] data) {
        int objectStart = beginSafeParcelObject(parcel);
        int dataStart = beginVariableField(parcel, 1);
        parcel.writeByteArray(data);
        finishVariableField(parcel, dataStart);
        finishSafeParcelObject(parcel, objectStart);
    }

    private static void writeEmptySafeParcel(Parcel parcel) {
        int objectStart = beginSafeParcelObject(parcel);
        finishSafeParcelObject(parcel, objectStart);
    }

    @SuppressWarnings("SameParameterValue")
    private static void writeStringField(Parcel parcel, int fieldId, String value) {
        int fieldStart = beginVariableField(parcel, fieldId);
        parcel.writeString(value);
        finishVariableField(parcel, fieldStart);
    }

    private static void writeIntField(Parcel parcel, int fieldId, int value) {
        parcel.writeInt((4 << 16) | fieldId);
        parcel.writeInt(value);
    }

    @SuppressWarnings("SameParameterValue")
    private static void writeLongField(Parcel parcel, int fieldId, long value) {
        parcel.writeInt((8 << 16) | fieldId);
        parcel.writeLong(value);
    }

    private static int beginSafeParcelObject(Parcel parcel) {
        return beginVariableField(parcel, 20293);
    }

    private static int beginVariableField(Parcel parcel, int fieldId) {
        parcel.writeInt(0xFFFF0000 | fieldId);
        parcel.writeInt(0);
        return parcel.dataPosition();
    }

    private static void finishSafeParcelObject(Parcel parcel, int fieldStart) {
        finishVariableField(parcel, fieldStart);
    }

    private static void finishVariableField(Parcel parcel, int fieldStart) {
        int end = parcel.dataPosition();
        parcel.setDataPosition(fieldStart - 4);
        parcel.writeInt(end - fieldStart);
        parcel.setDataPosition(end);
    }
}
