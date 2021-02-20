package mavenplugin.failtest;

import mavenplugin.IncrementalMojo;
import mavenplugin.io.IOFunctions;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Collectors;
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

        Path targetDirectory = getTargetLocation();
        assert targetDirectory.getFileName().toString().equals("target") : "not in the target directory";

        int countFailedTests = countFailedTestsWithSurefire();
        markDirectory(targetDirectory, countFailedTests > 0);

        boolean hasFailedTests = hasFailTestMark(targetDirectory) || countFailedTests > 0;
        if (!hasFailedTests) return;

        info("This project or module might have tests with errors");
        mojo.getProject().getProperties().setProperty("skipTests",
                initialValueOfSkipTests); // restore skipTests
    }

    protected int countFailedTestsWithSurefire() {
        Path surefireOutputDirectory = getSurefireLocation();
        return surefireFilesWithErrors(surefireOutputDirectory).collect(
                Collectors.reducing(0, c -> 1, Integer::sum));
    }

    protected Path getSurefireLocation() {
        Path defaultSurefireLocation = getTargetLocation().resolve("surefire-reports");
        return findSurefireLocationInConfig(
                Stream.concat(mojo.getProject().getBuildPlugins().stream(),
                        mojo.getProject().getPluginManagement().getPlugins().stream()))
                .orElse(defaultSurefireLocation);
    }

    private Optional<Path> findSurefireLocationInConfig(Stream<Plugin> stream) {
        Optional<Path> configuredSurefireLocation = Optional.empty();
    try {
        configuredSurefireLocation = stream
            .filter(plugin -> plugin.getArtifactId().equals("maven-surefire-plugin")
                    && null != plugin.getConfiguration()
                    && ((Xpp3Dom) plugin.getConfiguration()).getChild("reportsDirectory") != null)
            .map(plugin -> ((Xpp3Dom) plugin.getConfiguration()).getChild("reportsDirectory"))
            .findFirst().map(property -> Paths.get(property.getValue()));
    } catch (Exception e) {
        // null or class cast turned into a warning
        warn("Unable to parse reportsDirectory from maven-surefire-plugin config: %s", e);
    }
        return configuredSurefireLocation;
    }

    private Path getTargetLocation() {
        return mojo.getOutputDirectory().getParentFile().toPath();
    }

    private boolean hasFailTestMark(Path location) {
        File markFile = new File(location.toFile(), MARK_FAILTEST_FILE);
        return markFile.exists();
    }

    private void markDirectory(Path location, boolean hasFailedTests) {
        Path mark = new File(location.toFile(), MARK_FAILTEST_FILE).toPath();
        if (mark.toFile().exists()) IOFunctions.deleteFiles(mark);
        if (hasFailedTests) IOFunctions.touch(mark);
    }

    private Stream<File> surefireFilesWithErrors(Path surefireLocation) {
        return streamOfNullable(
                surefireLocation.toFile().listFiles(
                        f -> f.getName().endsWith("Test.txt")))
                .map(File::toPath)
                .filter(f -> getLinesOrNothing(f).limit(5) // head of the file
                            .anyMatch(s -> (s.contains("Errors: ") && !s.contains("Errors: 0")))
                ).map(Path::toFile);
    }

    private <T> Stream<T> streamOfNullable(T[] array) {
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

    private void info(String template, Object... args) { mojo.getLog().info(String.format(template, args)); }
    private void warn(String template, Object... args) { mojo.getLog().warn(String.format(template, args)); }
}
