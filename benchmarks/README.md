# Simple Benchmarking

> [!WARNING]
> This might not be the best way to benchmark. If you know other ways to benchmark, please share with us.

> [!TIP]
> It is recommended to write unit tests to automate testing and stress system resources to their limits, ensuring accurate and reliable benchmarking, rather than relying on manual testing.

## Function Execution Time Test

Use [func_speed.py](func_speed.py) to test function speed. Ensure the logger in the app prints specific messages in the format `<function_name> took <time> ms`.

Example code:

```java
public void functionName() {
    long startTime = System.nanoTime();

    // Function logic goes here

    long endTime = System.nanoTime();
    Logger.printDebug(() -> "functionName took " + ((endTime - startTime) / 1_000_000.0) + " ms");
}
```

Then, capture logs and run:

```bash
python func_speed.py -f <path_to_log_file>
```

<details><summary>Example table</summary>

| Function Name        | Count | Min (ms) | Avg (ms) | Median (ms) | Mode (ms) | Max (ms) |
|----------------------|-------|----------|----------|-------------|-----------|----------|
| displaySearchHistory | 8     | 0.3486   | 0.5311   | 0.5608      | 0.7170    | 0.7170   |
| filterPreferences    | 16    | 2.8073   | 7.7168   | 6.1802      | 9.9255    | 13.8172  |

- **Count**: The number of times the function was called, as recorded in the logs.
- **Min (ms)**: The minimum execution time of all function calls.
- **Avg (ms)**: The average execution time across all function calls.
- **Median (ms)**: The median execution time, representing the middle value when all call times are sorted.
- **Mode (ms)**: The most frequently occurring execution time among all function calls. If multiple values occur with the same frequency, the lowest value is shown.
- **Max (ms)**: The maximum execution time of all function calls.

</details>

## CPU and Memory Performance Test

