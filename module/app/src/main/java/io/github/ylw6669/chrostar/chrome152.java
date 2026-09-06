// ChroStar v2.1.0 — Chrome 152 符号表(逆向实证 152.0.7977.82, 全部经 152 反编译交叉验证)
package io.github.ylw6669.chrostar;

public final class Chrome152 {
    // 稳定公开类与 145 相同(见 HookEntry), 差异仅在混淆短名与字段名。

    // R8 短名 — verified against Chrome 152.0.7977.82
    public static final String COMMAND_FLAGS = "nw0";      // 命令行消费者(实际判定走公开 CommandLine.d.c)
    public static final String HOMEPAGE = "w5c";           // 145: jza
    public static final String TAB_CREATOR = "iq4";        // 145: l04 (NTP 创建: iq4.m(LoadUrlParams,...))
    public static final String TAB_SELECTOR = "k3r";       // Tab 选择器 k(boolean)->TabModel
    public static final String DOWNLOAD_MESSAGE = "ia8";   // 145: je7 (d(OfflineItem,boolean,boolean,boolean) 同签名)
    public static final String MESSAGE_DISPATCHER = "gig"; // 145: nze (b(PM,WC,int,boolean)+c(PM,boolean) 同签名)
    public static final String CLOSE_ALL_DIALOG = "q35";   // 关全部标签对话框处理器(替代 145 id4.run 菜单路径)
    public static final String DOWNLOAD_SERVICE = "ud4";   // 下载消息服务宿主(ia8 的宿主)

    // OfflineItem 字段 145 -> 152
    public static final String OI_STATE = "q0";            // 145: m0 (完成=2)
    public static final String OI_CONTENT_ID = "S";

    // DownloadInfo 字段(公开类公开字段, Java 直读)
    public static final String DI_MIME = "c";
    public static final String DI_NAME = "e";
    public static final String DI_PATH = "g";

    // J.N 通用通道 selector: 145 VIOOOOOOO 在 152 无同形态命中 —
    // 历史清理在 152 走语义降级(见 HomeCleaner), 不硬编码不可验证的 selector。

    public static final String NTP = "chrome-native://newtab/";

    private Chrome152() {}

    /** 运行时校验: 关键短名类可加载且方法签名匹配才启用 152 硬编码路径 */
    public static boolean matches(ClassLoader cl) {
        try {
            Class.forName("org.chromium.chrome.browser.ChromeTabbedActivity", false, cl);
            Class<?> ia8 = Class.forName("ia8", false, cl);
            boolean ia8Ok = false;
            for (java.lang.reflect.Method m : ia8.getDeclaredMethods()) {
                Class<?>[] ps = m.getParameterTypes();
                if (m.getName().equals("d") && ps.length == 4
                        && ps[0].getName().endsWith("OfflineItem")) { ia8Ok = true; break; }
            }
            if (!ia8Ok) return false;
            Class<?> gig = Class.forName("gig", false, cl);
            boolean gigOk = false;
            for (java.lang.reflect.Method m : gig.getDeclaredMethods()) {
                Class<?>[] ps = m.getParameterTypes();
                if (m.getName().equals("b") && ps.length == 4
                        && ps[0].getName().endsWith("PropertyModel")
                        && ps[1].getName().endsWith("WebContents")) { gigOk = true; break; }
            }
            return gigOk;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
