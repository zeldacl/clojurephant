package dev.clojurephant.plugin.common.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

import dev.clojurephant.plugin.clojure.ClojureBasePlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.tasks.SourceSetContainer;

public class ClojureCommonBasePlugin implements Plugin<Project> {
  public static final String TOOLS_CONFIGURATION_NAME = "clojureTools";

  @Override
  public void apply(Project project) {
    project.getPluginManager().apply(JavaBasePlugin.class);

    SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
    configureSourceSets(project, sourceSets);
    configureToolsConfigurations(project);
  }

  private void configureSourceSets(Project project, SourceSetContainer sourceSets) {
    sourceSets.all(sourceSet -> {
      sourceSet.getResources().exclude("**/.keep");
    });
  }

  private void configureToolsConfigurations(Project project) {
    Configuration tools = project.getConfigurations().create(TOOLS_CONFIGURATION_NAME);
    tools.defaultDependencies(deps -> {
      deps.add(project.getDependencies().create("dev.clojurephant:clojurephant-tools:" + getVersion()));
    });

    // TODO does this JAR get included via shadow or application plugins?
    project.getExtensions().getByType(SourceSetContainer.class).all(sourceSet -> {
      project.getConfigurations().getByName(sourceSet.getCompileClasspathConfigurationName()).extendsFrom(tools);
      project.getConfigurations().getByName(sourceSet.getRuntimeClasspathConfigurationName()).extendsFrom(tools);
    });
  }

  private String getVersion() {
    try (InputStream stream = ClojureBasePlugin.class.getResourceAsStream("/clojurephant.properties")) {
      Properties props = new Properties();
      props.load(stream);
      return props.getProperty("version");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
