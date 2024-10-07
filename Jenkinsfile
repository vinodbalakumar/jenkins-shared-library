pipeline {
    agent any
    
    stages {
        stage('Checkout') {
            steps {
                git 'https://github.com/vinodbalakumar/employee-portal.git'
            }
        }
        
        stage('Build and Deploy') {
            steps {
                sh 'jenkins-jobs/build-and-deploy.sh'
            }
        }
    }
}