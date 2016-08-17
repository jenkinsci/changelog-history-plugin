/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Alan Harder
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
package hudson.plugins.changelog_history;

import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.Hudson;
import hudson.model.Job;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.recipes.LocalData;

/**
 * Test interaction of changelog-history plugin with Jenkins core.
 * @author Alan Harder
 */
public class ChangeLogHistoryTest extends HudsonTestCase {

    @LocalData
    public void testPlugin() throws Exception {
        Job job = (Job)Hudson.getInstance().getItem("test-job");
        assertNotNull("job missing.. @LocalData problem?", job);

        // Delete some builds.. plugin's RunListener will copy changelog data
        job.getBuildByNumber(2).delete();
        job.getBuildByNumber(3).delete();
        job.getBuildByNumber(5).delete();
        job.getBuildByNumber(4).delete();
        assertNotNull(job.getBuildByNumber(6).getAction(ChangeLogHistoryAction.class));

        // Now access the views for changelog-history display
        WebClient wc = new WebClient();
        HtmlPage page = wc.goTo("job/test-job/6/");
        HtmlAnchor link = page.getAnchorByText("Change Log History");
        assertNotNull("changelog-history link", link);
        page = (HtmlPage)link.click();
        String mainContent = page.getElementById("main-panel").asText();
        // All of the svn commit comments should be present:
        for (String comment : new String[] { "add xml file", "add stuff", "remove file" }) {
            assertTrue("svn commit comment '" + comment + "' should be present in main content:\n"
                       + mainContent, mainContent.contains(comment));
        }
        link = page.getAnchorByHref("5/changes#detail0");
        assertNotNull("5/changes#detail0 detail link", link);
        page = (HtmlPage)link.click();
        mainContent = page.getElementById("main-panel").asText();
        assertTrue("'remove file' commit comment should be found in detail content:\n"
                   + mainContent, mainContent.contains("remove file"));
        assertTrue("'trunk/test.txt' file path should be found in detail content:\n"
                   + mainContent, mainContent.contains("trunk/test.txt"));

        // Verify "More change log history" link on Recent Changes page
        page = wc.goTo("job/test-job/changes");
        link = page.getAnchorByText("More change log history");
        assertNotNull("'More change log history' link should be present on changes page", link);
    }
}
