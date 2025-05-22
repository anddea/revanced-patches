WITH
    TargetProcessesByName AS (
        SELECT
            'APP 1' as app_marker,
            'test1.youtube' AS name, -- package name
            1 as app_id
        UNION ALL
        SELECT
            'APP 2' as app_marker,
            'test2.youtube' AS name,
            2 as app_id
    ),
    TargetAppUPIDs AS (
        SELECT DISTINCT
            tpbn.app_marker,
            tpbn.app_id,
            p.upid,
            p.pid
        FROM
            process p
            JOIN TargetProcessesByName tpbn ON p.name = tpbn.name
    ),
    raw_app_counters AS (
        -- MEMORY COUNTERS
        SELECT
            c.ts,
            target.app_marker AS app_label,
            pct.name AS counter_name,
            CAST(c.value AS REAL) AS value
        FROM
            counter AS c
            JOIN process_counter_track AS pct ON c.track_id = pct.id
            JOIN TargetAppUPIDs target ON pct.upid = target.upid
        UNION ALL
        -- CPU - SLICE DURATIONS
        SELECT
            s.ts,
            target.app_marker,
            'CPU - All Threads Slice Duration (ns)',
            CAST(s.dur AS REAL)
        FROM
            sched_slice s
            JOIN thread t ON s.utid = t.utid
            JOIN TargetAppUPIDs target ON t.upid = target.upid
        UNION ALL
        SELECT
            s.ts,
            target.app_marker,
            'CPU - Main Thread Slice Duration (ns)',
            CAST(s.dur AS REAL)
        FROM
            sched_slice s
            JOIN thread t ON s.utid = t.utid
            JOIN TargetAppUPIDs target ON t.upid = target.upid
            AND t.tid = target.pid
        UNION ALL
        -- CPU - PROCESS INSTANCE TOTAL TIME
        SELECT
            0 AS ts,
            target.app_marker,
            'CPU - Process Instance Total Time (ms)',
            CAST(SUM(s.dur) / 1000000.0 AS REAL)
        FROM
            sched_slice s
            JOIN thread t ON s.utid = t.utid
            JOIN TargetAppUPIDs target ON t.upid = target.upid
        GROUP BY
            target.app_marker,
            target.upid
        UNION ALL
        -- CPU - TOTAL TIME BY THREAD CATEGORY (ms)
        SELECT
            0 AS ts,
            target.app_marker,
            'CPU - MainThread Total Time (ms)',
            CAST(SUM(s.dur) / 1000000.0 AS REAL)
        FROM
            sched_slice s
            JOIN thread t ON s.utid = t.utid
            JOIN TargetAppUPIDs target ON t.upid = target.upid
            AND t.tid = target.pid
        GROUP BY
            target.app_marker,
            target.upid
        UNION ALL
        SELECT
            0 AS ts,
            target.app_marker,
            'CPU - RenderThreads Total Time (ms)',
            CAST(SUM(s.dur) / 1000000.0 AS REAL)
        FROM
            sched_slice s
            JOIN thread t ON s.utid = t.utid
            JOIN TargetAppUPIDs target ON t.upid = target.upid
            AND t.name LIKE 'RenderThread%'
        GROUP BY
            target.app_marker,
            target.upid
        UNION ALL
        SELECT
            0 AS ts,
            target.app_marker,
            'CPU - BinderThreads Total Time (ms)',
            CAST(SUM(s.dur) / 1000000.0 AS REAL)
        FROM
            sched_slice s
            JOIN thread t ON s.utid = t.utid
            JOIN TargetAppUPIDs target ON t.upid = target.upid
            AND t.name LIKE 'binder:%'
        GROUP BY
            target.app_marker,
            target.upid
        UNION ALL
        SELECT
            0 AS ts,
            target.app_marker,
            'CPU - GC (HeapTaskDaemon) Total Time (ms)',
            CAST(SUM(s.dur) / 1000000.0 AS REAL)
        FROM
            sched_slice s
            JOIN thread t ON s.utid = t.utid
            JOIN TargetAppUPIDs target ON t.upid = target.upid
            AND t.name = 'HeapTaskDaemon'
        GROUP BY
            target.app_marker,
            target.upid
        UNION ALL
        SELECT
            0 AS ts,
            target.app_marker,
            'CPU - ExoPlayer Threads Total Time (ms)',
            CAST(SUM(s.dur) / 1000000.0 AS REAL)
        FROM
            sched_slice s
            JOIN thread t ON s.utid = t.utid
            JOIN TargetAppUPIDs target ON t.upid = target.upid
            AND t.name LIKE 'ExoPlayer:%'
        GROUP BY
            target.app_marker,
            target.upid
        UNION ALL
        SELECT
            0 AS ts,
            target.app_marker,
            'CPU - JIT Pool Total Time (ms)',
            CAST(SUM(s.dur) / 1000000.0 AS REAL)
        FROM
            sched_slice s
            JOIN thread t ON s.utid = t.utid
            JOIN TargetAppUPIDs target ON t.upid = target.upid
            AND (
                t.name LIKE 'Jit thread pool%'
                OR t.name LIKE 'JIT thread pool%'
            )
        GROUP BY
            target.app_marker,
            target.upid
        UNION ALL
        SELECT
            0 AS ts,
            target.app_marker,
            'CPU - YTCritical Threads Total Time (ms)',
            CAST(SUM(s.dur) / 1000000.0 AS REAL)
        FROM
            sched_slice s
            JOIN thread t ON s.utid = t.utid
            JOIN TargetAppUPIDs target ON t.upid = target.upid
            AND t.name LIKE 'yt-critica%l Thr%'
        GROUP BY
            target.app_marker,
            target.upid
        UNION ALL
        SELECT
            0 AS ts,
            target.app_marker,
            'CPU - Runtime Workers Total Time (ms)',
            CAST(SUM(s.dur) / 1000000.0 AS REAL)
        FROM
            sched_slice s
            JOIN thread t ON s.utid = t.utid
            JOIN TargetAppUPIDs target ON t.upid = target.upid
            AND t.name LIKE 'Runtime worker%'
        GROUP BY
            target.app_marker,
            target.upid
        UNION ALL
        SELECT
            0 AS ts,
            target.app_marker,
            'CPU - HWUI Tasks Total Time (ms)',
            CAST(SUM(s.dur) / 1000000.0 AS REAL)
        FROM
            sched_slice s
            JOIN thread t ON s.utid = t.utid
            JOIN TargetAppUPIDs target ON t.upid = target.upid
            AND t.name LIKE 'hwuiTask%'
        GROUP BY
            target.app_marker,
            target.upid
        UNION ALL
        SELECT
            0 AS ts,
            target.app_marker,
            'CPU - SharedPreferences Total Time (ms)',
            CAST(SUM(s.dur) / 1000000.0 AS REAL)
        FROM
            sched_slice s
            JOIN thread t ON s.utid = t.utid
            JOIN TargetAppUPIDs target ON t.upid = target.upid
            AND t.name LIKE 'SharedPreferenc%'
        GROUP BY
            target.app_marker,
            target.upid
        UNION ALL
        SELECT
            0 AS ts,
            target.app_marker,
            'CPU - Networking-Ads-SDK Total Time (ms)',
            CAST(SUM(s.dur) / 1000000.0 AS REAL)
        FROM
            sched_slice s
            JOIN thread t ON s.utid = t.utid
            JOIN TargetAppUPIDs target ON t.upid = target.upid
            AND (
                t.name LIKE 'Cronet%'
                OR t.name LIKE 'ChromiumNet%'
                OR t.name LIKE 'OkHttp%'
                OR t.name LIKE 'gads-%'
                OR t.name LIKE 'GassClient%'
                OR t.name LIKE 'firebase-%'
                OR t.name LIKE 'Firebase%'
                OR t.name LIKE 'ScionFrontendAp%'
            )
        GROUP BY
            target.app_marker,
            target.upid
        UNION ALL
        SELECT
            0 AS ts,
            target.app_marker,
            'CPU - Glide Threads Total Time (ms)',
            CAST(SUM(s.dur) / 1000000.0 AS REAL)
        FROM
            sched_slice s
            JOIN thread t ON s.utid = t.utid
            JOIN TargetAppUPIDs target ON t.upid = target.upid
            AND (
                t.name LIKE 'glide-%'
                OR t.name LIKE 'Glide%'
            )
        GROUP BY
            target.app_marker,
            target.upid
        UNION ALL
        SELECT
            0 AS ts,
            target.app_marker,
            'CPU - GPU Driver (Mali-Gralloc) Total Time (ms)',
            CAST(SUM(s.dur) / 1000000.0 AS REAL)
        FROM
            sched_slice s
            JOIN thread t ON s.utid = t.utid
            JOIN TargetAppUPIDs target ON t.upid = target.upid
            AND (
                t.name LIKE 'mali-%'
                OR t.name LIKE 'GrallocUploadTh%'
            )
        GROUP BY
            target.app_marker,
            target.upid
        UNION ALL
        SELECT
            0 AS ts,
            target.app_marker,
            'CPU - Java Finalization-Refs Total Time (ms)',
            CAST(SUM(s.dur) / 1000000.0 AS REAL)
        FROM
            sched_slice s
            JOIN thread t ON s.utid = t.utid
            JOIN TargetAppUPIDs target ON t.upid = target.upid
            AND (
                t.name LIKE 'ReferenceQueueD%'
                OR t.name LIKE 'FinalizerDaemon%'
                OR t.name LIKE 'FinalizerWatchd%'
            )
        GROUP BY
            target.app_marker,
            target.upid
        UNION ALL
        SELECT
            0 AS ts,
            target.app_marker,
            'CPU - Primes SDK Total Time (ms)',
            CAST(SUM(s.dur) / 1000000.0 AS REAL)
        FROM
            sched_slice s
            JOIN thread t ON s.utid = t.utid
            JOIN TargetAppUPIDs target ON t.upid = target.upid
            AND t.name LIKE 'Primes-%'
        GROUP BY
            target.app_marker,
            target.upid
        UNION ALL
        SELECT
            0 AS ts,
            target.app_marker,
            'CPU - V8 DefaultWorkers Total Time (ms)',
            CAST(SUM(s.dur) / 1000000.0 AS REAL)
        FROM
            sched_slice s
            JOIN thread t ON s.utid = t.utid
            JOIN TargetAppUPIDs target ON t.upid = target.upid
            AND t.name LIKE 'V8 DefaultWorke%'
        GROUP BY
            target.app_marker,
            target.upid
        UNION ALL
        SELECT
            0 AS ts,
            target.app_marker,
            'CPU - Generic BG Threads Total Time (ms)',
            CAST(SUM(s.dur) / 1000000.0 AS REAL)
        FROM
            sched_slice s
            JOIN thread t ON s.utid = t.utid
            JOIN TargetAppUPIDs target ON t.upid = target.upid
            AND t.name LIKE 'BG Thre%ad #%'
        GROUP BY
            target.app_marker,
            target.upid
        UNION ALL
        SELECT
            0 AS ts,
            target.app_marker,
            'CPU - Generic Blocking Threads Total Time (ms)',
            CAST(SUM(s.dur) / 1000000.0 AS REAL)
        FROM
            sched_slice s
            JOIN thread t ON s.utid = t.utid
            JOIN TargetAppUPIDs target ON t.upid = target.upid
            AND t.name LIKE 'Blocking Thread%'
        GROUP BY
            target.app_marker,
            target.upid
        UNION ALL
        SELECT
            0 AS ts,
            target.app_marker,
            'CPU - Perfetto Hprof Total Time (ms)',
            CAST(SUM(s.dur) / 1000000.0 AS REAL)
        FROM
            sched_slice s
            JOIN thread t ON s.utid = t.utid
            JOIN TargetAppUPIDs target ON t.upid = target.upid
            AND t.name LIKE 'perfetto_hprof%'
        GROUP BY
            target.app_marker,
            target.upid
        UNION ALL
        -- CPU - SCHEDULING LATENCY (RUNNABLE TIME) BY THREAD CATEGORY (ms)
        SELECT
            0 AS ts,
            target.app_marker,
            'CPU - MainThread Sched Latency (ms)',
            CAST(SUM(ts.dur) / 1000000.0 AS REAL)
        FROM
            thread_state ts
            JOIN thread t ON ts.utid = t.utid
            JOIN TargetAppUPIDs target ON t.upid = target.upid
            AND t.tid = target.pid
        WHERE
            ts.state = 'R'
        GROUP BY
            target.app_marker,
            target.upid
        UNION ALL
        SELECT
            0 AS ts,
            target.app_marker,
            'CPU - RenderThreads Sched Latency (ms)',
            CAST(SUM(ts.dur) / 1000000.0 AS REAL)
        FROM
            thread_state ts
            JOIN thread t ON ts.utid = t.utid
            JOIN TargetAppUPIDs target ON t.upid = target.upid
            AND t.name LIKE 'RenderThread%'
        WHERE
            ts.state = 'R'
        GROUP BY
            target.app_marker,
            target.upid
    ),
    ranked_counters AS (
        SELECT
            app_label,
            counter_name,
            value,
            ROW_NUMBER() OVER (
                PARTITION BY
                    app_label,
                    counter_name
                ORDER BY
                    value ASC
            ) as rn_asc,
            COUNT(*) OVER (
                PARTITION BY
                    app_label,
                    counter_name
            ) as total_count
        FROM
            raw_app_counters
    ),
    median_values AS (
        SELECT
            app_label,
            counter_name,
            AVG(value) AS median_value
        FROM
            ranked_counters
        WHERE
            rn_asc IN ((total_count + 1) / 2, (total_count + 2) / 2)
        GROUP BY
            app_label,
            counter_name
    ),
    value_frequencies AS (
        SELECT
            app_label,
            counter_name,
            value,
            COUNT(*) as value_occurrence_count
        FROM
            raw_app_counters
        GROUP BY
            app_label,
            counter_name,
            value
    ),
    ranked_frequencies AS (
        SELECT
            app_label,
            counter_name,
            value,
            value_occurrence_count,
            ROW_NUMBER() OVER (
                PARTITION BY
                    app_label,
                    counter_name
                ORDER BY
                    value_occurrence_count DESC,
                    value ASC
            ) as rn_freq
        FROM
            value_frequencies
    ),
    mode_values AS (
        SELECT
            app_label,
            counter_name,
            value AS mode_value
        FROM
            ranked_frequencies
        WHERE
            rn_freq = 1
    ),
    aggregated_stats_no_median_mode AS (
        SELECT
            app_label,
            counter_name,
            MIN(value) AS min_value,
            AVG(value) AS avg_value,
            MAX(value) AS max_value,
            COUNT(value) AS count_values
        FROM
            raw_app_counters
        GROUP BY
            app_label,
            counter_name
    ),
    combined_stats AS (
        SELECT
            s.app_label,
            s.counter_name,
            s.min_value,
            s.avg_value,
            m.median_value,
            mo.mode_value,
            s.max_value,
            s.count_values
        FROM
            aggregated_stats_no_median_mode s
            LEFT JOIN median_values m ON s.app_label = m.app_label
            AND s.counter_name = m.counter_name
            LEFT JOIN mode_values mo ON s.app_label = mo.app_label
            AND s.counter_name = mo.counter_name
    ),
    app1_metrics AS (
        SELECT
            *
        FROM
            combined_stats
        WHERE
            app_label = 'APP 1'
    ),
    app2_metrics AS (
        SELECT
            *
        FROM
            combined_stats
        WHERE
            app_label = 'APP 2'
    )
    -- Final pivoted table for comparison
