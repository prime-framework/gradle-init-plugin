/*
 * Copyright (c) 2012, Inversoft Inc., All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.primeframework.gradle.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar

/**
 * The Integration Plugin provides the "int" task, which is used to upload integration builds
 * into a local repository.  Integration builds are annotated with a "-SNAPSHOT" on the end
 * of the artifact file.
 *
 * e.g. prime-mock-1.0-SNAPSHOT.jar
 *
 * It"s worth mentioning that integration builds are not intended to be deployed into a remote repository
 *
 * @author James Humphrey
 */
class InitPlugin implements Plugin<Project> {
  def void apply(Project project) {

    // make sure debug level is set for compilation
    project.compileJava.options.debugOptions.debugLevel = "source,lines,vars"
    project.compileTestJava.options.debugOptions.debugLevel = "source,lines,vars"

    // add the inversoft public repository
    project.repositories {
      ivy {
        name "inversoftPublic"
        ivyPattern "http://hawking.inversoft.com/repository/public/[organisation]/[module]/[revision]/ivy.xml"
        artifactPattern "http://hawking.inversoft.com/repository/public/[organisation]/[module]/[revision]/[type]s/[artifact]-[revision].[ext]"
      }
    }

    // add source configuration
    project.configurations.add("sources")

    // init some configurations
    project.configurations {
      compileOnly {
        transitive = false
        visible = false
      }
      sources {
        description "Configuration for sources"
      }
      compile {
        transitive = false
      }
      testCompile {
        transitive = false
      }
    }

    // IDEA config
    project.idea {
      pathVariables USER_HOME: project.file(System.getProperty("user.home"))

      module {
        scopes.PROVIDED.plus += project.configurations.compileOnly

        if (project.gradle.startParameter.mergedSystemProperties.containsKey("current")) {
          name = "$project.name-current"
        }

        iml {
          whenMerged { module ->
            module.dependencies*.exported = false
          }
        }
      }
    }

    // Tweak the JAR to include the description and version
    project.jar {
      manifest {
        attributes "Implementation-Title": project.description, "Implementation-Version": project.version
      }
    }

    // Let"s assume they are using Java for now
    project.sourceSets {
      main {
        compileClasspath += project.configurations.compileOnly
      }
      test {
        compileClasspath += project.configurations.compileOnly
        runtimeClasspath += project.configurations.compileOnly
      }
    }

    // Add the source jar task
    project.task("sourceJar", type: Jar) {
      if (project.sourceSets.main.hasProperty("groovy")) {
        from project.sourceSets.main.groovy
      }
      if (project.sourceSets.main.hasProperty("java")) {
        from project.sourceSets.main.java
      }
      classifier = "sources"
    }

    // add source jar artifact
    project.artifacts {
      sources(project.sourceJar) {
        type "source"
      }
    }
  }
}