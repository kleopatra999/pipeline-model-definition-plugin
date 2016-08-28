/*
 * The MIT License
 *
 * Copyright (c) 2016, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.pipeline.config.parser;

import hudson.slaves.DumbSlave;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.pipeline.config.AbstractConfigTest;
import org.jenkinsci.plugins.pipeline.config.ast.ConfigASTPipelineDef;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.List;

import static org.jenkinsci.plugins.pipeline.config.BaseParserLoaderTest.getJSONErrorReport;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Parameterized.class)
public class ExecuteConvertedTest extends AbstractConfigTest {
    private String configName;

    public ExecuteConvertedTest(String configName) {
        this.configName = configName;
    }

    @Parameterized.Parameters(name="Name: {0}")
    public static Iterable<Object[]> generateParameters() {
        List<Object[]> result = new ArrayList<>();
        for (String c : AbstractConfigTest.SHOULD_PASS_CONFIGS) {
            // Temporary hack to skip Docker and postBuildAndNotifications (since that last one has a failing build deliberately)
            if (!c.equals("agentDocker") && !c.equals("postBuildAndNotifications")) {
                result.add(new Object[]{c});
            }
        }

        return result;
    }

    @Test
    public void testGroovyToASTToGroovyExecution() throws Exception {
        ConfigASTPipelineDef origRoot = Converter.urlToPipelineDef(getClass().getResource("/" + configName + ".groovy"));

        assertNotNull(origRoot);

        String prettyGroovy = origRoot.toPrettyGroovy();
        assertNotNull(prettyGroovy);

        prepRepoWithJenkinsfileFromString(prettyGroovy);

        executeBuild();
    }

    @Test
    public void testJSONToASTToGroovyExecution() throws Exception {
        JSONObject json = JSONObject.fromObject(fileContentsFromResources("json/" + configName + ".json"));
        assertNotNull("Couldn't parse JSON for " + configName, json);

        JSONParser jp = new JSONParser(json);
        ConfigASTPipelineDef origRoot = jp.parse();

        assertEquals(getJSONErrorReport(jp, configName), 0, jp.getErrorCollector().getErrorCount());
        assertNotNull("Pipeline null for " + configName, origRoot);

        String prettyGroovy = origRoot.toPrettyGroovy();
        assertNotNull(prettyGroovy);

        prepRepoWithJenkinsfileFromString(prettyGroovy);

        executeBuild();
    }

    private void executeBuild() throws Exception {
        DumbSlave s = j.createOnlineSlave();
        s.setLabelString("some-label");
        s.getNodeProperties().add(new EnvironmentVariablesNodeProperty(new EnvironmentVariablesNodeProperty.Entry("ONSLAVE", "true")));

        WorkflowRun b = getAndStartBuild();
        j.assertBuildStatusSuccess(j.waitForCompletion(b));
    }
}