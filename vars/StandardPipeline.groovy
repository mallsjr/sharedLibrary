def call(body) {

        def config = [:]
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body.delegate = config
        body()


node {
def mvnHome
def AppName
 mvnHome = tool "${config.Maven_tool}"
env.JAVA_HOME = tool "${config.Java_tool}"

stage('SCM-Checkout') {
  echo 'Source Checkout started...'
  try {
   //checkout([$class: 'SubversionSCM', additionalCredentials: [], excludedCommitMessages: '', excludedRegions: '', excludedRevprop: '', excludedUsers: '', filterChangelog: false, ignoreDirPropChanges: false, includedRegions: '', locations: [[credentialsId: 'jenkinsSCM', depthOption: 'infinity', ignoreExternalsOption: true, local: '.', remote: "${config.SCM_url}"]], workspaceUpdater: [$class: 'UpdateUpdater']])

   checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'gitHub', url: 'https://github.com/mallsjr/JenkinsTest.git']]])

  } catch (err) {
   echo err.message
  }
}
 stage('Build') {
  echo 'Checkout successful.Build started...'
  try {
    //withCredentials([usernamePassword(credentialsId: '22f6b51e-82d9-4410-ac04-23aa63494b2f', passwordVariable: 'NexusPassword', usernameVariable: 'NexusUser')]) {
    sh "'${mvnHome}/bin/mvn' -f pom.xml ${config.MVN_build}"
  } catch (err) {
   echo err.message
  }
}
stage('SonarQube Analysis') {
  echo 'Starting SonarQube Analysis'
}
stage('JUnit') {
  echo 'JUnit started...'
}
stage('JaCoCo') {
  echo 'Publishing JaCoCo report..'
}
stage("Findbugs & Checkstyle & PMD") {
  echo 'Publishing FindBugs report..'
}
 stage('Nexus Upload') {
  echo 'Uploading Artifacts to Nexus'
}
}
}
