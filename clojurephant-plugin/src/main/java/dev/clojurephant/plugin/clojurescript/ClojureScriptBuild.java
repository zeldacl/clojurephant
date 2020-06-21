package dev.clojurephant.plugin.clojurescript;

import dev.clojurephant.plugin.clojurescript.tasks.ClojureScriptCompileOptions;
import dev.clojurephant.plugin.clojurescript.tasks.ClojureScriptSourceSet;
import groovy.lang.Closure;
import org.apache.commons.text.WordUtils;
import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;

public class ClojureScriptBuild implements Named {
  private final String name;
  private final DirectoryProperty outputDir;
  private final Property<SourceSet> sourceSet;
  private final ClojureScriptCompileOptions compiler;

  public ClojureScriptBuild(Project project, String name) {
    this.name = name;
    this.outputDir = project.getObjects().directoryProperty();
    this.sourceSet = project.getObjects().property(SourceSet.class);
    this.compiler = new ClojureScriptCompileOptions(project, outputDir);
  }

  @Override
  public String getName() {
    return name;
  }

  public DirectoryProperty getOutputDir() {
    return outputDir;
  }

  public Property<SourceSet> getSourceSet() {
    return sourceSet;
  }

  Provider<FileCollection> getSourceRoots() {
    return getSourceSet().map(sourceSet -> {
      ClojureScriptSourceSet clojure = (ClojureScriptSourceSet) new DslObject(sourceSet).getConvention().getPlugins().get("clojurescript");
      return clojure.getClojureScript().getSourceDirectories();
    });
  }

  public boolean isCompilerConfigured() {
    return compiler.getOutputTo().isPresent() || compiler.getModules().values().stream()
        .anyMatch(module -> module.getOutputTo().isPresent());
  }

  public ClojureScriptCompileOptions getCompiler() {
    return compiler;
  }

  public void compiler(Action<? super ClojureScriptCompileOptions> configureAction) {
    configureAction.execute(compiler);
  }

  /*
   * We only have this variant (instead of just Action) since Gradle doesn't currently (as of 4.7)
   * instrument Action methods on nested config objects
   */
  public void compiler(Closure<?> configureAction) {
    configureAction.setResolveStrategy(Closure.DELEGATE_FIRST);
    configureAction.setDelegate(compiler);
    configureAction.call(compiler);
  }

  String getTaskName(String task) {
    if ("main".equals(name)) {
      return String.format("%sClojureScript", task);
    } else {
      return String.format("%s%sClojureScript", task, WordUtils.capitalize(name));
    }
  }
}
