package com.novoda.staticanalysis.internal.idea

import com.novoda.test.Fixtures
import com.novoda.test.TestProject
import com.novoda.test.TestProjectRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import static com.novoda.test.LogsSubject.assertThat

@RunWith(Parameterized.class)
class IdeaInspectionsIntegrationTest {

    private static final String IDEA_NOT_APPLIED = 'The Idea Inspections plugin is configured but not applied. Please apply the plugin in your build script.'

    private static final String DEFAULT_CONFIG = '''
        inspections {
        }
    '''

    @Parameterized.Parameters
    static def rules() {
        return [
                [TestProjectRule.forKotlinProject(), '0.2.2'],
//                [TestProjectRule.forAndroidKotlinProject(), '0.2.2'],
//                [TestProjectRule.forJavaProject(), '0.2.2'],
        ]*.toArray()
    }

    @Rule
    public final TestProjectRule projectRule
    private final toolsVersion

    IdeaInspectionsIntegrationTest(TestProjectRule projectRule, toolsVersion) {
        this.projectRule = projectRule
        this.toolsVersion = toolsVersion
    }

    @Test
    void shouldNotFailWhenIdeaIsNotConfigured() {
        def result = createProject()
                .withSourceSet('main', Fixtures.IdeaInspections.SOURCES_WITH_WARNINGS)
                .build('evaluateViolations')

        assertThat(result.logs).doesNotContainIdeaViolations()
    }

    @Test
    void shouldNotFailWhenIdeaIsConfiguredButHasNoSources() {
        def result = createProject()
                .withToolsConfig(DEFAULT_CONFIG)
                .build('evaluateViolations')
        
        assertThat(result.logs).doesNotContainIdeaViolations()
    }

    @Test
    void shouldFailBuildOnConfigurationWhenIdeaConfiguredButNotApplied() {
        def result = projectRule.newProject()
                .withToolsConfig(DEFAULT_CONFIG)
                .buildAndFail('evaluateViolations')

        assertThat(result.logs).contains(IDEA_NOT_APPLIED)
    }

    @Test
    void shouldFailWhenIdeaWarningsAreOverThreshold() {
        def result = createProject()
                .withSourceSet('main', Fixtures.IdeaInspections.SOURCES_WITH_WARNINGS)
                .withToolsConfig(DEFAULT_CONFIG)
                .buildAndFail('evaluateViolations')

        assertThat(result.logs).containsIdeaInspectionsViolations(0, 1, 'reports/inspections/main.xml')
    }

    private TestProject createProject() {
        projectRule.newProject()
                .withAdditionalBuildscriptConfiguration("""
                    repositories {
                        maven { url 'https://dl.bintray.com/kotlin/kotlin-dev' }
                    }
                    dependencies {
                        classpath 'org.jetbrains.intellij.plugins:inspection-plugin:$toolsVersion'
                    }
                """)
                .withAdditionalConfiguration("apply plugin: 'org.jetbrains.intellij.inspections'")
                .withPenalty('failFast')
    }
}