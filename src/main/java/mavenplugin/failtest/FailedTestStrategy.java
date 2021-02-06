package mavenplugin.failtest;

public interface FailedTestStrategy {

    boolean hasFailedTests();

    void prepareForCompilation();
}
