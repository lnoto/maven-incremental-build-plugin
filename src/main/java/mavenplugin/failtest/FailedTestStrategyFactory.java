package mavenplugin.failtest;

import mavenplugin.IncrementalMojo;

public class FailedTestStrategyFactory {

    private final IncrementalMojo mojo;

    public FailedTestStrategyFactory(IncrementalMojo mojo) {
        this.mojo = mojo;
    }

    public FailedTestStrategy make() {

        if (hasSurefirePlugin()) return new SurefireFailedTestStrategy(mojo);

        // other strategies here

        return new DoNothingStrategy();
    }

    private boolean hasSurefirePlugin() {
        boolean hasSurefireDependency = mojo.getProject().getBuildPlugins().stream()
                .anyMatch(dependency ->
                        dependency.getArtifactId().equals("maven-surefire-plugin"));
        return hasSurefireDependency;
    }

    private final class DoNothingStrategy implements FailedTestStrategy {
        @Override
        public void apply() { }

    }
}
