pipeline {
    agent {
        label "streamotion-maven-java11"
    }
    environment {
//        ORG = 'customize!project.org'
        ORG = 'fsa-streamotion' // Please do not edit this line! Managed by customize.sh
//        APP_NAME = 'customize!project.name'
        APP_NAME = 'platform-integration-sf-customer-management-api' // Please do not edit this line! Managed by customize.sh
        MVN_BIN = 'mvn --batch-mode --update-snapshots'
    }

    stages {

        stage('Generate Version File') {
            steps {
                container('maven') {
                    generateVersionFile()
                    sh 'echo "Version is $(cat VERSION)"'
                }
            }
        }

        stage('Compile & Test') {
            steps {
                container('maven') {
                    sh "$MVN_BIN versions:set -DnewVersion=\$(cat VERSION)"
                    sh "$MVN_BIN verify -Dtest.unit.skip=false -Dtest.component.skip=true -Dspring-boot.run.profiles=demo"
                }
            }
        }

        stage('Tag Version') {
            when {
                branch 'master'
            }
            steps {
                container('maven') {
                    sh "jx step tag --version \$(cat VERSION) --charts-value-repository $DOCKER_REGISTRY/$ORG/$APP_NAME"

                    script {
                        def releaseVersion = readFile "${env.WORKSPACE}/VERSION"
                        currentBuild.description = releaseVersion
                        currentBuild.displayName = releaseVersion
                    }
                }
            }
        }

        stage('Build and Push Image') {
            when {
                anyOf {
                    branch 'master'
                    branch 'PR-*'
                }
            }
            steps {
                container('maven') {
                    //== Start Of Breaking Change
                    // The below won't work in the old Jenkins (gitpos-production). Ideally we want you to migrate the pipeline to the 1.19 Jenkins, but
                    // if you can't at the moment, then comment it out and add the statement below:
                    //
                    // sh 'export VERSION=$(cat VERSION) && skaffold version && skaffold build -f skaffold.yaml'
                    //
                    sh '''
                       aws sts assume-role-with-web-identity \
                       --role-arn $AWS_ROLE_ARN \
                       --role-session-name ecraccess \
                       --web-identity-token file://\$AWS_WEB_IDENTITY_TOKEN_FILE \
                       --duration-seconds 900 > /tmp/ecr-access.txt
                    '''
                    sh '''
                        set +x
                        export VERSION=$(cat VERSION) && export AWS_ACCESS_KEY_ID=\$(cat /tmp/ecr-access.txt | jq -r '.Credentials.AccessKeyId') \
                        && export AWS_SECRET_ACCESS_KEY=\$(cat /tmp/ecr-access.txt | jq -r '.Credentials.SecretAccessKey')\
                        && export AWS_SESSION_TOKEN=\$(cat /tmp/ecr-access.txt | jq -r '.Credentials.SessionToken')\
                        && set -x\
                        && skaffold version\
                        && skaffold build -f skaffold.yaml
                    '''
                    //== End Of Breaking Change
                    sh "jx step post build --image $DOCKER_REGISTRY/$ORG/$APP_NAME:\$(cat VERSION)"
                }
            }
        }

        stage('Deploy Preview') {
            when {
                branch 'PR-*'
            }
            environment {
                PREVIEW_VERSION = getPreviewVersion()
                PREVIEW_NAMESPACE = getPreviewNameSpace()
            }
            steps {
                container('maven') {
                    dir('charts/preview') {
                        sh "export PREVIEW_VERSION=$PREVIEW_VERSION && make preview"

                        script {
                            try {
                                sh "export PREVIEW_VERSION=$PREVIEW_VERSION && jx preview --app preview --name=$APP_NAME --namespace=$PREVIEW_NAMESPACE --dir ../.."
                            }
                            catch(Exception e) {
                                /* jx preview is doing some check after deploy which fails for SSL
                                 * interestingly it only happens on the 1st preview build request, all subsequent builds pass thru without problem!!!
                                 * until there is a better alternative....
                                 * the first build fails with something like:
                                 * error: error checking if preview application http://coldrock-k8s.coldrock-k8s-pr-39.dev.cluster.blahsports-gitops-prod.com.au is available: Get https://coldrock-k8s.coldrock-k8s-pr-39.dev.cluster.blahsports-gitops-prod.com.au/: x509: certificate is valid for *.cluster.blahsports-gitops-prod.com.au, *.dev.cluster.blahsports-gitops-prod.com.au, *.open.cluster.blahsports-gitops-prod.com.au*/
                                echo "preview gen failed but URL is probably there (seems like the url generation takes time and jx is too quick to test)....Let the next check URL check step to do test it"
                            }
                        }
                        sh """kubectl patch namespace ${PREVIEW_NAMESPACE.toLowerCase()} -p '{"metadata": {"annotations":{"iam.amazonaws.com/permitted":".*"}}}'"""
                    }

                    script {
                        currentBuild.description = "$APP_NAME.$PREVIEW_NAMESPACE"
                    }
                }
            }
        }

        stage('Component Test') {
            when {
                branch 'PR-*'
            }
            environment {
                PREVIEW_NAMESPACE = getPreviewNameSpace()
                PREVIEW_URL = "http://preview.${PREVIEW_NAMESPACE}.svc.cluster.local"
                WIREMOCK_URI = "http://wiremock-server.${PREVIEW_NAMESPACE}.svc.cluster.local"
            }
            steps {
                container('maven') {
                    waitForPreviewEnvironment PREVIEW_URL
                    sh "$MVN_BIN failsafe:integration-test failsafe:verify -Dtest.unit.skip=true -Dtest.component.skip=false -Dtest.component.targetUri=$PREVIEW_URL -Dspring-boot.run.profiles=preview -Dtest.component.wiremockUri=$WIREMOCK_URI"
                }
            }
        }

        stage('Promote to Environments') {
            when {
                branch 'master'
            }
            steps {
                container('maven') {
                    sh "mv charts/helm-release  charts/$APP_NAME"

                    dir("charts/$APP_NAME") {
                        sh "jx step changelog --generate-yaml=false --version \$(cat ../../VERSION)"

                        sh "export RELEASE_VERSION=\$(cat ../../VERSION) && make release"

                        // promote through all 'Staging' promotion Environments
                        sh "jx promote -b --no-poll=true --helm-repo-url=$CHART_REPOSITORY --no-poll=true --no-merge=true --no-wait=true --env=blahtel-integration-customer-sf-staging --version \$(cat ../../VERSION)"

                        // promote through all 'UAT' promotion Environments
                        sh "jx promote -b --no-poll=true --helm-repo-url=$CHART_REPOSITORY --no-poll=true --no-merge=true --no-wait=true --env=blahtel-integration-customer-sf-uat --version \$(cat ../../VERSION)"

                        //promote through all 'Production' promotion Environments
                        //sh "jx promote -b --no-poll=true --helm-repo-url=$CHART_REPOSITORY --no-poll=true --no-merge=true --no-wait=true --env=<my-cool-production-environment> --version \$(cat ../../VERSION)"
                    }

                }
            }
        }
    }

    post {
        always {
            container('maven') {
                junit(
                        testResults: 'target/surefire-reports/**/*.xml,target/failsafe-reports/**/*.xml',
                        allowEmptyResults: true
                )
                archiveArtifacts(
                        artifacts: 'target/surefire-reports/**' +
                                ',target/failsafe-reports/**' +
                                ',target/jacoco/**' +
                                ',target/jacoco-it/**' +
                                ',target/checkstyle-*.xml' +
                                ',target/spotbugsXml.xml',
                        allowEmptyArchive: true
                )
                publishAllureResults()
            }
            cleanWs()
        }
    }
}

