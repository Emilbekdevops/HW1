def k8slabel = "jenkins-pipeline-${UUID.randomUUID().toString()}"
def slavePodTemplate = """
      metadata:
        labels:
          k8s-label: ${k8slabel}
        annotations:
          jenkinsjoblabel: ${env.JOB_NAME}-${env.BUILD_NUMBER}
      spec:
        affinity:
          podAntiAffinity:
            requiredDuringSchedulingIgnoredDuringExecution:
            - labelSelector:
                matchExpressions:
                - key: component
                  operator: In
                  values:
                  - jenkins-jenkins-master
              topologyKey: "kubernetes.io/hostname"
        containers:
        - name: githubtoken
          image: fuchicorp/buildtools:latest
          imagePullPolicy: IfNotPresent
          command:
          - cat
          tty: true
          volumeMounts:
            - mountPath: /var/run/docker.sock
              name: docker-sock
        serviceAccountName: default
        securityContext:
          runAsUser: 0
          fsGroup: 0
        volumes:
          - name: docker-sock
            hostPath:
              path: /var/run/docker.sock
    """
    properties([
      parameters([
        choice(choices: ['fsadykov', 'antonbabenko', 'mojombo', 'defunkt', 'Emilbekdevops'], 
        description: 'Select the user', 
        name: 'github_user')
          ])
      ])
    podTemplate(name: k8slabel, label: k8slabel, yaml: slavePodTemplate, showRawYaml: false) {
      node(k8slabel){
        container( "githubtoken") {
        withCredentials([string(credentialsId: 'adminGittoken', variable: 'adminGittoken')]) {
           stage("Check User Credentials") {
              sh  'curl -H "Authorization: token $adminGittoken" -X GET "https://api.github.com/users" -I |grep "HTTP/1.1 200 OK" '
          }
           stage("Get User Credentials"){
              sh  "curl -H \"Authorization: token $adminGittoken \" -X GET 'https://api.github.com/users/${github_user}' "
           }
          }
        }
      }
    }