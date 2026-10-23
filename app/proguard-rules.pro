# Room / Compose / WorkManager ship their own consumer rules. Keep the app's
# data classes readable in crash reports.
-keepnames class com.salaryflow.app.data.** { *; }
