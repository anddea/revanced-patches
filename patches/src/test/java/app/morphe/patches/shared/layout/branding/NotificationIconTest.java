package app.morphe.patches.shared.layout.branding;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class NotificationIconTest {
    private Document vector(String body) throws Exception {
        String xml = "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\" "
                + "android:width=\"108dp\" android:height=\"108dp\" "
                + "android:viewportWidth=\"108\" android:viewportHeight=\"108\">" + body + "</vector>";
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private String path(String data) {
        return "<path android:fillColor=\"#000\" android:pathData=\"" + data + "\"/>";
    }

    private double attribute(Element element, String name) {
        return Double.parseDouble(element.getAttribute("android:" + name));
    }

    @Test public void fitsVisibleArtworkAndPreservesTransparentPadding() throws Exception {
        Document doc = vector("<path android:fillColor=\"#00000000\" android:pathData=\"M0 0H108V108H0Z\"/>"
                + "<path android:fillColor=\"#80000000\" android:fillAlpha=\"0.5\" "
                + "android:fillType=\"evenOdd\" android:pathData=\"M40 44H60V54H40Z M45 46H55V50H45Z\"/>");
        assertTrue(NotificationIconKt.normalizeNotificationIcon(doc));
        Element group = (Element) doc.getElementsByTagName("group").item(0);
        assertEquals(1.1, attribute(group, "scaleX"), 1e-8);
        assertEquals(1, 40 * attribute(group, "scaleX") + attribute(group, "translateX"), 1e-8);
        assertEquals(23, 60 * attribute(group, "scaleX") + attribute(group, "translateX"), 1e-8);
        Element padding = (Element) doc.getElementsByTagName("path").item(0);
        Element artwork = (Element) doc.getElementsByTagName("path").item(1);
        assertEquals("#00000000", padding.getAttribute("android:fillColor"));
        assertEquals("#80ffffff", artwork.getAttribute("android:fillColor"));
        assertEquals("0.5", artwork.getAttribute("android:fillAlpha"));
        assertEquals("evenOdd", artwork.getAttribute("android:fillType"));
    }

    @Test public void handlesRelativeCurvesAndRepeatedCoordinates() throws Exception {
        Document doc = vector(path("M10 10c0 0 10 0 10 10s10 10 10 0q10 -10 20 0t20 0h10v10l-10 0 0 -10z"));
        assertTrue(NotificationIconKt.normalizeNotificationIcon(doc));
        Element group = (Element) doc.getElementsByTagName("group").item(0);
        assertEquals(22.0 / 70, attribute(group, "scaleX"), 1e-8);
    }

    @Test public void includesCircleArcsAndStrokeWidth() throws Exception {
        Document doc = vector("<path android:fillColor=\"#0000\" android:strokeColor=\"#000\" "
                + "android:strokeWidth=\"4\" android:strokeLineJoin=\"round\" "
                + "android:pathData=\"M54 54m-20 0a20 20 0 1 1 40 0a20 20 0 1 1 -40 0\"/>");
        assertTrue(NotificationIconKt.normalizeNotificationIcon(doc));
        Element group = (Element) doc.getElementsByTagName("group").item(0);
        assertEquals(0.5, attribute(group, "scaleX"), 1e-8);
        assertEquals(-15, attribute(group, "translateX"), 1e-8);
        assertEquals("#ffffffff", ((Element) doc.getElementsByTagName("path").item(0))
                .getAttribute("android:strokeColor"));
    }

    @Test public void unsupportedOrEmptyArtworkIsUnchanged() throws Exception {
        for (String body : new String[]{"<group>" + path("M1 1L2 2") + "</group>",
                path("M1 1"), path("M1 1R2 2"),
                "<path android:fillColor=\"@color/custom\" android:pathData=\"M1 1L2 2\"/>"}) {
            Document doc = vector(body);
            assertFalse(NotificationIconKt.normalizeNotificationIcon(doc));
            assertEquals("108dp", doc.getDocumentElement().getAttribute("android:width"));
            assertEquals("108", doc.getDocumentElement().getAttribute("android:viewportWidth"));
        }
    }

    @Test public void normalizesEveryBundledMonochromeWithoutChangingPaths() throws Exception {
        Path resources = Path.of("src/main/resources");
        if (!Files.isDirectory(resources)) resources = Path.of("patches/src/main/resources");
        int count = 0;
        try (var files = Files.walk(resources)) {
            for (Path file : files.filter(p -> p.toString().contains("/branding/")
                    && p.toString().contains("/monochrome/drawable/")
                    && p.toString().endsWith(".xml")).toList()) {
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file.toFile());
                var paths = doc.getElementsByTagName("path");
                String[] originalPaths = new String[paths.getLength()];
                for (int i = 0; i < paths.getLength(); i++) {
                    originalPaths[i] = ((Element) paths.item(i)).getAttribute("android:pathData");
                }
                assertTrue(file.toString(), NotificationIconKt.normalizeNotificationIcon(doc));
                assertEquals("24dp", doc.getDocumentElement().getAttribute("android:width"));
                assertEquals("24", doc.getDocumentElement().getAttribute("android:viewportWidth"));
                for (int i = 0; i < paths.getLength(); i++) {
                    assertEquals(originalPaths[i], ((Element) paths.item(i)).getAttribute("android:pathData"));
                }
                count++;
            }
        }
        assertTrue("Expected bundled monochrome icons", count >= 32);
    }
}
