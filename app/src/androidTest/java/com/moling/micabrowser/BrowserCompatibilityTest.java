package com.moling.micabrowser;

import static org.junit.Assert.assertEquals;

import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Base64;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.xwalk.core.XWalkNavigationHistory;
import org.xwalk.core.XWalkView;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class BrowserCompatibilityTest {
    @Test
    public void crosswalkRendersJavaScriptAndNavigatesBack() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        BrowserActivity browser = (BrowserActivity) instrumentation.startActivitySync(
                new Intent(instrumentation.getTargetContext(), BrowserActivity.class)
                        .setData(page("document.title = 'Mica JS ' + (6 * 7)"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            XWalkView view = browser.findViewById(R.id.xwalkview);
            awaitTitle(instrumentation, view, "Mica JS 42");

            instrumentation.runOnMainSync(() -> view.loadUrl(
                    page("document.title = 'Mica second page'").toString()));
            awaitTitle(instrumentation, view, "Mica second page");

            instrumentation.runOnMainSync(() -> view.getNavigationHistory()
                    .navigate(XWalkNavigationHistory.Direction.BACKWARD, 1));
            awaitTitle(instrumentation, view, "Mica JS 42");
        } finally {
            instrumentation.runOnMainSync(browser::finish);
        }
    }

    private static Uri page(String script) {
        String html = "<!doctype html><meta name='viewport' content='width=device-width'>"
                + "<title>Loading</title><p>MicaBrowser compatibility test</p>"
                + "<script>" + script + "</script>";
        return Uri.parse("data:text/html;base64," + Base64.encodeToString(
                html.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP));
    }

    private static void awaitTitle(Instrumentation instrumentation, XWalkView view,
                                   String expected) {
        AtomicReference<String> actual = new AtomicReference<>();
        long deadline = SystemClock.elapsedRealtime() + 30000;
        do {
            instrumentation.runOnMainSync(() -> {
                try {
                    actual.set(view.getTitle());
                } catch (RuntimeException e) {
                    if (!"Crosswalk's APIs are not ready yet".equals(e.getMessage())) {
                        throw e;
                    }
                }
            });
            if (expected.equals(actual.get())) {
                return;
            }
            SystemClock.sleep(100);
        } while (SystemClock.elapsedRealtime() < deadline);
        assertEquals(expected, actual.get());
    }
}
