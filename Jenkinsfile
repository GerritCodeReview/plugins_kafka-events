pipeline {
    options { skipDefaultCheckout true }
    agent { label 'bazel-debian' }
    stages {
        stage('Checkout') {
            steps {
                sh "git clone -b ${env.GERRIT_BRANCH} https://gerrit.googlesource.com/plugins/kafka-events"
                sh "cd kafka-events && git fetch origin refs/changes/${BRANCH_NAME} && git merge FETCH_HEAD"
            }
        }
        stage('Formatting') {
            steps {
                gerritCheck (checks: ['gerritforge:plugins-kafka-events-code-style': 'RUNNING'], url: "${env.BUILD_URL}console")
                sh "find kafka-events -name '*.java' | xargs /home/jenkins/format/google-java-format-1.7 -i"
                script {
                    def formatOut = sh (script: 'cd kafka-events && git status --porcelain', returnStdout: true)
                    if (formatOut.trim()) {
                        def files = formatOut.split('\n').collect { it.split(' ').last() }
                        files.each { gerritComment path:it, message: 'Needs reformatting with GJF' }
                        gerritCheck (checks: ['gerritforge:plugins-kafka-events-code-style': 'FAILED'], url: "${env.BUILD_URL}console")
                    } else {
                        gerritCheck (checks: ['gerritforge:plugins-kafka-events-code-style': 'SUCCESSFUL'], url: "${env.BUILD_URL}console")
                    }
                }
            }
        }
        stage('build') {
             environment {
                 DOCKER_HOST = """${sh(
                     returnStdout: true,
                     script: "/sbin/ip route|awk '/default/ {print  \"tcp://\"\$3\":2375\"}'"
                 )}"""
            }
            steps {
                gerritCheck (checks: ['gerritforge:plugins-kafka-events-build-test': 'RUNNING'], url: "${env.BUILD_URL}console")
                sh 'git clone --recursive -b $GERRIT_BRANCH https://gerrit.googlesource.com/gerrit'
                sh 'cd gerrit/plugins && ln -sf ../../kafka-events . && ln -sf kafka-events/external_plugin_deps.bzl .'
                dir ('gerrit') {
                    sh 'bazelisk build plugins/kafka-events'
                    sh 'bazelisk test --test_env DOCKER_HOST=$DOCKER_HOST plugins/kafka-events:kafka_events_tests'
                }
            }
        }
    }
    post {
        success {
          gerritCheck (checks: ['gerritforge:plugins-kafka-events-build-test': 'SUCCESSFUL'], url: "${env.BUILD_URL}console")
        }
        unstable {
          gerritCheck (checks: ['gerritforge:plugins-kafka-events-build-test': 'FAILED'], url: "${env.BUILD_URL}console")
        }
        failure {
          gerritCheck (checks: ['gerritforge:plugins-kafka-events-build-test': 'FAILED'], url: "${env.BUILD_URL}console")
        }
    }
}
