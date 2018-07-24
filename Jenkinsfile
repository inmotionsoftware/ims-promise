pipeline {
  agent any
  stages {
    stage('Prepare Environment') {
      parallel {
        stage('Update Android SDK') {
          steps {
            sh '''#!/bin/bash

            # check if ANDROID_HOME is set
            printf "%s\\n" "${ANDROID_HOME:?You must set ANDROID_HOME}"
            echo "ANDROID_HOME=${ANDROID_HOME}"

            # download the Android sdkmanager to the user directory
            export ANDROID_TOOLS_DIR=~/tools

            if [ -d "$ANDROID_TOOLS_DIR" ]
              then
                echo "using Android tools at $ANDROID_TOOLS_DIR"
              else
                echo "Android tools directory not found in $ANDROID_TOOLS_DIR\\ndownloading tools"
                pushd $PWD
                mkdir -p $ANDROID_TOOLS_DIR
                cd $ANDROID_TOOLS_DIR
                curl -H "Accept: application/zip" -O https://dl.google.com/android/repository/sdk-tools-darwin-4333796.zip
                unzip sdk-tools-darwin-*.zip
                mv tools/* .
                rm -r tools/
                rm sdk-tools-darwin-*.zip
                popd
            fi

            $ANDROID_TOOLS_DIR/bin/sdkmanager --update --sdk_root=$ANDROID_HOME
            yes|$ANDROID_TOOLS_DIR/bin/sdkmanager --licenses --sdk_root=$ANDROID_HOME
            '''
          }
        }
        stage('Configure Gradle') {
          steps {
            sh '''#!/bin/bash

            # create user level gradle properties
            if [ ! -f ~/.gradle/gradle.properties ]
              then
                mkdir -p ~/.gradle
                echo 'org.gradle.daemon=false' >> ~/.gradle/gradle.properties
                echo 'org.gradle.parallel=true' >> ~/.gradle/gradle.properties
                echo 'org.gradle.jvmargs=-Xmx4608m -XX:MaxPermSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8 -Dorg.jenkinsci.plugins.durabletask.BourneShellScript.HEARTBEAT_CHECK_INTERVAL=300' >> ~/.gradle/gradle.properties
            fi
            '''
          }
        }
        stage('echo env') {
          steps {
            sh '''#!/bin/bash
            echo "url = $ARTIFACTORY_URL"
            echo "username = $ARTIFACTORY_USR"
            echo "password = $ARTIFACTORY_PSW"
            '''
          }
        }
      }
    }
    stage('Clone promisekt') {
      steps {
        dir('promisekt') {
          git branch: 'build/artifactory', credentialsId: 'github', url: 'https://github.com/inmotionsoftware/ims-promise/'
        }
      }
    }
//    stage('Unit test') {
//      steps {
//        dir('promisekt') {
//          sh './gradlew testDebugUnitTest testDebugUnitTest'
//          junit '**/TEST-*.xml'
//        }
//      }
//    }
    stage('Build APK') {
      steps {
        dir('promisekt') {
          sh './gradlew promisekt:artifactoryPublish'
          archiveArtifacts '**/*.apk'
        }
      }
    }
    stage('Static analysis') {
      steps {
        dir('promisekt') {
          sh './gradlew lintDebug'
          androidLint pattern: '**/lint-results-*.xml'
          archiveArtifacts '**/lint-results-*.html'
          archiveArtifacts '**/lint-results-*.xml'
        }
      }
    }
  }
  environment {
    ANDROID_HOME = '/var/jenkins_home/.android/cache/sdk/'
    ARTIFACTORY_URL = 'http://10.0.5.201:8081/artifactory/'
    ARTIFACTORY = credentials('ARTIFACTORY')
  }
  post {
    failure {
      mail(to: 'vude@inmotionsoftware.com', subject: 'Oops!', body: "Build ${env.BUILD_NUMBER} failed; ${env.BUILD_URL}")
    }
  }
  options {
    skipDefaultCheckout true
    skipStagesAfterUnstable()
  }
}