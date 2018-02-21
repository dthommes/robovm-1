package org.robovm.compiler.util.update;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Test;
import org.robovm.compiler.Version;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests {@link UpdateChecker}.
 */

public class UpdateCheckerTest {

    @Test
    public void testBrokenJson() {
        JSONObject versionJson = (JSONObject) JSONValue.parse("{\"release\" : {}}");
        UpdateChecker checker = new UpdateChecker(new Version("1.0.0"));
        try {
            checker.parseVersionJson(versionJson);
            fail();
        } catch (Throwable ignored) {
        }
    }

    @Test
    public void testUpdateNotRequiredSame() {
        UpdateChecker checker = new UpdateChecker(new Version("1.0.0"));
        JSONObject versionJson = (JSONObject) JSONValue.parse("{\"release\" : {\"version\" : \"1.0.0\"}}");
        assertNull(checker.parseVersionJson(versionJson));
        // check legacy
        versionJson = (JSONObject) JSONValue.parse("{\"version\" : \"1.0.0\"}");
        assertNull(checker.parseVersionJson(versionJson));
    }


    @Test
    public void testUpdateNotRequiredOlder() {
        UpdateChecker checker = new UpdateChecker(new Version("1.0.0"));
        JSONObject versionJson = (JSONObject) JSONValue.parse("{\"release\" : {\"version\" : \"0.9.0\"}}");
        assertNull(checker.parseVersionJson(versionJson));
        // check legacy
        versionJson = (JSONObject) JSONValue.parse("{\"version\" : \"0.9.0\"}");
        assertNull(checker.parseVersionJson(versionJson));
    }

    @Test
    public void testReleseToReleaseUpdate() {
        UpdateChecker checker = new UpdateChecker(new Version("1.0.0"));
        JSONObject versionJson = (JSONObject) JSONValue.parse("{\"release\" : {\"version\" : \"1.1.0\"}}");
        assertTrue(checker.parseVersionJson(versionJson).toString().matches("(.*)new version(.*)1.0.0(.*)1.1.0(.*)"));
        // check legacy
        versionJson = (JSONObject) JSONValue.parse("{\"version\" : \"1.1.0\"}");
        assertTrue(checker.parseVersionJson(versionJson).toString().matches("(.*)new version(.*)1.0.0(.*)1.1.0(.*)"));
    }

    @Test
    public void testSnapshotToReleaseUpdate() {
        UpdateChecker checker = new UpdateChecker(new Version("1.0.0-SNAPSHOT"));
        JSONObject versionJson = (JSONObject) JSONValue.parse("{\"release\" : {\"version\" : \"1.1.0\"}}");
        assertTrue(checker.parseVersionJson(versionJson).toString().matches("(.*)stable version(.*)1.0.0-SNAPSHOT(.*)1.1.0(.*)"));

        // check legacy
        versionJson = (JSONObject) JSONValue.parse("{\"version\" : \"1.1.0\"}");
        assertTrue(checker.parseVersionJson(versionJson).toString().matches("(.*)stable version(.*)1.0.0-SNAPSHOT(.*)1.1.0(.*)"));

        // same version but stable
        versionJson = (JSONObject) JSONValue.parse("{\"release\" : {\"version\" : \"1.0.0\"}}");
        assertTrue(checker.parseVersionJson(versionJson).toString().matches("(.*)stable version(.*)1.0.0-SNAPSHOT(.*)1.0.0(.*)"));

        // check legacy
        versionJson = (JSONObject) JSONValue.parse("{\"version\" : \"1.0.0\"}");
        assertTrue(checker.parseVersionJson(versionJson).toString().matches("(.*)stable version(.*)1.0.0-SNAPSHOT(.*)1.0.0(.*)"));
    }

    @Test
    public void testSnapshotToSnapshot() {
        UpdateChecker checker = new UpdateChecker(new Version("1.0.0-SNAPSHOT", 20180203));
        JSONObject versionJson = (JSONObject) JSONValue.parse("{\"snapshot\" : {\"version\" : \"1.0.0\", \"build.timestamp\"  : 20180204}}");
        assertTrue(checker.parseVersionJson(versionJson).toString().matches("(.*)new snapshot(.*)1.0.0-SNAPSHOT(.*)1.0.0(.*)"));

        versionJson = (JSONObject) JSONValue.parse("{\"snapshot\" : {\"version\" : \"1.1.0\", \"build.timestamp\"  : 20180204}}");
        assertTrue(checker.parseVersionJson(versionJson).toString().matches("(.*)new snapshot(.*)1.0.0-SNAPSHOT(.*)1.1.0(.*)"));

        // even if TS is older (should not happen)
        versionJson = (JSONObject) JSONValue.parse("{\"snapshot\" : {\"version\" : \"1.1.0\", \"build.timestamp\"  : 20110101}}");
        assertTrue(checker.parseVersionJson(versionJson).toString().matches("(.*)new snapshot(.*)1.0.0-SNAPSHOT(.*)1.1.0(.*)"));
    }

    @Test
    public void testSnapshotUpdateNotRequiredSame() {
        UpdateChecker checker = new UpdateChecker(new Version("1.0.0-SNAPSHOT", 20180203));
        JSONObject versionJson = (JSONObject) JSONValue.parse("{\"snapshot\" : {\"version\" : \"1.0.0\", \"build.timestamp\"  : 20180203}}");
        assertNull(checker.parseVersionJson(versionJson));
    }


    @Test
    public void testSnapshotUpdateNotRequiredOlder() {
        UpdateChecker checker = new UpdateChecker(new Version("1.0.0-SNAPSHOT", 20180203));
        JSONObject versionJson = (JSONObject) JSONValue.parse("{\"snapshot\" : {\"version\" : \"1.0.0\", \"build.timestamp\"  : 20180202}}");
        assertNull(checker.parseVersionJson(versionJson));

        versionJson = (JSONObject) JSONValue.parse("{\"snapshot\" : {\"version\" : \"0.9.0\", \"build.timestamp\"  : 202380202}}");
        assertNull(checker.parseVersionJson(versionJson));
    }

}