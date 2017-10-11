pipeline {
  agent any
  stages {
    stage('Build') {
      steps {
        sh '''which mvn
pwd
echo $PATH
mvn clean test'''
      }
    }
    stage('Report') {
      steps {
        junit '**/target/surefire-reports/*'
      }
    }
    stage('Cleanup') {
      steps {
        sh 'mvn clean'
      }
    }
    stage('End Credits') {
      steps {
        echo 'Done!'
      }
    }
  }
}