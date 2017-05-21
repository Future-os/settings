/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.fuelgauge.anomaly.checker;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.BatteryStats;
import android.text.format.DateUtils;
import android.util.ArrayMap;

import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.anomaly.Anomaly;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class WakeLockAnomalyDetectorTest {
    private static final long ANOMALY_WAKELOCK_TIME_MS = DateUtils.HOUR_IN_MILLIS;
    private static final long NORMAL_WAKELOCK_TIME_MS = DateUtils.SECOND_IN_MILLIS;
    private static final int ANOMALY_UID = 111;
    private static final int NORMAL_UID = 222;
    @Mock
    private BatteryStatsHelper mBatteryStatsHelper;
    @Mock
    private BatterySipper mAnomalySipper;
    @Mock
    private BatteryStats.Timer mAnomalyTimer;
    @Mock
    private BatteryStats.Uid.Wakelock mAnomalyWakelock;
    @Mock
    private BatterySipper mNormalSipper;
    @Mock
    private BatteryStats.Timer mNormalTimer;
    @Mock
    private BatteryStats.Uid.Wakelock mNormalWakelock;
    @Mock
    private BatteryStats.Uid mAnomalyUid;
    @Mock
    private BatteryStats.Uid mNormalUid;
    @Mock
    private BatteryUtils mBatteryUtils;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private ApplicationInfo mApplicationInfo;

    private ArrayMap<String, BatteryStats.Uid.Wakelock> mAnomalyWakelocks;
    private ArrayMap<String, BatteryStats.Uid.Wakelock> mNormalWakelocks;
    private WakeLockAnomalyDetector mWakelockAnomalyDetector;
    private Context mContext;
    private List<BatterySipper> mUsageList;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);

        doReturn(false).when(mBatteryUtils).shouldHideSipper(any());
        doReturn(mPackageManager).when(mContext).getPackageManager();
        doReturn(mApplicationInfo).when(mPackageManager).getApplicationInfo(anyString(), anyInt());

        mAnomalySipper.uidObj = mAnomalyUid;
        mAnomalyWakelocks = new ArrayMap<>();
        mAnomalyWakelocks.put("", mAnomalyWakelock);
        doReturn(mAnomalyWakelocks).when(mAnomalyUid).getWakelockStats();
        doReturn(mAnomalyTimer).when(mAnomalyWakelock).getWakeTime(BatteryStats.WAKE_TYPE_PARTIAL);
        doReturn(ANOMALY_UID).when(mAnomalyUid).getUid();

        mNormalSipper.uidObj = mNormalUid;
        mNormalWakelocks = new ArrayMap<>();
        mNormalWakelocks.put("", mNormalWakelock);
        doReturn(mNormalTimer).when(mNormalWakelock).getWakeTime(BatteryStats.WAKE_TYPE_PARTIAL);
        doReturn(mNormalWakelocks).when(mNormalUid).getWakelockStats();
        doReturn(NORMAL_UID).when(mNormalUid).getUid();

        mUsageList = new ArrayList<>();
        mUsageList.add(mAnomalySipper);
        mUsageList.add(mNormalSipper);
        doReturn(mUsageList).when(mBatteryStatsHelper).getUsageList();

        mWakelockAnomalyDetector = spy(new WakeLockAnomalyDetector(mContext));
        mWakelockAnomalyDetector.mBatteryUtils = mBatteryUtils;
        doReturn(ANOMALY_WAKELOCK_TIME_MS).when(mWakelockAnomalyDetector).getTotalDurationMs(
                eq(mAnomalyTimer), anyLong());
        doReturn(NORMAL_WAKELOCK_TIME_MS).when(mWakelockAnomalyDetector).getTotalDurationMs(
                eq(mNormalTimer), anyLong());
    }

    @Test
    public void testDetectAnomalies_containsAnomaly_detectIt() {
        final Anomaly anomaly = new Anomaly.Builder()
                .setUid(ANOMALY_UID)
                .setType(Anomaly.AnomalyType.WAKE_LOCK)
                .build();

        List<Anomaly> mAnomalies = mWakelockAnomalyDetector.detectAnomalies(mBatteryStatsHelper);

        assertThat(mAnomalies).containsExactly(anomaly);
    }
}