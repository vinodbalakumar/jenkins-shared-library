package org.vinod

class DeployPipeline implements Serializable {

    def runPipeline(script, String dockerRepo, String kubeconfigId, String awsCredentialsId) {
        script.pipeline {
            agent any
            environment {
                DOCKER_REPO = dockerRepo
                KUBECONFIG_CREDENTIALS_ID = kubeconfigId
                AWS_CREDENTIALS_ID = awsCredentialsId
            }
            stages {
                stage('Checkout') {
                    steps {
                        script.checkout(script.scm)
                    }
                }

                stage('Terraform Init & Plan') {
                    steps {
                        script.withCredentials([script.usernamePassword(credentialsId: AWS_CREDENTIALS_ID, usernameVariable: 'AWS_ACCESS_KEY', passwordVariable: 'AWS_SECRET_KEY')]) {
                            script.sh '''
                                cd terraform/${env.BRANCH_NAME}
                                terraform init
                                terraform plan -var-file="env/${env.BRANCH_NAME}.tfvars"
                            '''
                        }
                    }
                }

                stage('Terraform Apply') {
                    steps {
                        script.withCredentials([script.usernamePassword(credentialsId: AWS_CREDENTIALS_ID, usernameVariable: 'AWS_ACCESS_KEY', passwordVariable: 'AWS_SECRET_KEY')]) {
                            script.sh '''
                                cd terraform/${env.BRANCH_NAME}
                                terraform apply -auto-approve -var-file="env/${env.BRANCH_NAME}.tfvars"
                            '''
                        }
                    }
                }

                stage('Build Docker Image') {
                    steps {
                        script.dockerImage = script.docker.build("${script.DOCKER_REPO}:${script.env.BUILD_NUMBER}")
                    }
                }

                stage('Push Docker Image') {
                    steps {
                        script.docker.withRegistry('https://index.docker.io/v1/', 'docker-credentials') {
                            script.dockerImage.push()
                        }
                    }
                }

                stage('Deploy to Kubernetes') {
                    steps {
                        script.deployToKubernetes(script)
                    }
                }
            }

            post {
                always {
                    script.cleanWs()
                }
            }
        }
    }

    def deployToKubernetes(script) {
        script.echo "Deploying to environment: ${script.env.BRANCH_NAME}"
        def environment = getEnvironment(script.env.BRANCH_NAME)
        script.withKubeConfig([credentialsId: "${script.KUBECONFIG_CREDENTIALS_ID}", serverUrl: "https://eks-cluster-${environment}.aws.com"]) {
            script.sh "kubectl set image deployment/spring-boot-app spring-boot-app=${script.DOCKER_REPO}:${script.env.BUILD_NUMBER} --namespace=${environment}"
        }
    }

    def getEnvironment(String branchName) {
        switch (branchName) {
            case "develop":
                return "dev"
            case "staging":
                return "staging"
            case "master":
                return "prod"
            default:
                return "dev"
        }
    }
}
