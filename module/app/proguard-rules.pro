# LSPosed 入口必须保留类名（框架通过接口实现发现模块）
-keep class io.github.ylw6669.chrostar.HookEntry { *; }

# Activity 由 manifest 引用，R8 自动保留；这里显式声明以防混淆
-keep class io.github.ylw6669.chrostar.MainActivity { *; }

# Xposed API 是 compileOnly，运行时由框架提供，不需要打进去
-dontwarn de.robv.android.xposed.**
-dontwarn org.lsposed.lspd.**

# 反射字符串引用的类都在目标应用(Chrome)中，不在本 APK 内，无需 keep
# miuix/compose 自带 consumer rules，AGP 会自动应用

# v2.1.0: Chrome152 符号表常量与 matches 反射被 R8 误裁
-keep class io.github.ylw6669.chrostar.Chrome152 { *; }

# v2.2.0: 新功能类(反射字段名/跨类调用)
-keep class io.github.ylw6669.chrostar.OverwriteAndAutoOpen { *; }
-keep class io.github.ylw6669.chrostar.LocationAndDiagnostics { *; }
