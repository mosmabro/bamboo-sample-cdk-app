import com.atlassian.bamboo.specs.api.BambooSpec;
import com.atlassian.bamboo.specs.api.builders.AtlassianModule;
import com.atlassian.bamboo.specs.api.builders.BambooOid;
import com.atlassian.bamboo.specs.api.builders.deployment.Deployment;
import com.atlassian.bamboo.specs.api.builders.deployment.Environment;
import com.atlassian.bamboo.specs.api.builders.deployment.ReleaseNaming;
import com.atlassian.bamboo.specs.api.builders.permission.DeploymentPermissions;
import com.atlassian.bamboo.specs.api.builders.permission.EnvironmentPermissions;
import com.atlassian.bamboo.specs.api.builders.permission.PermissionType;
import com.atlassian.bamboo.specs.api.builders.permission.Permissions;
import com.atlassian.bamboo.specs.api.builders.plan.PlanIdentifier;
import com.atlassian.bamboo.specs.api.builders.task.AnyTask;
import com.atlassian.bamboo.specs.builders.task.ArtifactDownloaderTask;
import com.atlassian.bamboo.specs.builders.task.CleanWorkingDirectoryTask;
import com.atlassian.bamboo.specs.builders.task.DownloadItem;
import com.atlassian.bamboo.specs.builders.task.ScriptTask;
import com.atlassian.bamboo.specs.util.BambooServer;
import com.atlassian.bamboo.specs.util.MapBuilder;

@BambooSpec
public class PlanSpec {
    
