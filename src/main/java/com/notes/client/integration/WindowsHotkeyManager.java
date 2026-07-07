package com.notes.client.integration;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

import java.awt.event.KeyEvent;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public final class WindowsHotkeyManager implements AutoCloseable {
    private static final int HOTKEY_ID = 1;
    private static final int MOD_ALT = 0x0001;
    private static final int MOD_CONTROL = 0x0002;

    private final Runnable onHotkey;
    private final AtomicBoolean running = new AtomicBoolean();
    private Thread loopThread;
    private int threadId;

    private WindowsHotkeyManager(Runnable onHotkey) {
        this.onHotkey = onHotkey;
    }

    public static WindowsHotkeyManager tryCreate(Runnable onHotkey) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("win") ? new WindowsHotkeyManager(onHotkey) : null;
    }

    public void start() {
        if (running.getAndSet(true)) {
            return;
        }
        loopThread = new Thread(this::runLoop, "notes-global-hotkey");
        loopThread.setDaemon(true);
        loopThread.start();
    }

    private void runLoop() {
        threadId = Kernel32.INSTANCE.GetCurrentThreadId();
        boolean registered = User32.INSTANCE.RegisterHotKey(null, HOTKEY_ID, MOD_CONTROL | MOD_ALT, KeyEvent.VK_SPACE);
        if (!registered) {
            running.set(false);
            return;
        }
        WinUser.MSG message = new WinUser.MSG();
        while (running.get() && User32.INSTANCE.GetMessage(message, null, 0, 0) > 0) {
            if (message.message == WinUser.WM_HOTKEY && message.wParam.intValue() == HOTKEY_ID) {
                onHotkey.run();
            }
        }
        User32.INSTANCE.UnregisterHotKey(null, HOTKEY_ID);
    }

    @Override
    public void close() {
        if (!running.getAndSet(false)) {
            return;
        }
        User32.INSTANCE.PostThreadMessage(threadId, WinUser.WM_QUIT, new WinDef.WPARAM(0), new WinDef.LPARAM(0));
    }
}
