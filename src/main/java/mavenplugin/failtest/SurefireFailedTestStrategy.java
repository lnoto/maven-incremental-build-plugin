package mavenplugin.failtest;

import mavenplugin.IncrementalMojo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class SurefireFailedTestStrategy implements FailedTestStrategy {

    private final IncrementalMojo mojo;

    private String initialValueOfSkipTests;
    private long failedTests = 0;

    public SurefireFailedTestStrategy(IncrementalMojo mojo) {
        this.mojo = mojo;
        initialValueOfSkipTests = mojo.getSkipTests();
    }

    @Override
    public boolean hasFailedTests() {
        File targetDirectory = mojo.getOutputDirectory().getParentFile();
        String surefireOutputDirectory = targetDirectory + File.separator + "surefire-reports";
        failedTests = surefireFilesWithErrors(surefireOutputDirectory).count();
        return failedTests != 0;
    }

    @Override
    public void prepareForCompilation() {
        info("Tests with errors: %s .. force cleaning on failing tests", failedTests);

        Path rootTarget = mojo.getOutputDirectory().getParentFile().toPath();
        assert rootTarget.getFileName().toString().equals("target") : "not in the target directory";

        mojo.cleanTargetLocation(rootTarget);
        mojo.createTimeStampFile(rootTarget);

        mojo.getProject().getProperties().setProperty("skipTests",
                initialValueOfSkipTests); // restore skipTests

    }

    private Stream<File> surefireFilesWithErrors(String sureFireLocation) {
        return streamOfNullable(
                Paths.get(sureFireLocation).toFile().listFiles(
                        (file) -> file.getName().endsWith("Test.txt")))
                .map(File::toPath)
                .filter((file)-> {
                    return getLinesOrNothing(file).limit(5) // head of the file
                            .anyMatch(s -> (s.contains("Errors: ") && !s.contains("Errors: 0")));
                }).map(Path::toFile);
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
