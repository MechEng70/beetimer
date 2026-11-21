# Work breakdown (alpha)

Estimates are agent working time, not calendar days. The total target is about one working session plus your review stops.

```
Bee Timer alpha
1. Foundation
   1.1 T1 Scaffold build ............................ 0.5 h
   1.2 T1b Alarm spike APK .......................... 0.5 h   STOP: test locked on phone
2. Core (pure Kotlin, :scheduler)
   2.1 T2 Model, presets, validation ................ 0.5 h
   2.2 T3 Scheduler reducer, restore, tests ......... 1.5 h   STOP: review tests
3. Android platform (:app)
   3.1 T4 Room persistence .......................... 0.5 h
   3.2 T5 Engine, alarms, receivers ................. 1.0 h
   3.3 T6 Notifications, alarm screen ............... 1.0 h
   3.4 T9 First-run permissions ..................... 0.5 h
4. UI
   4.1 T7 Main UI (status bar, All, Hive tabs) ...... 1.5 h
   4.2 T8 Settings, durations, hives .............. 1.0 h
5. Delivery
   5.1 T10 APK, README, manual test script .......... 0.5 h   STOP: install on phone
                                          Total ~ 8.5 h
```

Dependencies: T1 -> T1b -> T2 -> T3 -> T4 -> T5 -> T6 -> T7 -> T8 -> T9 -> T10. T9 can run alongside T7/T8.

Spec checkpoints map to: step 1 plan = this set of docs (STOP now); step 2 = T3 STOP; steps 3 and 4 = T4 to T9; step 5 = T10 STOP.

Biggest risks:
- AGP 9.5 built-in Kotlin alongside a kotlin-jvm `:scheduler` module, if the plugin setup has gotchas. Mitigation: settle this in T1 before writing any logic.
- Alarm reliability with no foreground service. Mitigation: `setAlarmClock`, verified with the reboot and locked-screen steps in the manual script.
- OEM battery killers on your phone. Mitigation: the one-time battery exemption ask.
