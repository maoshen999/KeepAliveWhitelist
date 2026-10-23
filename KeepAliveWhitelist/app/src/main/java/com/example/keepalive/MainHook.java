package com.example.keepalive;

import java.lang.reflect.Method;
import java.util.Set;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public class MainHook extends XposedModule {
    private Set<String> whitelist;

    @Override
    public void onSystemServerStarting(XposedModuleInterface.SystemServerStartingParam param) {
        whitelist = WhitelistConfig.readWhitelist();
        if (whitelist.isEmpty()) {
            // 👈 修正 1：使用 log(int, String, String)
            log(4, "KeepAlive", "白名单为空，跳过 Hook"); 
            return;
        }

        ClassLoader cl = param.getClassLoader();
        Class<?> amsClass = null;
        try {
            amsClass = Class.forName("com.android.server.am.ActivityManagerService", false, cl);
        } catch (Throwable t) {
            log(6, "KeepAlive", "未找到 ActivityManagerService：" + t); // 6 = ERROR
            return;
        }

        // 👈 修正 2：手动遍历方法，代替不存在的 hookAllMethods
        for (Method method : amsClass.getDeclaredMethods()) {
            String methodName = method.getName();
            if (methodName.equals("destroyActivityLocked")) {
                hook(method).intercept(new DestroyHook());
            } else if (methodName.equals("cleanUpActivityLocked")) {
                hook(method).intercept(new CleanUpHook());
            }
        }

        log(4, "KeepAlive", "已安装 Hook，白名单数量=" + whitelist.size());
    }

    // ... 下面的 DestroyHook, CleanUpHook, isSystemApp 等方法保持完全不变 ...
    private class DestroyHook implements XposedInterface.Hooker {
        @Override
        public Object intercept(XposedInterface.Chain chain) throws Throwable {
            Object record = chain.getArg(0);
            String pkg = extractPackage(record);
            
            if (pkg != null && whitelist.contains(pkg)) {
                if (isSystemApp(record)) {
                    log(4, "KeepAlive", "包名在白名单内但属于系统应用，放行销毁：" + pkg);
                    return chain.proceed();
                }
                log(4, "KeepAlive", "阻止销毁白名单第三方 Activity：" + pkg);
                return null;
            }
            return chain.proceed();
        }
    }

    private class CleanUpHook implements XposedInterface.Hooker {
        @Override
        public Object intercept(XposedInterface.Chain chain) throws Throwable {
            return chain.proceed();
        }
    }

    // (isSystemApp, extractPackage, tryStringField, tryObjectField, tryIntField 这些方法原样保留，不要动)
    private boolean isSystemApp(Object record) {
        if (record == null) return false;
        Object info = tryObjectField(record, "info");
        if (info == null) info = tryObjectField(record, "mInfo");
        if (info == null) return false;
        Object appInfo = tryObjectField(info, "applicationInfo");
        if (appInfo == null) return false;
        int flags = tryIntField(appInfo, "flags");
        return (flags & 0x00000001) != 0 || (flags & 0x00000080) != 0;
    }

    private static String extractPackage(Object record) {
        if (record == null) return null;
        String pkg = tryStringField(record, "packageName");
        if (pkg == null) pkg = tryStringField(record, "mPackageName");
        if (pkg != null) return pkg;

        Object info = tryObjectField(record, "info");
        if (info == null) info = tryObjectField(record, "mInfo");
        if (info != null) {
            pkg = tryStringField(info, "packageName");
            if (pkg != null) return pkg;
        }
        return null;
    }

    private static String tryStringField(Object obj, String name) {
        try {
            java.lang.reflect.Field f = obj.getClass().getDeclaredField(name);
            f.setAccessible(true);
            Object v = f.get(obj);
            return v instanceof String ? (String) v : null;
        } catch (Throwable ignored) { return null; }
    }

    private static Object tryObjectField(Object obj, String name) {
        try {
            java.lang.reflect.Field f = obj.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(obj);
        } catch (Throwable ignored) { return null; }
    }

    private static int tryIntField(Object obj, String name) {
        try {
            java.lang.reflect.Field f = obj.getClass().getDeclaredField(name);
            f.setAccessible(true);
            Object v = f.get(obj);
            return v instanceof Integer ? (Integer) v : 0;
        } catch (Throwable ignored) { return 0; }
    }
}
