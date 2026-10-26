pipeline {
    agent any

    stages {

        stage('Build Backend') {
            steps {
                echo 'Building Spring Boot backend...'

                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Verify JAR') {
            steps {
                echo 'Checking backend JAR...'

                sh '''
                    test -f target/msp-0.0.1-SNAPSHOT.jar

                    echo "Backend JAR:"
                    ls -lh target/msp-0.0.1-SNAPSHOT.jar
                '''
            }
        }

        stage('Deploy Backend to Staging') {
            steps {
                echo 'Deploying backend to Windows staging...'

                withCredentials([
                    usernamePassword(
                        credentialsId: 'windows-staging-winrm',
                        usernameVariable: 'WIN_USER',
                        passwordVariable: 'WIN_PASSWORD'
                    )
                ]) {

                    sh '''
                        ansible-playbook \
                          -i /var/lib/jenkins/ansible/backend/inventory.ini \
                          /var/lib/jenkins/ansible/backend/deploy.yml \
                          -e "backend_jar=$WORKSPACE/target/msp-0.0.1-SNAPSHOT.jar" \
                          -e "ansible_user=$WIN_USER" \
                          -e "ansible_password=$WIN_PASSWORD"
                    '''
                }
            }
        }
    }

    post {
        success {
            echo 'Backend build and deployment completed successfully.'
        }

        failure {
            echo 'Backend build or deployment failed.'
        }
    }
}