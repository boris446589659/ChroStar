// ChroStar v2.2.0 — 功能 1+2: 同名覆盖保留原文件 + 下载自动打开类型扩展
// 纯 Java IO + 公开类字段, 不依赖混淆名, Chrome 145/152 通用。
package io.github.ylw6669.chrostar;

import android.os.Handler;
import android.os.Looper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class OverwriteAndAutoOpen {

    private static final String BAK_SUFFIX = ".chrostar-bak";
    private static final long RESTORE_DELAY_MS = 60_000L;

    /** 已做的备份: 目标路径 -> 备份路径 */
    private static final Map<String, String> sBackups = new HashMap<>();
    private static final Object sLock = new Object();

    private OverwriteAndAutoOpen() {}

    private static boolean is152() {
        return "chrome152".equals(HookEntry.engineVersion);
    }

    // ------------------------------------------------------------------
    // 功能 1: 同名覆盖保留原文件
    // hook DuplicateDownloadDialogBridge(公开类, 145/152 同名) 的确认回调,
    // 在用户选择"下载"(allow)时, 把旧文件移到 .chrostar-bak, 让 Chrome 拿回原名。
    // ------------------------------------------------------------------
        public static void hookOverwriteDuplicate(XC_LoadPackage.LoadPackageParam lpparam) {
        // v2.2.0: 备份动作由 DownloadSafetyBypass.hookDuplicate 的确认点调用 armBackup()。
        // 这里仅做 hook 安装标记, 保证 LSPosed 日志可见。
        XposedBridge.log(HookEntry.TAG + ": overwrite-duplicate armed (via duplicate dialog confirm)");
    }

    /** 从确认回调参数中提取目标文件路径, 做事务备份 */
    public static void armBackup(Object[] args) {
        try {
            for (Object a : args) {
                if (!(a instanceof String)) continue;
                String s = (String) a;
                if (!s.contains("/")) continue;
                File target = new File(s);
                File parent = target.getParentFile();
                if (parent == null) continue;
                // 目录白名单: 仅公共 Download / Chrome 下载目录, 避免误操作系统目录
                String pl = parent.getAbsolutePath().toLowerCase();
                if (!(pl.contains("/download") || pl.contains("download"))) continue;
                // 只处理"已存在同名旧文件"的场景(= 用户看到了重复下载确认)
                if (!target.exists()) continue;
                File bak = new File(parent, target.getName() + BAK_SUFFIX);
                if (bak.exists()) continue; // 已在事务中
                boolean ok = target.renameTo(bak);
                if (ok) {
                    synchronized (sLock) {
                        sBackups.put(target.getAbsolutePath(), bak.getAbsolutePath());
                    }
                    XposedBridge.log(HookEntry.TAG + ": same-name backup -> " + bak.getName());
                    scheduleRestore(target.getAbsolutePath(), bak.getAbsolutePath());
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(HookEntry.TAG + ": backup error -> " + t);
        }
    }

    /** 60 秒后: 新文件已落地 -> 删备份; 未落地 -> 还原备份 */
    private static void scheduleRestore(final String targetPath, final String backupPath) {
        // 轮询式: 大文件下载可能超过 60s, 每 10s 检查一次, 最多 12 次(2 分钟)
        final Handler h = new Handler(Looper.getMainLooper());
        final int[] attempts = {0};
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    attempts[0]++;
                    File target = new File(targetPath);
                    File backup = new File(backupPath);
                    if (target.exists() && target.length() > 0) {
                        if (backup.delete()) {
                            XposedBridge.log(HookEntry.TAG
                                    + ": same-name overwrite committed, backup removed");
                        }
                        synchronized (sLock) { sBackups.remove(targetPath); }
                        return;
                    }
                    if (!backup.exists()) {
                        synchronized (sLock) { sBackups.remove(targetPath); }
                        return; // 备份已不在, 放弃
                    }
                    if (attempts[0] >= 12) {
                        if (backup.renameTo(target)) {
                            XposedBridge.log(HookEntry.TAG
                                    + ": same-name backup restored (download timeout)");
                        }
                        synchronized (sLock) { sBackups.remove(targetPath); }
                        return;
                    }
                    h.postDelayed(this, 10_000L);
                } catch (Throwable t) {
                    XposedBridge.log(HookEntry.TAG + ": restore error -> " + t);
                }
            }
        }, 10_000L);
    }

    // ------------------------------------------------------------------
    // 功能 2: 下载自动打开类型扩展
    // AutoInstallApk 的 isApk 判定点替换为 MIME 白名单(键: auto_open_*)
    // ------------------------------------------------------------------
    public static boolean shouldAutoOpen(String mime, String name) {
        // APK 保持原键(老用户无感)
        if (HookEntry.isApk(mime, name)) {
            return HookEntry.readPrefBoolean(HookEntry.KEY_AUTO_INSTALL_APK, true);
        }
        if (!HookEntry.readPrefBoolean(HookEntry.KEY_AUTO_OPEN_EXT, false)) {
            return false;
        }
        String m = mime == null ? "" : mime.toLowerCase();
        String n = name == null ? "" : name.toLowerCase();

        if (m.startsWith("image/") || isExt(n, ".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp")) {
            return HookEntry.readPrefBoolean(HookEntry.KEY_AUTO_OPEN_IMAGE, false);
        }
        if (m.startsWith("video/") || isExt(n, ".mp4", ".mkv", ".webm", ".avi", ".mov")) {
            return HookEntry.readPrefBoolean(HookEntry.KEY_AUTO_OPEN_VIDEO, false);
        }
        if (m.startsWith("audio/") || isExt(n, ".mp3", ".flac", ".ogg", ".wav", ".m4a")) {
            return HookEntry.readPrefBoolean(HookEntry.KEY_AUTO_OPEN_AUDIO, false);
        }
        if (m.equals("application/pdf") || n.endsWith(".pdf")) {
            return HookEntry.readPrefBoolean(HookEntry.KEY_AUTO_OPEN_PDF, false);
        }
        if (isExt(n, ".zip", ".7z", ".rar", ".tar", ".gz")) {
            return HookEntry.readPrefBoolean(HookEntry.KEY_AUTO_OPEN_ARCHIVE, false);
        }
        if (isExt(n, ".doc", ".docx", ".odt", ".rtf")) {
            return HookEntry.readPrefBoolean(HookEntry.KEY_AUTO_OPEN_DOCUMENT, false);
        }
        if (isExt(n, ".xls", ".xlsx", ".csv", ".ods")) {
            return HookEntry.readPrefBoolean(HookEntry.KEY_AUTO_OPEN_SPREADSHEET, false);
        }
        if (isExt(n, ".ppt", ".pptx", ".odp")) {
            return HookEntry.readPrefBoolean(HookEntry.KEY_AUTO_OPEN_PRESENTATION, false);
        }
        if (m.startsWith("text/") || isExt(n, ".txt", ".md", ".log")) {
            return HookEntry.readPrefBoolean(HookEntry.KEY_AUTO_OPEN_TEXT, false);
        }
        if (isExt(n, ".epub", ".mobi")) {
            return HookEntry.readPrefBoolean(HookEntry.KEY_AUTO_OPEN_EBOOK, false);
        }
        return false;
    }

    private static boolean isExt(String name, String... exts) {
        for (String e : exts) {
            if (name.endsWith(e)) return true;
        }
        return false;
    }
}