SELECT
    COALESCE(a1.counter_name, a2.counter_name) AS counter_name,
    a1.min_value AS min_app1,
    a2.min_value AS min_app2,
    a1.avg_value AS avg_app1,
    a2.avg_value AS avg_app2,
    a1.median_value AS median_app1,
    a2.median_value AS median_app2,
    a1.mode_value AS mode_app1,
    a2.mode_value AS mode_app2,
    a1.max_value AS max_app1,
    a2.max_value AS max_app2,
    a1.count_values AS count_app1,
    a2.count_values AS count_app2,
    CASE
        WHEN a1.avg_value IS NULL
        AND a2.avg_value IS NOT NULL THEN 'APP 2 Only'
        WHEN a2.avg_value IS NULL
        AND a1.avg_value IS NOT NULL THEN 'APP 1 Only'
        WHEN COALESCE(a1.counter_name, a2.counter_name) LIKE 'CPU - %Slice Duration (ns)' THEN CASE
            WHEN a1.avg_value < a2.avg_value THEN 'APP 1 Lower Avg Slice Duration'
            WHEN a1.avg_value > a2.avg_value THEN 'APP 2 Lower Avg Slice Duration'
            ELSE 'Similar Avg Slice Duration'
        END
        WHEN COALESCE(a1.counter_name, a2.counter_name) LIKE 'CPU - %Total Time (ms)'
        OR COALESCE(a1.counter_name, a2.counter_name) LIKE 'CPU - %Sched Latency (ms)'
        OR COALESCE(a1.counter_name, a2.counter_name) LIKE 'mem.%'
        OR COALESCE(a1.counter_name, a2.counter_name) = 'GPU Memory' THEN CASE
            WHEN a1.avg_value < a2.avg_value THEN 'APP 1 Better (Lower Avg)'
            WHEN a1.avg_value > a2.avg_value THEN 'APP 2 Better (Lower Avg)'
            ELSE 'Similar (Avg)'
        END
        WHEN a1.avg_value < a2.avg_value THEN 'APP 1 Better (Lower Avg)'
        WHEN a1.avg_value > a2.avg_value THEN 'APP 2 Better (Lower Avg)'
        WHEN a1.avg_value = a2.avg_value THEN 'Similar (Avg)'
        ELSE 'N/A or No Data'
    END AS comparison_avg
FROM
    app1_metrics a1
    FULL OUTER JOIN app2_metrics a2 ON a1.counter_name = a2.counter_name
ORDER BY
    CASE
        WHEN COALESCE(a1.counter_name, a2.counter_name) LIKE 'CPU - %' THEN 1
        WHEN COALESCE(a1.counter_name, a2.counter_name) LIKE 'GPU %' THEN 2
        WHEN COALESCE(a1.counter_name, a2.counter_name) LIKE 'mem.%' THEN 3
        ELSE 4
    END,
    counter_name;
