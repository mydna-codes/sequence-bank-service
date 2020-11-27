pipeline {

    agent any

    environment {
        dockerCredentials = "mydnacodes-docker-user"
        nexusCredentials = "mydnacodes-nexus-user"
        dockerImageTag = "mydnacodes/sequence-bank"
        dockerImage = ""
        version = ""
        commitAuthor = ""
    }

    tools {
        maven "mvn-3.6"
        jdk "jdk-11"
    }

    stages {
        stage("Cloning git") {
            steps {
                git branch: "master",
                    credentialsId: "github",
                    url: "https://github.com/mydna-codes/sequence-bank.git"
            }
        }
        stage("Set environment variables") {
            steps {
                script {
                    pom = readMavenPom file:"pom.xml"
                    version = pom.version
                    sh "git --no-pager show -s --format='%ae' > COMMIT_INFO"
                    commitAuthor = readFile("COMMIT_INFO").trim()
                }
            }
        }
        stage("Packaging application") {
            steps {
                sh "mvn clean package -DskipTests=true"
            }
        }
        stage("Building docker image") {
            steps {
                script {
                    dockerImage = docker.build dockerImageTag
                }
            }
        }
        stage("Publishing docker image") {
            steps {
                script {
                    docker.withRegistry("", dockerCredentials) {
                        dockerImage.push("$version")
                        dockerImage.push("latest")
                    }
                }
            }
        }
        stage("Cleaning up docker images") {
            steps {
                sh "docker rmi $dockerImageTag:$version"
                sh "docker rmi $dockerImageTag:latest"
            }
        }
        stage("Deploying libraries") {
           steps {
               withCredentials([usernamePassword(credentialsId: nexusCredentials, passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                   sh "mvn clean deploy -DskipTests=true -Dnexus.username=$USERNAME -Dnexus.password=$PASSWORD --settings .ci/settings.xml -P lib"
               }
           }
       }
    }
    post {
       success {
           slackSend (color: '#00FF00', message: "SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' from ${commitAuthor} (${env.BUILD_URL})")
       }
       failure {
           slackSend (color: '#FF0000', message: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' from ${commitAuthor} (${env.BUILD_URL})")
       }
    }
}