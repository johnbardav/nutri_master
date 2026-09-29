package org.gradle.wrapper;
/** Compatibility entrypoint. This bootstrap is not the official Gradle wrapper. */
public final class GradleWrapperMain {
    public static void main(String[] args) {
        com.poligran.nutrimaster.bootstrap.GradleBootstrap.main(args);
    }
}
