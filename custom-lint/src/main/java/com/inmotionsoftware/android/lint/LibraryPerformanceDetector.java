package com.inmotionsoftware.android.lint;

import com.android.tools.lint.checks.JavaPerformanceDetector;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;

public class LibraryPerformanceDetector extends JavaPerformanceDetector {
    public static final Issue ISSUE = Issue.create(
            "IMS-java-perf",
            "Java Performance Check",
            "This check looks for problems with java library performance",
            Category.CORRECTNESS, 6, Severity.WARNING,
            new Implementation(LibraryPerformanceDetector.class, Scope.JAVA_FILE_SCOPE));
}
