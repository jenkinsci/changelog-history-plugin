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

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.listeners.RunListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.tools.ant.BuildException;

/**
 * When builds are deleted, archive the changelog data in a later build.
 * @author Alan Harder
 */
@Extension
public class ChangeLogHistoryRunListener extends RunListener<AbstractBuild> {

    public ChangeLogHistoryRunListener() {
        super(AbstractBuild.class);
    }

    @Override
    public void onDeleted(AbstractBuild build) {
        try {
            copyChangeLogs(build);
        } catch (Exception ex) {
            Logger.getLogger(ChangeLogHistoryRunListener.class.getName()).log(
                Level.WARNING, "changelog-history failure", ex);
        }
    }

    private void copyChangeLogs(AbstractBuild build) throws IOException, BuildException {
        // Find next build
        AbstractBuild nextBuild = (AbstractBuild)build.getNextBuild();
        if (nextBuild == null) return; // No where to copy history
        File baseDir = new File(nextBuild.getRootDir(), "changelog-history");
        boolean copied = false;
        // Copy changelog for this build, if any
        File changeLog = new File(build.getRootDir(), "changelog.xml");
        if (changeLog.isFile() && !build.getChangeSet().isEmptySet()) {
            checkDir(baseDir);
            Util.copyFile(changeLog, new File(baseDir, build.getNumber() + ".xml"));
            copied = true;
        }
        // Copy changelog-history in this build, if any
        File changeHistory = new File(build.getRootDir(), "changelog-history");
        if (changeHistory != null && changeHistory.isDirectory()) {
            File[] files = changeHistory.listFiles();
            if (files != null) {
                checkDir(baseDir);
                for (File file : files) {
                    Util.copyFile(file, new File(baseDir, file.getName()));
                }
            }
            copied = true;
        }
        // Ensure next build has action for viewing data
        if (copied && nextBuild.getAction(ChangeLogHistoryAction.class) == null) {
            nextBuild.getActions().add(new ChangeLogHistoryAction(nextBuild));
            nextBuild.save();
        }
    }

    private void checkDir(File dir) throws IOException {
        if (!dir.isDirectory()) {
            if (!dir.mkdir())
                throw new IOException("Failed to mkdir: " + dir);
        }
    }
}
