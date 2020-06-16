/*
 * Copyright (c) 2018 Elastos Foundation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.elastos.carrier.webrtc;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.webrtc.PeerConnection;

import java.io.File;
import java.io.IOException;

class RtcEventLog {
    private static final String TAG = "RtcEventLog";
    private static final int OUTPUT_FILE_MAX_BYTES = 10_000_000;
    private final PeerConnection peerConnection;
    private RtcEventLogState state = RtcEventLogState.INACTIVE;

    public RtcEventLog(PeerConnection peerConnection) {
        if (peerConnection == null) {
            throw new NullPointerException("The peer connection is null.");
        }
        this.peerConnection = peerConnection;
    }

    public void start(final File outputFile) {
        if (state == RtcEventLogState.STARTED) {
            Log.e(TAG, "RtcEventLog has already started.");
            return;
        }
        final ParcelFileDescriptor fileDescriptor;
        try {
            fileDescriptor = ParcelFileDescriptor.open(outputFile,
                    ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE
                            | ParcelFileDescriptor.MODE_TRUNCATE);
        } catch (IOException e) {
            Log.e(TAG, "Failed to create a new file", e);
            return;
        }
        // Passes ownership of the file to WebRTC.
        boolean success =
                peerConnection.startRtcEventLog(fileDescriptor.detachFd(), OUTPUT_FILE_MAX_BYTES);
        if (!success) {
            Log.e(TAG, "Failed to start RTC event log.");
            return;
        }
        state = RtcEventLogState.STARTED;
        Log.d(TAG, "RtcEventLog started.");
    }

    public void stop() {
        if (state != RtcEventLogState.STARTED) {
            Log.e(TAG, "RtcEventLog was not started.");
            return;
        }
        peerConnection.stopRtcEventLog();
        state = RtcEventLogState.STOPPED;
        Log.d(TAG, "RtcEventLog stopped.");
    }

    enum RtcEventLogState {
        INACTIVE,
        STARTED,
        STOPPED,
    }
}
