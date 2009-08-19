/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Alan Harder
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

import hudson.Extension;
import hudson.MarkupText;
import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogAnnotator;
import hudson.scm.ChangeLogSet;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Include changelog-history links when viewing changes
 * @author Alan.Harder@sun.com
 */
@Extension
public class ChangeLogHistoryAnnotator extends ChangeLogAnnotator {

    @Override
    public void annotate(AbstractBuild<?,?> build, ChangeLogSet.Entry change, MarkupText text) {
        // Nothing to do if this build has no changelog history:
        if (build.getAction(ChangeLogHistoryAction.class) == null) return;

        // Only add links on "changes" page (for project or build):
        StaplerRequest req = Stapler.getCurrentRequest();
        if (!req.getRequestURI().endsWith("/changes")) return;

        // Add link on last item in list:
        Object[] items = change.getParent().getItems();
        if (change == items[items.length-1]) {
            text.wrapBy("", "<div style=\"float:right\"><a href=\""
                + req.getContextPath() + '/' + change.getParent().build.getUrl()
                + "changelog-history/\">More change log history</a></div>");
        }
    }
}