    public Deployment rootObject() {
        final Deployment rootObject = new Deployment(new PlanIdentifier("SAM", "MAIN")
                .oid(new BambooOid("1wue09d566yv7")),
            "deployment-plan")
            .oid(new BambooOid("1wuqh8ubvop35"))
            .description("deployment plan")
            .releaseNaming(new ReleaseNaming("release-5")
                    .autoIncrement(true))
            .environments(new Environment("dev")
                    .description("dev environment")
                    .tasks(new CleanWorkingDirectoryTask(),
                        new ArtifactDownloaderTask()
                            .description("Download release contents")
                            .artifacts(new DownloadItem()
                                    .allArtifacts(true)),
                        new AnyTask(new AtlassianModule("net.utoolity.atlassian.bamboo.tasks-for-aws:aws.cloudformation.stack"))
                            .description("[Bootstrap] preparing the deployment env")
                            .configuration(new MapBuilder()
                                    .put("changeSetDescription", "")
                                    .put("stackPolicyURL", "")
                                    .put("stackPolicyDuringUpdateURL", "")
                                    .put("ignoreNoOpUpdateExceptionUpdate", "true")
                                    .put("awsIamRoleAgentsArn", "")
                                    .put("ignoreNoopCreateChangeSetFailure", "")
                                    .put("stackName", "bamboo-assets-stack")
                                    .put("pluginVersionOnSave", "2.20.2")
                                    .put("enableIAM", "")
                                    .put("ignoreNoOpUpdateExceptionCreate", "true")
                                    .put("changeSetName", "")
                                    .put("resourceAction", "Create")
                                    .put("pluginConfigVersionOnSave", "11")
                                    .put("templateParameters", "")
                                    .put("roleArn", "")
                                    .put("resourceRegionVariable", "")
                                    .put("stackPolicyBody", "")
                                    .put("changeSetType", "CREATE_OR_UPDATE")
                                    .put("doNotFailIfNotExists", "")
                                    .put("templateParametersJson", "")
                                    .put("createIfNotExists", "")
                                    .put("stackNameOrId", "")
                                    .put("stackPolicyDuringUpdateBody", "")
                                    .put("changeSetNameOrArn", "")
                                    .put("capabilities", "[]")
                                    .put("secretKey", "BAMSCRT@0@0@bG5KkO3j7/8pXd5215pVaA==")
                                    .put("templateSource", "BODY")
                                    .put("onFailureOption", "ROLLBACK")
                                    .put("resourceRegion", "ap-southeast-2")
                                    .put("templateURL", "")
                                    .put("snsTopic", "")
                                    .put("stackNameOrIdWithChangeSetName", "")
                                    .put("accessKey", "")
                                    .put("creationTimeout", "")
                                    .put("templateBody", "AWSTemplateFormatVersion: 2010-09-09\nResources:\n  MyPipelineAssetsDeploymentBucket:\n    Type: AWS::S3::Bucket\nOutputs:\n  MyPipelineAssetsDeploymentBucket:\n    Value: !Ref MyPipelineAssetsDeploymentBucket\n    Description: Assets deployment bucket")
                                    .put("sessionToken", "")
                                    .put("stackPolicyDuringUpdateSource", "URL")
                                    .put("awsCredentialsSource", "IFAWS_CONNECTOR")
                                    .put("awsConnectorIdVariable", "")
                                    .put("stackTags", "")
                                    .put("stackPolicySource", "URL")
                                    .put("awsConnectorId", "a2e0f3f0-4bf0-4adc-8dd6-d04249c431a0")
                                    .put("updateIfExists", "true")
                                    .build()),
                        new ScriptTask()
                            .description("[Publish] Zip assets and prepare CF params file")
                            .inlineBody("#!/bin/bash\nassets=$(cat manifest.json | jq '.artifacts.CdkBambooPipelineStack.metadata.\"/CdkBambooPipelineStack\" | map(select(.type | contains (\"aws:cdk:asset\")))')\nassetsCount=$(echo $assets | jq '. | length')\necho \"[\" >> params.properties\nfor (( c=0; c<assetsCount; c++ ))\ndo  \n   if [[ $c -gt 0 ]]\n   then\n    echo \",\" >> params.properties\n   fi \n   asset=$(echo $assets | jq --arg ind $c '.[$ind|tonumber]') \n   assetPath=$(echo $asset | jq '.data.path')\n   s3BucketParameter=$(echo $asset | jq '.data.s3BucketParameter')\n   s3KeyParameter=$(echo $asset | jq '.data.s3KeyParameter')\n   artifactHashParameter=$(echo $asset | jq '.data.artifactHashParameter')\n   sourceHash=$(echo $asset | jq '.data.sourceHash')\n   assetPath=$(sed -e 's/^\"//' -e 's/\"$//' <<<\"$assetPath\")\n   echo \"{\\\"ParameterKey\\\": ${s3BucketParameter}, \\\"ParameterValue\\\": \\\"${bamboo.custom.aws.cfn.stack.resources.bamboo-assets-stack.outputs.MyPipelineAssetsDeploymentBucket}\\\"},\" >> params.properties\n   echo \"{\\\"ParameterKey\\\": ${s3KeyParameter}, \\\"ParameterValue\\\": \\\"${assetPath}.zip||\\\"},\" >> params.properties\n   echo \"{\\\"ParameterKey\\\": ${artifactHashParameter}, \\\"ParameterValue\\\": ${sourceHash}},\" >> params.properties\n   cd ${assetPath}\n   zip -r \"${assetPath}.zip\" .\n   mv  \"${assetPath}.zip\" ..\n   cd ..\n   echo $assetPath\ndone\necho \"]\" >> params.properties\ncat params.properties\necho \"new line\"\ncat ${bamboo.build.working.directory}/params.properties"),
                        new AnyTask(new AtlassianModule("net.utoolity.atlassian.bamboo.tasks-for-aws:aws.s3.object"))
                            .description("[Publish] Upload assets")
                            .configuration(new MapBuilder()
                                    .put("useAntDefaultExcludes", "true")
                                    .put("useAntPattern", "true")
                                    .put("doNotFailIfNothingToCopy", "")
                                    .put("awsIamRoleAgentsArn", "")
                                    .put("presignMethod", "GET")
                                    .put("pluginVersionOnSave", "2.20.2")
                                    .put("sourceObjectKey", "")
                                    .put("presignBucketName", "")
                                    .put("resourceAction", "Upload")
                                    .put("pluginConfigVersionOnSave", "11")
                                    .put("targetBucketName", "${bamboo.custom.aws.cfn.stack.resources.bamboo-assets-stack.outputs.MyPipelineAssetsDeploymentBucket}")
                                    .put("doNotFailIfBucketDoesNotExist", "")
                                    .put("uploadAsZipArchive", "")
                                    .put("targetObjectKeyZip", "")
                                    .put("resourceRegionVariable", "")
                                    .put("presignObjectKey", "")
                                    .put("useRegexRename", "")
                                    .put("regexRenamePatternTo", "")
                                    .put("doNotFailIfNothingToUpload", "")
                                    .put("regexRenamePatternFrom", "")
                                    .put("tagsJson", "")
                                    .put("artifactToUpload", "-2:-1:-1:LOCAL_FILES")
                                    .put("sourceBucketName", "")
                                    .put("metadataConfigurationJson", "")
                                    .put("secretKey", "BAMSCRT@0@0@bG5KkO3j7/8pXd5215pVaA==")
                                    .put("sourceLocalPath", "*.zip")
                                    .put("resourceRegion", "ap-southeast-2")
                                    .put("doNotFailIfNothingToDelete", "true")
                                    .put("presignExpiration", "3600")
                                    .put("accessKey", "")
                                    .put("sessionToken", "")
                                    .put("doNotFailIfNothingToDownload", "")
                                    .put("awsCredentialsSource", "IFAWS_CONNECTOR")
                                    .put("awsConnectorIdVariable", "")
                                    .put("targetLocalPath", "")
                                    .put("awsConnectorId", "a2e0f3f0-4bf0-4adc-8dd6-d04249c431a0")
                                    .put("targetObjectKey", "")
                                    .build()),
                        new AnyTask(new AtlassianModule("net.utoolity.atlassian.bamboo.tasks-for-aws:aws.s3.object"))
                            .description("[Deploy] Upload CDK CloudFormation template")
                            .configuration(new MapBuilder()
                                    .put("useAntDefaultExcludes", "true")
                                    .put("useAntPattern", "")
                                    .put("doNotFailIfNothingToCopy", "")
                                    .put("awsIamRoleAgentsArn", "")
                                    .put("presignMethod", "GET")
                                    .put("pluginVersionOnSave", "2.20.2")
                                    .put("sourceObjectKey", "")
                                    .put("presignBucketName", "")
                                    .put("resourceAction", "Upload")
                                    .put("pluginConfigVersionOnSave", "11")
                                    .put("targetBucketName", "${bamboo.custom.aws.cfn.stack.resources.bamboo-assets-stack.outputs.MyPipelineAssetsDeploymentBucket}")
                                    .put("doNotFailIfBucketDoesNotExist", "")
                                    .put("uploadAsZipArchive", "")
                                    .put("targetObjectKeyZip", "")
                                    .put("resourceRegionVariable", "")
                                    .put("presignObjectKey", "")
                                    .put("useRegexRename", "")
                                    .put("regexRenamePatternTo", "")
                                    .put("doNotFailIfNothingToUpload", "")
                                    .put("regexRenamePatternFrom", "")
                                    .put("tagsJson", "")
                                    .put("artifactToUpload", "-2:-1:-1:LOCAL_FILES")
                                    .put("sourceBucketName", "")
                                    .put("metadataConfigurationJson", "")
                                    .put("secretKey", "BAMSCRT@0@0@bG5KkO3j7/8pXd5215pVaA==")
                                    .put("sourceLocalPath", "CdkBambooPipelineStack.template.json")
                                    .put("resourceRegion", "ap-southeast-2")
                                    .put("doNotFailIfNothingToDelete", "true")
                                    .put("presignExpiration", "3600")
                                    .put("accessKey", "AKIA6RF3PCENLYXV27UV")
                                    .put("sessionToken", "")
                                    .put("doNotFailIfNothingToDownload", "")
                                    .put("awsCredentialsSource", "IFAWS_CONNECTOR")
                                    .put("awsConnectorIdVariable", "")
                                    .put("targetLocalPath", "")
                                    .put("awsConnectorId", "a2e0f3f0-4bf0-4adc-8dd6-d04249c431a0")
                                    .put("targetObjectKey", "")
                                    .build()),
                        new AnyTask(new AtlassianModule("net.utoolity.atlassian.bamboo.tasks-for-aws:aws.s3.object"))
                            .description("[Deploy] Generate pre-signed URL")
                            .configuration(new MapBuilder()
                                    .put("useAntDefaultExcludes", "true")
                                    .put("useAntPattern", "false")
                                    .put("doNotFailIfNothingToCopy", "")
                                    .put("awsIamRoleAgentsArn", "")
                                    .put("presignMethod", "GET")
                                    .put("pluginVersionOnSave", "2.20.2")
                                    .put("sourceObjectKey", "")
                                    .put("presignBucketName", "${bamboo.custom.aws.cfn.stack.resources.bamboo-assets-stack.outputs.MyPipelineAssetsDeploymentBucket}")
                                    .put("resourceAction", "Presign")
                                    .put("pluginConfigVersionOnSave", "11")
                                    .put("targetBucketName", "")
                                    .put("doNotFailIfBucketDoesNotExist", "")
                                    .put("uploadAsZipArchive", "")
                                    .put("targetObjectKeyZip", "")
                                    .put("resourceRegionVariable", "")
                                    .put("presignObjectKey", "CdkBambooPipelineStack.template.json")
                                    .put("useRegexRename", "false")
                                    .put("regexRenamePatternTo", "")
                                    .put("doNotFailIfNothingToUpload", "")
                                    .put("regexRenamePatternFrom", "")
                                    .put("tagsJson", "")
                                    .put("artifactToUpload", "-1:2:0:SAM-MAIN: all artifacts")
                                    .put("sourceBucketName", "")
                                    .put("metadataConfigurationJson", "")
                                    .put("secretKey", "BAMSCRT@0@0@bG5KkO3j7/8pXd5215pVaA==")
                                    .put("sourceLocalPath", "")
                                    .put("resourceRegion", "ap-southeast-2")
                                    .put("doNotFailIfNothingToDelete", "true")
                                    .put("presignExpiration", "3600")
                                    .put("accessKey", "")
                                    .put("sessionToken", "")
                                    .put("doNotFailIfNothingToDownload", "")
                                    .put("awsCredentialsSource", "IFAWS_CONNECTOR")
                                    .put("awsConnectorIdVariable", "")
                                    .put("targetLocalPath", "")
                                    .put("awsConnectorId", "a2e0f3f0-4bf0-4adc-8dd6-d04249c431a0")
                                    .put("targetObjectKey", "")
                                    .build()),
                        new AnyTask(new AtlassianModule("net.utoolity.atlassian.bamboo.tasks-for-aws:aws.cloudformation.stack"))
                            .description("[Deploy] Deploy CDK CloudFormation template(s)")
                            .configuration(new MapBuilder()
                                    .put("changeSetDescription", "")
                                    .put("stackPolicyURL", "")
                                    .put("stackPolicyDuringUpdateURL", "")
                                    .put("ignoreNoOpUpdateExceptionUpdate", "true")
                                    .put("awsIamRoleAgentsArn", "")
                                    .put("ignoreNoopCreateChangeSetFailure", "")
                                    .put("stackName", "bamboo-cdk-sample-app")
                                    .put("pluginVersionOnSave", "2.20.2")
                                    .put("enableIAM", "")
                                    .put("ignoreNoOpUpdateExceptionCreate", "true")
                                    .put("changeSetName", "")
                                    .put("resourceAction", "Create")
                                    .put("pluginConfigVersionOnSave", "11")
                                    .put("templateParameters", "")
                                    .put("roleArn", "")
                                    .put("resourceRegionVariable", "")
                                    .put("stackPolicyBody", "")
                                    .put("changeSetType", "CREATE_OR_UPDATE")
                                    .put("doNotFailIfNotExists", "")
                                    .put("templateParametersJson", "file://${bamboo.build.working.directory}/params.properties")
                                    .put("createIfNotExists", "")
                                    .put("stackNameOrId", "")
                                    .put("stackPolicyDuringUpdateBody", "")
                                    .put("changeSetNameOrArn", "")
                                    .put("capabilities", "[\"CAPABILITY_IAM\"]")
                                    .put("secretKey", "BAMSCRT@0@0@bG5KkO3j7/8pXd5215pVaA==")
                                    .put("templateSource", "URL")
                                    .put("onFailureOption", "ROLLBACK")
                                    .put("resourceRegion", "ap-southeast-2")
                                    .put("templateURL", "${bamboo.custom.aws.s3.object.first.PresignedUrl.password}")
                                    .put("snsTopic", "")
                                    .put("stackNameOrIdWithChangeSetName", "")
                                    .put("accessKey", "AKIA6RF3PCENLYXV27UV")
                                    .put("creationTimeout", "")
                                    .put("templateBody", "")
                                    .put("sessionToken", "")
                                    .put("stackPolicyDuringUpdateSource", "URL")
                                    .put("awsCredentialsSource", "IFAWS_CONNECTOR")
                                    .put("awsConnectorIdVariable", "")
                                    .put("stackTags", "")
                                    .put("stackPolicySource", "URL")
                                    .put("awsConnectorId", "a2e0f3f0-4bf0-4adc-8dd6-d04249c431a0")
                                    .put("updateIfExists", "true")
                                    .build())));
        return rootObject;
    }
    