String getPreviewNameSpace() {
    // The K8S DNS entry is going to be "preview.<APP_NAME>-<PR_NUMBER>.svc.cluster.local", and can't be
    // longer than 63 characters. This means The maximum length of the namespace is 37 characters. To ensure this, we
    // will trim the APP_NAME as much as possible. All non-alphanumeric characters, with the exception
    // of the hyphen (-), will be removed from the APP_NAME, and some words will be abbreviated.

    def appName = cleanAppName()
    def prNumber = prNumber()

    if (appName.length() + prNumber.length() > 36) {
        appName = appName.substring(0, Math.min(appName.length(), 36 - prNumber.length()))
    }

    return "$appName-$prNumber".toLowerCase()
}

void generateVersionFile() {
    if (BRANCH_NAME == 'master') {
        sh "git config --global credential.helper store && jx step git credentials"
        sh "echo \$(jx-release-version) > VERSION"
    } else {
        sh "echo $previewVersion > VERSION"
    }
}

String getPreviewVersion() {
    // This is used to find the JIRA number in the branch.
    def matcher = BRANCH_NAME =~ /((?<!([A-Za-z]{1,10})-?)[A-Za-z]+-\d+)/

    if (BRANCH_NAME.startsWith('PR-')) {
        matcher.find() // This will prevent the script from thinking the jira number is PR-XX
    }

    def version
    if (matcher.find()) {
        version = "0.0.0-${matcher.group().toUpperCase()}-PR-${prNumber()}-$BUILD_NUMBER-SNAPSHOT"
    } else {
        version = "0.0.0-PR-${prNumber()}-$BUILD_NUMBER-SNAPSHOT"
    }
    return version
}

