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

import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.scm.ChangeLogParser;
import hudson.scm.ChangeLogSet;
import hudson.scm.SubversionChangeLogSet;
import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Action to display changelog-history data.
 * @author Alan.Harder@sun.com
 */
public class ChangeLogHistoryAction implements Action {
    private final AbstractBuild<?,?> build;

    public ChangeLogHistoryAction(AbstractBuild<?,?> build) {
        this.build = build;
    }

    public AbstractBuild<?,?> getBuild() { return build; }
    public String getUrlName() { return "changelog-history"; }
    public String getDisplayName() { return Messages.actionTitle(); }
    public String getIconFileName() { return "notepad.gif"; }

    /**
     * Get all changelog detail; used by index.jelly
     */
    public Map<Long,ChangeLogSet> getChangeLogSets() throws Exception {
        ChangeLogParser parser = build.getProject().getScm().createChangeLogParser();
        File baseDir = new File(build.getRootDir(), "changelog-history");

        // Find {build#}.xml files in changelog-history dir
        File[] files = baseDir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isFile() && pathname.getName().matches("\\d+\\.xml");
            }
        });

        // Descending sort by build number
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return Long.signum(getBuildNumber(f2) - getBuildNumber(f1));
            }
        });

        // Parse data files and add to list
        Map<Long,ChangeLogSet> result = new LinkedHashMap<Long,ChangeLogSet>(files.length*2);
        for (File file : files) {
            ChangeLogSet changeLogSet = parser.parse(build, file);
            // Hack to avoid showing revision info (it's for this build, not the old builds)
            if (changeLogSet instanceof SubversionChangeLogSet) try {
                Field f = SubversionChangeLogSet.class.getDeclaredField("revisionMap");
                f.setAccessible(true);
                f.set(changeLogSet, new HashMap(0));
            } catch (Exception ex) { /* ignore */ }
            result.put(getBuildNumber(file), changeLogSet);
        }
        return result;
    }

    private static long getBuildNumber(File file) {
        String fn = file.getName();
        return Long.parseLong(fn.substring(0, fn.indexOf('.')));
    }

    /**
     * Handle requests to view changelog details.
     */
    public Object getDynamic(String oldBuild, StaplerRequest req, StaplerResponse rsp)
            throws Exception {
        File f = new File(new File(build.getRootDir(), "changelog-history"), oldBuild + ".xml");
        if (!f.isFile())
            return null;

        ChangeLogParser parser = build.getProject().getScm().createChangeLogParser();
        req.setAttribute("changeSet", parser.parse(build, f));
        req.setAttribute("buildNumber", oldBuild);
        return this;
    }
}
