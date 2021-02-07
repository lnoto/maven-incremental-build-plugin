package mavenplugin.failtest;

import mavenplugin.IncrementalMojo;
import mavenplugin.io.IOFunctions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class SurefireFailedTestStrategy implements FailedTestStrategy {

    private static final String MARK_FAILTEST_FILE = "failtest.mark";
    private final IncrementalMojo mojo;

    private String initialValueOfSkipTests;

    public SurefireFailedTestStrategy(IncrementalMojo mojo) {
        this.mojo = mojo;
        initialValueOfSkipTests = mojo.getSkipTests();
    }

    @Override
    public void apply() {

        Path rootTarget = mojo.getOutputDirectory().getParentFile().toPath();
        assert rootTarget.getFileName().toString().equals("target") : "not in the target directory";

        boolean hasFailedTests = hasFailTestMark(rootTarget) || countFailedTestsWithSurefire() > 0;
        markDirectoryIfFailedTests(rootTarget, hasFailedTests);

        if (!hasFailedTests) return;
        info("This project or module might have tests with errors .. forcing tests");

        mojo.getProject().getProperties().setProperty("skipTests",
                initialValueOfSkipTests); // restore skipTests
    }

    private boolean hasFailTestMark(Path rootTarget) {
        File markFile = new File(rootTarget.toFile(), MARK_FAILTEST_FILE);
        return markFile.exists();
    }

    private long countFailedTestsWithSurefire() {
        File targetDirectory = mojo.getOutputDirectory().getParentFile();
        String surefireOutputDirectory = targetDirectory + File.separator + "surefire-reports";
        return surefireFilesWithErrors(surefireOutputDirectory).count();
    }

    private void markDirectoryIfFailedTests(Path rootTarget, boolean hasFailedTests) {
        Path mark = new File(rootTarget.toFile(), MARK_FAILTEST_FILE).toPath();
        if (mark.toFile().exists()) IOFunctions.deleteFiles(mark);
        if (hasFailedTests) IOFunctions.touch(mark);
    }

    private Stream<File> surefireFilesWithErrors(String sureFireLocation) {
        return streamOfNullable(
                Paths.get(sureFireLocation).toFile().listFiles(
                        f -> f.getName().endsWith("Test.txt")))
                .map(File::toPath)
                .filter(f -> getLinesOrNothing(f).limit(5) // head of the file
                            .anyMatch(s -> (s.contains("Errors: ") && !s.contains("Errors: 0")))
                ).map(Path::toFile);
    }

    public <T> Stream<T> streamOfNullable(T[] array) {
        return array == null
                ? Stream.empty()
                : Stream.of(array);
    }

    private Stream<String> getLinesOrNothing(Path path) {
        try {
            return Files.lines(path);
        } catch (IOException e) {
            return Stream.empty();
        }
    }

    private void info(String template, Object... args) {
        mojo.getLog().info(String.format(template, args));
    }
}
