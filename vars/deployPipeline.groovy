def call(String dockerRepo, String kubeconfigId, String awsCredentialsId) {
    def deployPipeline = new org.vinod.DeployPipeline()
    deployPipeline.runPipeline(this, dockerRepo, kubeconfigId, awsCredentialsId)
}
