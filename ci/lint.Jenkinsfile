#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/local_ruleset.yaml"
def ci_ruleset = "ci/common_ruleset2.0.yaml"

try {
    stage('Custom Lint') {
        parallel(
            "lint markdown": {
                sh "${bob} -r ${ci_ruleset} lint:markdownlint lint:vale"
            },
            "lint helm": {
                sh "${bob} -r ${ci_ruleset} lint:helm"
            },
            "lint helm design rule checker": {
                sh "${bob} -r ${ruleset} lint:helm-chart-check"
            },
            "lint code": {
                sh "${bob} -r ${ci_ruleset} lint:license-check"
            },
            "lint static-checking": {
                sh "${bob} -r ${ruleset} lint:static-checking"
                archiveArtifacts([
                    allowEmptyArchive: true,
                    artifacts: '**/target/site/checkstyle.html, **/target/site/pmd.html, **/target/site/cpd.html'
                ])
            },
            "lint OpenAPI spec": {
                sh "${bob} -r ${ci_ruleset} lint:oas-bth-linter"
            },
            "lint metrics": {
                sh "${bob} -r ${ruleset} lint:metrics-check"
            }
        )
    }
} catch (e) {
    throw e
} finally {
    archiveArtifacts allowEmptyArchive: true, artifacts: '**/design-rule-check-report.*, **/target/site/checkstyle.html, **/target/site/pmd.html, **/target/site/cpd.html'
}

stage('Custom Build') {
    sh "${bob} -r ${ruleset} build"
}

try {
    stage('Custom Test') {
        withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS')]){
            sh "${bob} -r ${ruleset} test"
        }
    }
} catch (e) {
    throw e
} finally {
    archiveArtifacts allowEmptyArchive: true, artifacts: '**/design-rule-check-report.*, **/target/site/checkstyle.html, **/target/site/pmd.html, **/target/site/cpd.html'
}

stage('Custom Generate Docs') {
    sh "${bob} -r ${ruleset} generate-docs"
    archiveArtifacts "build/doc/**/*.*"
    publishHTML(target: [
        allowMissing: false,
        alwaysLinkToLastBuild: false,
        keepAll: true,
        reportDir: 'build/doc',
        reportFiles: 'CTA_api.html',
        reportName: 'Documentation'
    ])
}

stage('Custom Open API Spec') {
    sh "${bob} -r ${ci_ruleset} rest-2-html:check-has-open-api-been-modified"
    script {
        def val = readFile '.bob/var.has-openapi-spec-been-modified'
        if (val.trim().equals("true")) {
            sh "${bob} -r ${ci_ruleset} rest-2-html:zip-open-api-doc"
            sh "${bob} -r ${ci_ruleset} rest-2-html:generate-html-output-files"

            manager.addInfoBadge("OpenAPI spec has changed. Review the Archived HTML Output files: rest2html*.zip")
            archiveArtifacts allowEmptyArchive: true, artifacts: "rest_conversion_log.txt, rest2html*.zip"
            echo "Sending email to CPI document reviewers distribution list: ${env.EMAIL}"
            try {
                mail to: "${env.EMAIL}",
                    from: "${env.GERRIT_PATCHSET_UPLOADER_EMAIL}",
                    cc: "${env.GERRIT_PATCHSET_UPLOADER_EMAIL}",
                    subject: "[${env.JOB_NAME}] OpenAPI specification has been updated and is up for review",
                    body: "The OpenAPI spec documentation has been updated.<br><br>" +
                    "Please review the patchset and archived HTML output files (rest2html*.zip) linked here below:<br><br>" +
                    "&nbsp;&nbsp;Gerrit Patchset: ${env.GERRIT_CHANGE_URL}<br>" +
                    "&nbsp;&nbsp;HTML output files: ${env.BUILD_URL}artifact <br><br><br><br>" +
                    "<b>Note:</b> This mail was automatically sent as part of the following Jenkins job: ${env.BUILD_URL}",
                    mimeType: 'text/html'
            } catch (Exception e) {
                echo "Email notification was not sent."
                print e
            }
        }
    }
}
