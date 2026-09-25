# Phase 12 Performance Evidence

The deterministic JVM harness measured five conversation copies for each
dataset and one 10,000-chunk append run:

| Dataset | Measured result |
|---|---:|
| 0 messages | 0.115 ms |
| 10 messages | 0.211 ms |
| 100 messages | 2.500 ms |
| 1,000 messages | 5.557 ms |
| 10,000 stream chunks | 768.798 ms |

The harness also copied 100 attachment metadata records and verified complete
retention. These are local JVM measurements, not Android frame-time, memory,
radio, or battery benchmarks. Android performance at device scale is therefore
Not tested.