    public DeploymentPermissions deploymentPermission() {
        final DeploymentPermissions deploymentPermission = new DeploymentPermissions("deployment-plan")
            .permissions(new Permissions()
                    .userPermissions("mosmabro", PermissionType.EDIT, PermissionType.VIEW)
                    .loggedInUserPermissions(PermissionType.VIEW)
                    .anonymousUserPermissionView());
        return deploymentPermission;
    }
    
    public EnvironmentPermissions environmentPermission1() {
        final EnvironmentPermissions environmentPermission1 = new EnvironmentPermissions("deployment-plan")
            .environmentName("dev")
            .permissions(new Permissions()
                    .userPermissions("mosmabro", PermissionType.EDIT, PermissionType.VIEW, PermissionType.BUILD)
                    .loggedInUserPermissions(PermissionType.VIEW)
                    .anonymousUserPermissionView());
        return environmentPermission1;
    }
    
    public static void main(String... argv) {
        //By default credentials are read from the '.credentials' file.
        BambooServer bambooServer = new BambooServer("http://192.168.1.12:8085");
        final PlanSpec planSpec = new PlanSpec();
        
        final Deployment rootObject = planSpec.rootObject();
        bambooServer.publish(rootObject);
        
        final DeploymentPermissions deploymentPermission = planSpec.deploymentPermission();
        bambooServer.publish(deploymentPermission);
        
        final EnvironmentPermissions environmentPermission1 = planSpec.environmentPermission1();
        bambooServer.publish(environmentPermission1);
    }
}