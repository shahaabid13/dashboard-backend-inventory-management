pipeline {
    agent any

    stages {

        stage('Build Backend') {
            steps {
                echo 'Building Spring Boot backend...'
                sh './mvnw clean package -DskipTests'
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
    }

    post {
        success {
            echo 'Backend build completed successfully.'
        }

        failure {
            echo 'Backend build failed.'
        }
    }
}
