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
   checkout([$class: 'SubversionSCM', additionalCredentials: [], excludedCommitMessages: '', excludedRegions: '', excludedRevprop: '', excludedUsers: '', filterChangelog: false, ignoreDirPropChanges: false, includedRegions: '', locations: [[credentialsId: 'jenkinsSCM', depthOption: 'infinity', ignoreExternalsOption: true, local: '.', remote: "${config.SCM_url}"]], workspaceUpdater: [$class: 'UpdateUpdater']])
  } catch (err) {
   echo err.message
  }
}

 stage('Build') {
  echo 'Checkout successful.Build started...'
  try {
    withCredentials([usernamePassword(credentialsId: '22f6b51e-82d9-4410-ac04-23aa63494b2f', passwordVariable: 'NexusPassword', usernameVariable: 'NexusUser')]) {
    sh "'${mvnHome}/bin/mvn' -f pom.xml ${config.MVN_build}"
  }
  } catch (err) {
   echo err.message
  }
}

 stage('SonarQube Analysis') {
  echo 'Starting SonarQube Analysis'
  withSonarQubeEnv('SonarQube') {
   sh "'${mvnHome}/bin/mvn' -f pom.xml ${config.SonarQube_Analysis}"
  }
}

stage('JUnit') {
  echo 'JUnit started...'
  try {
   //junit healthScaleFactor: "${config.junit_healthScaleFactor}", testResults: "${config.junit_path}"
   junit "${config.junit_path}"
   echo 'JUnit Results generated'
  } catch (err) {
   echo err.message
  }
}

stage('JaCoCo') {
  echo 'Publishing JaCoCo report..'
  try {
   //With Threshold
   //step([$class: 'JacocoPublisher', changeBuildStatus: true, maximumBranchCoverage: "${jacoco_maximumBranchCoverages}", maximumClassCoverage: "${jacoco_maximumClassCoverage}", maximumComplexityCoverage: "${jacoco_maximumComplexityCoverage}", maximumInstructionCoverage: "${jacoco_maximumInstructionCoverage}", maximumLineCoverage: "${jacoco_maximumLineCoverage}", maximumMethodCoverage: "${jacoco_maximumMethodCoverage}", minimumBranchCoverage: "${jacoco_minimumBranchCoverage}", minimumClassCoverage: "${jacoco_minimumClassCoverage}", minimumComplexityCoverage: "${jacoco_minimumComplexityCoverage}", minimumInstructionCoverage: "${jacoco_minimumInstructionCoverage}", minimumLineCoverage: "${jacoco_minimumLineCoverage}", minimumMethodCoverage: "${jacoco_minimumMethodCoverage}"])
   //Without Threshold
     step([$class: 'JacocoPublisher'])
  } catch (err) {
   echo err.message
  }
}

stage("Findbugs & Checkstyle & PMD") {
parallel  'Findbugs': {
  echo 'Publishing Findbugs report...'
  try {
  //with threshold
    step([$class: 'FindBugsPublisher', canComputeNew: false, defaultEncoding: '', excludePattern: '', failedTotalHigh: "${config.findbugs_failedTotalHigh}", failedTotalLow: "${config.findbugs_failedTotalLow}", failedTotalNormal: "${config.findbugs_failedTotalNormal}", healthy: "${config.findbugs_healthy}", includePattern: '', pattern: "${config.findbugs_pattern}", unHealthy: "${config.findbugs_unHealthy}", unstableTotalHigh: "${config.findbugs_unstableTotalHigh}", unstableTotalLow: "${config.findbugs_unstableTotalLow}", unstableTotalNormal: "${config.findbugs_unstableTotalNormal}"])
  //without threshold
  //step([$class: 'FindBugsPublisher', canComputeNew: false, defaultEncoding: '', excludePattern: '', healthy: '', includePattern: '', pattern: '**/findbugsXml.xml', unHealthy: ''])
  } catch (err) {
   echo err.message
  }
},'Checkstyle':{
  echo 'Publishing Checkstyle report...'
  try {
   //with threshold
     checkstyle canComputeNew: false, defaultEncoding: '', failedTotalAll: "${config.checkstyle_failedTotalAll}", failedTotalHigh: "${config.checkstyle_failedTotalHigh}", failedTotalLow: "${config.checkstyle_failedTotalLow}", failedTotalNormal: "${config.checkstyle_failedTotalNormal}", healthy: "${config.checkstyle_healthy}", pattern: "${config.checkstyle_pattern}", unHealthy: "${config.checkstyle_unHealthy}", unstableTotalAll: "${config.checkstyle_unHealthy}", unstableTotalHigh: "${config.checkstyle_unstableTotalHigh}", unstableTotalLow: "${config.checkstyle_unstableTotalLow}", unstableTotalNormal: "${config.checkstyle_unstableTotalNormal}"
   //without threshold
   //checkstyle canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/checkstyle-result.xml', unHealthy: ''
  } catch (err) {
   echo err.message
  }
},'PMD':{
echo 'Publishing PMD results...'
  try {
    //with threshold
      step([$class: 'PmdPublisher', canComputeNew: false, defaultEncoding: '', healthy: "${config.PMD_healthy}", pattern: "${config.PMD_pattern}", unHealthy: "${config.PMD_unHealthy}", unstableTotalHigh: "${config.PMD_unstableTotalHigh}", unstableTotalLow: "${config.PMD_unstableTotalLow}", unstableTotalNormal: "${config.PMD_unstableTotalNormal}"])
                //without threshold
                //step([$class: 'PmdPublisher', canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/target/pmd.xml', unHealthy: ''])
  } catch (err) {
   echo err.message
  }
}
}
 stage('Nexus Upload') {
  echo 'Uploading Artifacts to Nexus'
  try {
      if (currentBuild.result != 'FAILURE')
      {
        withCredentials([usernamePassword(credentialsId: '22f6b51e-82d9-4410-ac04-23aa63494b2f', passwordVariable: 'NexusPassword', usernameVariable: 'NexusUser')]) {
        sh "'${mvnHome}/bin/mvn' -f pom.xml ${config.Nexus_Deploy}"

                    echo 'Nexus Artifacts Uploaded, Starting archiving artifacts...'
        archiveArtifacts 'target/*.war'
       }
      }
      else{
        echo 'Build failure, Can not upload to Nexus'
      }
} catch (err) {
   echo err.message
  }
}
}
}