String cleanAppName() {
    def abbreviations = [
            billing    : 'bill',
            broadcast  : 'bcst',
            commerce   : 'com',
            content    : 'cnt',
            digital    : 'digi',
            blahsports  : 'fsa',
            library    : 'lib',
            platform   : 'pfm',
            service    : 'svc',
            streamtech : 'stec',
            streamotion: 'sm',
            workflow   : 'wfw',
            worker     : 'wkr',
            workers    : 'wkr'
    ]

    ((String) APP_NAME).replaceAll(/[^a-zA-Z0-9\-]/, '')
            .tokenize('-')
            .collect { abbreviations.get(it) ?: it }
            .join('-')
}

String prNumber() {
    def matcher = BRANCH_NAME =~ /^PR-(\d+)$/
    matcher.matches() ? matcher.group(1) : ''
}

void waitForPreviewEnvironment(url) {
    echo 'Waiting for preview environment to be up...'

    waitFor {
        script {
            try {
                echo "Checking if $url is up..."
                def checkCommand = """wget -qO- $url/actuator/health | jq -r '.status' """
                def commandOutput = sh script: checkCommand, returnStdout: true
                echo "Response from command: $commandOutput"
                return 'UP' == commandOutput?.toString()?.trim()
            } catch (Exception e) {
                echo "Check failed due to: $e"
                return false
            }
        }
    }

    echo 'Preview environment is up.'
}

void waitFor(Integer timeoutInMinutes = 3, Closure until) {
    final def timeoutInMillis = System.currentTimeMillis() + (timeoutInMinutes * 60000)

    println "Timeout out in $timeoutInMillis..."

    while (System.currentTimeMillis() < timeoutInMillis) {
        println "Running command at ${System.currentTimeMillis()}..."

        def success = timeout(time: 10, unit: 'SECONDS', until)

        if (success) {
            println "Command executed successfully."
            return
        }

        println "Command execution failed... Retrying"
        sleep 2
    }

    error("Command timed out...")
}

void publishAllureResults() {
    def results = []
    if (fileExists('target/allure-results')) {
        results << [path: 'target/allure-results']
        sh 'chmod -R o+xw target/allure-results'
    }
    if (fileExists('target/allure-results-it')) {
        results << [path: 'target/allure-results-it']
        sh 'chmod -R o+xw target/allure-results-it'
    }
    if (results) {
        try {
            sh 'chmod -R o+xw target/allure-results'
            script {
                allure([
                        includeProperties: false,
                        jdk              : '',
                        properties       : [],
                        reportBuildPolicy: 'ALWAYS',
                        results          : results
                ])
            }
        } catch (error) {
            println "Error publishing allure reports due to: ${error}"
        }
    }

}
