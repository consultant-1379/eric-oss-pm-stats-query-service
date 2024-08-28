#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/local_ruleset.yaml"
def ci_ruleset = "ci/common_ruleset2.0.yaml"

try {
    stage ('Custom Upload License Agreement') {
        withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
            sh "${bob} -r ${ruleset} upload-license-agreement-ci-internal"
        }
    }
} catch (e) {
    throw e
} finally {
    archiveArtifacts allowEmptyArchive: true, artifacts: '**/design-rule-check-report.*, **/target/site/checkstyle.html, **/target/site/pmd.html, **/target/site/cpd.html'
}

try {
    stage ('Custom Upload Service Ports') {
        withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
            sh "${bob} -r ${ruleset} upload-service-ports-ci-internal"
        }
    }
} catch (e) {
    throw e
} finally {
    archiveArtifacts allowEmptyArchive: true, artifacts: '**/design-rule-check-report.*, **/target/site/checkstyle.html, **/target/site/pmd.html, **/target/site/cpd.html'
}

try {
    stage ('Custom Upload Structured Data') {
        withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]) {
            sh "${bob} -r ${ruleset} generate-structured-data-ci-internal"
            sh "${bob} -r ${ruleset} validate-structured-data"
            sh "${bob} -r ${ruleset} upload-structured-data-ci-internal"
        }
    }
} catch (e) {
    throw e
} finally {
    archiveArtifacts allowEmptyArchive: true, artifacts: '**/design-rule-check-report.*, **/target/site/checkstyle.html, **/target/site/pmd.html, **/target/site/cpd.html'
}

if (!env.RELEASE) {
    try {
        stage('Custom Upload Marketplace Documentation') {
            withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS'),
                         string(credentialsId: 'PMSQS_ADP_PORTAL_API_KEY', variable: 'ADP_PORTAL_API_KEY')]) {
                // upload development version
               script {
                       echo "Marketplace upload"
                       sh "${bob} -r ${ruleset} marketplace-upload-dev"
               }
            }
        }
    } catch (e) {
        throw e
    } finally {
        archiveArtifacts allowEmptyArchive: true, artifacts: '**/design-rule-check-report.*, **/target/site/checkstyle.html, **/target/site/pmd.html, **/target/site/cpd.html'
    }
}