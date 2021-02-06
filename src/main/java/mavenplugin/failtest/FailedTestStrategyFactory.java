package mavenplugin.failtest;

import mavenplugin.IncrementalMojo;

public class FailedTestStrategyFactory {

    private final IncrementalMojo mojo;
    private String initialValueOfSkipTests;

    public FailedTestStrategyFactory(IncrementalMojo mojo) {
        this.mojo = mojo;
        initialValueOfSkipTests = mojo.getSkipTests();
    }

    public FailedTestStrategy make() {

        if (hasSkipTests()) return new DoNothingStrategy();

        if (hasSurefirePlugin()) return new SurefireFailedTestStrategy(mojo);

        // other strategies here


        return new DoNothingStrategy();
    }

    private boolean hasSkipTests() {
        return "true".equals(initialValueOfSkipTests);
    }

    private boolean hasSurefirePlugin() {
        boolean hasSurefireDependency = mojo.getProject().getBuildPlugins().stream()
                .anyMatch(dependency ->
                        dependency.getArtifactId().equals("maven-surefire-plugin"));
        return hasSurefireDependency;
    }


    private final class DoNothingStrategy implements FailedTestStrategy {
        @Override
        public boolean hasFailedTests() { return false; }

        @Override
        public void prepareForCompilation() { }

    }
}