Use [Perfetto](https://ui.perfetto.dev/) for logging, refer to their documentation. Run two versions of the app you want to compare in a single record, such as the old version and the new version (which is presumably improved, or why else would you write a new version). Then, create a simple table using [compare.sql](compare.sql) (you can run it in Perfetto's SQL query tab).

<details><summary>Example table</summary>

| counter_name                                    | min_app1     | min_app2     | avg_app1           | avg_app2           | median_app1  | median_app2  | mode_app1    | mode_app2    | max_app1     | max_app2     | count_app1 | count_app2 | comparison_avg                 |
|-------------------------------------------------|--------------|--------------|--------------------|--------------------|--------------|--------------|--------------|--------------|--------------|--------------|------------|------------|--------------------------------|
| CPU - All Threads Slice Duration (ns)           | 2230         | 2077         | 721845.5684821475  | 712689.6050472033  | 183308       | 190539       | 7385         | 5231         | 190508231    | 141058538    | 38594      | 43747      | APP 2 Lower Avg Slice Duration |
| CPU - BinderThreads Total Time (ms)             | 902.088249   | 893.0651     | 902.088249         | 893.0651           | 902.088249   | 893.0651     | 902.088249   | 893.0651     | 902.088249   | 893.0651     | 1          | 1          | APP 2 Better (Lower Avg)       |
| CPU - ExoPlayer Threads Total Time (ms)         | 5.950234     | 6.461614     | 5.950234           | 6.461614           | 5.950234     | 6.461614     | 5.950234     | 6.461614     | 5.950234     | 6.461614     | 1          | 1          | APP 1 Better (Lower Avg)       |
| CPU - GC (HeapTaskDaemon) Total Time (ms)       | 812.957305   | 532.843923   | 812.957305         | 532.843923         | 812.957305   | 532.843923   | 812.957305   | 532.843923   | 812.957305   | 532.843923   | 1          | 1          | APP 2 Better (Lower Avg)       |
| CPU - GPU Driver (Mali-Gralloc) Total Time (ms) | 1840.029893  | 2127.51428   | 1840.029893        | 2127.51428         | 1840.029893  | 2127.51428   | 1840.029893  | 2127.51428   | 1840.029893  | 2127.51428   | 1          | 1          | APP 1 Better (Lower Avg)       |
| CPU - Generic BG Threads Total Time (ms)        | 1906.690698  | 2800.062682  | 1906.690698        | 2800.062682        | 1906.690698  | 2800.062682  | 1906.690698  | 2800.062682  | 1906.690698  | 2800.062682  | 1          | 1          | APP 1 Better (Lower Avg)       |
| CPU - Generic Blocking Threads Total Time (ms)  | 25.760304    | 28.378158    | 25.760304          | 28.378158          | 25.760304    | 28.378158    | 25.760304    | 28.378158    | 25.760304    | 28.378158    | 1          | 1          | APP 1 Better (Lower Avg)       |
| CPU - Glide Threads Total Time (ms)             | 112.696778   | 277.342989   | 112.696778         | 277.342989         | 112.696778   | 277.342989   | 112.696778   | 277.342989   | 112.696778   | 277.342989   | 1          | 1          | APP 1 Better (Lower Avg)       |
| CPU - HWUI Tasks Total Time (ms)                | 4.035081     | 3.853769     | 4.035081           | 3.853769           | 4.035081     | 3.853769     | 4.035081     | 3.853769     | 4.035081     | 3.853769     | 1          | 1          | APP 2 Better (Lower Avg)       |
| CPU - JIT Pool Total Time (ms)                  | 58.261457    | 77.077458    | 58.261457          | 77.077458          | 58.261457    | 77.077458    | 58.261457    | 77.077458    | 58.261457    | 77.077458    | 1          | 1          | APP 1 Better (Lower Avg)       |
| CPU - Java Finalization-Refs Total Time (ms)    | 574.12931    | 153.097079   | 574.12931          | 153.097079         | 574.12931    | 153.097079   | 574.12931    | 153.097079   | 574.12931    | 153.097079   | 1          | 1          | APP 2 Better (Lower Avg)       |
| CPU - Main Thread Slice Duration (ns)           | 3154         | 2077         | 2668918.5752053387 | 2449842.378504673  | 241423       | 246461.5     | 88846        | 149846       | 132352923    | 141058538    | 3896       | 4280       | APP 2 Lower Avg Slice Duration |
| CPU - MainThread Sched Latency (ms)             | 440.754902   | 493.976465   | 440.754902         | 493.976465         | 440.754902   | 493.976465   | 440.754902   | 493.976465   | 440.754902   | 493.976465   | 1          | 1          | APP 1 Better (Lower Avg)       |
| CPU - MainThread Total Time (ms)                | 10398.106769 | 10485.32538  | 10398.106769       | 10485.32538        | 10398.106769 | 10485.32538  | 10398.106769 | 10485.32538  | 10398.106769 | 10485.32538  | 1          | 1          | APP 1 Better (Lower Avg)       |
| CPU - Networking-Ads-SDK Total Time (ms)        | 1623.779158  | 1848.070485  | 1623.779158        | 1848.070485        | 1623.779158  | 1848.070485  | 1623.779158  | 1848.070485  | 1623.779158  | 1848.070485  | 1          | 1          | APP 1 Better (Lower Avg)       |
| CPU - Perfetto Hprof Total Time (ms)            | 0.311077     | 0.221538     | 0.311077           | 0.221538           | 0.311077     | 0.221538     | 0.311077     | 0.221538     | 0.311077     | 0.221538     | 1          | 1          | APP 2 Better (Lower Avg)       |
| CPU - Primes SDK Total Time (ms)                | 103.49231    | 94.501078    | 103.49231          | 94.501078          | 103.49231    | 94.501078    | 103.49231    | 94.501078    | 103.49231    | 94.501078    | 1          | 1          | APP 2 Better (Lower Avg)       |
| CPU - Process Instance Total Time (ms)          | 27858.90787  | 31178.032152 | 27858.90787        | 31178.032152       | 27858.90787  | 31178.032152 | 27858.90787  | 31178.032152 | 27858.90787  | 31178.032152 | 1          | 1          | APP 1 Better (Lower Avg)       |
| CPU - RenderThreads Sched Latency (ms)          | 468.449145   | 560.46191    | 468.449145         | 560.46191          | 468.449145   | 560.46191    | 468.449145   | 560.46191    | 468.449145   | 560.46191    | 1          | 1          | APP 1 Better (Lower Avg)       |
| CPU - RenderThreads Total Time (ms)             | 7227.507519  | 8606.267912  | 7227.507519        | 8606.267912        | 7227.507519  | 8606.267912  | 7227.507519  | 8606.267912  | 7227.507519  | 8606.267912  | 1          | 1          | APP 1 Better (Lower Avg)       |
| CPU - Runtime Workers Total Time (ms)           | 1.816308     | 1.832        | 1.816308           | 1.832              | 1.816308     | 1.832        | 1.816308     | 1.832        | 1.816308     | 1.832        | 1          | 1          | APP 1 Better (Lower Avg)       |
| CPU - SharedPreferences Total Time (ms)         | 25.375228    | 34.431539    | 25.375228          | 34.431539          | 25.375228    | 34.431539    | 25.375228    | 34.431539    | 25.375228    | 34.431539    | 1          | 1          | APP 1 Better (Lower Avg)       |
| CPU - V8 DefaultWorkers Total Time (ms)         | 1.09377      | 0.794386     | 1.09377            | 0.794386           | 1.09377      | 0.794386     | 1.09377      | 0.794386     | 1.09377      | 0.794386     | 1          | 1          | APP 2 Better (Lower Avg)       |
| CPU - YTCritical Threads Total Time (ms)        | 267.655392   | 552.090318   | 267.655392         | 552.090318         | 267.655392   | 552.090318   | 267.655392   | 552.090318   | 267.655392   | 552.090318   | 1          | 1          | APP 1 Better (Lower Avg)       |
| GPU Memory                                      | 4096         | 4096         | 61545730.94252873  | 64981145.52758133  | 71088128     | 75960320     | 74432512     | 80904192     | 103911424    | 113229824    | 696        | 707        | APP 1 Better (Lower Avg)       |
| mem.locked                                      | 0            | 0            | 0                  | 0                  | 0            | 0            | 0            | 0            | 0            | 0            | 508        | 484        | Similar (Avg)                  |
| mem.rss                                         | 41910272     | 106659840    | 567400794.7086614  | 445986460.56198347 | 533112832    | 311672832    | 514162688    | 292458496    | 735502336    | 669233152    | 508        | 484        | APP 2 Better (Lower Avg)       |
| mem.rss.anon                                    | 13160448     | 13160448     | 196656959.7564482  | 160162344.77116513 | 195510272    | 174190592    | 162955264    | 54697984     | 370151424    | 289497088    | 2365       | 2386       | APP 2 Better (Lower Avg)       |
| mem.rss.file                                    | 1716224      | 1716224      | 304633568.2644628  | 276861714.2291022  | 349483008    | 258072576    | 349483008    | 236269568    | 377274368    | 387379200    | 968        | 969        | APP 2 Better (Lower Avg)       |
| mem.rss.shmem                                   | 262144       | 905216       | 1697700.179337232  | 1614908.977412731  | 1720320      | 1495040      | 1724416      | 1490944      | 1732608      | 1830912      | 513        | 487        | APP 2 Better (Lower Avg)       |
| mem.rss.watermark                               | 41910272     | 106659840    | 708799294.488189   | 478893555.3057851  | 745328640    | 322772992    | 745328640    | 322772992    | 745328640    | 678703104    | 508        | 484        | APP 2 Better (Lower Avg)       |
| mem.swap                                        | 22913024     | 22401024     | 23371482.324528303 | 24898089.40433925  | 22974464     | 26562560     | 22913024     | 26562560     | 34025472     | 34025472     | 530        | 507        | APP 1 Better (Lower Avg)       |
| mem.virt                                        | 6677377024   | 6986354688   | 15539884902.80315  | 15154180781.487604 | 15652196352  | 15049850880  | 15616389120  | 14979989504  | 16142131200  | 15679868928  | 508        | 484        | APP 2 Better (Lower Avg)       |
| oom_score_adj                                   | -1000        | 0            | 490.25925925925924 | 495.7693836978131  | 700          | 700          | 900          | 905          | 900          | 915          | 513        | 503        | APP 1 Better (Lower Avg)       |

This hypothetical example shows:

- **App 1 Strengths**: More CPU-efficient overall, more CPU-efficient RenderThreads, lower scheduling latency for Main and Render threads, lower GPU memory, lower swap usage, and better OOM score. This suggests potentially better UI performance and stability under pressure.
- **App 2 Strengths**: Significantly more RAM-efficient (lower RSS, anon, file) and much lower GC overhead. This is crucial for low-memory devices and reducing GC-induced pauses.

</details>
